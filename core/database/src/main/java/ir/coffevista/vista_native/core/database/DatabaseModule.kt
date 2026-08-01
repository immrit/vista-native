package ir.coffevista.vista_native.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideFoundationDatabase(
        @ApplicationContext context: Context,
    ): VistaFoundationDatabase = Room.databaseBuilder(
        context,
        VistaFoundationDatabase::class.java,
        VistaFoundationDatabase.DATABASE_NAME,
    )
        .addMigrations(
            VistaFoundationDatabase.MIGRATION_1_2,
            VistaFoundationDatabase.MIGRATION_2_3,
            VistaFoundationDatabase.MIGRATION_3_4,
            VistaFoundationDatabase.MIGRATION_4_5,
            VistaFoundationDatabase.MIGRATION_5_6,
            VistaFoundationDatabase.MIGRATION_6_7,
        )
        .build()

    @Provides
    fun provideVerifiedTlsPolicyDao(
        database: VistaFoundationDatabase,
    ): VerifiedTlsPolicyDao = database.verifiedTlsPolicyDao()

    @Provides
    fun provideOwnProfileDao(
        database: VistaFoundationDatabase,
    ): ir.coffevista.vista_native.core.database.profile.OwnProfileDao = database.ownProfileDao()

    @Provides
    fun provideFeedDao(
        database: VistaFoundationDatabase,
    ): ir.coffevista.vista_native.core.database.feed.FeedDao = database.feedDao()

    @Provides
    fun providePublicProfileDao(
        database: VistaFoundationDatabase,
    ): ir.coffevista.vista_native.core.database.profile.PublicProfileDao =
        database.publicProfileDao()

    @Provides
    fun provideSearchHistoryDao(
        database: VistaFoundationDatabase,
    ): ir.coffevista.vista_native.core.database.search.SearchHistoryDao =
        database.searchHistoryDao()
}
