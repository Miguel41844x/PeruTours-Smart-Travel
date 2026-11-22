package com.example.perutours.domain.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoucherDateTimeParserTest {
    @Test
    fun readsTheYapeDateAndConvertsTheExplicitPmMarker() {
        val reading = VoucherDateTimeParser.parse("21 nov. 2026 | 07:25 p. m.")
        assertEquals(VoucherDate(2026, 11, 21), reading.date)
        assertEquals(VoucherTime(19, 25), reading.time)
    }

    @Test
    fun readsFullSpanishMonthsAndCompactOcrSpacing() {
        for (text in listOf("21 de noviembre de 2026 7:25pm", "21nov.2026 19:25")) {
            val reading = VoucherDateTimeParser.parse(text)
            assertEquals(VoucherDate(2026, 11, 21), reading.date)
            assertEquals(VoucherTime(19, 25), reading.time)
        }
    }

    @Test
    fun supportsSeptemberSpellingsAndAbbreviations() {
        for (month in listOf("set", "sep.", "sept.", "septiembre", "setiembre")) {
            assertEquals(
                VoucherDate(2026, 9, 21),
                VoucherDateTimeParser.parse("21 " + month + " 2026").date
            )
        }
    }

    @Test
    fun readsNumericBcpDatesWithNoonAndIsoDatesWithSeconds() {
        val bcp = VoucherDateTimeParser.parse("Fecha de proceso: 27/08/2021 12:11 PM")
        assertEquals(VoucherDate(2021, 8, 27), bcp.date)
        assertEquals(VoucherTime(12, 11), bcp.time)
        val iso = VoucherDateTimeParser.parse("2026-11-21T19:25:33")
        assertEquals(VoucherDate(2026, 11, 21), iso.date)
        assertEquals(VoucherTime(19, 25, 33), iso.time)
    }

    @Test
    fun recoversOcrConfusionsInsideAnExplicitDateField() {
        val reading = VoucherDateTimeParser.parse("Fecha de proceso: 27/O8/2021 12 11 PM")
        assertEquals(VoucherDate(2021, 8, 27), reading.date)
        assertEquals(VoucherTime(12, 11), reading.time)
    }

    @Test
    fun doesNotRepairNumbersWithoutLabelsAndKeepsInvalidDatesUnresolved() {
        val unlabeled = VoucherDateTimeParser.parse("27/O8/2021 12 11 PM")
        assertNull(unlabeled.date)
        assertNull(unlabeled.time)
        assertNull(VoucherDateTimeParser.parse("Fecha de proceso: 31/O2/2026").date)
        assertNull(VoucherDateTimeParser.parse("12 11 PM").time)
    }

    @Test
    fun recoversAMissingTimeSeparatorOnlyWithTheHourLabelAndMeridiem() {
        val labeled = VoucherDateTimeParser.parse("Hora de operación: 07 25 PM")
        assertEquals(VoucherTime(19, 25), labeled.time)
        assertNull(VoucherDateTimeParser.parse("Hora de operación: 07 25").time)
        assertNull(VoucherDateTimeParser.parse("Hora de operación: 19 25 PM").time)
    }

    @Test
    fun accepts24HourTimesAndTheHourSuffixUsedByPlin() {
        val reading = VoucherDateTimeParser.parse("08 agosto 2024, 16:53 h")
        assertEquals(VoucherDate(2024, 8, 8), reading.date)
        assertEquals(VoucherTime(16, 53), reading.time)
    }

    @Test
    fun treatsTwelveAmAsMidnightAndTwelvePmAsNoon() {
        assertEquals(VoucherTime(0, 0), VoucherDateTimeParser.parse("21/11/2026 12:00am").time)
        assertEquals(VoucherTime(12, 0), VoucherDateTimeParser.parse("21/11/2026 12:00pm").time)
    }

    @Test
    fun validatesCalendarDatesIncludingLeapYears() {
        assertEquals(VoucherDate(2024, 2, 29), VoucherDateTimeParser.parse("29/02/2024").date)
        for (text in listOf("29/02/2025", "31/04/2026", "32/11/2026", "21/13/2026", "00/11/2026")) {
            assertNull(text, VoucherDateTimeParser.parse(text).date)
        }
    }

    @Test
    fun rejectsInvalidHoursMinutesSecondsAndMeridiemCombinations() {
        for (time in listOf("24:00", "19:60", "07:25:60", "14:25pm", "00:25am")) {
            val reading = VoucherDateTimeParser.parse("21/11/2026 " + time)
            assertEquals(VoucherDate(2026, 11, 21), reading.date)
            assertNull(time, reading.time)
        }
    }

    @Test
    fun retainsTheDateWithoutInventingATime() {
        val reading = VoucherDateTimeParser.parse("Fecha de operación: 21/11/2026")
        assertEquals(VoucherDate(2026, 11, 21), reading.date)
        assertNull(reading.time)
    }

    @Test
    fun doesNotTakeThePhoneClockAsTheTransactionTime() {
        val reading = parse(
            line("13:20", 30, 10, 100),
            line("21 nov. 2026", 50, 400, 220),
            line("07:25 p. m.", 400, 400, 210)
        )
        assertEquals(VoucherDate(2026, 11, 21), reading.date)
        assertEquals(VoucherTime(19, 25), reading.time)
        assertNull(parse(line("13:20", 30, 10, 100)).time)
    }

    @Test
    fun associatesLabelsAndValuesAcrossColumnsEvenWhenOcrOrderIsShuffled() {
        val reading = parse(
            line("07:25 p.m.", 500, 300, 210),
            line("21/11/2026", 500, 100, 210),
            line("Fecha de operación", 50, 100, 300),
            line("Hora de operación", 50, 300, 300)
        )
        assertEquals(VoucherDate(2026, 11, 21), reading.date)
        assertEquals(VoucherTime(19, 25), reading.time)
    }

    @Test
    fun associatesAPmMarkerRecognizedInAnotherColumn() {
        val reading = parse(
            line("21 nov. 2026", 50, 400, 240),
            line("07:25", 400, 400, 100),
            line("p. m.", 520, 400, 80)
        )
        assertEquals(VoucherTime(19, 25), reading.time)
    }

    @Test
    fun acceptsALabeledTimeEvenWhenTheDateIsMissing() {
        val reading = VoucherDateTimeParser.parse("Hora de operación:\n19:25")
        assertNull(reading.date)
        assertEquals(VoucherTime(19, 25), reading.time)
    }

    @Test
    fun favorsTheOperationDateOverExpiryAndPrintingDates() {
        val reading = parse(
            line("Fecha de vencimiento", 50, 100, 300),
            line("21/12/2026", 500, 100, 210),
            line("Fecha de operación", 50, 300, 300),
            line("21/11/2026 19:25", 500, 300, 350),
            line("Fecha de impresión", 50, 500, 300),
            line("22/11/2026 08:15", 500, 500, 350)
        )
        assertEquals(VoucherDate(2026, 11, 21), reading.date)
        assertEquals(VoucherTime(19, 25), reading.time)
    }

    @Test
    fun anInlineOperationLabelTakesPriorityOverTheExpiryLabelInThePreviousRow() {
        val reading = parse(
            line("Fecha de vencimiento: 21/12/2026", 50, 100, 750),
            line("Fecha de operación: 21/11/2026 19:25", 50, 140, 800)
        )
        assertEquals(VoucherDate(2026, 11, 21), reading.date)
        assertEquals(VoucherTime(19, 25), reading.time)
    }

    @Test
    fun leavesMultipleDatesOrTimesUnresolved() {
        assertNull(VoucherDateTimeParser.parse("21/11/2026\n22/11/2026").date)
        val reading = VoucherDateTimeParser.parse("21/11/2026 07:25 19:25")
        assertEquals(VoucherDate(2026, 11, 21), reading.date)
        assertNull(reading.time)
    }

    @Test
    fun doesNotReadCodesAsDatesOrBorrowATimeFromADistantRow() {
        assertNull(VoucherDateTimeParser.parse("Número de operación: 2026-11-21").date)
        assertNull(parse(
            line("21/11/2026", 50, 100, 220),
            line("19:25", 50, 500, 100)
        ).time)
        assertNull(VoucherDateTimeParser.parse("Codigo de seguridad: 1925\n28484182").date)
    }

    @Test
    fun integratesWithTheReceiptResultWhenOnlyDateAndTimeCanBeRead() {
        val result = VoucherTextParser.parse("21 nov. 2026 | 07:25 p. m.")
        assertEquals(VoucherDate(2026, 11, 21), result.transactionDate)
        assertEquals(VoucherTime(19, 25), result.transactionTime)
        assertEquals(true, result.hasData)
        assertNull(result.amount)
        assertNull(result.operationNumber)
    }

    private fun parse(vararg lines: VoucherTextLine) =
        VoucherDateTimeParser.parse(lines.joinToString("\n") { it.text }, lines.toList())

    private fun line(text: String, left: Int, top: Int, width: Int, height: Int = 30) =
        VoucherTextLine(text, left, top, left + width, top + height)
}
