package com.example.perutours.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// PALETA PRINCIPAL PERUTOURS (Material 3)
// ==========================================

// --- Tema Claro (Light Theme - 40s) ---
// Primario: Dorado Inca / Ámbar Cálido (Botones principales, marca, destacados)
val PeruGold40 = Color(0xFFD97706)          // Amber 600
val PeruGoldContainerLight = Color(0xFFFEF3C7) // Amber 100 suave para tarjetas y chips
val OnPeruGoldContainerLight = Color(0xFF78350F) // Ámbar oscuro legible

// Secundario: Terracota / Ocre Perú (Acentos de aventura, itinerarios)
val PeruTerracotta40 = Color(0xFFEA580C)    // Orange 600
val PeruTerracottaContainerLight = Color(0xFFFFEDD5)

// Terciario: Verde Valle Sagrado / Esmeralda (Naturaleza, estados aprobados, atención)
val PeruEmerald40 = Color(0xFF059669)       // Emerald 600
val PeruEmeraldContainerLight = Color(0xFFD1FAE5)

// Neutros y Superficies (Inspirados en piedra andina / Stone)
val BackgroundLight = Color(0xFFFAFAF9)     // Blanco cálido piedra (evita blanco hospitalario)
val SurfaceLight = Color(0xFFFFFFFF)        // Fondo de tarjetas táctiles elevadas
val SurfaceVariantLight = Color(0xFFF5F5F4) // Stone 100 para divisores y campos de texto
val OnSurfaceLight = Color(0xFF1C1917)      // Stone 900 (texto principal de alto contraste)
val OnSurfaceVariantLight = Color(0xFF78716C) // Stone 500 (subtítulos y detalles)


// --- Tema Oscuro (Dark Theme - 80s / Splash) ---
val PeruGold80 = Color(0xFFFBBF24)          // Amber 400 (brillante para fondo oscuro)
val PeruGoldContainerDark = Color(0xFF78350F)

val PeruTerracotta80 = Color(0xFFFB923C)    // Orange 400
val PeruTerracottaContainerDark = Color(0xFF7C2D12)

val PeruEmerald80 = Color(0xFF34D399)       // Emerald 400
val PeruEmeraldContainerDark = Color(0xFF064E3B)

val BackgroundDark = Color(0xFF1C1917)      // Stone 900 (fondo noche andina como el Splash)
val SurfaceDark = Color(0xFF292524)         // Stone 800 para tarjetas en modo noche
val SurfaceVariantDark = Color(0xFF44403C)  // Stone 700
val OnSurfaceDark = Color(0xFFF5F5F4)
val OnSurfaceVariantDark = Color(0xFFA8A29E)


// ==========================================
// COLORES SEMÁNTICOS (Estados y Roles)
// ==========================================
val StatusPending = Color(0xFFD97706)       // Ámbar: Pendiente de cotización / pago
val StatusApproved = Color(0xFF16A34A)      // Verde: Confirmada / Pagada
val StatusReview = Color(0xFF0284C7)        // Azul: En revisión / Derivada a agente
val StatusCancelled = Color(0xFFDC2626)     // Rojo: Cancelada / Rechazada

// Colores de tarjetas de selección de rol (al registrarse)
val RoleTouristAccent = Color(0xFFD97706)   // Ámbar
val RoleSupportAccent = Color(0xFF059669)   // Esmeralda
val RoleAgentAccent = Color(0xFF0284C7)     // Azul cielo
val RoleAdminAccent = Color(0xFF9333EA)     // Púrpura administrativo
val RoleManagerAccent = Color(0xFF4F46E5)   // Índigo analítico