package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

object Constants {

    const val DATE_TIME_FORMAT_AZURE = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX"

    // supported universally
    const val DATE_TIME_FORMAT_RFC822 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    const val DATE_TIME_FORMAT_NO_MILLI_RFC822 = "yyyy-MM-dd'T'HH:mm:ssZ"

    // Only supported by Android SimpleDateFormat or Java 8 DateTimeFormatter
    const val DATE_TIME_FORMAT_ISO8601_ANDROID = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ"
    const val DATE_TIME_FORMAT_NO_MILLI_ISO8601_ANDROID = "yyyy-MM-dd'T'HH:mm:ssZZZZZ"

    // Only supported by Android version 24+ SimpleDateFormat or Java 8 DateTimeFormatter
    const val DATE_TIME_FORMAT_ISO8601_JAVA_TIME_Z_FOR_0 = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    const val DATE_TIME_FORMAT_NO_MILLI_ISO8601_JAVA_TIME_Z_FOR_0 = "yyyy-MM-dd'T'HH:mm:ssXXX"

    // Only supported by Java 8 DateTimeFormatter
    const val DATE_TIME_FORMAT_ISO8601_JAVA_TIME_0_FOR_0 = "yyyy-MM-dd'T'HH:mm:ss.SSSxxx"
    const val DATE_TIME_FORMAT_NO_MILLI_ISO8601_JAVA_TIME_0_FOR_0 = "yyyy-MM-dd'T'HH:mm:ssxxx"

    const val LOCAL_DATE_FORMAT = "yyyy-MM-dd"
    const val LOCAL_TIME_FORMAT = "HH:mm:ss"
    const val LOCAL_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"

    val TIME_ZONE_UTC by lazy { TimeZone.getTimeZone("UTC") }
    val TIME_ZONE_AEST by lazy { TimeZone.getTimeZone("Australia/Brisbane") }
    val ZONE_OFFSET_DEFAULT by lazy { ZoneOffset.from(OffsetDateTime.now()) }
    val ZONE_ID_DEFAULT by lazy { ZoneId.systemDefault() }
    val ZONE_ID_UTC by lazy { ZoneId.of("UTC") }
    val ZONE_ID_AEST by lazy { ZoneId.of("Australia/Brisbane") }
}