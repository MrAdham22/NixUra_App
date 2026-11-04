package com.adhamelgendy.nixura.presentation.integrations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adhamelgendy.nixura.domain.auth.SessionManager
import com.adhamelgendy.nixura.domain.auth.SessionState
import com.adhamelgendy.nixura.domain.integration.DisconnectIntegrationUseCase
import com.adhamelgendy.nixura.domain.integration.GoogleCalendarCatalog
import com.adhamelgendy.nixura.domain.integration.GoogleFeed
import com.adhamelgendy.nixura.domain.integration.GoogleMapping
import com.adhamelgendy.nixura.domain.integration.LinkReply
import com.adhamelgendy.nixura.domain.integration.NotionDatabaseChoice
import com.adhamelgendy.nixura.domain.integration.NotionLink
import com.adhamelgendy.nixura.domain.integration.NotionMapping
import com.adhamelgendy.nixura.domain.integration.GoogleAuthResult
import com.adhamelgendy.nixura.domain.integration.GoogleAuthorizer
import com.adhamelgendy.nixura.domain.integration.IntegrationError
import com.adhamelgendy.nixura.domain.integration.IntegrationProvider
import com.adhamelgendy.nixura.domain.integration.IntegrationState
import com.adhamelgendy.nixura.domain.integration.IntegrationStateStore
import com.adhamelgendy.nixura.domain.integration.SyncIntegrationsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Something the screen should tell the user once. */
enum class IntegrationNotice { GOOGLE_NOT_AVAILABLE, GOOGLE_CANCELLED, GOOGLE_DENIED, OFFLINE, FAILED, NOTION_NOT_CONNECTED }

data class IntegrationsUiState(
    val signedIn: Boolean = false,
    val google: IntegrationState = IntegrationState(),
    val notion: IntegrationState = IntegrationState(),
    val notionDatabases: List<NotionDatabaseChoice> = emptyList(),
    val busy: IntegrationProvider? = null,
    val notice: IntegrationNotice? = null,
)

/**
 * Settings → Integrations. Google is authorised on the device (read-only scopes) and the user picks what to import
 * (events, birthdays, tasks, holidays for their country);
 * Notion is authorised in the browser through NixUra's server, then the user picks a database and its title and
 * date properties (suggested automatically).
 */
class IntegrationsViewModel(
    private val states: IntegrationStateStore,
    private val sync: SyncIntegrationsUseCase,
    private val disconnect: DisconnectIntegrationUseCase,
    private val googleAuth: GoogleAuthorizer,
    private val google: GoogleCalendarCatalog,
    private val notion: NotionLink,
    private val sessions: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(IntegrationsUiState())
    val state: StateFlow<IntegrationsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Re-reads everything; also called when the app returns from Notion's sign-in page in the browser. */
    fun refresh() {
        viewModelScope.launch {
            val signedIn = sessions.state.value is SessionState.SignedIn
            var notionState = states.get(IntegrationProvider.NOTION)
            if (signedIn) {
                // The server is the truth for Notion: the link may have been made (or removed) in the browser.
                when (val s = notion.status()) {
                    is LinkReply.Ok -> {
                        if (s.value.connected != notionState.connected || s.value.workspace != notionState.accountLabel) {
                            notionState = if (s.value.connected) notionState.copy(connected = true, accountLabel = s.value.workspace) else IntegrationState()
                            states.save(IntegrationProvider.NOTION, notionState)
                        }
                    }
                    else -> Unit
                }
            }
            _state.update {
                it.copy(signedIn = signedIn, google = states.get(IntegrationProvider.GOOGLE), notion = notionState)
            }
            if (signedIn && notionState.connected && _state.value.notionDatabases.isEmpty()) loadNotionDatabases()
        }
    }

    fun onConnectGoogle() = launchBusy(IntegrationProvider.GOOGLE) {
        when (val r = googleAuth.authorize(interactive = true)) {
            is GoogleAuthResult.Granted -> {
                // If Google cannot be asked now the link is not saved, so it never looks connected with nothing to pick.
                val holidays = when (val h = google.holidayCalendar()) {
                    is LinkReply.Ok -> h.value
                    LinkReply.Offline -> return@launchBusy notice(IntegrationNotice.OFFLINE)
                    else -> return@launchBusy notice(IntegrationNotice.FAILED)
                }
                val mapping = holidays?.let { mapOf(GoogleMapping.HOLIDAY_CALENDAR to it.id, GoogleMapping.HOLIDAY_NAME to it.name) }.orEmpty()
                val s = IntegrationState(
                    connected = true,
                    accountLabel = r.accountLabel,
                    selection = GoogleFeed.defaults.map { it.key },
                    mapping = mapping,
                )
                states.save(IntegrationProvider.GOOGLE, s)
                _state.update { it.copy(google = s) }
                sync(IntegrationProvider.GOOGLE)
            }
            GoogleAuthResult.NotAvailable -> notice(IntegrationNotice.GOOGLE_NOT_AVAILABLE)
            GoogleAuthResult.Offline -> notice(IntegrationNotice.OFFLINE)
            GoogleAuthResult.Cancelled -> notice(IntegrationNotice.GOOGLE_CANCELLED)
            GoogleAuthResult.Denied, GoogleAuthResult.NeedsConsent -> notice(IntegrationNotice.GOOGLE_DENIED)
        }
    }

    fun onToggleGoogleFeed(feed: GoogleFeed) = launchBusy(IntegrationProvider.GOOGLE) {
        val current = states.get(IntegrationProvider.GOOGLE)
        val id = feed.key
        val selection = if (id in current.selection) current.selection - id else current.selection + id
        // A different selection starts over, so events of what is no longer picked are removed.
        val s = current.copy(selection = selection, cursor = null)
        states.save(IntegrationProvider.GOOGLE, s)
        _state.update { it.copy(google = s) }
        sync(IntegrationProvider.GOOGLE)
    }

    /** Returns the Notion authorisation URL to open in the browser, or null (and a notice) when it is not possible. */
    fun onConnectNotion(open: (String) -> Unit) = launchBusy(IntegrationProvider.NOTION) {
        when (val r = notion.startUrl()) {
            is LinkReply.Ok -> open(r.value)
            LinkReply.Offline -> notice(IntegrationNotice.OFFLINE)
            else -> notice(IntegrationNotice.FAILED)
        }
    }

    fun onPickNotionDatabase(db: NotionDatabaseChoice, titleProperty: String? = null, dateProperty: String? = null) =
        launchBusy(IntegrationProvider.NOTION) {
            val title = titleProperty ?: db.titleProperties.firstOrNull()
            val date = dateProperty ?: NotionPropertyGuess.dateProperty(db.dateProperties)
            val current = states.get(IntegrationProvider.NOTION)
            val mapping = buildMap {
                if (title != null) put(NotionMapping.TITLE, title)
                if (date != null) put(NotionMapping.DATE, date)
            }
            val s = current.copy(selection = listOf(db.id), mapping = mapping, cursor = null)
            states.save(IntegrationProvider.NOTION, s)
            _state.update { it.copy(notion = s) }
            sync(IntegrationProvider.NOTION)
        }

    fun onSyncNow(provider: IntegrationProvider) = launchBusy(provider) { sync(provider) }

    fun onDisconnect(provider: IntegrationProvider) = launchBusy(provider) {
        disconnect(provider)
        _state.update {
            when (provider) {
                IntegrationProvider.GOOGLE -> it.copy(google = IntegrationState())
                IntegrationProvider.NOTION -> it.copy(notion = IntegrationState(), notionDatabases = emptyList())
            }
        }
    }

    fun onNoticeShown() = _state.update { it.copy(notice = null) }

    private suspend fun sync(provider: IntegrationProvider) {
        val error = sync.invoke(provider)[provider]
        if (error == IntegrationError.OFFLINE) notice(IntegrationNotice.OFFLINE)
        _state.update { it.copy(google = states.get(IntegrationProvider.GOOGLE), notion = states.get(IntegrationProvider.NOTION)) }
    }

    private suspend fun loadNotionDatabases() {
        when (val r = notion.databases()) {
            is LinkReply.Ok -> _state.update { it.copy(notionDatabases = r.value) }
            LinkReply.NotConnected -> notice(IntegrationNotice.NOTION_NOT_CONNECTED)
            else -> Unit
        }
    }

    private fun notice(n: IntegrationNotice) = _state.update { it.copy(notice = n) }

    private fun launchBusy(provider: IntegrationProvider, block: suspend () -> Unit) {
        if (_state.value.busy != null) return
        viewModelScope.launch {
            _state.update { it.copy(busy = provider) }
            try {
                block()
            } finally {
                _state.update { it.copy(busy = null) }
            }
        }
    }
}

/** Picks the date property people usually mean ("Date", "Due", "When"...), else the first one. */
object NotionPropertyGuess {
    private val preferred = listOf("date", "when", "due", "deadline", "event date", "start", "time", "scheduled")

    fun dateProperty(candidates: List<String>): String? =
        preferred.firstNotNullOfOrNull { p -> candidates.firstOrNull { it.trim().lowercase() == p } }
            ?: candidates.firstOrNull { c -> preferred.any { c.lowercase().contains(it) } }
            ?: candidates.firstOrNull()
}
