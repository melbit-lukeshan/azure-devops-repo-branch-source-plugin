package org.jenkinsci.plugins.azure_devops_repo_branch_source.util

import kotlin.reflect.KClass

interface JsonProcessor {
    fun <T : Any> instanceFromJson(json: String?, clazz: KClass<T>): T?
    fun <T : Any> instanceToJson(instance: T?): String?
}