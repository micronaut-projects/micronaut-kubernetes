package io.micronaut.kubernetes.client.openapi.operator.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Reconciler;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Result;
import io.micronaut.kubernetes.client.openapi.operator.workqueue.RateLimitingQueue;
import io.micronaut.kubernetes.client.openapi.util.ThreadFactoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * The default implementation of a controller.
 *
 * <p>
 * A typical controller contains:
 * <ul>
 *     <li>a reconciler implemented by developers specifying what to do in reaction of notifications</li>
 *     <li>a work-queue continuously filled with task items managed by the Informer framework</li>
 *     <li>a set of worker threads actually running reconciler</li>
 * </ul>
 * </p>
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/controller/DefaultController.java">DefaultController</a>
 * </p>
 */
final class DefaultController implements Controller {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultController.class);

    private final Reconciler reconciler;
    private final String name;
    private final RateLimitingQueue<Request> workQueue;
    private final List<Supplier<Boolean>> readyFuncs;
    private final MeterRegistry meterRegistry;

    private final int workerCount;
    private final ScheduledExecutorService workerThreadPool;

    private final Duration readyTimeout;
    private final Duration readyCheckInternal;

    DefaultController(String name,
                      Reconciler reconciler,
                      RateLimitingQueue<Request> workQueue,
                      int workerCount,
                      ThreadFactoryUtil threadFactoryUtil,
                      MeterRegistry meterRegistry,
                      List<Supplier<Boolean>> readyFuncs,
                      Duration readyTimeout,
                      Duration readyCheckInternal) {
        this.name = name;
        this.reconciler = reconciler;
        this.workQueue = workQueue;
        this.meterRegistry = meterRegistry;
        this.readyFuncs = readyFuncs;
        this.readyTimeout = readyTimeout;
        this.readyCheckInternal = readyCheckInternal;
        this.workerCount = workerCount;
        workerThreadPool = Executors.newScheduledThreadPool(
            workerCount,
            threadFactoryUtil.getNamedThreadFactory(name + "-controller-%d"));
    }

    @Override
    public String getName() {
        return name;
    }

    private boolean preFlightCheck() {
        if (workerCount <= 0) {
            LOG.error("Fail to start controller {}: worker count must be positive.", name);
            return false;
        }
        if (workerThreadPool == null) {
            LOG.error("Fail to start controller {}: missing worker thread-pool.", name);
            return false;
        }
        if (!isReady()) {
            LOG.error("Fail to start controller {}: Timed out waiting for cache to be synced.", name);
            return false;
        }
        return true;
    }

    private boolean isReady() {
        if (CollectionUtils.isEmpty(readyFuncs)) {
            return true;
        }

        long waitLimit = System.currentTimeMillis() + readyTimeout.toMillis();
        while (waitLimit > System.currentTimeMillis()) {
            boolean ready = true;
            for (Supplier<Boolean> readyFunc : readyFuncs) {
                if (!readyFunc.get()) {
                    ready = false;
                    break;
                }
            }
            if (ready) {
                return true;
            }
            try {
                Thread.sleep(readyCheckInternal.toMillis());
            } catch (InterruptedException e) {
                LOG.warn("The thread has been interrupted while waiting for ready func", e);
                Thread.currentThread().interrupt();
                break;
            }
        }
        return false;
    }

    @Override
    public void run() {
        if (!preFlightCheck()) {
            LOG.error("Controller {} failed pre-run check, exiting..", name);
            return;
        }

        // spawns worker threads for the controller.
        CountDownLatch latch = new CountDownLatch(workerCount);
        for (int i = 0; i < workerCount; i++) {
            final int workerIndex = i;
            workerThreadPool.scheduleWithFixedDelay(
                () -> {
                    LOG.debug("Starting controller {} worker {}..", name, workerIndex);
                    try {
                        worker();
                    } catch (Throwable t) {
                        LOG.error("Unexpected loop abortion, controller {} worker {} ", name, workerIndex, t);
                    } finally {
                        latch.countDown();
                        LOG.debug("Exiting controller {} worker {}..", name, workerIndex);
                    }
                },
                0,
                1,
                TimeUnit.SECONDS);
        }
        try {
            LOG.debug("Controller {} bootstrapping..", name);
            latch.await();
        } catch (InterruptedException e) {
            LOG.error("Aborting {} controller", name, e);
        } finally {
            LOG.info("Controller {} exited", name);
        }
    }

    @Override
    public void shutdown() {
        // shutdown work-queue before the thread-pool
        workQueue.shutdown();
        workerThreadPool.shutdown();
    }

    private void worker() {
        // taking tasks from work-queue in a loop
        while (!workQueue.isShutdown()) {
            meterRegistry.gauge("controller_work_queue_length", Tags.of("name", name), workQueue.length());

            Request request = null;
            try {
                request = workQueue.get();
            } catch (InterruptedException e) {
                // we're reaching here mostly because of forcibly shutting down the controller.
                LOG.error("Controller worker interrupted.. keeps working until work-queue shutdown", e);
            }
            // request is expected to be null, when the work-queue is shutting-down.
            if (request == null) {
                LOG.info("Controller {} worker exiting because work-queue has shutdown..", name);
                return;
            }
            LOG.debug("Controller {} start reconciling {}..", name, request);

            Result result;
            try {
                // do reconciliation, invoke user customized logic
                result = reconciler.reconcile(request);
            } catch (Throwable t) {
                LOG.error("Reconciler aborted unexpectedly", t);
                result = new Result(true);
            }

            meterRegistry.counter("controller_reconcile_count_total", "name", name, "requeue", Boolean.toString(result.isRequeue())).increment();

            try {
                // checks whether do a re-queue (on failure)
                if (result.isRequeue()) {
                    if (result.getRequeueAfter() == null) {
                        LOG.debug("Controller {} reconciling {} failed, requeuing ..", name, request);
                        workQueue.addRateLimited(request);
                    } else {
                        LOG.debug("Controller {} reconciling {} failed, requeuing after {}..", name, request, result.getRequeueAfter());
                        workQueue.addAfter(request, result.getRequeueAfter());
                    }
                } else {
                    workQueue.forget(request);
                }
            } finally {
                workQueue.done(request);
                meterRegistry.gauge("controller_work_queue_length", Tags.of("name", name), workQueue.length());
                LOG.debug("Controller {} finished reconciling {}..", name, request);
            }
        }
    }
}
