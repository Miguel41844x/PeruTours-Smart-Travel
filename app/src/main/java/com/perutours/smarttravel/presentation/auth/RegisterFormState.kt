package com.perutours.smarttravel.presentation.auth

enum class RegisterStep(val label: String) {
    PERSONAL_DATA("Datos Personales"),
    ROLE("Rol"),
    PREFERENCES("Preferencias")
}

data class RegisterFieldState(
    val value: String = "",
    val error: String? = null
)

data class RegisterFormState(
    val currentStep: RegisterStep = RegisterStep.PERSONAL_DATA,
    val fullName: RegisterFieldState = RegisterFieldState(),
    val email: RegisterFieldState = RegisterFieldState(),
    val phone: RegisterFieldState = RegisterFieldState(),
    val password: RegisterFieldState = RegisterFieldState(),
    val isPasswordVisible: Boolean = false,
    val selectedRole: TouristRole? = null,
    val selectedPreferences: Set<TravelPreference> = emptySet()
)

enum class TouristRole(val title: String, val description: String) {
    CLIENTE("Cliente / Turista", "Experiencias de viaje personalizadas. Explora destinos peruanos, crea solicitudes de viaje, aprueba cotizaciones y gestiona pagos."),
    ATENCION("Personal de Atención Turística", "Primer contacto y asesoría. Gestiona la bandeja de solicitudes entrantes, asesora a los clientes y deriva requerimientos a los agentes."),
    AGENTE("Agente Turístico", "Especialista en cotizaciones y reservas. Diseña paquetes a medida, calcula costos y márgenes, envía cotizaciones y gestiona reservas activas."),
    ADMIN("Administrador", "Operaciones y finanzas. Controla el catálogo de proveedores (hoteles, transporte, guías) y valida comprobantes de pago de clientes."),
    GERENTE("Gerente Comercial", "Inteligencia de negocio y KPIs. Monitorea el dashboard analítico en vivo, tendencias de ventas, tasas de conversión y métricas comerciales.")
}

enum class TravelPreference(val label: String) {
    ARQUEOLOGIA("Arqueología e Historia Inca"),
    TREKKING("Trekking y Montaña"),
    GASTRONOMIA("Gastronomía Peruana VIP"),
    HOTELES("Hoteles Boutique y Spa"),
    AMAZONIA("Amazonía y Ecoturismo"),
    COSTAS("Costas y Playas del Norte"),
    PAREJA("Viaje en Pareja"),
    FAMILIAR("Familiar con Niños")
}
