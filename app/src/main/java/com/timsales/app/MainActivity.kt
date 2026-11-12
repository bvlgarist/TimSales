package com.timsales.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val Bg = Color(0xFF101410)
private val Panel = Color(0xFF1C241F)
private val Green = Color(0xFF42D392)
private val White = Color(0xFFF2F5F3)
private val Muted = Color(0xFF9AA69E)

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimSalesApp()
        }
    }
}

fun dateText(millis: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(millis))
}

fun dateMillis(value: String): Long {
    return try {
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        formatter.parse(value)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

fun loadProducts(context: android.content.Context): List<Product> {
    val prefs = context.getSharedPreferences("timsales", 0)
    val json = prefs.getString("products", "[]") ?: "[]"
    val result = mutableListOf<Product>()

    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            result.add(
                Product(
                    id = item.getInt("id"),
                    name = item.getString("name"),
                    buyPrice = item.getDouble("buyPrice"),
                    sellPrice = item.getDouble("sellPrice")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return result
}

fun loadAppointments(context: android.content.Context): List<Appointment> {
    val prefs = context.getSharedPreferences("timsales", 0)
    val json = prefs.getString("appointments", "[]") ?: "[]"
    val result = mutableListOf<Appointment>()

    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            result.add(
                Appointment(
                    id = item.getInt("id"),
                    client = item.getString("client"),
                    date = item.getString("date"),
                    time = item.getString("time"),
                    place = item.getString("place"),
                    products = item.getString("products"),
                    status = item.optString("status", "Предстоящая")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return result
}

fun saveData(
    context: android.content.Context,
    products: List<Product>,
    appointments: List<Appointment>
) {
    val productsArray = JSONArray()

    products.forEach { product ->
        productsArray.put(
            JSONObject().apply {
                put("id", product.id)
                put("name", product.name)
                put("buyPrice", product.buyPrice)
                put("sellPrice", product.sellPrice)
            }
        )
    }

    val appointmentsArray = JSONArray()

    appointments.forEach { appointment ->
        appointmentsArray.put(
            JSONObject().apply {
                put("id", appointment.id)
                put("client", appointment.client)
                put("date", appointment.date)
                put("time", appointment.time)
                put("place", appointment.place)
                put("products", appointment.products)
                put("status", appointment.status)
            }
        )
    }

    context.getSharedPreferences("timsales", 0)
        .edit()
        .putString("products", productsArray.toString())
        .putString("appointments", appointmentsArray.toString())
        .apply()
}

@OptIn(ExperimentalMaterial3Api::class)
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

    var nextProductId by remember {
        mutableIntStateOf((products.maxOfOrNull { it.id } ?: 0) + 1)
    }

    var nextAppointmentId by remember {
        mutableIntStateOf((appointments.maxOfOrNull { it.id } ?: 0) + 1)
    }

    var page by remember {
        mutableStateOf("Планировщик")
    }

    LaunchedEffect(products.toList(), appointments.toList()) {
        saveData(context, products, appointments)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg,
            surface = Panel,
            primary = Green,
            onPrimary = Bg,
            onBackground = White,
            onSurface = White
        )
    ) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar(
                    containerColor = Panel
                ) {
                    val pages = listOf(
                        "План",
                        "Товары",
                        "Продажи",
                        "Долги",
                        "Статистика",
                        "Главная"
                    )

                    pages.forEach { item ->
                        NavigationBarItem(
                            selected = page == item,
                            onClick = { page = item },
                            icon = {
                                Text(
                                    when (item) {
                                        "План" -> "📅"
                                        "Товары" -> "📦"
                                        "Продажи" -> "💰"
                                        "Долги" -> "🧾"
                                        "Статистика" -> "📊"
                                        else -> "🏠"
                                    },
                                    fontSize = 17.sp
                                )
                            },
                            label = {
                                Text(item, fontSize = 9.sp)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Green,
                                selectedTextColor = Green,
                                indicatorColor = Color(0xFF263B30),
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Bg)
                    .padding(16.dp)
            ) {
                when (page) {
                    "План" -> PlannerScreen(
                        appointments = appointments,
                        onAdd = { client, date, time, place, productsText ->
                            appointments.add(
                                Appointment(
                                    id = nextAppointmentId++,
                                    client = client,
                                    date = date,
                                    time = time,
                                    place = place,
                                    products = productsText
                                )
                            )
                        },
                        onUpdate = { updated ->
                            val index = appointments.indexOfFirst {
                                it.id == updated.id
                            }
                            if (index >= 0) {
                                appointments[index] = updated
                            }
                        }
                    )

                    "Товары" -> ProductsScreen(
                        products = products,
                        onAdd = { name, buy, sell ->
                            products.add(
                                Product(
                                    nextProductId++,
                                    name,
                                    buy,
                                    sell
                                )
                            )
                        },
                        onUpdate = { updated ->
                            val index = products.indexOfFirst {
                                it.id == updated.id
                            }
                            if (index >= 0) {
                                products[index] = updated
                            }
                        },
                        onDelete = { product ->
                            products.remove(product)
                        }
                    )

                    "Продажи" -> SimpleScreen(
                        "Продажи",
                        "Здесь появится добавление продаж, расчёт выручки и прибыли."
                    )

                    "Долги" -> SimpleScreen(
                        "Долги",
                        "Здесь появится учёт задолженностей и погашений."
                    )

                    "Статистика" -> SimpleScreen(
                        "Статистика",
                        "Здесь появится статистика выручки и прибыли."
                    )

                    else -> SimpleScreen(
                        "Главная",
                        "Товаров: ${products.size}\nПредстоящих встреч: ${
                            appointments.count { it.status == "Предстоящая" }
                        }\n\nФинансовая статистика появится после добавления раздела продаж."
                    )
                }
            }
        }
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

    val upcoming = appointments.filter {
        it.status == "Предстоящая"
    }

    val history = appointments.filter {
        it.status != "Предстоящая"
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Планировщик",
            color = Green,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Предстоящие встречи: ${upcoming.size}",
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

        if (upcoming.isEmpty()) {
            Text(
                "Пока нет запланированных встреч.",
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
                onComplete = {
                    onUpdate(appointment.copy(status = "Завершена"))
                },
                onCancel = {
                    onUpdate(appointment.copy(status = "Отменена"))
                }
            )
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "История",
                color = Green,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            history.forEach { appointment ->
                AppointmentCard(
                    appointment = appointment,
                    onEdit = {
                        editing = appointment
                        showForm = true
                    },
                    onComplete = {},
                    onCancel = {},
                    historyMode = true
                )
            }
        }
    }

    if (showForm) {
        AppointmentForm(
            initial = editing,
            onDismiss = { showForm = false },
            onSave = { client, date, time, place, productsText ->
                if (editing == null) {
                    onAdd(client, date, time, place, productsText)
                } else {
                    onUpdate(
                        editing!!.copy(
                            client = client,
                            date = date,
                            time = time,
                            place = place,
                            products = productsText
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
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    historyMode: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            appointment.client,
            color = Green,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "${appointment.date}  •  ${appointment.time}",
            color = White
        )

        if (appointment.place.isNotBlank()) {
            Text("Место: ${appointment.place}", color = Muted)
        }

        if (appointment.products.isNotBlank()) {
            Text("Товары: ${appointment.products}", color = Muted)
        }

        Text(
            "Статус: ${appointment.status}",
            color = if (appointment.status == "Завершена") Green else Muted
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            ) {
                Text("Изменить", fontSize = 11.sp)
            }

            if (!historyMode) {
                OutlinedButton(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Готово", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Отмена", fontSize = 11.sp)
                }
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

    var selectedDate by remember(initial) {
        mutableLongStateOf(
            dateMillis(initial?.date ?: dateText(System.currentTimeMillis()))
        )
    }

    var time by remember(initial) {
        mutableStateOf(initial?.time ?: "")
    }

    var place by remember(initial) {
        mutableStateOf(initial?.place ?: "")
    }

    var productsText by remember(initial) {
        mutableStateOf(initial?.products ?: "")
    }

    var showCalendar by remember {
        mutableStateOf(false)
    }

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
    )

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
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                FormField("Имя клиента", client) {
                    client = it
                }

                OutlinedButton(
                    onClick = { showCalendar = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Дата: ${dateText(selectedDate)}",
                        color = Green
                    )
                }

                FormField("Время (например, 15:30)", time) {
                    time = it
                }

                FormField("Место встречи", place) {
                    place = it
                }

                FormField("Товары и количество", productsText) {
                    productsText = it
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (client.isNotBlank()) {
                        onSave(
                            client.trim(),
                            dateText(selectedDate),
                            time.trim(),
                            place.trim(),
                            productsText.trim()
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

    Column(
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text(
            "Справочник товаров",
            color = Green,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "${products.size} товаров сохранено",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel, RoundedCornerShape(16.dp))
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    product.name,
                    color = Green,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text("Закупка: ${product.buyPrice} AZN", color = White)
                Text("Продажа: ${product.sellPrice} AZN", color = White)

                Text(
                    "Прибыль с единицы: ${product.sellPrice - product.buyPrice} AZN",
                    color = Green
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            editing = product
                            showForm = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Изменить")
                    }

                    OutlinedButton(
                        onClick = { onDelete(product) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF7777)
                        )
                    ) {
                        Text("Удалить")
                    }
                }
            }
        }
    }

    if (showForm) {
        ProductForm(
            initial = editing,
            onDismiss = { showForm = false },
            onSave = { name, buy, sell ->
                if (editing == null) {
                    onAdd(name, buy, sell)
                } else {
                    onUpdate(
                        editing!!.copy(
                            name = name,
                            buyPrice = buy,
                            sellPrice = sell
                        )
                    )
                }
                showForm = false
            }
        )
    }
}

@Composable
fun ProductForm(
    initial: Product?,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double) -> Unit
) {
    var name by remember(initial) {
        mutableStateOf(initial?.name ?: "")
    }

    var buyPrice by remember(initial) {
        mutableStateOf(initial?.buyPrice?.toString() ?: "")
    }

    var sellPrice by remember(initial) {
        mutableStateOf(initial?.sellPrice?.toString() ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = {
            Text(
                if (initial == null) "Новый товар" else "Изменить товар",
                color = Green
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FormField("Название товара", name) {
                    name = it
                }

                FormField("Цена закупки (AZN)", buyPrice) {
                    buyPrice = it
                }

                FormField("Цена продажи (AZN)", sellPrice) {
                    sellPrice = it
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val buy = buyPrice.replace(",", ".").toDoubleOrNull()
                    val sell = sellPrice.replace(",", ".").toDoubleOrNull()

                    if (
                        name.isNotBlank() &&
                        buy != null &&
                        sell != null &&
                        buy >= 0 &&
                        sell >= 0
                    ) {
                        onSave(name.trim(), buy, sell)
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
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        )
    )
}


@Composable
fun SimpleScreen(
    title: String,
    description: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title,
            color = Green,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            description,
            color = Muted,
            fontSize = 15.sp
        )
    }
}
