package io.micronaut.docs.operator
//tag::lockprovider[]
import io.micronaut.context.annotation.Replaces
import io.micronaut.kubernetes.client.operator.leaderelection.LockIdentityProvider
import jakarta.inject.Singleton
//end::lockprovider[]
import io.micronaut.context.annotation.Requires

import java.util.Random

@Requires(property = "spec.name", value = "CustomLockIdentityProviderSpec")
//tag::lockprovider[]

@Singleton
@Replaces(LockIdentityProvider::class)
class CustomLockIdentityProvider : LockIdentityProvider {

    override fun getIdentity(): String {
        // ...
        //end::lockprovider[]
        return Random().nextInt().toString()
        //tag::lockprovider[]
    }
}
//end::lockprovider[]
