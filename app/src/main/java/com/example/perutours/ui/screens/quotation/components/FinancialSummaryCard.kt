package com.example.perutours.ui.screens.quotation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FinancialSummaryCard(
    baseCost: Double,
    marginPercent: Int,
    onMarginChange: (Int) -> Unit,
    totalAmount: Double,
    travelerCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Estructura Financiera y Rentabilidad",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Costo Base Neto de Proveedores:", fontSize = 12.sp, color = Color(0xFF4B5563))
                Text("$$baseCost USD", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Margen de Ganancia de Agencia:", fontSize = 12.sp, color = Color(0xFF4B5563))
                Text("+$marginPercent%", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFD97706))
            }

            Slider(
                value = marginPercent.toFloat(),
                onValueChange = { onMarginChange(it.toInt()) },
                valueRange = 5f..35f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFD97706),
                    activeTrackColor = Color(0xFFD97706)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tarjeta de Total
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1917), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL COTIZADO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9CA3AF)
                        )
                        Text(
                            text = "(${travelerCount} viajeros • $${Math.round(totalAmount / travelerCount.coerceAtLeast(1))} c/u)",
                            fontSize = 11.sp,
                            color = Color(0xFFFBBF24)
                        )
                    }
                    Text(
                        text = "$$totalAmount USD",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFBBF24)
                    )
                }
            }
        }
    }
}