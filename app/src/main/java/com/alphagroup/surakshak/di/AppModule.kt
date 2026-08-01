package com.alphagroup.surakshak.di

import android.content.Context
import android.content.pm.PackageManager
import com.alphagroup.surakshak.c2pa.C2PAManifestBuilder
import com.alphagroup.surakshak.c2pa.C2PAManifestBuilderImpl
import com.alphagroup.surakshak.security.IntegrityManager
import com.alphagroup.surakshak.security.StrongBoxKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePackageManager(@ApplicationContext context: Context): PackageManager {
        return context.packageManager
    }

    @Provides
    @Singleton
    fun provideC2PAManifestBuilder(
        strongBoxKeyManager: StrongBoxKeyManager,
        integrityManager: IntegrityManager
    ): C2PAManifestBuilder {
        return C2PAManifestBuilderImpl(strongBoxKeyManager, integrityManager)
    }
}
