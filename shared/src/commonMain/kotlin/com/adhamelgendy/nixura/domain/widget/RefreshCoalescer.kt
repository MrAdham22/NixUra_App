package com.adhamelgendy.nixura.domain.widget

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Merges redraw requests that arrive while a redraw is running (latest state wins).
 *
 * Every widget tap still makes its own write; only the drawing is merged. [request] returns once a [render] pass
 * that **started after** the call has drawn the requested keys, so the picture always shows data at least as new as
 * the caller's write. Five fast taps: five writes, but at most two redraws (the running one, then one for the rest).
 *
 * Keys keep the order they were first asked in (the widget the user touched is drawn first).
 * A failing [render] fails the requests of that pass only; later passes still run.
 */
class RefreshCoalescer<K>(private val render: suspend (Set<K>) -> Unit) {
    private val state = Mutex()
    private var pending = LinkedHashSet<K>()
    private var waiters = ArrayList<CompletableDeferred<Unit>>()
    private var running = false

    suspend fun request(keys: Set<K>) {
        val done = CompletableDeferred<Unit>()
        val drainHere = state.withLock {
            pending.addAll(keys)
            waiters.add(done)
            (!running).also { if (it) running = true }
        }
        // The first caller draws; callers that arrive meanwhile only wait for the next pass.
        if (drainHere) drain()
        done.await()
    }

    private suspend fun drain() {
        while (true) {
            val (batch, batchWaiters) = state.withLock {
                if (pending.isEmpty()) {
                    running = false
                    return
                }
                val taken = pending to waiters
                pending = LinkedHashSet()
                waiters = ArrayList()
                taken
            }
            try {
                render(batch)
                batchWaiters.forEach { it.complete(Unit) }
            } catch (e: CancellationException) {
                // The drawing coroutine is gone: release everyone (now and queued) so nobody waits forever.
                // NonCancellable: taking a Mutex in a cancelled coroutine would throw before the cleanup runs.
                val queued = withContext(NonCancellable) {
                    state.withLock { running = false; waiters.also { waiters = ArrayList(); pending = LinkedHashSet() } }
                }
                (batchWaiters + queued).forEach { it.completeExceptionally(e) }
                throw e
            } catch (e: Exception) {
                batchWaiters.forEach { it.completeExceptionally(e) }
            }
        }
    }
}
