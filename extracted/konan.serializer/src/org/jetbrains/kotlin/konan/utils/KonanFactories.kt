package org.jetbrains.kotlin.konan.utils

import org.jetbrains.kotlin.descriptors.konan.KonanModuleDescriptorFactory
import org.jetbrains.kotlin.descriptors.konan.impl.KonanModuleDescriptorFactoryImpl
import org.jetbrains.kotlin.serialization.konan.KonanDeserializedModuleDescriptorFactory
import org.jetbrains.kotlin.serialization.konan.KonanDeserializedPackageFragmentsFactory
import org.jetbrains.kotlin.serialization.konan.KonanResolvedModuleDescriptorsFactory
import org.jetbrains.kotlin.serialization.konan.impl.KonanDeserializedModuleDescriptorFactoryImpl
import org.jetbrains.kotlin.serialization.konan.impl.KonanDeserializedPackageFragmentsFactoryImpl
import org.jetbrains.kotlin.serialization.konan.impl.KonanResolvedModuleDescriptorsFactoryImpl

/**
 * The default Kotlin/Native factories.
 */
object KonanFactories {

    /**
     * The default [KonanModuleDescriptorFactory] factory instance.
     */
    val DefaultDescriptorFactory: KonanModuleDescriptorFactory = KonanModuleDescriptorFactoryImpl

    /**
     * The default [KonanDeserializedPackageFragmentsFactory] factory instance.
     */
    val DefaultPackageFragmentsFactory: KonanDeserializedPackageFragmentsFactory =
            KonanDeserializedPackageFragmentsFactoryImpl

    /**
     * The default [KonanDeserializedModuleDescriptorFactory] factory instance.
     */
    val DefaultDeserializedDescriptorFactory: KonanDeserializedModuleDescriptorFactory =
            createDefaultKonanDeserializedModuleDescriptorFactory(
                    DefaultDescriptorFactory, DefaultPackageFragmentsFactory)

    /**
     * The default [KonanResolvedModuleDescriptorsFactory] factory instance.
     */
    val DefaultResolvedDescriptorsFactory: KonanResolvedModuleDescriptorsFactory =
            createDefaultKonanResolvedModuleDescriptorsFactory(DefaultDeserializedDescriptorFactory)

    fun createDefaultKonanDeserializedModuleDescriptorFactory(
            descriptorFactory: KonanModuleDescriptorFactory,
            packageFragmentsFactory: KonanDeserializedPackageFragmentsFactory
    ): KonanDeserializedModuleDescriptorFactory =
            KonanDeserializedModuleDescriptorFactoryImpl(descriptorFactory, packageFragmentsFactory)

    fun createDefaultKonanResolvedModuleDescriptorsFactory(
            moduleDescriptorFactory: KonanDeserializedModuleDescriptorFactory
    ): KonanResolvedModuleDescriptorsFactory = KonanResolvedModuleDescriptorsFactoryImpl(moduleDescriptorFactory)
}
