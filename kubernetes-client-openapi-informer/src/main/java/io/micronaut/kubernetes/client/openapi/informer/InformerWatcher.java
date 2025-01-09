package io.micronaut.kubernetes.client.openapi.informer;

import io.micronaut.core.util.StringUtils;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.ResponseClosedException;
import io.micronaut.kubernetes.client.openapi.common.KubernetesListObject;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.cache.DeltaFIFO;
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta;
import io.micronaut.kubernetes.client.openapi.model.V1Status;
import io.micronaut.kubernetes.client.openapi.watcher.WatchEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.util.retry.RetryBackoffSpec;

import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gets list of all items and the resource version at the moment of call and then uses
 * the resource version to watch for new events.
 * @param <ApiType>
 */
class InformerWatcher<ApiType extends KubernetesObject> {

    private static final Logger LOG = LoggerFactory.getLogger(InformerWatcher.class);

    private static final Duration WATCH_CLIENT_SIDE_TIMEOUT = Duration.ofMinutes(5);

    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final AtomicReference<Disposable> listDisposable = new AtomicReference<>();
    private final AtomicReference<Disposable> watcherDisposable = new AtomicReference<>();

    private final Class<ApiType> apiTypeClass;
    private final InformerApiCall<ApiType> informerApiCall;
    private final DeltaFIFO deltaFifo;

    private volatile boolean relistObjects;
    private volatile String lastSyncResourceVersion;
    private volatile boolean isLastSyncResourceVersionUnavailable;

    InformerWatcher(Class<ApiType> apiTypeClass, InformerApiCall<ApiType> informerApiCall, DeltaFIFO deltaFifo) {
        this.apiTypeClass = apiTypeClass;
        this.informerApiCall = informerApiCall;
        this.deltaFifo = deltaFifo;
    }

    void stop() {
        stopped.set(true);
        if (listDisposable.get() != null) {
            listDisposable.get().dispose();
        }
        if (watcherDisposable.get() != null) {
            watcherDisposable.get().dispose();
        }
        logInfo("Stopped informer watcher");
    }

    void start() {
        logInfo("Starting informer watcher");
        listObjects();
    }

    private void restart() {
        logDebug("Restarting informer watcher after client timed out or thrown error");
        if (relistObjects) {
            listObjects();
        } else {
            startWatcher();
        }
    }

    private void listObjects() {
        if (stopped.get()) {
            return;
        }

        logDebug("Getting list of existing objects");

        Disposable newDisposable = informerApiCall.list(getRelistResourceVersion())
            .retryWhen(RetryBackoffSpec.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(30))
                .doBeforeRetry(it -> logInfo("Failed to get a list of existing objects, retrying...[{}]", it)))
            .subscribe(this::replaceObjectsAndStartWatcher);

        if (stopped.get()) {
            newDisposable.dispose();
        } else {
            listDisposable.set(newDisposable);
        }
    }

    private void replaceObjectsAndStartWatcher(KubernetesListObject list) {
        lastSyncResourceVersion = list.getMetadata().getResourceVersion();
        isLastSyncResourceVersionUnavailable = false;
        relistObjects = false;

        logDebug("Found resourceVersion={} in retrieved list metadata", lastSyncResourceVersion);

        deltaFifo.replace((List<KubernetesObject>) list.getItems());

        startWatcher();
    }

    private void startWatcher() {
        if (stopped.get()) {
            return;
        }

        int jitteredTimeoutSeconds = Double.valueOf(WATCH_CLIENT_SIDE_TIMEOUT.getSeconds() * (1 + Math.random())).intValue();
        logDebug("Starting watcher with resourceVersion={}, watchTime={}sec", lastSyncResourceVersion, jitteredTimeoutSeconds);

        Disposable newDisposable = informerApiCall.watch(lastSyncResourceVersion, jitteredTimeoutSeconds)
            .retryWhen(RetryBackoffSpec.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(30))
                .filter(this::isConnectException)
                .doBeforeRetry(it -> logInfo("Failed to start watcher, retrying...[{}]", it)))
            .doAfterTerminate(this::restart)
            .subscribe(this::handleWatchEvent, this::handleError);

        if (stopped.get()) {
            newDisposable.dispose();
        } else {
            watcherDisposable.set(newDisposable);
        }
    }

    private String getRelistResourceVersion() {
        // if the lastSyncResourceVersion is unavailable, use "" as resource version to read from etcd.
        if (isLastSyncResourceVersionUnavailable) {
            return "";
        }
        // For performance reasons, initial list performed by reflector uses "0" as resource version
        // to allow it to be served from the watch cache if it is enabled
        return StringUtils.isEmpty(lastSyncResourceVersion) ? "0" : lastSyncResourceVersion;
    }

    private boolean isConnectException(Throwable t) {
        return t instanceof HttpClientException
            && t.getCause() != null
            && t.getCause().getCause() instanceof ConnectException;
    }

    private void handleError(Throwable t) {
        logError("Watcher failure", t);
        // If this is the response closed exception (thrown when the api server becomes
        // unavailable after streaming started), don't relist objects because most likely
        // we will be able to restart watch where we ended.
        if (t instanceof ResponseClosedException) {
            return;
        }
        // unknown error, relist objects
        relistObjects = true;
    }

    private void handleWatchEvent(WatchEvent<ApiType> watchEvent) {
        Optional<EventType> eventType = EventType.findByType(watchEvent.type());
        if (eventType.isEmpty()) {
            logError("Unrecognized event type: {}", watchEvent);
            return;
        }
        if (eventType.get() == EventType.ERROR) {
            V1Status status = watchEvent.status();
            if (status == null) {
                logError("Received ERROR event without status: {}", watchEvent);
            } else if (status.getCode() == HttpURLConnection.HTTP_GONE) {
                relistObjects = true;
                isLastSyncResourceVersionUnavailable = true;
                logError("Resource version and watch connection expired, resourceVersion={}, statusMessage={}",
                    lastSyncResourceVersion,
                    status.getMessage());
            } else {
                logError("Received ERROR event: {}", watchEvent);
            }
            return;
        }

        ApiType object = watchEvent.object();
        V1ObjectMeta meta = object.getMetadata();
        String newResourceVersion = meta.getResourceVersion();
        switch (eventType.get()) {
            case ADDED:
                deltaFifo.add(object);
                break;
            case MODIFIED:
                deltaFifo.update(object);
                break;
            case DELETED:
                deltaFifo.delete(object);
                break;
            case BOOKMARK:
                break;
            // A `Bookmark` means watch has synced here, just update the resourceVersion
        }
        lastSyncResourceVersion = newResourceVersion;
        logDebug("Updated resource version, resourceVersion={}", lastSyncResourceVersion);
    }

    private void logError(String message, Object... arguments) {
        if (LOG.isErrorEnabled()) {
            LOG.error(createLogMessage(message), createLogArgs(arguments));
        }
    }

    private void logInfo(String message, Object... arguments) {
        if (LOG.isInfoEnabled()) {
            LOG.info(createLogMessage(message), createLogArgs(arguments));
        }
    }

    private void logDebug(String message, Object... arguments) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(createLogMessage(message), createLogArgs(arguments));
        }
    }

    private String createLogMessage(String message) {
        String namespace = informerApiCall.getNamespace();
        String messagePrefix = StringUtils.isEmpty(namespace) ? "Type={}: " : "Type={}, Namespace={}: ";
        return messagePrefix + message;
    }

    private Object[] createLogArgs(Object... arguments) {
        String namespace = informerApiCall.getNamespace();
        Object[] newArguments;
        if (StringUtils.isEmpty(namespace)) {
            newArguments = new Object[arguments.length + 1];
            newArguments[0] = apiTypeClass.getSimpleName();
        } else {
            newArguments = new Object[arguments.length + 2];
            newArguments[0] = apiTypeClass.getSimpleName();
            newArguments[1] = namespace;
        }
        if (arguments.length > 0) {
            int destPos = StringUtils.isEmpty(namespace) ? 1 : 2;
            System.arraycopy(arguments, 0, newArguments, destPos, arguments.length);
        }
        return newArguments;
    }

    private enum EventType {
        ADDED, MODIFIED, DELETED, BOOKMARK, ERROR;

        private static final Map<String, EventType> TYPES =
            Arrays.stream(EventType.values()).collect(Collectors.toMap(Enum::name, Function.identity()));

        private static Optional<EventType> findByType(String type) {
            return Optional.ofNullable(TYPES.get(type.toUpperCase()));
        }
    }
}
