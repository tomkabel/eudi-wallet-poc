package ee.cyber.wallet.crypto

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * The dispatcher blocking bridges wait on (JVM-H1/D10). Nimbus's JWSSigner and
 * the COSE provider SPIs are synchronous, so every suspension point behind
 * them is a runBlocking — this keeps those blocking waits OFF the caller's
 * thread (possibly Main) and on a single dedicated daemon thread. Single-thread
 * because Keystore operations are serialized by design; no fairness is needed
 * at one operation deep.
 */
object BlockingDispatchers {

    val keystore: CoroutineDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "keystore-blocking-bridge").apply { isDaemon = true }
    }.asCoroutineDispatcher()
}
