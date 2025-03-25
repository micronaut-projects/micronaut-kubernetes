package io.micronaut.kubernetes.client.openapi.operator;

import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

final class OperatorFilter {

    static class OnAdd implements Predicate<KubernetesObject> {
        @Override
        public boolean test(KubernetesObject kubernetesObject) {
            return true;
        }
    }

    static class OnUpdate implements BiPredicate<KubernetesObject, KubernetesObject> {
        @Override
        public boolean test(KubernetesObject kubernetesObject, KubernetesObject kubernetesObject2) {
            return true;
        }
    }

    static class OnDelete implements BiPredicate<KubernetesObject, Boolean> {
        @Override
        public boolean test(KubernetesObject kubernetesObject, Boolean aBoolean) {
            return true;
        }
    }
}
