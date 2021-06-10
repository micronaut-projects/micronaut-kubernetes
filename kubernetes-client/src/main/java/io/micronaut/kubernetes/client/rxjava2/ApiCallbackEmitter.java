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
package io.micronaut.kubernetes.client.rxjava2;

import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import io.reactivex.SingleEmitter;

import java.util.List;
import java.util.Map;

/**
 * Bridges the {@link ApiCallback} interface to an RxJava {@link SingleEmitter}.
 *
 * @author Pavol Gressa
 * @since 2.4
 */
public class ApiCallbackEmitter<T> implements ApiCallback<T> {

    private final SingleEmitter<T> emitter;

    public ApiCallbackEmitter(SingleEmitter<T> emitter) {
        this.emitter = emitter;
    }

    @Override
    public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
        emitter.onError(e);
    }

    @Override
    public void onSuccess(T result, int statusCode, Map<String, List<String>> responseHeaders) {
        emitter.onSuccess(result);
    }

    @Override
    public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
        // no-op
    }

    @Override
    public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
        // no-op
    }
}
