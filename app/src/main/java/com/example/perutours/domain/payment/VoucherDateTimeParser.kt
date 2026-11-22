package com.example.perutours.domain.payment

import java.text.Normalizer
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Fecha de operación en formato peruano; no utiliza el reloj del dispositivo. */
internal object VoucherDateTimeParser {
    data class Reading(val date: VoucherDate?, val time: VoucherTime?)

    private val numericDate = Regex("""(?<![A-Z0-9])(\d{1,2})[/.-](\d{1,2})[/.-](\d{4})(?!\d)""")
    private val isoDate = Regex("""(?<![A-Z0-9])(\d{4})[-/](\d{1,2})[-/](\d{1,2})(?!\d)""")
    private val confusedNumericDate = Regex(
        """(?<![A-Z0-9])([0-9O]{1,2})[/.-]([0-9O]{1,2})[/.-]([0-9O]{4})(?![A-Z0-9])"""
    )
    private val namedDate = Regex(
        """(?<![A-Z0-9])(\d{1,2})\s*(?:DE\s+)?([A-Z]+)\.?\s*(?:DE\s+)?(\d{4})(?!\d)"""
    )
    private val clock = Regex(
        """(?:(?<![A-Z0-9])|(?<=\dT))(\d{1,2}):(\d{2})(?::(\d{2}))?(?:\s*([AP])\s*\.?\s*M\.?)?(?![\d:])"""
    )
    private val meridiemOnly = Regex("""^([AP])\s*\.?\s*M\.?$""")
    private val spacedClock = Regex(
        """(?<![A-Z0-9])(\d{1,2})[ \t]+(\d{2})[ \t]*([AP])\s*\.?\s*M\.?(?![A-Z0-9])"""
    )
    private val dateLabel = Regex(
        """^FECHA(?:\s*(?:Y|/)\s*HORA)?(?:\s+(?:DE|DEL)\s+(?:LA\s+)?(?:OPERACION|PROCESO|PAGO|TRANSFERENCIA|TRANSACCION))?(?:\s*:|\s*$)"""
    )
    private val timeLabel = Regex(
        """^(?:HORA(?:\s+(?:DE|DEL)\s+(?:LA\s+)?(?:OPERACION|PROCESO|PAGO|TRANSFERENCIA|TRANSACCION))?|FECHA\s*(?:Y|/)\s*HORA)(?:\s*:|\s*$)"""
    )
    private val unrelatedDate = Regex(
        """^(?:FECHA\s+(?:DE\s+)?(?:VENCIMIENTO|NACIMIENTO|IMPRESION|EMISION)|ULTIM[AO]\s+(?:ACCESO|CONEXION)|VIGENCIA|VENCE|(?:NUMERO|NRO\.?)\s+(?:DE\s+)?OPERACION|CODIGO DE SEGURIDAD)\b"""
    )
    private val months = listOf(
        listOf("ENE", "ENERO"), listOf("FEB", "FEBRERO"), listOf("MAR", "MARZO"),
        listOf("ABR", "ABRIL"), listOf("MAY", "MAYO"), listOf("JUN", "JUNIO"),
        listOf("JUL", "JULIO"), listOf("AGO", "AGOSTO"),
        listOf("SEP", "SEPT", "SET", "SEPTIEMBRE", "SETIEMBRE"),
        listOf("OCT", "OCTUBRE"), listOf("NOV", "NOVIEMBRE"), listOf("DIC", "DICIEMBRE")
    ).flatMapIndexed { index, names -> names.map { it to index + 1 } }.toMap()

    fun parse(text: String, positionedLines: List<VoucherTextLine> = emptyList()): Reading {
        val layout = positionedLines.filter(VoucherTextLine::hasBounds)
        val lines = if (layout.isEmpty()) {
            text.lineSequence().map(String::trim).filter(String::isNotBlank)
                .mapIndexed { index, value -> Line(index, normalize(value), null) }.toList()
        } else {
            layout.mapIndexed { index, value -> Line(index, normalize(value.text), value) }
        }
        val dateLabels = lines.filter { dateLabel.containsMatchIn(it.text) && !isUnrelated(it) }
        val dates = lines.flatMap { line ->
            datesIn(line.text, dateLabels.any { association(it, line) != null }).map { DateAt(it, line) }
        }
            .filterNot { candidate -> associatedWithUnrelatedLabel(candidate.line, lines) }
        val eligibleDates = if (dateLabels.isEmpty()) dates else dates.filter { candidate ->
            dateLabels.any { association(it, candidate.line) != null }
        }
        val date = eligibleDates.map(DateAt::date).toSet().singleOrNull()
        val datesChosen = eligibleDates.filter { it.date == date }
        val times = lines.flatMap { line -> timesIn(line, lines).map { TimeAt(it, line) } }
            .filterNot { candidate -> associatedWithUnrelatedLabel(candidate.line, lines) }
        val timeLabels = lines.filter { timeLabel.containsMatchIn(it.text) && !isUnrelated(it) }
        val eligibleTimes = if (timeLabels.isNotEmpty()) {
            times.filter { candidate -> timeLabels.any { association(it, candidate.line) != null } }
        } else {
            times.filter { candidate -> datesChosen.any { datesAreNear(it.line, candidate.line) } }
        }
        return Reading(date, eligibleTimes.map(TimeAt::time).toSet().singleOrNull())
    }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").uppercase(Locale.ROOT).replace('\u00a0', ' ').trim()

    private fun datesIn(text: String, allowCorrections: Boolean = false): Set<VoucherDate> {
        val numeric = numericDate.findAll(text).mapNotNull { match ->
            val (day, month, year) = match.destructured
            validDate(year.toInt(), month.toInt(), day.toInt())
        }
        val iso = isoDate.findAll(text).mapNotNull { match ->
            val (year, month, day) = match.destructured
            validDate(year.toInt(), month.toInt(), day.toInt())
        }
        val named = namedDate.findAll(text).mapNotNull { match ->
            val (day, name, year) = match.destructured
            months[name]?.let { validDate(year.toInt(), it, day.toInt()) }
        }
        // O/0 se normaliza únicamente en un campo identificado como fecha,
        // y el resultado sigue pasando por la validación estricta del calendario.
        val recovered = if (allowCorrections) confusedNumericDate.findAll(text).mapNotNull { match ->
            val (day, month, year) = match.groupValues.drop(1).map { it.replace('O', '0').toInt() }
            validDate(year, month, day)
        } else emptySequence()
        return (numeric + iso + named + recovered).toSet()
    }

    private fun validDate(year: Int, month: Int, day: Int): VoucherDate? {
        if (year < 1 || month !in 1..12 || day !in 1..31) return null
        return try {
            GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply {
                isLenient = false
                clear()
                set(year, month - 1, day)
            }.timeInMillis
            VoucherDate(year, month, day)
        } catch (_: IllegalArgumentException) { null }
    }

    private fun timesIn(line: Line, lines: List<Line>): List<VoucherTime> {
        val regular = clock.findAll(line.text).mapNotNull { match ->
            val hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toInt()
            val second = match.groupValues[3].takeIf(String::isNotEmpty)?.toInt()
            val inlineMarker = match.groupValues[4].takeIf(String::isNotEmpty)
            val markers = if (inlineMarker != null) setOf(inlineMarker) else lines.mapNotNull { other ->
                if (other.index == line.index || association(line, other) == null) null
                else meridiemOnly.matchEntire(other.text)?.groupValues?.get(1)
            }.toSet()
            if (markers.size > 1) null else validTime(hour, minute, second, markers.singleOrNull())
        }
        val identifiedTime = lines.any {
            !isUnrelated(it) && timeLabel.containsMatchIn(it.text) && association(it, line) != null
        }
        val identifiedDate = datesIn(line.text, true).isNotEmpty() && lines.any {
            !isUnrelated(it) && dateLabel.containsMatchIn(it.text) && association(it, line) != null
        }
        // Si el OCR pierde los dos puntos, se necesitan AM/PM y una etiqueta
        // de fecha/hora explícita. Números sueltos nunca se convierten en horas.
        val recovered = if (identifiedTime || identifiedDate) spacedClock.findAll(line.text).mapNotNull {
            validTime(it.groupValues[1].toInt(), it.groupValues[2].toInt(), null, it.groupValues[3])
        } else emptySequence()
        return (regular + recovered).toList()
    }

    private fun validTime(hour: Int, minute: Int, second: Int?, marker: String?): VoucherTime? {
        if (minute !in 0..59 || (second != null && second !in 0..59)) return null
        val converted = if (marker == null) {
            hour.takeIf { it in 0..23 }
        } else {
            hour.takeIf { it in 1..12 }?.let { it % 12 + if (marker == "P") 12 else 0 }
        } ?: return null
        return VoucherTime(converted, minute, second)
    }

    private fun isUnrelated(line: Line): Boolean = unrelatedDate.containsMatchIn(line.text)

    private fun associatedWithUnrelatedLabel(value: Line, lines: List<Line>): Boolean {
        if (isUnrelated(value)) return true
        val unrelated = lines.filter(::isUnrelated).mapNotNull { association(it, value) }.minOrNull()
            ?: return false
        val operation = lines.filter {
            !isUnrelated(it) && (dateLabel.containsMatchIn(it.text) || timeLabel.containsMatchIn(it.text))
        }.mapNotNull { association(it, value) }.minOrNull()
        return operation == null || unrelated <= operation
    }

    /** Etiqueta seguida de su valor, en la misma fila o justo debajo. */
    private fun association(label: Line, value: Line): Int? {
        if (label.index == value.index) return INLINE
        val left = label.bounds
        val right = value.bounds
        if (left == null || right == null) {
            return BELOW.takeIf { value.index == label.index + 1 }
        }
        val height = max(left.height, right.height).toDouble()
        if (right.left >= left.right - height * 0.2 && sameRow(left, right)) return SAME_ROW
        return BELOW.takeIf { isBelow(left, right) }
    }

    private fun datesAreNear(date: Line, time: Line): Boolean {
        if (date.index == time.index) return true
        val left = date.bounds
        val right = time.bounds
        if (left == null || right == null) return time.index == date.index + 1
        return sameRow(left, right) || isBelow(left, right)
    }

    private fun sameRow(left: VoucherTextLine, right: VoucherTextLine): Boolean {
        val overlap = min(left.bottom, right.bottom) - max(left.top, right.top)
        return overlap >= min(left.height, right.height) * 0.45 ||
            abs(left.centerY - right.centerY) <= max(left.height, right.height) * 0.4
    }

    private fun isBelow(top: VoucherTextLine, bottom: VoucherTextLine): Boolean {
        val height = max(top.height, bottom.height).toDouble()
        val gap = bottom.top - top.bottom
        val overlap = min(top.right, bottom.right) - max(top.left, bottom.left)
        return gap >= 0 && gap <= height * 1.5 &&
            (overlap > 0 || abs(bottom.left - top.left) <= height)
    }

    private data class Line(val index: Int, val text: String, val bounds: VoucherTextLine?)
    private data class DateAt(val date: VoucherDate, val line: Line)
    private data class TimeAt(val time: VoucherTime, val line: Line)
    private const val INLINE = 0
    private const val SAME_ROW = 1
    private const val BELOW = 2
}
