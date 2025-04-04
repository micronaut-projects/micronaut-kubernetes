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
package io.micronaut.kubernetes.client.openapi.operator.workqueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * The default delaying queue implementation.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/workqueue/DefaultDelayingQueue.java">DefaultDelayingQueue</a>
 * </p>
 *
 * @param <T> item type
 */
@SuppressWarnings("java:S899")
public class DefaultDelayingQueue<T> extends DefaultWorkQueue<T> implements DelayingQueue<T> {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDelayingQueue.class);

    private static final Duration HEART_BEAT_INTERVAL = Duration.ofSeconds(10);

    private final DelayQueue<WaitForEntry<T>> delayQueue = new DelayQueue<>();
    private final ConcurrentMap<T, WaitForEntry<T>> waitingEntryByData = new ConcurrentHashMap<>();
    private final BlockingQueue<WaitForEntry<T>> newEntryQueue = new LinkedBlockingQueue<>(1000);
    private final Supplier<Long> timeSource = () -> System.nanoTime() / 1000000;
    private final ExecutorService waitingWorker;

    private Future<?> waitingFuture;

    public DefaultDelayingQueue(ExecutorService waitingWorker) {
        this.waitingWorker = waitingWorker;
    }

    @Override
    public synchronized void start() {
        clearDelayingQueues();
        super.start();
        waitingFuture = waitingWorker.submit(this::waitingLoop);
    }

    @Override
    public synchronized void shutdown() {
        super.shutdown();
        waitingFuture.cancel(true);
        clearDelayingQueues();
    }

    private void clearDelayingQueues() {
        delayQueue.clear();
        waitingEntryByData.clear();
        newEntryQueue.clear();
    }

    /**
     * Adds given item to the queue after given duration expires.
     *
     * @param item     item to add
     * @param duration specific duration
     */
    @Override
    public void addAfter(T item, Duration duration) {
        if (super.isShutdown()) {
            return;
        }

        // immediately add things w/o delay
        if (duration.isZero()) {
            super.add(item);
            return;
        }

        boolean queued = newEntryQueue.offer(new WaitForEntry<>(item, timeSource.get() + duration.toMillis(), timeSource));
        if (!queued) {
            LOG.error("Item has not been added to the delaying queue, item={}", item);
        }
    }

    private void waitingLoop() {
        while (true) {
            // underlying work-queue is shutting down, quit the loop.
            if (super.isShutdown()) {
                return;
            }

            Duration nextReadyAt = HEART_BEAT_INTERVAL;

            // peek the head item from the delay queue
            WaitForEntry<T> entry = delayQueue.peek();
            if (entry != null) {
                // check whether the head item is ready
                long delay = entry.getDelay(TimeUnit.MILLISECONDS);
                if (delay <= 0) {
                    // the item is ready so remove it from the delay-queue and push it into underlying work-queue
                    delayQueue.remove(entry);
                    waitingEntryByData.remove(entry.data);
                    super.add(entry.data);
                    continue;
                }
                // refresh the next ready-at time
                nextReadyAt = Duration.ofMillis(delay);
            }

            // wait on a new entry until the head item of delay queue is ready or
            // heart beat interval expires if delay queue is empty
            WaitForEntry<T> newEntry = null;
            try {
                newEntry = newEntryQueue.poll(nextReadyAt.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOG.debug("The thread has been interrupted while waiting on new entry", e);
                Thread.currentThread().interrupt();
            }

            if (newEntry != null) {
                if (timeSource.get() < newEntry.readyAtMillis) {
                    // the item is not yet ready, insert it to the delay-queue
                    delay(newEntry);
                } else {
                    // the item is ready as soon as received, fire it to the work-queue directly
                    super.add(newEntry.data);
                }
            }
        }
    }

    private void delay(WaitForEntry<T> entry) {
        WaitForEntry<T> existing = waitingEntryByData.get(entry.data);
        if (existing == null) {
            delayQueue.offer(entry);
            waitingEntryByData.put(entry.data, entry);
            return;
        }
        // if the new entry will be ready before the existing one, modify its position in the delay queue
        if (entry.readyAtMillis < existing.readyAtMillis) {
            delayQueue.remove(existing);
            existing.readyAtMillis = entry.readyAtMillis;
            delayQueue.add(existing);
        }
    }

    // WaitForEntry holds the data to add and the time it should be added.
    private static final class WaitForEntry<T> implements Delayed {
        private final T data;
        private long readyAtMillis;
        private final Supplier<Long> timeSource;

        private WaitForEntry(T data, long readyAtMillis, Supplier<Long> timeSource) {
            this.data = data;
            this.readyAtMillis = readyAtMillis;
            this.timeSource = timeSource;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long duration = readyAtMillis - timeSource.get();
            return unit.convert(duration, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
        }
    }
}
