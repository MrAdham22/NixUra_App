package com.example.reclaimapp.data.integration

import com.example.reclaimapp.domain.integration.GoogleAuthorizer
import com.example.reclaimapp.domain.integration.IntegrationProvider
import com.example.reclaimapp.domain.integration.IntegrationRevoker

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
