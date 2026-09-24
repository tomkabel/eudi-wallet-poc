package ee.cyber.wallet.data.datastore

import androidx.datastore.core.DataStore
import ee.cyber.wallet.UserPreferencesProto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ARF OIA_08e/08f: the DC API disclosure switch defaults to on and round-trips through the
 * proto. The setter and the read mapping must agree on the stored polarity, or the switch can
 * never turn off.
 */
class UserPreferencesDataSourceTest {

    private class InMemoryDataStore : DataStore<UserPreferencesProto> {
        private val state = MutableStateFlow(UserPreferencesProto.getDefaultInstance())
        override val data: Flow<UserPreferencesProto> = state
        override suspend fun updateData(transform: suspend (t: UserPreferencesProto) -> UserPreferencesProto) =
            transform(state.value).also { state.value = it }
    }

    private val source = UserPreferencesDataSource(InMemoryDataStore())

    private suspend fun enabled() = source.userPreferences.first().dcApiDisclosureEnabled

    @Test
    fun `an unset preference reads as enabled`() = runTest {
        assertTrue(enabled())
    }

    @Test
    fun `disabling persists and reads back disabled, enabling reads back enabled`() = runTest {
        source.setDcApiDisclosureEnabled(false)
        assertFalse(enabled())

        source.setDcApiDisclosureEnabled(true)
        assertTrue(enabled())
    }
}
