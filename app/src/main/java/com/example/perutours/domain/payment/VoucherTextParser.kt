package com.example.perutours.domain.payment

import java.math.BigDecimal
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal object VoucherTextParser {
    private val accents = Regex("\\p{M}+")
    private val money = Regex(
        """(?<![A-Z0-9])(S\s*/\.?|PEN|USD|US\s*\$)\s*:?\s*(\d+(?:[., ]\d+)*)(?![A-Z0-9.,])"""
    )
    private val amountLabel = Regex(
        """^(?:MONTO|IMPORTE|TOTAL)(?:\s*:|\s*$|\s+(?:PAGADO|TRANSFERIDO|ENVIADO|RECIBIDO|ABONADO|DE LA (?:OPERACION|TRANSFERENCIA))\b)"""
    )
    private val unrelatedAmount = Regex("""^(?:COMISION(?:ES)?|SALDO|ITF)\b""")
    private val operationLabel = Regex(
        """\b(?:N[°º.]{0,2}\s*|(?:NRO\.?|NUMERO|CODIGO)\s+)(?:DE\s+)?OPERACION\b"""
    )
    private val operationCode = Regex("""[A-Z0-9-]{4,24}""")
    private val plainNumber = Regex("""\d+(?:[.,]\d{2})?""")
    private val groupedWithComma = Regex("""\d{1,3}(?:,\d{3})+(?:\.\d{2})?""")
    private val groupedWithDot = Regex("""\d{1,3}(?:\.\d{3})+(?:,\d{2})?""")
    private val groupedWithSpace = Regex("""\d{1,3}(?: \d{3})+(?:[.,]\d{2})?""")
    private val currencyOnly = Regex("""^(?:S\s*/\.?|PEN|USD|US\s*\$)\s*:?\s*$""")
    private val currencyAtEnd = Regex("""(?:S\s*/\.?|PEN|USD|US\s*\$)\s*:?\s*$""")
    private val calendarDate = Regex("""(?:\d{4}-\d{1,2}-\d{1,2}|\d{1,2}-\d{1,2}-\d{4})""")
    private val currencyLabel = Regex("""^MONEDA(?:\s*:|\s*$)""")
    private val dollarSymbolConfusion = Regex("""(?<![A-Z0-9])US\s*S(?=\s*:?\s*\d)""")
    private val nonAmountLabel = Regex(
        """^(?:MONEDA|CUENTA|FECHA|HORA|DESTINO|TIPO DE OPERACION|CODIGO DE SEGURIDAD|NRO\.?\s+DE CELULAR)\b"""
    )

    fun parse(text: String, positionedLines: List<VoucherTextLine> = emptyList()): VoucherOcrResult {
        val layout = positionedLines.filter(VoucherTextLine::hasBounds)
            .map { it.copy(text = normalize(it.text)) }
        return if (layout.isEmpty()) parseText(text) else parseLayout(layout)
    }

    private fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(accents, "")
        .uppercase(Locale.ROOT)
        .replace('№', 'N')
        .replace('\u00a0', ' ')
        .trim()

    /** Respaldo cuando el proveedor de OCR no entrega posiciones. */
    private fun parseText(text: String): VoucherOcrResult {
        val lines = text
            .lineSequence()
            .map(::normalize)
            .filter(String::isNotBlank)
            .toList()
        val amounts = mutableSetOf<VoucherAmount>()
        val labeledAmounts = mutableSetOf<VoucherAmount>()
        val operations = mutableSetOf<String>()

        lines.forEachIndexed { index, line ->
            val lineAmounts = readPaymentAmounts(line)
            amounts += lineAmounts
            if (amountLabel.containsMatchIn(line)) {
                labeledAmounts += lineAmounts.ifEmpty {
                    readPaymentAmounts(lines.getOrNull(index + 1).orEmpty())
                }
            }

            operationLabel.find(line)?.let { label ->
                val afterLabel = line.substring(label.range.last + 1).trimStart(' ', ':', '#')
                val candidate = afterLabel.ifBlank { lines.getOrNull(index + 1).orEmpty() }
                readOperation(candidate)?.let(operations::add)
            }
        }

        val dateTime = VoucherDateTimeParser.parse(text)
        return VoucherOcrResult(
            amount = labeledAmounts.ifEmpty { amounts }.singleOrNull(),
            operationNumber = operations.singleOrNull(),
            transactionDate = dateTime.date,
            transactionTime = dateTime.time
        )
    }

    private fun parseLayout(lines: List<VoucherTextLine>): VoucherOcrResult {
        val operations = lines.mapNotNull { label ->
            val match = operationLabel.find(label.text) ?: return@mapNotNull null
            val inline = label.text.substring(match.range.last + 1)
            readOperation(inline) ?: nearestValues(label, lines, ::readOperation).singleOrNull()
        }.toSet()
        val currency = readCurrencyField(lines)
        val amounts = lines.flatMap { line ->
            val inline = readLayoutAmounts(line, lines, currency).map { AmountAt(it, line) }
            if (inline.isNotEmpty()) inline else splitCurrencyAmounts(line, lines)
        }
        val classified = amounts.map { candidate ->
            candidate to closestAmountLabel(candidate.line, lines)
        }
        val labeled = classified.filter { it.second == AmountLabel.PAYMENT }.map { it.first.amount }.toSet()
        val remaining = classified.filter { it.second != AmountLabel.OTHER }.map { it.first.amount }.toSet()
        val dateTime = VoucherDateTimeParser.parse("", lines)
        return VoucherOcrResult(
            amount = labeled.ifEmpty { remaining }.singleOrNull(),
            operationNumber = operations.singleOrNull(),
            transactionDate = dateTime.date,
            transactionTime = dateTime.time
        )
    }

    private fun readCurrencyField(lines: List<VoucherTextLine>): String? = lines.mapNotNull { label ->
        val match = currencyLabel.find(label.text) ?: return@mapNotNull null
        readCurrency(label.text.substring(match.range.last + 1))
            ?: nearestValues(label, lines, ::readCurrency).singleOrNull()
    }.toSet().singleOrNull()

    private fun readCurrency(text: String): String? = when (text.trim(' ', ':')) {
        "USD", "US$", "US $", "DOLARES", "DOLARES AMERICANOS" -> "USD"
        "PEN", "S/", "S/.", "SOL", "SOLES" -> "PEN"
        else -> null
    }

    private fun readLayoutAmounts(
        line: VoucherTextLine,
        lines: List<VoucherTextLine>,
        currency: String?
    ): List<VoucherAmount> {
        // En capturas pequeñas ML Kit puede leer US$ como USS. Se admite
        // esa confusión solo junto a Monto/Importe y con Moneda: Dólares
        // confirmado en el mismo comprobante; nunca se inventan decimales.
        val text = if (currency == "USD" && closestAmountLabel(line, lines) == AmountLabel.PAYMENT) {
            dollarSymbolConfusion.replace(line.text) { "US$" }
        } else line.text
        return readPaymentAmounts(text)
    }

    private fun readOperation(text: String): String? {
        val code = text.trim(' ', ':', '#')
        return code.takeIf {
            operationCode.matches(it) && it.any(Char::isDigit) && !calendarDate.matches(it)
        }
    }

    /**
     * Una columna puede ser reconocida completa antes que la otra. El orden
     * textual no vincula los datos; se usa la misma fila o un valor cercano
     * debajo de la etiqueta. Dos candidatos distintos se dejan sin resolver.
     */
    private fun <T> nearestValues(
        label: VoucherTextLine,
        lines: List<VoucherTextLine>,
        read: (String) -> T?
    ): Set<T> {
        val candidates = lines.filter { it !== label }.mapNotNull { candidate ->
            val placement = placement(label, candidate) ?: return@mapNotNull null
            read(candidate.text)?.let { placement to it }
        }
        val best = candidates.minOfOrNull { it.first } ?: return emptySet()
        return candidates.filter { it.first == best }.map { it.second }.toSet()
    }

    private fun splitCurrencyAmounts(
        currency: VoucherTextLine,
        lines: List<VoucherTextLine>
    ): List<AmountAt> {
        if (closestAmountLabel(currency, lines) == AmountLabel.OTHER) return emptyList()
        if (!currencyOnly.matches(currency.text) && !amountLabel.containsMatchIn(currency.text)) {
            return emptyList()
        }
        val prefix = currencyAtEnd.find(currency.text)?.value ?: return emptyList()
        val candidates = lines.filter { it !== currency }.mapNotNull { number ->
            val position = placement(currency, number) ?: return@mapNotNull null
            val horizontalGap = number.left - currency.right
            if (position == SAME_ROW && horizontalGap > max(currency.height, number.height) * 4) {
                return@mapNotNull null
            }
            val value = readPaymentAmounts(prefix + " " + number.text).singleOrNull()
                ?: return@mapNotNull null
            if (parseAmount(number.text) == null) return@mapNotNull null
            Triple(position, value, number)
        }
        val best = candidates.minOfOrNull { it.first } ?: return emptyList()
        return candidates.filter { it.first == best }.map { AmountAt(it.second, it.third) }
    }

    private fun closestAmountLabel(value: VoucherTextLine, lines: List<VoucherTextLine>): AmountLabel? {
        val matches = lines.mapNotNull { label ->
            val kind = when {
                amountLabel.containsMatchIn(label.text) -> AmountLabel.PAYMENT
                unrelatedAmount.containsMatchIn(label.text) ||
                    operationLabel.containsMatchIn(label.text) ||
                    nonAmountLabel.containsMatchIn(label.text) -> AmountLabel.OTHER
                else -> return@mapNotNull null
            }
            val position = if (label === value) INLINE else placement(label, value)
            position ?: return@mapNotNull null
            val distance = if (position == SAME_ROW) {
                abs(value.left - label.right).toDouble()
            } else {
                abs(value.top - label.bottom).toDouble()
            }
            LabelAt(kind, position, distance)
        }
        return matches.minWithOrNull(compareBy(LabelAt::placement, LabelAt::distance))?.kind
    }

    private fun placement(label: VoucherTextLine, value: VoucherTextLine): Int? {
        val height = max(label.height, value.height).toDouble()
        val overlap = min(label.bottom, value.bottom) - max(label.top, value.top)
        if (value.left >= label.right - height * 0.2 &&
            (overlap >= min(label.height, value.height) * 0.45 ||
                abs(label.centerY - value.centerY) <= height * 0.4)
        ) return SAME_ROW
        val gap = value.top - label.bottom
        val horizontalOverlap = min(label.right, value.right) - max(label.left, value.left)
        if (gap >= 0 && gap <= height * 1.5 &&
            (horizontalOverlap > 0 || abs(value.left - label.left) <= height)
        ) return BELOW
        return null
    }

    private fun readPaymentAmounts(line: String): List<VoucherAmount> {
        if (unrelatedAmount.containsMatchIn(line)) return emptyList()
        return money.findAll(line).mapNotNull(::readAmount).toList()
    }

    private fun readAmount(match: MatchResult): VoucherAmount? {
        val (token, digits) = match.destructured
        val amount = parseAmount(digits) ?: return null
        val currency = if (token == "PEN" || token.startsWith("S")) "PEN" else "USD"
        return VoucherAmount(amount, currency)
    }

    private fun parseAmount(value: String): BigDecimal? {
        val canonical = when {
            plainNumber.matches(value) -> value.replace(',', '.')
            groupedWithComma.matches(value) -> value.replace(",", "")
            groupedWithDot.matches(value) -> value.replace(".", "").replace(',', '.')
            groupedWithSpace.matches(value) -> value.replace(" ", "").replace(',', '.')
            else -> return null
        }
        return canonical.toBigDecimalOrNull()
            ?.takeIf { it.signum() > 0 && it.precision() - it.scale() <= 9 }
            ?.setScale(2)
    }

    private data class AmountAt(val amount: VoucherAmount, val line: VoucherTextLine)
    private data class LabelAt(val kind: AmountLabel, val placement: Int, val distance: Double)
    private enum class AmountLabel { PAYMENT, OTHER }
    private const val INLINE = 0
    private const val SAME_ROW = 1
    private const val BELOW = 2
}
