# tag::lockprovider[]
import random

from jakarta.inject import Singleton
from micronaut.context.annotation import Replaces
from micronaut.kubernetes.client.operator.leaderelection import LockIdentityProvider
# end::lockprovider[]
from micronaut.context.annotation import Requires
# tag::lockprovider[]

# end::lockprovider[]

@Requires(property="spec.name", value="CustomLockIdentityProviderSpec")
# tag::lockprovider[]
@Singleton
@Replaces(LockIdentityProvider)
class CustomLockIdentityProvider(LockIdentityProvider):

    def getIdentity(self) -> str:
        # ...
        # end::lockprovider[]
        return str(random.randint(-2147483648, 2147483647))
        # tag::lockprovider[]
        ...
# end::lockprovider[]
