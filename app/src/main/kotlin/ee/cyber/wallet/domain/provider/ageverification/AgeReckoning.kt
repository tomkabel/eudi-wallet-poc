package ee.cyber.wallet.domain.provider.ageverification

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toLocalDateTime
import java.time.YearMonth
import kotlin.time.Instant

/**
 * How the Provider computes an `age_over_NN` predicate (EE-POA-027, EE-POA-028).
 *
 * TsÜS § 135(1) and § 136(2): a period counted in years ends on the corresponding month and day of
 * the last year, and the person has attained the age from the start of that day. § 136(4): where
 * that month has no such day, the period ends on the month's last day, so a 29 February birthday
 * falls on 28 February in a common year. `java.time.LocalDate.plusYears` clamps the same way.
 */
object AgeReckoning {

    val TALLINN: TimeZone = TimeZone.of("Europe/Tallinn")

    /** The register day in Estonia at [now], from the Provider's own clock (EE-POA-027). */
    fun today(now: Instant): LocalDate = now.toLocalDateTime(TALLINN).date

    /** The day a person born on [birthDate] attains [years]. */
    fun anniversary(birthDate: LocalDate, years: Int): LocalDate =
        birthDate.toJavaLocalDate().plusYears(years.toLong()).toKotlinLocalDate()

    fun isOver(birthDate: LocalDate, years: Int, today: LocalDate): Boolean =
        today >= anniversary(birthDate, years)

    /**
     * EE-POA-028: an incomplete register date of birth is read as the latest date consistent with
     * it — a year alone as 31 December, a year and month as the month's last day. Null when the
     * year is absent: no EE-PoA is issued then.
     */
    fun effectiveBirthDate(year: Int?, month: Int?, day: Int?): LocalDate? {
        if (year == null) return null
        if (month == null) return LocalDate(year, 12, 31)
        return LocalDate(year, month, day ?: YearMonth.of(year, month).lengthOfMonth())
    }
}
