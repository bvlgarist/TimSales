package com.timsales.app

import android.os.Bundle
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val Bg = Color(0xFF10131A)
private val Panel = Color(0xFF1B202B)
private val Green = Color(0xFF55D6A0)
private val White = Color(0xFFF3F5F8)
private val Muted = Color(0xFF929BAA)

data class Product(
    val id: Int,
    val name: String,
    val buyPrice: Double,
    val sellPrice: Double
)

data class Appointment(
    val id: Int,
    val client: String,
    val date: String,
    val time: String,
    val place: String,
    val products: String,
    val status: String = "Предстоящая"
)

private fun dateText(millis: Long): String {
    val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(millis))
}

private fun dateMillis(value: String): Long {
    return try {
        val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.parse(value)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}

private fun loadProducts(context: Context): List<Product> {
    return try {
        val array = JSONArray(
            context.getSharedPreferences("timsales", Context.MODE_PRIVATE)
                .getString("products", "[]")
        )
        (0 until array.length()).map {
            val item = array.getJSONObject(it)
            Product(
                item.getInt("id"),
                item.getString("name"),
                item.getDouble("buy"),
                item.getDouble("sell")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun loadAppointments(context: Context): List<Appointment> {
    return try {
        val array = JSONArray(
            context.getSharedPreferences("timsales", Context.MODE_PRIVATE)
                .getString("appointments", "[]")
        )
        (0 until array.length()).map {
            val item = array.getJSONObject(it)
            Appointment(
                item.getInt("id"),
                item.getString("client"),
                item.getString("date"),
                item.getString("time"),
                item.getString("place"),
                item.getString("products"),
                item.optString("status", "Предстоящая")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveData(
    context: Context,
    products: List<Product>,
    appointments: List<Appointment>
) {
    val productArray = JSONArray()
    products.forEach {
        productArray.put(
            JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("buy", it.buyPrice)
                put("sell", it.sellPrice)
            }
        )
    }

    val appointmentArray = JSONArray()
    appointments.forEach {
        appointmentArray.put(
            JSONObject().apply {
                put("id", it.id)
                put("client", it.client)
                put("date", it.date)
                put("time", it.time)
                put("place", it.place)
                put("products", it.products)
                put("status", it.status)
            }
        )
    }

    context.getSharedPreferences("timsales", Context.MODE_PRIVATE)
        .edit()
        .putString("products", productArray.toString())
        .putString("appointments", appointmentArray.toString())
        .apply()
}

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
    val context = LocalContext.current
    val products = remember {
        mutableStateListOf<Product>().apply {
            addAll(loadProducts(context))
        }
    }
    val appointments = remember {
        mutableStateListOf<Appointment>().apply {
            addAll(loadAppointments(context))
        }
    }

    var page by remember { mutableStateOf("Планировщик") }
    var nextProductId by remember {
        mutableIntStateOf((products.maxOfOrNull { it.id } ?: 0) + 1)
    }
    var nextAppointmentId by remember {
        mutableIntStateOf((appointments.maxOfOrNull { it.id } ?: 0) + 1)
    }

    LaunchedEffect(products.toList(), appointments.toList()) {
        saveData(context, products, appointments)
    }

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
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
                Spacer(Modifier.height(22.dp))

                when (page) {
                    "Планировщик" -> PlannerScreen(
                        appointments = appointments,
                        onAdd = { client, date, time, place, items ->
                            appointments.add(
                                Appointment(
                                    nextAppointmentId++,
                                    client, date, time, place, items
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

                    "Товары" -> ProductsScreen(
                        products = products,
                        onAdd = { name, buy, sell ->
                            products.add(
                                Product(nextProductId++, name, buy, sell)
                            )
                        },
                        onUpdate = { updated ->
                            val index = products.indexOfFirst {
                                it.id == updated.id
                            }
                            if (index >= 0) products[index] = updated
                        },
                        onDelete = { product ->
                            products.remove(product)
                        }
                    )

                    "Главная" -> HomeScreen(
                        appointments.size,
                        products.size
                    )

                    "Продажи" -> SimpleScreen(
                        "Продажи",
                        "Следующим этапом подключим товары, цены и автоматический расчёт прибыли."
                    )

                    "Долги" -> SimpleScreen(
                        "Долги",
                        "Здесь будет учёт задолженностей покупателей."
                    )

                    "Статистика" -> SimpleScreen(
                        "Статистика",
                        "Здесь появятся реальные показатели выручки и прибыли."
                    )
                }
            }

            NavigationBar(
                containerColor = Panel,
                contentColor = White
            ) {
                listOf(
                    "Планировщик",
                    "Товары",
                    "Продажи",
                    "Долги",
                    "Статистика",
                    "Главная"
                ).forEach { item ->
                    NavigationBarItem(
                        selected = page == item,
                        onClick = { page = item },
                        icon = {},
                        label = { Text(item, fontSize = 9.sp) },
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
fun HomeScreen(meetings: Int, productCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text(
            "Главная",
            color = White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        DashboardCard("Выручка сегодня", "0,00 AZN")
        DashboardCard("Прибыль сегодня", "0,00 AZN")
        DashboardCard("Общий долг", "0,00 AZN")
        DashboardCard("Встреч в планировщике", meetings.toString())
        DashboardCard("Товаров в справочнике", productCount.toString())
        Text(
            "Финансовые показатели подключим после реализации продаж.",
            color = Muted
        )
    }
}

@Composable
fun DashboardCard(title: String, value: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Text(title, color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(7.dp))
        Text(
            value,
            color = Green,
            fontSize = 23.sp,
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

    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text(
            "Планировщик",
            color = White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Text("Твои будущие встречи и заказы", color = Muted)

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
        val history = appointments.filter {
            it.status != "Предстоящая"
        }

        Text(
            "Предстоящие встречи (${upcoming.size})",
            color = Green,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        if (upcoming.isEmpty()) {
            Text("Пока встреч нет. Добавь первую!", color = Muted)
        }

        upcoming.forEach { appointment ->
            AppointmentCard(
                appointment,
                onEdit = {
                    editing = appointment
                    showForm = true
                },
                onUpdate = onUpdate
            )
        }

        if (history.isNotEmpty()) {
            Text(
                "История встреч",
                color = White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            history.forEach {
                AppointmentCard(it, null, onUpdate)
            }
        }
    }

    if (showForm) {
        AppointmentForm(
            initial = editing,
            onDismiss = { showForm = false },
            onSave = { client, date, time, place, items ->
                if (editing == null) {
                    onAdd(client, date, time, place, items)
                } else {
                    onUpdate(
                        editing!!.copy(
                            client = client,
                            date = date,
                            time = time,
                            place = place,
                            products = items
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
        Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(16.dp))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            appointment.client,
            color = Green,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${appointment.date} · ${appointment.time}",
            color = White
        )
        Text("Место: ${appointment.place}", color = Muted)
        Text("Товары:\n${appointment.products}", color = White)
        Text("Статус: ${appointment.status}", color = Muted)

        if (appointment.status == "Предстоящая") {
            if (onEdit != null) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Перенести / изменить")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentForm(
    initial: Appointment?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var client by remember(initial) {
        mutableStateOf(initial?.client ?: "")
    }
    var time by remember(initial) {
        mutableStateOf(initial?.time ?: "")
    }
    var place by remember(initial) {
        mutableStateOf(initial?.place ?: "")
    }
    var items by remember(initial) {
        mutableStateOf(initial?.products ?: "")
    }
    var showCalendar by remember { mutableStateOf(false) }
    var selectedDate by remember(initial) {
        mutableLongStateOf(
            initial?.date?.let { dateMillis(it) }
                ?: System.currentTimeMillis()
        )
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
                Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                FormField("Имя покупателя", client) { client = it }

                OutlinedButton(
                    onClick = { showCalendar = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Дата: ${dateText(selectedDate)}",
                        color = Green
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("📅")
                }

                FormField("Время (например, 16:30)", time) {
                    time = it
                }
                FormField("Место встречи", place) {
                    place = it
                }
                FormField(
                    "Товары и количество",
                    items,
                    singleLine = false
                ) {
                    items = it
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (
                        client.isNotBlank() &&
                        time.isNotBlank() &&
                        place.isNotBlank() &&
                        items.isNotBlank()
                    ) {
                        onSave(
                            client,
                            dateText(selectedDate),
                            time,
                            place,
                            items
                        )
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

    if (showCalendar) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            selectedDate = it
                        }
                        showCalendar = false
                    }
                ) {
                    Text("Выбрать", color = Green)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendar = false }) {
                    Text("Назад", color = Muted)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun ProductsScreen(
    products: SnapshotStateList<Product>,
    onAdd: (String, Double, Double) -> Unit,
    onUpdate: (Product) -> Unit,
    onDelete: (Product) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text(
            "Справочник товаров",
            color = White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text("${products.size} товаров сохранено", color = Muted)

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
            Text("+ Добавить товар")
        }

        if (products.isEmpty()) {
            Text(
                "Пока товаров нет. Добавь первый товар.",
                color = Muted
            )
        }

        products.forEach { product ->
            Column(
       
