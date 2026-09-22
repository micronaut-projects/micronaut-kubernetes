from micronaut.kubernetes.client.informer import SharedIndexInformerFactory

from io.kubernetes.client.informer import SharedIndexInformer
from io.kubernetes.client.openapi.models import V1ConfigMap, V1ConfigMapList


class SharedIndexInformerFactoryExample:

    def __init__(self, factory: SharedIndexInformerFactory):
        self.factory = factory

    def create_informer(self) -> SharedIndexInformer[V1ConfigMap]:
        # tag::create[]
        shared_index_informer = self.factory.sharedIndexInformerFor(
                V1ConfigMap,  # <1>
                V1ConfigMapList,  # <2>
                "configmaps",  # <3>
                "",  # <4>
                "default",  # <5>
                None,
                None,
                True
        )
        # end::create[]
        return shared_index_informer

    def get_informer(self, namespace: str) -> SharedIndexInformer[V1ConfigMap]:
        # tag::get[]
        shared_index_informer = self.factory.getExistingSharedIndexInformer(
                "default",  # <1>
                V1ConfigMap)  # <2>
        # end::get[]
        return shared_index_informer
