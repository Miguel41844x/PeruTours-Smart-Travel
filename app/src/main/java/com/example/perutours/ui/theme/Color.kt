package com.example.perutours.ui.theme

import androidx.compose.ui.graphics.Color

// Bosque, arena y arcilla: una misma identidad para catálogo, venta y operación.
// Se conservan los nombres anteriores para no romper las pantallas existentes.
val PeruBrick = Color(0xFF245440)
val PeruBrickDark = Color(0xFF173D2E)
val PeruBrickContainer = Color(0xFFDCECDF)
val OnPeruBrickContainer = Color(0xFF153C29)

val PeruMaize = Color(0xFFC19A56)
val PeruMaizeDark = Color(0xFF785A29)
val PeruMaizeContainer = Color(0xFFF3E6CA)
val OnPeruMaizeContainer = Color(0xFF30230E)

val PeruValley = Color(0xFF176B5B)
val PeruValleyDark = Color(0xFF0C5144)
val PeruValleyContainer = Color(0xFFA8F2DD)
val OnPeruValleyContainer = Color(0xFF002019)

val BackgroundLight = Color(0xFFF7F7F2)
val SurfaceLight = Color(0xFFFFFEFA)
val SurfaceVariantLight = Color(0xFFEBEEE6)
val OutlineLight = Color(0xFF748074)
val OutlineVariantLight = Color(0xFFDCE2D8)
val OnSurfaceLight = Color(0xFF203128)
val OnSurfaceVariantLight = Color(0xFF58665B)

val BackgroundDark = Color(0xFF101B16)
val SurfaceDark = Color(0xFF19251E)
val SurfaceVariantDark = Color(0xFF29372D)
val OutlineDark = Color(0xFF90958F)
val OutlineVariantDark = Color(0xFF414743)
val OnSurfaceDark = Color(0xFFE5E8E4)
val OnSurfaceVariantDark = Color(0xFFC2C7C2)

val StatusPending = Color(0xFF8A5A00)
val StatusApproved = Color(0xFF176B5B)
val StatusReview = Color(0xFF1F5F8B)
val StatusCancelled = Color(0xFFBA1A1A)

// Compatibility aliases for screens that still reference the original palette.
val PeruGold40 = PeruBrick
val PeruGold80 = Color(0xFFADD4B6)
val PeruGoldContainerLight = PeruBrickContainer
val OnPeruGoldContainerLight = OnPeruBrickContainer
val PeruGoldContainerDark = Color(0xFF2D503B)
val PeruTerracotta40 = PeruMaizeDark
val PeruTerracotta80 = Color(0xFFDFC18B)
val PeruTerracottaContainerLight = PeruMaizeContainer
val PeruTerracottaContainerDark = Color(0xFF664400)
val PeruEmerald40 = PeruValley
val PeruEmerald80 = Color(0xFF8BD8C5)
val PeruEmeraldContainerLight = PeruValleyContainer
val PeruEmeraldContainerDark = Color(0xFF005143)

val RoleTouristAccent = PeruBrick
val RoleSupportAccent = PeruValley
val RoleAgentAccent = StatusReview
val RoleAdminAccent = Color(0xFF745B91)
val RoleManagerAccent = Color(0xFF4F5F90)
