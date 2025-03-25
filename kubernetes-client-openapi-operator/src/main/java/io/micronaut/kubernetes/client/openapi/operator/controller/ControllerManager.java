package io.micronaut.kubernetes.client.openapi.operator.controller;

import io.micronaut.kubernetes.client.openapi.operator.leaderelection.event.LeaseAcquiredEvent;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.event.LeaseLostEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The type Controller manager manages a set of controllers' lifecycle and also their informer
 * factory.
 *
 * <p>
 * The code has been copied from the official client and modified:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/extended/src/main/java/io/kubernetes/client/extended/controller/ControllerManager.java">ControllerManager</a>
 * </p>
 */
@Singleton
public final class ControllerManager {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerManager.class);

    private final Map<String, Controller> controllers = new ConcurrentHashMap<>();

    private ExecutorService controllerThreadPool;

    public void addController(Controller controller) {
        String name = controller.getName();
        if (controllers.containsKey(name)) {
            throw new IllegalStateException("Controller with name '" + name + "' has been already created");
        }
        controllers.put(name, controller);
    }

    @EventListener
    void startControllers(LeaseAcquiredEvent leaseAcquiredEvent) {
        LOG.info("Lease acquired, starting controllers");
        startControllers();
    }

    @EventListener
    void stopControllers(LeaseLostEvent leaseLostEvent) {
        LOG.info("Lease lost, shutting down controllers");
        stopControllers();
    }

    private void startControllers() {
        if (controllers.isEmpty()) {
            LOG.debug("There are no controllers registered in the manager");
            return;
        }
        CountDownLatch latch = new CountDownLatch(controllers.size());
        controllerThreadPool = Executors.newFixedThreadPool(controllers.size());
        for (Controller controller : controllers.values()) {
            controllerThreadPool.submit(
                () -> {
                    try {
                        LOG.debug("Starting controller manager");
                        controller.run();
                    } catch (Throwable t) {
                        LOG.error("Unexpected controller termination", t);
                    } finally {
                        latch.countDown();
                        LOG.debug("Exiting controller manager");
                    }
                });
        }
        try {
            LOG.debug("Controller-Manager bootstrapping.");
            latch.await();
        } catch (InterruptedException e) {
            LOG.error("Aborting controller-manager.", e);
        } finally {
            LOG.info("Controller-Manager exited");
        }
    }

    private void stopControllers() {
        for (Controller controller : controllers.values()) {
            controller.shutdown();
        }
        if (controllerThreadPool != null) {
            controllerThreadPool.shutdown();
        }
    }
}
