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
package io.micronaut.kubernetes.client.openapi.operator.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Reconciler;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Result;
import io.micronaut.kubernetes.client.openapi.operator.workqueue.RateLimitingQueue;
import io.micronaut.kubernetes.client.openapi.util.ThreadFactoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private final MeterRegistry meterRegistry;

    private final int workerCount;
    private final ScheduledExecutorService workerThreadPool;

    DefaultController(String name,
                      Reconciler reconciler,
                      RateLimitingQueue<Request> workQueue,
                      int workerCount,
                      ThreadFactoryUtil threadFactoryUtil,
                      MeterRegistry meterRegistry) {
        this.name = name;
        this.reconciler = reconciler;
        this.workQueue = workQueue;
        this.meterRegistry = meterRegistry;
        this.workerCount = workerCount;
        workerThreadPool = Executors.newScheduledThreadPool(
            workerCount,
            threadFactoryUtil.getNamedThreadFactory(name + "-controller-%d"));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void run() {
        for (int i = 0; i < workerCount; i++) {
            final int workerIndex = i;
            workerThreadPool.scheduleWithFixedDelay(
                () -> {
                    LOG.debug("Starting controller {} worker {}", name, workerIndex);
                    try {
                        worker();
                    } catch (Throwable t) {
                        LOG.error("Unexpected loop abortion, controller {} worker {} ", name, workerIndex, t);
                    } finally {
                        LOG.debug("Exiting controller {} worker {}", name, workerIndex);
                    }
                },
                0,
                1,
                TimeUnit.SECONDS);
        }
    }

    @Override
    public void shutdown() {
        LOG.info("Shutting down work queue and workers");
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
                LOG.info("Controller {} worker exiting because work-queue has shutdown", name);
                return;
            }
            LOG.debug("Controller {} start reconciling {}", name, request);

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
                        LOG.debug("Controller {} reconciling {} failed, requeuing", name, request);
                        workQueue.addRateLimited(request);
                    } else {
                        LOG.debug("Controller {} reconciling {} failed, requeuing after {}", name, request, result.getRequeueAfter());
                        workQueue.addAfter(request, result.getRequeueAfter());
                    }
                } else {
                    workQueue.forget(request);
                }
            } finally {
                workQueue.done(request);
                meterRegistry.gauge("controller_work_queue_length", Tags.of("name", name), workQueue.length());
                LOG.debug("Controller {} finished reconciling {}", name, request);
            }
        }
    }
}
