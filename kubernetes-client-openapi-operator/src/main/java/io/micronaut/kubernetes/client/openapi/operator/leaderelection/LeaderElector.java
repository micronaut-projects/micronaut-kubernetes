/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.kubernetes.client.openapi.operator.leaderelection;

import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.kubernetes.client.openapi.operator.configuration.LeaderElectionConfiguration;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.event.LeaderChangedEvent;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.event.LeaseAcquiredEvent;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.event.LeaseLostEvent;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.resourcelock.Lock;
import io.micronaut.kubernetes.client.openapi.util.ThreadFactoryUtil;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs the leader election. It publishes the following events:
 * <ul>
 *     <li>LeaseAcquiredEvent - when this instance has acquired the lease</li>
 *     <li>LeaseLostEvent - when this instance has lost the lease</li>
 *     <li>LeaderChangedEvent - when the leader has changed</li>
 * </ul>
 */
@Singleton
final class LeaderElector {
    private static final Logger LOG = LoggerFactory.getLogger(LeaderElector.class);

    private static final double JITTER_FACTOR = 1.2;

    private final LeaderElectionConfiguration leaderElectionConfiguration;
    private final Lock lock;
    private final ApplicationEventPublisher<LeaseAcquiredEvent> leaseAcquiredEventPublisher;
    private final ApplicationEventPublisher<LeaseLostEvent> leaseLostEventPublisher;
    private final ApplicationEventPublisher<LeaderChangedEvent> leaderChangedEventPublisher;
    private final ScheduledExecutorService scheduledWorkers;
    private final ExecutorService leaseWorkers;

    private LeaderElectionRecord observedRecord;
    private long observedTimeMilliSeconds;

    // used to implement OnNewLeader(), may lag slightly from the
    // value observedRecord.HolderIdentity if the transition has not yet been reported.
    private String reportedLeader;

    private final AtomicBoolean active = new AtomicBoolean(false);

    LeaderElector(LeaderElectionConfiguration leaderElectionConfiguration,
                  Lock lock,
                  ApplicationEventPublisher<LeaseAcquiredEvent> leaseAcquiredEventPublisher,
                  ApplicationEventPublisher<LeaseLostEvent> leaseLostEventPublisher,
                  ApplicationEventPublisher<LeaderChangedEvent> leaderChangedEventPublisher,
                  ThreadFactoryUtil threadFactoryUtil) {
        this.leaderElectionConfiguration = leaderElectionConfiguration;
        this.lock = lock;
        this.leaseAcquiredEventPublisher = leaseAcquiredEventPublisher;
        this.leaseLostEventPublisher = leaseLostEventPublisher;
        this.leaderChangedEventPublisher = leaderChangedEventPublisher;
        leaseWorkers = Executors.newSingleThreadExecutor(threadFactoryUtil.getNamedThreadFactory("leader-elector-lease-worker-%d"));
        scheduledWorkers = Executors.newSingleThreadScheduledExecutor(threadFactoryUtil.getNamedThreadFactory("leader-elector-scheduled-worker-%d"));

        List<String> errors = new ArrayList<>();
        if (leaderElectionConfiguration.getLeaseDuration().compareTo(leaderElectionConfiguration.getRenewDeadline()) <= 0) {
            errors.add("LeaseDuration must be greater than renewDeadline");
        }
        if (leaderElectionConfiguration.getRenewDeadline().compareTo(leaderElectionConfiguration.getRetryPeriod()) <= 0) {
            errors.add("RenewDeadline must be greater than retryPeriod");
        }
        if (leaderElectionConfiguration.getLeaseDuration().isZero() || leaderElectionConfiguration.getLeaseDuration().isNegative()) {
            errors.add("LeaseDuration must be greater than zero");
        }
        if (leaderElectionConfiguration.getRenewDeadline().isZero() || leaderElectionConfiguration.getRenewDeadline().isNegative()) {
            errors.add("RenewDeadline must be greater than zero");
        }
        if (leaderElectionConfiguration.getRetryPeriod().isZero() || leaderElectionConfiguration.getRetryPeriod().isNegative()) {
            errors.add("RetryPeriod must be greater than zero");
        }
        if (errors.size() > 0) {
            throw new IllegalArgumentException(String.join(",", errors));
        }
    }

    /**
     * Runs the leader election by entering an acquisition loop and trying to get a lease of
     * the lock object set in configuration. When the lease is successfully acquired, the leader elector
     * enters a renewal loop where it continuously renews the lease following the provided configuration.
     */
    void run() {
        LOG.info("Start leader election with lock {}", lock);
        active.set(true);
        while (active.get()) {
            try {
                if (!acquire()) {
                    // Fail to acquire leadership
                    return;
                }
                LOG.info("Successfully acquired lease, became leader");
                leaseAcquiredEventPublisher.publishEventAsync(new LeaseAcquiredEvent(observedRecord));
                renewLoop();
                LOG.info("Failed to renew lease, lose leadership");
            } catch (Throwable t) {
                LOG.error("Leader election failure", t);
            } finally {
                // if shutdown initiated, the lease lost event will be sent by the stop method
                if (active.get()) {
                    leaseLostEventPublisher.publishEvent(new LeaseLostEvent(observedRecord));
                }
            }
        }
    }

    private boolean acquire() {
        LOG.debug("Attempting to acquire leader lease");
        long retryPeriodMillis = leaderElectionConfiguration.getRetryPeriod().toMillis();
        AtomicBoolean acquired = new AtomicBoolean(false);

        ScheduledFuture scheduledFuture = scheduledWorkers.scheduleWithFixedDelay(
            () -> {
                Future<Boolean> future = leaseWorkers.submit(this::tryAcquire);
                try {
                    Boolean success = future.get(retryPeriodMillis, TimeUnit.MILLISECONDS);
                    LOG.debug("Lease {} acquired", success ? "successfully" : "not");
                    acquired.set(success);
                } catch (CancellationException e) {
                    LOG.info("Processing of tryAcquire successfully canceled");
                } catch (Throwable t) {
                    LOG.error("Unexpected error on acquiring the lease", t);
                    future.cancel(true); // make sure acquire work doesn't overlap
                } finally {
                    maybeReportTransition();
                }
            },
            0,
            Double.valueOf(retryPeriodMillis * (JITTER_FACTOR * Math.random() + 1)).longValue(),
            TimeUnit.MILLISECONDS);

        try {
            while (!acquired.get()) {
                Thread.sleep(retryPeriodMillis);
            }
        } catch (InterruptedException e) {
            LOG.error("LeaderElection acquire loop gets interrupted", e);
            return false;
        } finally {
            scheduledFuture.cancel(true);
        }
        return true;
    }

    private void renewLoop() {
        LOG.debug("Attempting to renew leader lease");
        long retryPeriodMillis = leaderElectionConfiguration.getRetryPeriod().toMillis();
        long renewDeadlineMillis = leaderElectionConfiguration.getRenewDeadline().toMillis();

        try {
            boolean renewResult = true;
            while (active.get() && renewResult) {
                final Future<Boolean> future = leaseWorkers.submit(
                    () -> {
                        try {
                            // retry until success or interrupted
                            while (!tryRenew()) {
                                Thread.sleep(retryPeriodMillis);
                                maybeReportTransition();
                            }
                            return true;
                        } catch (InterruptedException e) {
                            return false;
                        }
                    });

                try {
                    renewResult = future.get(renewDeadlineMillis, TimeUnit.MILLISECONDS);
                } catch (ExecutionException | TimeoutException t) {
                    LOG.debug("Failed to renew lease", t);
                    renewResult = false;
                } catch (Throwable t) {
                    LOG.error("Unexpected exception when renewing lease in the background", t);
                    renewResult = false;
                } finally {
                    future.cancel(true); // make the lease worker doesn't overlap
                }
                LOG.debug("Lease {} renewed", renewResult ? "successfully" : "not");
                if (renewResult) {
                    Thread.sleep(retryPeriodMillis);
                }
            }
        } catch (Exception e) {
            LOG.error("LeaderElection renew loop exception", e);
        }
    }

    private boolean tryAcquire() {
        LOG.debug("Trying to acquire lease");
        boolean acquired = tryAcquireOrRenew();
        LOG.debug("Lease {} acquired", acquired ? "successfully" : "not");
        return acquired;
    }

    private boolean tryRenew() {
        LOG.debug("Trying to renew lease");
        boolean renewed = tryAcquireOrRenew();
        LOG.debug("Lease {} renewed", renewed ? "successfully" : "not");
        return renewed;
    }

    private boolean tryAcquireOrRenew() {
        Date now = new Date();

        // 1. obtain or create the ElectionRecord
        LeaderElectionRecord oldLeaderElectionRecord;
        try {
            oldLeaderElectionRecord = lock.get();
        } catch (Exception e) {
            LOG.error("Error retrieving resource lock {}", lock, e);
            return false;
        }

        if (oldLeaderElectionRecord == null) {
            LOG.debug("Lock not found, try to create it");
            // No Lock resource exists, try to get leadership by creating it
            return createLock(lock, createLeaderElectionRecord(now, now, 0));
        }

        // alright, we have an existing lock resource
        // 1. Is Lock Empty? --> try to get leadership by updating it
        // 2. Am I the Leader? --> update info and renew lease by updating it
        // 3. I am not the Leader?
        // 3.1 is Lock expired? --> try to get leadership by updating it
        // 3.2 Lock not expired? --> update info, try later

        if (oldLeaderElectionRecord.acquireTime() == null
            || oldLeaderElectionRecord.renewTime() == null
            || oldLeaderElectionRecord.holderIdentity() == null) {
            // We found the lock resource with an empty LeaderElectionRecord, try to get leadership by updating it
            return updateLock(lock, createLeaderElectionRecord(now, now, oldLeaderElectionRecord.leaderTransitions() + 1));
        }

        // 2. Record obtained with LeaderElectionRecord, check the Identity & Time
        if (!oldLeaderElectionRecord.equals(observedRecord)) {
            observedRecord = oldLeaderElectionRecord;
            observedTimeMilliSeconds = System.currentTimeMillis();
        }

        if (observedTimeMilliSeconds + leaderElectionConfiguration.getLeaseDuration().toMillis() > now.getTime()
            && !isLeader()) {
            LOG.debug("Lock is held by {} and has not yet expired", oldLeaderElectionRecord.holderIdentity());
            return false;
        }

        // 3. We're going to try to update. The leaderElectionRecord is set to it's default
        // here. Let's correct it before updating.
        LeaderElectionRecord leaderElectionRecord;
        if (isLeader()) {
            leaderElectionRecord = createLeaderElectionRecord(oldLeaderElectionRecord.acquireTime(), now, oldLeaderElectionRecord.leaderTransitions());
        } else {
            leaderElectionRecord = createLeaderElectionRecord(now, now, oldLeaderElectionRecord.leaderTransitions() + 1);
        }
        return updateLock(lock, leaderElectionRecord);
    }

    private LeaderElectionRecord createLeaderElectionRecord(Date acquireTime, Date renewTime, int leaderTransitions) {
        return new LeaderElectionRecord(
            lock.getIdentity(),
            Long.valueOf(leaderElectionConfiguration.getLeaseDuration().getSeconds()).intValue(),
            acquireTime,
            renewTime,
            leaderTransitions);
    }

    private boolean createLock(Lock lock, LeaderElectionRecord leaderElectionRecord) {
        LOG.debug("Creating resource lock to get lease");
        boolean createSuccess = lock.create(leaderElectionRecord);
        if (!createSuccess) {
            return false;
        }
        observedRecord = leaderElectionRecord;
        observedTimeMilliSeconds = System.currentTimeMillis();
        return true;
    }

    private boolean updateLock(Lock lock, LeaderElectionRecord leaderElectionRecord) {
        LOG.debug("Updating resource lock to get lease");
        boolean updateSuccess = lock.update(leaderElectionRecord);
        if (!updateSuccess) {
            return false;
        }
        observedRecord = leaderElectionRecord;
        observedTimeMilliSeconds = System.currentTimeMillis();
        return true;
    }

    private boolean isLeader() {
        return lock.getIdentity().equals(observedRecord.holderIdentity());
    }

    private void maybeReportTransition() {
        if (observedRecord == null) {
            return;
        }
        if (observedRecord.holderIdentity().equals(reportedLeader)) {
            return;
        }
        reportedLeader = observedRecord.holderIdentity();
        LOG.info("LeaderElection lock is currently held by {}", reportedLeader);
        leaderChangedEventPublisher.publishEvent(new LeaderChangedEvent(observedRecord));
    }

    void stop() {
        LOG.info("Stopping the leader elector");
        active.set(false);
        scheduledWorkers.shutdown();
        leaseWorkers.shutdown();

        // Ensure that all executors have stopped
        try {
            boolean isTerminated = scheduledWorkers.awaitTermination(leaderElectionConfiguration.getRetryPeriod().getSeconds(), TimeUnit.SECONDS);
            if (!isTerminated) {
                LOG.warn("Timed out waiting to terminate scheduledWorkers");
                scheduledWorkers.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOG.warn("Failed to ensure scheduledWorkers termination", e);
            scheduledWorkers.shutdownNow();
        }

        try {
            boolean isTerminated = leaseWorkers.awaitTermination(leaderElectionConfiguration.getRetryPeriod().getSeconds(), TimeUnit.SECONDS);
            if (!isTerminated) {
                LOG.warn("Timed out waiting to terminate leaseWorkers");
                leaseWorkers.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOG.warn("Failed to ensure leaseWorkers termination", e);
            leaseWorkers.shutdownNow();
        }

        // If I am the leader, free the lock so that other candidates can take it immediately
        if (observedRecord != null && isLeader()) {
            LOG.info("Giving up the lock");
            LeaderElectionRecord emptyRecord = new LeaderElectionRecord(
                null,
                // LeaseLock impl requires a non-zero value for leaseDuration
                Long.valueOf(leaderElectionConfiguration.getLeaseDuration().getSeconds()).intValue(),
                null,
                null,
                // maintain leaderTransitions count
                observedRecord.leaderTransitions());

            boolean status = lock.update(emptyRecord);
            if (!status) {
                LOG.warn("Failed to give up the lock.");
            }
            leaseLostEventPublisher.publishEvent(new LeaseLostEvent(observedRecord));
        }
        LOG.info("Closed");
    }
}
