/*
 * Copyright 2017-2021 original authors
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

import io.micronaut.core.annotation.NonNull;
import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Simplified version of {@link okhttp3.logging.HttpLoggingInterceptor} that respects the logger configuration.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
public class OkHttpClientLogging implements Interceptor {

    private static final Logger LOG = LoggerFactory.getLogger(OkHttpClientLogging.class);

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Connection connection = chain.connection();


        if (LOG.isTraceEnabled()) {
            try {
                String method = request.method();
                RequestBody requestBody = request.body();
                Headers headers = request.headers();
                LOG.trace("--> {} {} {} {}-byte body", method, request.url(),
                        (connection != null ? connection.protocol() : ""),
                        (requestBody != null ? requestBody.contentLength() : "0"));
                // log headers for trace
                for (int i = 0; i < headers.size(); i++) {
                    LOG.trace("{}: {}", headers.name(i), headers.value(i));
                }
            } catch (Exception e) {
                LOG.warn("Failed to generate OkHttpClient request log: " + e.getMessage(), e);
            }
        }

        Response response = null;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("HTTP FAILED: " + e.getMessage(), e);
            }
            throw e;
        }

        if (LOG.isDebugEnabled()) {
            try {
                ResponseBody responseBody = response.body();
                long contentLength = responseBody.contentLength();
                String contentLengthString = contentLength == -1L ? "unknown-length" : String.format("%s-byte", contentLength);

                Headers headers = response.headers();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("<-- {} {} {} {}", response.code(), response.message(), response.request().url(), contentLengthString);
                    for (int i = 0; i < headers.size(); i++) {
                        LOG.trace("{}: {}", headers.name(i), headers.value(i));
                    }
                } else {
                    String method = request.method();
                    RequestBody requestBody = request.body();

                    LOG.debug("KubeApi {} {} {} OUT<{}-byte> IN <{}> {}", method, request.url(),
                            (connection != null ? connection.protocol() : ""),
                            (requestBody != null ? requestBody.contentLength() : "0"),
                            contentLengthString, response.code());
                }
            } catch (Exception e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to generate OkHttpClient Response log: " + e.getMessage(), e);
                }
            }
        }

        return response;
    }
}
