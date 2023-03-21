package io.micronaut.kubernetes.client;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ConnectException;

public class OkHttpClientRetryConnectionRefused implements Interceptor {

    static int RETRY_CONNECTION_REFUSE_TIMES = 5;
    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        int tryCount = 0;
        while (tryCount < RETRY_CONNECTION_REFUSE_TIMES) {
            tryCount ++;
            try {
                return chain.proceed(request);
            } catch (ConnectException e) {
                if (e.getMessage().contains("Connection refused")) {
                    if (tryCount == RETRY_CONNECTION_REFUSE_TIMES -1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            } catch (IOException e) {
                throw e;
            }
        }
    }
}
