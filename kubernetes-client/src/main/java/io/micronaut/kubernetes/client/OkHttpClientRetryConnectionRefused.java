package io.micronaut.kubernetes.client;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;

public class OkHttpClientRetryConnectionRefused implements Interceptor {

    private static final Logger LOG = LoggerFactory.getLogger(OkHttpClientRetryConnectionRefused.class);

    static int RETRY_CONNECTION_REFUSE_TIMES = 5;

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        int tryCount = 0;
        while (tryCount < (RETRY_CONNECTION_REFUSE_TIMES - 1)) {
            tryCount ++;
            try {
                return chain.proceed(request);
            } catch (ConnectException e) {
                if (e.getMessage().startsWith("Connection refused")) {
                    try {
                        LOG.debug("Waiting 1s to try again");
                        Thread.sleep( 1000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    throw e;
                }
            } catch (IOException e) {
                throw e;
            }
        }
        return chain.proceed(request);
    }
}
