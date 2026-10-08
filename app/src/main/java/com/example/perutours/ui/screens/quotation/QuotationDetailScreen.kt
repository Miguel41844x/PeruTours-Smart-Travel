package com.example.perutours.ui.screens.quotation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.perutours.data.model.Quotation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationDetailScreen(
    quotation: Quotation,
    onNavigateBack: () -> Unit,
    onAcceptQuotation: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cotización #${quotation.id.takeLast(6)}", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Button(
                    onClick = { onAcceptQuotation(quotation.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 8.dp
                        )
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669)
                    )
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        "Aceptar Cotización y Generar Reserva",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = quotation.destination,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1F2937)
                    )
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = quotation.status, // "Cotizado"
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Asesor Turístico: ${quotation.agentName} • Vigencia: ${quotation.validityDate}",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }

            item {
                Text(
                    text = "Servicios Turísticos Incluidos (${quotation.services.size}):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(quotation.services) { srv ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(srv.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Cant: ${srv.quantity} • Cat: ${srv.category}", fontSize = 11.sp, color = Color(0xFF6B7280))
                        }
                        Text("$${srv.subtotal} USD", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1917), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("PRECIO TOTAL PAQUETE", color = Color(0xFF9CA3AF), fontWeight = FontWeight.Bold)
                        Text("$${quotation.totalAmount} USD", color = Color(0xFFFBBF24), fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            item {
                Text("Condiciones Comerciales:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(quotation.conditions, fontSize = 11.sp, color = Color(0xFF4B5563))
            }
        }
    }
}