package com.example.perutours.domain.payment

/** Texto y posición reconocidos, independientes de Android y de ML Kit. */
internal data class VoucherTextLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val height: Int get() = bottom - top
    val centerY: Double get() = (top.toDouble() + bottom) / 2
    val hasBounds: Boolean get() = right > left && bottom > top
}
