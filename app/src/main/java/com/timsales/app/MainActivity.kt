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
private val Red = Color(0xFFFF7777)

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

data class Sale(
    val id: Int,
    val date: String,
    val productName: String,
    val quantity: Int,
    val buyPrice: Double,
    val sellPrice: Double,
    val deliveryFee: Double
) {
    val revenue: Double
        get() = sellPrice * quantity

    val cost: Double
        get() = buyPrice * quantity

    val productProfit: Double
        get() = revenue - cost

    val totalProfit: Double
        get() = productProfit + deliveryFee
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimSalesApp()
        }
    }
}

fun money(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
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

fun loadProducts(context: Context): List<Product> {
    val prefs = context.getSharedPreferences("timsales", 0)
    val json = prefs.getString("products", "[]") ?: "[]"
    val result = mutableListOf<Product>()

    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            result.add(
                Product(
                    item.getInt("id"),
                    item.getString("name"),
                    item.getDouble("buyPrice"),
                    item.getDouble("sellPrice")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return result
}

fun loadAppointments(context: Context): List<Appointment> {
    val prefs = context.getSharedPreferences("timsales", 0)
    val json = prefs.getString("appointments", "[]") ?: "[]"
    val result = mutableListOf<Appointment>()

    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            result.add(
                Appointment(
                    item.getInt("id"),
                    item.getString("client"),
                    item.getString("date"),
                    item.getString("time"),
                    item.getString("place"),
                    item.getString("products"),
                    item.optString("status", "Предстоящая")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return result
}

fun loadSales(context: Context): List<Sale> {
    val prefs = context.getSharedPreferences("timsales", 0)
    val json = prefs.getString("sales", "[]") ?: "[]"
    val result = mutableListOf<Sale>()

    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            result.add(
                Sale(
                    id = item.getInt("id"),
                    date = item.getString("date"),
                    productName = item.getString("productName"),
                    quantity = item.getInt("quantity"),
                    buyPrice = item.getDouble("buyPrice"),
                    sellPrice = item.getDouble("sellPrice"),
                    deliveryFee = item.optDouble("deliveryFee", 0.0)
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return result
}

fun saveData(
    context: Context,
    products: List<Product>,
    appointments: List<Appointment>,
    sales: List<Sale>
) {
    val productsArray = JSONArray()
    products.forEach { p ->
        productsArray.put(
            JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("buyPrice", p.buyPrice)
                put("sellPrice", p.sellPrice)
            }
        )
    }

    val appointmentsArray = JSONArray()
    appointments.forEach { a ->
        appointmentsArray.put(
            JSONObject().apply {
                put("id", a.id)
                put("client", a.client)
                put("date", a.date)
                put("time", a.time)
                put("place", a.place)
                put("products", a.products)
                put("status", a.status)
            }
        )
    }

    val salesArray = JSONArray()
    sales.forEach { s ->
        salesArray.put(
            JSONObject().apply {
                put("id", s.id)
                put("date", s.date)
                put("productName", s.productName)
                put("quantity", s.quantity)
                put("buyPrice", s.buyPrice)
                put("sellPrice", s.sellPrice)
                put("deliveryFee", s.deliveryFee)
            }
        )
    }

    context.getSharedPreferences("timsales", 0)
        .edit()
        .putString("products", productsArray.toString())
        .putString("appointments", appointmentsArray.toString())
        .putString("sales", salesArray.toString())
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

    val sales = remember {
        mutableStateListOf<Sale>().apply {
            addAll(loadSales(context))
        }
    }

    var nextProductId by remember {
        mutableIntStateOf((products.maxOfOrNull { it.id } ?: 0) + 1)
    }

    var nextAppointmentId by remember {
        mutableIntStateOf((appointments.maxOfOrNull { it.id } ?: 0) + 1)
    }

    var nextSaleId by remember {
        mutableIntStateOf((sales.maxOfOrNull { it.id } ?: 0) + 1)
    }

    var page by remember {
        mutableStateOf("Главная")
    }

    LaunchedEffect(products.toList(), appointments.toList(), sales.toList()) {
        saveData(context, products, appointments, sales)
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
                NavigationBar(containerColor = Panel) {
                    val pages = listOf(
                        "Главная",
                        "План",
                        "Товары",
                        "Продажи",
                        "Долги",
                        "Стата"
                    )

                    pages.forEach { item ->
                        NavigationBarItem(
                            selected = page == item,
                            onClick = { page = item },
                            icon = {
                                Text(
                                    when (item) {
                                        "Главная" -> "⌂"
                                        "План" -> "📅"
                                        "Товары" -> "📦"
                                        "Продажи" -> "💰"
                                        "Долги" -> "🧾"
                                        else -> "📊"
                                    },
                                    fontSize = 17.sp
                                )
                            },
                            label = {
                                Text(item, fontSize = 9.sp, maxLines = 1)
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (page) {
                    "Главная" -> HomeScreen(
                        appointments = appointments,
                        sales = sales,
                        onOpenPlanner = { page = "План" }
                    )

                    "План" -> PlannerScreen(
                        appointments = appointments,
                        onAdd = { client, date, time, place, productsText ->
                            appointments.add(
                                Appointment(
                                    nextAppointmentId++,
                                    client,
                                    date,
                                    time,
                                    place,
                                    productsText
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
                        onDelete = { products.remove(it) }
                    )

                    "Продажи" -> SalesScreen(
                        products = products,
                        sales = sales,
                        onAdd = { name, quantity, buy, sell, delivery ->
                            sales.add(
                                Sale(
                                    nextSaleId++,
                                    dateText(System.currentTimeMillis()),
                                    name,
                                    quantity,
                                    buy,
                                    sell,
                                    delivery
                                )
                            )
                        },
                        onUpdate = { updated ->
                            val index = sales.indexOfFirst {
                                it.id == updated.id
                            }
                            if (index >= 0) sales[index] = updated
                        },
                        onDelete = { sales.remove(it) }
                    )

                    "Долги" -> SimpleScreen(
                        "Долги",
                        "Учёт задолженностей добавим следующим этапом."
                    )

                    "Стата" -> StatisticsScreen(sales)
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    appointments: List<Appointment>,
    sales: List<Sale>,
    onOpenPlanner: () -> Unit
) {
    val upcoming = appointments.filter {
        it.status == "Предстоящая"
    }

    val revenue = sales.sumOf { it.revenue }
    val cost = sales.sumOf { it.cost }
    val delivery = sales.sumOf { it.deliveryFee }
    val profit = sales.sumOf { it.totalProfit }

    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text(
            "TimSales",
            color = Green,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text("Обзор бизнеса", color = Muted)

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MetricCard("Выручка", revenue, Modifier.weight(1f))
            MetricCard("Прибыль", profit, Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MetricCard("Себестоимость", cost, Modifier.weight(1f))
            MetricCard("Доставка", delivery, Modifier.weight(1f))
        }

        Spacer(Modifier.height(5.dp))

        Text(
            "Предстоящие встречи",
            color = Green,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        TextButton(onClick = onOpenPlanner) {
            Text("Открыть планировщик →", color = Green)
        }

        if (upcoming.isEmpty()) {
            Text("Запланированных встреч пока нет.", color = Muted)
        } else {
            upcoming.sortedBy { dateMillis(it.date) }.forEach { appointment ->
                AppointmentCard(
                    appointment = appointment,
                    onEdit = onOpenPlanner,
                    onComplete = {},
                    onCancel = {},
                    historyMode = true
                )
            }
        }

        Text(
            "Всего продаж: ${sales.size}",
            color = Muted
        )
    }
}

@Composable
fun MetricCard(
    label: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Panel, RoundedCornerShape(15.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(
            "${money(amount)} AZN",
            color = Green,
            fontSize = 17.sp,
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

    val upcoming = appointments.filter {
        it.status == "Предстоящая"
    }

    val history = appointments.filter {
        it.status != "Предстоящая"
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Планировщик",
            color = Green,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Text("Предстоящие встречи: ${upcoming.size}", color = Muted)

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
            Text("Пока нет запланированных встреч.", color = Muted)
        }

        upcoming.sortedBy { dateMillis(it.date) }.forEach { appointment ->
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

        if (appointment.pl
