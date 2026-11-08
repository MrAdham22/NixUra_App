package com.adhamelgendy.nixura.data.integration

import com.adhamelgendy.nixura.domain.integration.GoogleAuthorizer
import com.adhamelgendy.nixura.domain.integration.IntegrationProvider
import com.adhamelgendy.nixura.domain.integration.IntegrationRevoker

internal class IntegrationRevokerImpl(
    private val google: GoogleAuthorizer,
    private val notion: NotionBackendClient,
) : IntegrationRevoker {
    override suspend fun revoke(provider: IntegrationProvider) {
        when (provider) {
            IntegrationProvider.GOOGLE -> google.revoke()
            IntegrationProvider.NOTION -> notion.disconnect()
        }
    }
}
