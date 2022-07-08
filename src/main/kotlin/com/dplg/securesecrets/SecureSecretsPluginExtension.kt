package com.dplg.securesecrets

import org.gradle.api.provider.ListProperty

interface SecureSecretsPluginExtension {
    val buildTypeKeys: ListProperty<String>
    val buildTypesName: ListProperty<String>
}
