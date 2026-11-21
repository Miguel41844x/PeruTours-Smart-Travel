package com.example.perutours.data.model

enum class UserRole(val wireValue: String, val startRoute: String, val label: String) {
    CLIENT("cliente", "home", "Cliente"),
    AGENT("agente", "agent_home", "Agente"),
    ADMIN("administrador", "admin_dashboard", "Administrador"),
    MANAGER("gerente", "sales_dashboard", "Gerente"),
    UNKNOWN("", "", "Rol no reconocido");

    companion object {
        fun fromWire(value: String): UserRole = entries.firstOrNull {
            it != UNKNOWN && it.wireValue.equals(value.trim(), ignoreCase = true)
        } ?: UNKNOWN
    }
}
