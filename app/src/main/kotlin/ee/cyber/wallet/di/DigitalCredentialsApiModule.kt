package ee.cyber.wallet.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DigitalCredentialsApiModule {

    @Provides
    @Singleton
    fun provideAllowedAppsJson(@ApplicationContext context: Context): String {
        return context.assets.open("dcapi/dcapi_apps.json").bufferedReader().use { it.readText() }
    }
}