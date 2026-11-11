
package com.timsales.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF10131A)
private val Panel = Color(0xFF1B202B)
private val Green = Color(0xFF55D6A0)
private val White = Color(0xFFF3F5F8)
private val Muted = Color(0xFF929BAA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TimSalesApp()
            }
        }
    }
}

@Composable
fun TimSalesApp() {
    var page by remember { mutableStateOf("Главная") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Bg
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(20.dp)
            ) {
                Text(
                    "TimSales",
                    color = Green,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(25.dp))

                when (page) {
                    "Главная" -> HomeScreen()
                    "Продажи" -> SimpleScreen("Продажи", "Здесь будет учёт твоих продаж")
                    "Долги" -> SimpleScreen("Долги", "Здесь будет список должников")
                    "Статистика" -> SimpleScreen("Статистика", "Здесь будет аналитика прибыли")
                }
            }

            NavigationBar(
                containerColor = Panel,
                contentColor = White
            ) {
                listOf("Главная", "Продажи", "Долги", "Статистика").forEach { item ->
                    NavigationBarItem(
                        selected = page == item,
                        onClick = { page = item },
                        icon = {},
                        label = { Text(item, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Green,
                            selectedTextColor = Green,
                            unselectedTextColor = Muted,
                            indicatorColor = Color(0xFF263B38)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Добро пожаловать!", color = White, fontSize = 22.sp)
        Text("Твоя торговая панель", color = Muted)

        DashboardCard("Выручка сегодня", "0,00 AZN")
        DashboardCard("Чистая прибыль", "0,00 AZN")
        DashboardCard("Общий долг", "0,00 AZN")

        Text(
            "Управляй продажами и финансами в одном месте.",
            color = Muted,
            fontSize = 13.sp
        )
    }
}

@Composable
fun DashboardCard(title: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Text(title, color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            color = Green,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SimpleScreen(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text(title, color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(description, color = Muted)
    }
}
