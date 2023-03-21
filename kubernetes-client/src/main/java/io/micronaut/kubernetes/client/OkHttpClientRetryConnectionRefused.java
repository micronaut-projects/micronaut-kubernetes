/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.kubernetes.client;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;

/**
 * OkHttpClientRetryConnectionRefused retries Kubernetes requests if client receives Connection Refused error.
 *
 * @author Nemanja Mikic
 * @since 3.5
 */
public class OkHttpClientRetryConnectionRefused implements Interceptor {
    private static final int RETRY_CONNECTION_REFUSE_TIMES = 5;
    private static final Logger LOG = LoggerFactory.getLogger(OkHttpClientRetryConnectionRefused.class);

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        int tryCount = 0;
        while (tryCount < (RETRY_CONNECTION_REFUSE_TIMES - 1)) {
            tryCount ++;
            try {
                return chain.proceed(request.newBuilder().build());
            } catch (ConnectException e) {
                LOG.debug("Connection exception", e);
                if (e.getMessage().startsWith("Failed to connect")) {
                    try {
                        LOG.debug("Received error: {}. Waiting 1s to try again", e.getMessage());
                        Thread.sleep(1000);
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
