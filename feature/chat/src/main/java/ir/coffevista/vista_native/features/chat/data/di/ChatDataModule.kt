package ir.coffevista.vista_native.features.chat.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.coffevista.vista_native.core.common.ApplicationScope
import ir.coffevista.vista_native.core.network.AppEnvironment
import ir.coffevista.vista_native.core.network.ExternalMedia
import ir.coffevista.vista_native.core.network.InternalApi
import ir.coffevista.vista_native.core.network.InternalEnvironment
import ir.coffevista.vista_native.core.network.PolicyAwareInternalCallFactory
import ir.coffevista.vista_native.core.security.SessionStore
import ir.coffevista.vista_native.features.chat.data.local.ChatDao
import ir.coffevista.vista_native.features.chat.data.local.ChatDatabase
import ir.coffevista.vista_native.features.chat.data.remote.ChatMediaUploader
import ir.coffevista.vista_native.features.chat.data.remote.ChatMediaDownloader
import ir.coffevista.vista_native.features.chat.data.realtime.ChatRealtimeCoordinator
import ir.coffevista.vista_native.features.chat.data.realtime.ChatWebSocketFactory
import ir.coffevista.vista_native.features.chat.data.realtime.OkHttpChatWebSocketFactory
import ir.coffevista.vista_native.features.chat.data.remote.ChatApi
import ir.coffevista.vista_native.features.chat.data.remote.TenorGifCatalog
import ir.coffevista.vista_native.features.chat.data.repository.OfflineFirstChatRepository
import ir.coffevista.vista_native.features.chat.data.security.AndroidKeystoreChatContentCipher
import ir.coffevista.vista_native.features.chat.domain.repository.ChatAccount
import ir.coffevista.vista_native.features.chat.domain.repository.ChatContentCipher
import ir.coffevista.vista_native.features.chat.domain.repository.ChatRepository
import ir.coffevista.vista_native.features.chat.domain.repository.ChatSessionProvider
import ir.coffevista.vista_native.features.chat.domain.repository.ChatSessionRefresher
import ir.coffevista.vista_native.features.chat.domain.repository.GifCatalog
import java.util.UUID
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Call
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object ChatDataModule {
    @Provides
    @Singleton
    fun provideGifCatalog(implementation: TenorGifCatalog): GifCatalog = implementation

    @Provides
    @Singleton
    fun provideChatApi(@InternalApi retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)

    @Provides
    @Singleton
    fun provideChatDatabase(@ApplicationContext context: Context): ChatDatabase =
        Room.databaseBuilder(context, ChatDatabase::class.java, ChatDatabase.NAME)
            .addMigrations(
                ChatDatabase.MIGRATION_1_2,
                ChatDatabase.MIGRATION_2_3,
                ChatDatabase.MIGRATION_3_4,
                ChatDatabase.MIGRATION_4_5,
                ChatDatabase.MIGRATION_5_6,
            )
            .build()

    @Provides
    fun provideChatDao(database: ChatDatabase): ChatDao = database.chatDao()

    @Provides
    @Singleton
    fun provideChatContentCipher(): ChatContentCipher = AndroidKeystoreChatContentCipher()

    @Provides
    @Singleton
    fun provideChatSessionProvider(
        sessionStore: SessionStore,
        @ApplicationContext context: Context,
        refresher: ChatSessionRefresher,
    ): ChatSessionProvider = StoredSessionChatSessionProvider(sessionStore, context, refresher)

    @Provides
    @Singleton
    fun provideChatWebSocketFactory(
        @InternalApi callFactory: Call.Factory,
    ): ChatWebSocketFactory {
        val policyFactory = callFactory as? PolicyAwareInternalCallFactory
            ?: error("Internal Chat WebSocket requires the policy-aware network client")
        return OkHttpChatWebSocketFactory(policyFactory)
    }

    @Provides
    @Singleton
    fun provideChatMediaUploader(
        @ApplicationContext context: Context,
        api: ChatApi,
        @ExternalMedia externalClient: OkHttpClient,
    ): ChatMediaUploader = ChatMediaUploader(context, api, externalClient)

    @Provides
    @Singleton
    fun provideChatMediaDownloader(
        @ApplicationContext context: Context,
        @ExternalMedia externalClient: OkHttpClient,
        dao: ChatDao,
        cipher: ChatContentCipher,
        @ApplicationScope scope: CoroutineScope,
    ): ChatMediaDownloader = ChatMediaDownloader(context, externalClient, dao, cipher, scope)

    @Provides
    @Singleton
    fun provideChatRealtimeCoordinator(
        @ApplicationScope scope: CoroutineScope,
        sessionProvider: ChatSessionProvider,
        socketFactory: ChatWebSocketFactory,
        @InternalEnvironment environment: AppEnvironment,
    ): ChatRealtimeCoordinator = ChatRealtimeCoordinator(
        scope = scope,
        sessionProvider = sessionProvider,
        socketFactory = socketFactory,
        internalBaseUrl = environment.internalBaseUrl,
    )

    @Provides
    @Singleton
    fun provideChatE2EEService(@ApplicationContext context: Context): ir.coffevista.vista_native.features.chat.data.security.ChatE2EEService =
        ir.coffevista.vista_native.features.chat.data.security.ChatE2EEService(context)

    @Provides
    @Singleton
    fun provideChatRepository(
        api: ChatApi,
        dao: ChatDao,
        cipher: ChatContentCipher,
        sessionProvider: ChatSessionProvider,
        realtime: ChatRealtimeCoordinator,
        mediaUploader: ChatMediaUploader,
        mediaDownloader: ChatMediaDownloader,
        e2eeService: ir.coffevista.vista_native.features.chat.data.security.ChatE2EEService,
        @ApplicationScope scope: CoroutineScope,
    ): ChatRepository = OfflineFirstChatRepository(
        api = api,
        dao = dao,
        cipher = cipher,
        sessionProvider = sessionProvider,
        realtime = realtime,
        mediaUploader = mediaUploader,
        mediaDownloader = mediaDownloader,
        e2eeService = e2eeService,
        scope = scope,
    )
}

private class StoredSessionChatSessionProvider(
    private val sessionStore: SessionStore,
    context: Context,
    private val refresher: ChatSessionRefresher,
) : ChatSessionProvider {
    private val deviceId = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        .let { preferences ->
            preferences.getString(DEVICE_ID, null)?.takeIf(String::isNotBlank)
                ?: UUID.randomUUID().toString().also { generated ->
                    check(preferences.edit().putString(DEVICE_ID, generated).commit())
                }
        }
    private val mutableAccount = MutableStateFlow(readAccount())
    override val account: StateFlow<ChatAccount?> = mutableAccount

    override fun synchronize() {
        mutableAccount.value = readAccount()
    }

    override fun clear() {
        mutableAccount.value = null
    }

    override suspend fun refresh(): Boolean {
        val refreshed = refresher.refresh()
        synchronize()
        return refreshed && mutableAccount.value != null
    }

    private fun readAccount(): ChatAccount? = sessionStore.read()?.let { session ->
        ChatAccount(
            accountId = session.userId,
            accessToken = session.accessToken,
            deviceId = deviceId,
        )
    }

    private companion object {
        const val PREFERENCES = "vista_chat_runtime"
        const val DEVICE_ID = "device_id"
    }
}
