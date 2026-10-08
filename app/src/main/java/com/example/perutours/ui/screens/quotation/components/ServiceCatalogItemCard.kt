package com.example.perutours.ui.screens.quotation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.example.perutours.data.model.CatalogService

@Composable
fun ServiceCatalogItemCard(
    catalogService: CatalogService,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFEF3C7) else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFFD97706) else Color(0xFFE5E7EB)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${catalogService.city.uppercase()} • ${catalogService.category.uppercase()}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB45309)
                )
                Text(
                    text = catalogService.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = catalogService.description,
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$${catalogService.unitCost} USD",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) Color(0xFFB45309) else Color(0xFF374151)
            )
        }
    }
}