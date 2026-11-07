package com.example.perutours.ui.screens.chat

object ChatMessageValidator {

    const val MAX_LENGTH = 500

    fun normalize(input: String): String = input.trim()

    /** Un mensaje vacío o solo con espacios no se puede enviar. */
    fun isSendable(input: String): Boolean = input.isNotBlank()

    fun limit(input: String): String = input.take(MAX_LENGTH)
}