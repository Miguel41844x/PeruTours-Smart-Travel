package com.example.perutours.domain.payment

import java.math.BigDecimal
import java.text.Normalizer
import java.util.Locale

internal object VoucherTextParser {
    private val accents = Regex("\\p{M}+")
    private val money = Regex(
        """(?<![A-Z0-9])(S\s*/\.?|PEN|USD|US\s*\$)\s*:?\s*(\d+(?:[., ]\d+)*)(?![A-Z0-9.,])"""
    )
    private val amountLabel = Regex(
        """^(?:MONTO|IMPORTE|TOTAL)(?:\s*:|\s*$|\s+(?:PAGADO|TRANSFERIDO|ENVIADO|DE LA OPERACION)\b)"""
    )
    private val unrelatedAmount = Regex("""^(?:COMISION|SALDO|ITF)\b""")
    private val operationLabel = Regex(
        """\b(?:N[°º.]{0,2}\s*|(?:NRO\.?|NUMERO|CODIGO)\s+)(?:DE\s+)?OPERACION\b"""
    )
    private val operationCode = Regex("""[A-Z0-9-]{4,24}""")
    private val plainNumber = Regex("""\d+(?:[.,]\d{2})?""")
    private val groupedWithComma = Regex("""\d{1,3}(?:,\d{3})+(?:\.\d{2})?""")
    private val groupedWithDot = Regex("""\d{1,3}(?:\.\d{3})+(?:,\d{2})?""")
    private val groupedWithSpace = Regex("""\d{1,3}(?: \d{3})+(?:[.,]\d{2})?""")

    fun parse(text: String): VoucherOcrResult {
        val lines = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(accents, "")
            .uppercase(Locale.ROOT)
            .lineSequence()
            .map(String::trim)
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
                if (operationCode.matches(candidate) && candidate.any(Char::isDigit)) {
                    operations += candidate
                }
            }
        }

        return VoucherOcrResult(
            amount = labeledAmounts.ifEmpty { amounts }.singleOrNull(),
            operationNumber = operations.singleOrNull()
        )
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
}
