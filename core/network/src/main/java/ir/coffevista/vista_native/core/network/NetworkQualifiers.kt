package ir.coffevista.vista_native.core.network

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class InternalApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class InternalEnvironment

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BootstrapApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ExternalMedia
