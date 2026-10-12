package ee.cyber.wallet.domain.provider.ageverification

import ee.cyber.wallet.domain.credentials.CredentialType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * EE-POA-026 to EE-POA-029 on the JVM: the age reckoning of TsÜS §§ 135–136, the 7-day validity
 * epoch, incomplete dates of birth, and the wallet-side renewal schedule.
 */
class AgeReckoningTest {

    @Test
    fun `an age is attained from the start of the anniversary`() {
        val born = LocalDate(2008, 5, 10)
        assertFalse(AgeReckoning.isOver(born, 18, LocalDate(2026, 5, 9)))
        assertTrue(AgeReckoning.isOver(born, 18, LocalDate(2026, 5, 10)))
    }

    @Test
    fun `a 29 February birthday falls on 28 February in a common year`() {
        // TsÜS § 136(4): the last day of the month that lacks the corresponding date.
        val born = LocalDate(2008, 2, 29)
        assertEquals(LocalDate(2026, 2, 28), AgeReckoning.anniversary(born, 18))
        assertFalse(AgeReckoning.isOver(born, 18, LocalDate(2026, 2, 27)))
        assertTrue(AgeReckoning.isOver(born, 18, LocalDate(2026, 2, 28)))
        assertEquals(LocalDate(2028, 2, 29), AgeReckoning.anniversary(born, 20))
    }

    @Test
    fun `the register day is the Tallinn date, not UTC`() {
        // 22:30 UTC on 9 May is already 10 May in Tallinn (UTC+3 in summer).
        assertEquals(LocalDate(2026, 5, 10), AgeReckoning.today(Instant.parse("2026-05-09T22:30:00Z")))
    }

    @Test
    fun `an incomplete date of birth is read as the latest consistent date`() {
        assertEquals(LocalDate(1990, 12, 31), AgeReckoning.effectiveBirthDate(1990, null, null))
        assertEquals(LocalDate(2008, 2, 29), AgeReckoning.effectiveBirthDate(2008, 2, null))
        assertEquals(LocalDate(2007, 2, 28), AgeReckoning.effectiveBirthDate(2007, 2, null))
        assertEquals(LocalDate(2007, 6, 3), AgeReckoning.effectiveBirthDate(2007, 6, 3))
        assertNull(AgeReckoning.effectiveBirthDate(null, 6, 3))
    }

    @Test
    fun `validity is floored to a common 7-day epoch`() {
        val days = (0 until 7).map { LocalDate(2026, 10, 12).let { d -> LocalDate.fromEpochDays(d.toEpochDays() + it) } }
        val floors = days.map { AgeIssuanceConstants.epochFloor(it) }.toSet()
        // Every day of one epoch shares the floor, and the floor never lies after the day.
        assertTrue(floors.size <= 2)
        days.forEach { assertTrue(AgeIssuanceConstants.epochFloor(it) <= it) }
        assertEquals(0L, Math.floorMod(AgeIssuanceConstants.epochFloor(days[0]).toEpochDays(), 7L))
        val (from, until) = AgeIssuanceConstants.validity(LocalDate(2026, 10, 12))
        assertTrue(until.toEpochDays() - from.toEpochDays() <= AgeIssuanceConstants.EE_POA_MAX_VALIDITY_DAYS)
        assertEquals(0L, Math.floorMod(until.toEpochDays(), 7L))
    }

    private fun held(type: CredentialType, until: LocalDate?, vararg falseAt: Int) =
        AgeAttestationRenewal.Held(type, until, falseAt.toList())

    @Test
    fun `renewal is due 14 days before validUntil`() {
        val batch = List(3) { held(CredentialType.EE_POA, LocalDate(2026, 12, 31)) }
        assertFalse(AgeAttestationRenewal.isDue(batch, null, LocalDate(2026, 12, 16)))
        assertTrue(AgeAttestationRenewal.isDue(batch, null, LocalDate(2026, 12, 17)))
    }

    @Test
    fun `renewal is due on the birthday a false predicate turns true`() {
        val batch = List(3) { held(CredentialType.EE_POA, LocalDate(2027, 6, 1), 18) }
        val born = LocalDate(2008, 2, 29)
        assertFalse(AgeAttestationRenewal.isDue(batch, born, LocalDate(2026, 2, 27)))
        assertTrue(AgeAttestationRenewal.isDue(batch, born, LocalDate(2026, 2, 28)))
    }

    @Test
    fun `renewal is due when a batch is down to its last attestation`() {
        val held = listOf(held(CredentialType.AGE_VERIFICATION, LocalDate(2027, 6, 1))) +
            List(3) { held(CredentialType.EE_POA, LocalDate(2027, 6, 1)) }
        assertTrue(AgeAttestationRenewal.isDue(held, null, LocalDate(2026, 10, 12)))
        assertFalse(AgeAttestationRenewal.isDue(emptyList(), null, LocalDate(2026, 10, 12)))
    }
}
