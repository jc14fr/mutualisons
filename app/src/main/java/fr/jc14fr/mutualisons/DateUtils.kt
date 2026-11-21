package fr.jc14fr.mutualisons

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object DateUtils {
    // Format attendu par PocketBase : "2026-11-20 10:30:00.000Z"
    private val PB_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val FR_PARSE = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    private val FR_OUT = DateTimeFormatter.ofPattern("dd/MM/uuuu")

    /** Date du jour + [jours], au format JJ/MM/AAAA. */
    fun frDansJours(jours: Long): String = LocalDate.now().plusDays(jours).format(FR_OUT)

    /** Maintenant, en UTC, au format PocketBase. */
    fun pbNow(): String = LocalDateTime.now(ZoneOffset.UTC).format(PB_FORMAT) + "Z"

    /**
     * Convertit une date saisie JJ/MM/AAAA (heure locale) en date PocketBase UTC.
     * Début de journée, ou fin de journée si endOfDay est vrai. Renvoie null si invalide.
     */
    fun frToPbUtc(text: String, endOfDay: Boolean): String? = try {
        val date = LocalDate.parse(text.trim(), FR_PARSE)
        val heure = if (endOfDay) LocalTime.of(23, 59, 59) else LocalTime.MIDNIGHT
        ZonedDateTime.of(date, heure, ZoneId.systemDefault())
            .withZoneSameInstant(ZoneOffset.UTC)
            .format(PB_FORMAT) + "Z"
    } catch (e: Exception) {
        null
    }

    /** Vrai si la date PocketBase (UTC) est déjà passée. */
    fun isPast(pbDate: String): Boolean = try {
        LocalDateTime.parse(pbDate.take(19).replace(' ', 'T')).isBefore(LocalDateTime.now(ZoneOffset.UTC))
    } catch (e: Exception) {
        false
    }

    /** Affiche une date PocketBase (UTC) en JJ/MM/AAAA, heure locale. */
    fun dateFr(pbDate: String): String = try {
        LocalDateTime.parse(pbDate.take(19).replace(' ', 'T'))
            .atZone(ZoneOffset.UTC)
            .withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDate()
            .format(FR_OUT)
    } catch (e: Exception) {
        ""
    }
}