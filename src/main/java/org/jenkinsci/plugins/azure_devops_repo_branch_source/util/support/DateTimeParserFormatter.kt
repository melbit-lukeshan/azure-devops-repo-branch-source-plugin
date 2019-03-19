package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.support

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Pass the desired format to parse-from and format-to
 * Can pass null if only used to parse-from/format-to epoch milliseconds
 */
class DateTimeParserFormatter(private val format: String?) {
    private companion object {
        const val TIMEZONE_UTC_ISO_Z = "Z"
        const val TIMEZONE_UTC_ISO_0 = "+00:00"
    }

    private val dateFormat: ThreadLocal<DateFormat> =
            object : ThreadLocal<DateFormat>() {
                override fun initialValue(): DateFormat {
                    return SimpleDateFormat(format, Locale.US)
                }
            }

    private val dateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern(format, Locale.US)
    }

    fun toOffsetDateTime(string: String?): OffsetDateTime? = string?.let { dateTimeFormatter.parse(it, OffsetDateTime::from) }

    fun toString(offsetDateTime: OffsetDateTime?): String? = offsetDateTime?.let { dateTimeFormatter.format(it) }

    fun toLocalDateTime(string: String?): LocalDateTime? = string?.let { dateTimeFormatter.parse(it, LocalDateTime::from) }

    fun toString(localDateTime: LocalDateTime?): String? = localDateTime?.let { dateTimeFormatter.format(it) }

    fun toLocalDate(string: String?): LocalDate? = string?.let { dateTimeFormatter.parse(it, LocalDate::from) }

    fun toString(localDate: LocalDate?): String? = localDate?.let { dateTimeFormatter.format(it) }

    fun toLocalTime(string: String?): LocalTime? = string?.let { dateTimeFormatter.parse(it, LocalTime::from) }

    fun toString(localTime: LocalTime?): String? = localTime?.let { dateTimeFormatter.format(it) }

    fun toDate(string: String?): Date? = string?.let { dateFormat.get()?.parse(toZeroEndedString(it)) }

    fun toString(date: Date?): String? = date?.let { dateFormat.get()?.format(it) }

    fun toOffsetDateTime(epochMilli: Long?): OffsetDateTime? = epochMilli?.let { OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), Constants.ZONE_ID_DEFAULT) }

    fun toEpochMilli(offsetDateTime: OffsetDateTime?): Long? = offsetDateTime?.toInstant()?.toEpochMilli()

    fun toDate(epochMilli: Long?): Date? = epochMilli?.let { Date(epochMilli) }

    fun toEpochMilli(date: Date?): Long? = date?.time

    private fun toZeroEndedString(string: String?): String? =
            string?.let {
                if (it.endsWith(TIMEZONE_UTC_ISO_Z)) {
                    it.replace(TIMEZONE_UTC_ISO_Z, TIMEZONE_UTC_ISO_0)
                } else {
                    it
                }
            }
}