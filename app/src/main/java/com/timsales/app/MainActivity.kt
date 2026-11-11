package com.timsales.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
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

data class Appointment(
    val id: Int,
    val client: String,
    val date: String,
    val time: String,
    val place: String,
    val products: String,
    val status: String = "Предстоящая"
)

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
    val appointments = remember { mutableStateListOf<Appointment>() }
    var nextId by remember { mutableIntStateOf(1) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Bg
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
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
                    "Планировщик" -> PlannerScreen(
                        appointments = appointments,
                        onAdd = { client, date, time, place, products ->
                            appointments.add(
                                Appointment(
                                    nextId++, client, date, time,
                                    place, products
                                )
                            )
                        },
                        onUpdate = { updated ->
                            val index = appointments.indexOfFirst {
                                it.id == updated.id
                            }
                            if (index >= 0) appointments[index] = updated
                        }
                    )
                    "Продажи" -> SimpleScreen(
                        "Продажи",
                        "Здесь будет учёт твоих продаж"
                    )
                    "Долги" -> SimpleScreen(
                        "Долги",
                        "Здесь будет список должников"
                    )
                    "Статистика" -> SimpleScreen(
                        "Статистика",
                        "Здесь будет аналитика прибыли"
                    )
                }
            }

            NavigationBar(
                containerColor = Panel,
                contentColor = White
            ) {
                listOf(
                    "Главная",
                    "Планировщик",
                    "Продажи",
                    "Долги",
                    "Статистика"
                ).forEach { item ->
                    NavigationBarItem(
                        selected = page == item,
                        onClick = { page = item },
                        icon = {},
                        label = {
                            Text(item, fontSize = 9.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
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
fun PlannerScreen(
    appointments: SnapshotStateList<Appointment>,
    onAdd: (String, String, String, String, String) -> Unit,
    onUpdate: (Appointment) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Appointment?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Планировщик",
            color = White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Твои будущие встречи и заказы",
            color = Muted
        )

        Button(
            onClick = {
                editing = null
                showForm = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Green,
                contentColor = Bg
            )
        ) {
            Text("+ Добавить встречу")
        }

        val upcoming = appointments.filter {
            it.status == "Предстоящая"
        }

        val finished = appointments.filter {
            it.status != "Предстоящая"
        }

        Text(
            "Предстоящие встречи (${upcoming.size})",
            color = Green,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        if (upcoming.isEmpty()) {
            Text(
                "Пока встреч нет. Добавь первую!",
                color = Muted
            )
        }

        upcoming.forEach { appointment ->
            AppointmentCard(
                appointment = appointment,
                onEdit = {
                    editing = appointment
                    showForm = true
                },
                onUpdate = onUpdate
            )
        }

        if (finished.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "История встреч",
                color = White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            finished.forEach { appointment ->
                AppointmentCard(
                    appointment = appointment,
                    onEdit = null,
                    onUpdate = onUpdate
                )
            }
        }
    }

    if (showForm) {
        AppointmentForm(
            initial = editing,
            onDismiss = { showForm = false },
            onSave = { client, date, time, place, products ->
                if (editing == null) {
                    onAdd(client, date, time, place, products)
                } else {
                    onUpdate(
                        editing!!.copy(
                            client = client,
                            date = date,
                            time = time,
                            place = place,
                            products = products
                        )
                    )
                }
                showForm = false
            }
        )
    }
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    onEdit: (() -> Unit)?,
    onUpdate: (Appointment) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            appointment.client,
            color = Green,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${appointment.date} · ${appointment.time}",
            color = White
        )
        Text(
            "Место: ${appointment.place}",
            color = Muted
        )
        Text(
            "Товары:\n${appointment.products}",
            color = White
        )
        Text(
            "Статус: ${appointment.status}",
            color = if (appointment.status == "Предстоящая") Green else Muted
        )

        if (appointment.status == "Предстоящая") {
            if (onEdit != null) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Изменить встречу")
                }
            }

            OutlinedButton(
                onClick = {
                    onUpdate(appointment.copy(status = "Состоялась"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Встреча состоялась")
            }

            OutlinedButton(
                onClick = {
                    onUpdate(appointment.copy(status = "Отменена"))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF7777)
                )
            ) {
                Text("Отменить заказ")
            }
        }
    }
}

@Composable
fun AppointmentForm(
    initial: Appointment?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var client by remember(initial) {
        mutableStateOf(initial?.client ?: "")
    }
    var date by remember(initial) {
        mutableStateOf(initial?.date ?: "")
    }
    var time by remember(initial) {
        mutableStateOf(initial?.time ?: "")
    }
    var place by remember(initial) {
        mutableStateOf(initial?.place ?: "")
    }
    var products by remember(initial) {
        mutableStateOf(initial?.products ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = {
            Text(
                if (initial == null) "Новая встреча" else "Изменить встречу",
                color = Green
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                FormField("Имя покупателя", client) { client = it }
                FormField("Дата (например, 14.11.2026)", date) { date = it }
                FormField("Время (например, 16:30)", time) { time = it }
                FormField("Место встречи", place) { place = it }
                FormField(
                    "Товары и количество",
                    products,
                    singleLine = false
                ) { products = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (
                        client.isNotBlank() &&
                        date.isNotBlank() &&
                        time.isNotBlank() &&
                        place.isNotBlank() &&
                        products.isNotBlank()
                    ) {
                        onSave(client, date, time, place, products)
                    }
                }
            ) {
                Text("Сохранить", color = Green)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Назад", color = Muted)
            }
        }
    )
}

@Composable
fun FormField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = Green,
            focusedLabelColor = Green,
            unfocusedLabelColor = Muted,
            cursorColor = Green
        )
    )
}

@Composable
fun SimpleScreen(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text(
            title,
            color = White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Text(description, color = Muted)
    }
}
