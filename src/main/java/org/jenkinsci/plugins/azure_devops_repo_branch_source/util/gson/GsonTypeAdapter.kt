package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.gson

import kotlin.reflect.KClass

interface GsonTypeAdapter {
    fun getTargetClass(): KClass<*>
}