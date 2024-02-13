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

import io.kubernetes.client.openapi.apis.AdmissionregistrationApi;
import io.kubernetes.client.openapi.apis.AdmissionregistrationV1Api;
import io.kubernetes.client.openapi.apis.ApiextensionsApi;
import io.kubernetes.client.openapi.apis.ApiextensionsV1Api;
import io.kubernetes.client.openapi.apis.ApiregistrationApi;
import io.kubernetes.client.openapi.apis.ApiregistrationV1Api;
import io.kubernetes.client.openapi.apis.ApisApi;
import io.kubernetes.client.openapi.apis.AppsApi;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.AuthenticationApi;
import io.kubernetes.client.openapi.apis.AuthenticationV1Api;
import io.kubernetes.client.openapi.apis.AuthorizationApi;
import io.kubernetes.client.openapi.apis.AuthorizationV1Api;
import io.kubernetes.client.openapi.apis.AutoscalingApi;
import io.kubernetes.client.openapi.apis.AutoscalingV1Api;
import io.kubernetes.client.openapi.apis.AutoscalingV2Api;
import io.kubernetes.client.openapi.apis.BatchApi;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CertificatesApi;
import io.kubernetes.client.openapi.apis.CertificatesV1Api;
import io.kubernetes.client.openapi.apis.CoordinationApi;
import io.kubernetes.client.openapi.apis.CoordinationV1Api;
import io.kubernetes.client.openapi.apis.CoreApi;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.apis.DiscoveryApi;
import io.kubernetes.client.openapi.apis.DiscoveryV1Api;
import io.kubernetes.client.openapi.apis.EventsApi;
import io.kubernetes.client.openapi.apis.EventsV1Api;
import io.kubernetes.client.openapi.apis.FlowcontrolApiserverApi;
import io.kubernetes.client.openapi.apis.FlowcontrolApiserverV1beta2Api;
import io.kubernetes.client.openapi.apis.InternalApiserverApi;
import io.kubernetes.client.openapi.apis.InternalApiserverV1alpha1Api;
import io.kubernetes.client.openapi.apis.LogsApi;
import io.kubernetes.client.openapi.apis.NetworkingApi;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.apis.NodeApi;
import io.kubernetes.client.openapi.apis.NodeV1Api;
import io.kubernetes.client.openapi.apis.OpenidApi;
import io.kubernetes.client.openapi.apis.PolicyApi;
import io.kubernetes.client.openapi.apis.PolicyV1Api;
import io.kubernetes.client.openapi.apis.RbacAuthorizationApi;
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api;
import io.kubernetes.client.openapi.apis.SchedulingApi;
import io.kubernetes.client.openapi.apis.SchedulingV1Api;
import io.kubernetes.client.openapi.apis.StorageApi;
import io.kubernetes.client.openapi.apis.StorageV1Api;
import io.kubernetes.client.openapi.apis.VersionApi;
import io.kubernetes.client.openapi.apis.WellKnownApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Internal annotation to trigger the creation of Api clients.
 *
 * @since 3.0
 */
@Retention(value = RetentionPolicy.SOURCE)
public @interface Apis {

    /**
     * @return The array of client API objects to generate.
     */
    Class<?>[] value() default {
            AdmissionregistrationApi.class,
            AdmissionregistrationV1Api.class,
            ApiextensionsApi.class,
            ApiextensionsV1Api.class,
            ApiregistrationApi.class,
            ApiregistrationV1Api.class,
            ApisApi.class,
            AppsApi.class,
            AppsV1Api.class,
            AuthenticationApi.class,
            AuthenticationV1Api.class,
            AuthorizationApi.class,
            AuthorizationV1Api.class,
            AutoscalingApi.class,
            AutoscalingV1Api.class,
            AutoscalingV2Api .class,
            BatchApi.class,
            BatchV1Api.class,
            CertificatesApi.class,
            CertificatesV1Api.class,
            CoordinationApi.class,
            CoordinationV1Api.class,
            CoreApi.class,
            CoreV1Api.class,
            CustomObjectsApi.class,
            DiscoveryApi.class,
            DiscoveryV1Api .class,
            EventsApi.class,
            EventsV1Api.class,
            FlowcontrolApiserverApi.class,
            FlowcontrolApiserverV1beta2Api.class,
            InternalApiserverApi.class,
            InternalApiserverV1alpha1Api.class,
            LogsApi.class,
            NetworkingApi.class,
            NetworkingV1Api.class,
            NodeApi.class,
            NodeV1Api.class,
            OpenidApi.class,
            PolicyApi.class,
            PolicyV1Api .class,
            RbacAuthorizationApi.class,
            RbacAuthorizationV1Api.class,
            SchedulingApi.class,
            SchedulingV1Api.class,
            StorageApi.class,
            StorageV1Api.class,
            VersionApi.class,
            WellKnownApi.class,
    };

    /**
     * @return The type of client to generate.
     */
    Kind kind() default Kind.ASYNC;

    /**
     * the type of client to generate.
     */
    enum Kind {
        ASYNC,
        REACTOR,
        RXJAVA2,
        RXJAVA3
    }
}
