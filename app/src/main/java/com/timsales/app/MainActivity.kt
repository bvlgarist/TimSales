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

    
data class DebtPayment(
    val id: Int,
    val date: String,
    val amount: Double
)

data class Debt(
    val id: Int,
    val debtor: String,
    val date: String,
    val amount: Double,
    val note: String,
    val payments: List<DebtPayment> = emptyList()
) {
    val paid: Double
        get() = payments.sumOf { it.amount }

    val remaining: Double
        get() = (amount - paid).coerceAtLeast(0.0)

    val isPaid: Boolean
        get() = remaining <= 0.001
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

fun loadDebts(context: Context): List<Debt> {
    val prefs = context.getSharedPreferences("timsales", 0)
    val json = prefs.getString("debts", "[]") ?: "[]"
    val result = mutableListOf<Debt>()

    try {
        val array = JSONArray(json)

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val paymentsArray = item.optJSONArray("payments") ?: JSONArray()
            val payments = mutableListOf<DebtPayment>()

            for (j in 0 until paymentsArray.length()) {
                val payment = paymentsArray.getJSONObject(j)

                payments.add(
                    DebtPayment(
                        id = payment.getInt("id"),
                        date = payment.getString("date"),
                        amount = payment.getDouble("amount")
                    )
                )
            }

            result.add(
                Debt(
                    id = item.getInt("id"),
                    debtor = item.getString("debtor"),
                    date = item.getString("date"),
                    amount = item.getDouble("amount"),
                    note = item.optString("note", ""),
                    payments = payments
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
    sales: List<Sale>,
    debts: List<Debt>
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

    val debtsArray = JSONArray()
    debts.forEach { d ->
        val paymentsArray = JSONArray()

        d.payments.forEach { p ->
            paymentsArray.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("date", p.date)
                    put("amount", p.amount)
                }
            )
        }

        debtsArray.put(
            JSONObject().apply {
                put("id", d.id)
                put("debtor", d.debtor)
                put("date", d.date)
                put("amount", d.amount)
                put("note", d.note)
                put("payments", paymentsArray)
            }
        )
    }

    context.getSharedPreferences("timsales", 0)
        .edit()
        .putString("products", productsArray.toString())
        .putString("appointments", appointmentsArray.toString())
        .putString("sales", salesArray.toString())
        .putString("debts", debtsArray.toString())
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

    val debts = remember {
    mutableStateListOf<Debt>().apply {
        addAll(loadDebts(context))
    }
    }

    var nextProductId by remember {
        mutableIntStateOf((products.maxOfOrNull { it.id } ?: 0) + 1)
    }

    var nextAppointmentId by remember {
        mutableIntStateOf((appointments.maxOfOrNull { it.id } ?: 0) + 1)
    }

    val prefs = context.getSharedPreferences("timsales", 0)

var statsResetThroughId by remember {
    mutableIntStateOf(prefs.getInt("statsResetThroughId", 0))
}

var nextSaleId by remember {
    mutableIntStateOf(
        prefs.getInt(
            "nextSaleId",
            (sales.maxOfOrNull { it.id } ?: 0) + 1
        )
    )
}

    var page by remember {
        mutableStateOf("Главная")
    }

    LaunchedEffect(
    products.toList(),
    appointments.toList(),
    sales.toList(),
    statsResetThroughId,
    nextSaleId
) {
    saveData(context, products, appointments, sales, debts)

    prefs.edit()
        .putInt("statsResetThroughId", statsResetThroughId)
        .putInt("nextSaleId", nextSaleId)
        .apply()
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
    sales = sales.filter { it.id > statsResetThroughId },
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

                    "Стата" -> StatisticsScreen(
    sales = sales.filter { it.id > statsResetThroughId },
    onResetStats = {
        statsResetThroughId = sales.maxOfOrNull { it.id }
            ?: statsResetThroughId
    },
    onResetSales = {
        sales.clear()
        statsResetThroughId = 0
    }
)
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

        
        if (appointment.place.isNotBlank()) {
            Text("Место: ${appointment.place}", color = Muted)
        }

        if (appointment.products.isNotBlank()) {
            Text("Товары: ${appointment.products}", color = Muted)
        }

        Text("Статус: ${appointment.status}", color = Muted)

        if (!historyMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Изменить", fontSize = 11.sp)
                }

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
    var showCalendar by remember { mutableStateOf(false) }

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
                FormField("Имя клиента", client) {
                    client = it
                }

                OutlinedButton(
                    onClick = { showCalendar = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Дата: ${dateText(selectedDate)}", color = Green)
                }

                FormField("Время", time) {
                    time = it
                }

                FormField("Место встречи", place) {
                    place = it
                }

                FormField(
                    "Товары и количество",
                    productsText,
                    singleLine = false
                ) {
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
            color = Green,
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
            Text("Пока товаров нет.", color = Muted)
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

                Text("Закупка: ${money(product.buyPrice)} AZN", color = White)
                Text("Продажа: ${money(product.sellPrice)} AZN", color = White)

                Text(
                    "Прибыль с единицы: ${money(product.sellPrice - product.buyPrice)} AZN",
                    color = Green
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            contentColor = Red
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FormField("Название товара", name) {
                    name = it
                }
                FormField(
                    "Цена закупки (AZN)",
                    buyPrice,
                    keyboardType = KeyboardType.Decimal
                ) {
                    buyPrice = it
                }
                FormField(
                    "Цена продажи (AZN)",
                    sellPrice,
                    keyboardType = KeyboardType.Decimal
                ) {
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
                        buy != null && sell != null &&
                        buy >= 0 && sell >= 0
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
fun SalesScreen(
    products: SnapshotStateList<Product>,
    sales: SnapshotStateList<Sale>,
    onAdd: (String, Int, Double, Double, Double) -> Unit,
    onUpdate: (Sale) -> Unit,
    onDelete: (Sale) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Sale?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Продажи",
            color = Green,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Text("Всего операций: ${sales.size}", color = Muted)

        Button(
            onClick = {
                editing = null
                showForm = true
            },
            enabled = products.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Green,
                contentColor = Bg
            )
        ) {
            Text("+ Добавить продажу")
        }

        if (products.isEmpty()) {
            Text("Сначала добавь товары в справочник.", color = Muted)
        }

        if (sales.isEmpty()) {
            Text("История продаж пока пуста.", color = Muted)
        }

        sales.forEach { sale ->
            SaleCard(
                sale = sale,
                onEdit = {
                    editing = sale
                    showForm = true
                },
                onDelete = { onDelete(sale) }
            )
        }
    }

    if (showForm) {
        SaleForm(
            products = products,
            initial = editing,
            onDismiss = { showForm = false },
            onSave = { name, quantity, buy, sell, delivery ->
                if (editing == null) {
                    onAdd(name, quantity, buy, sell, delivery)
                } else {
                    onUpdate(
                        editing!!.copy(
                            productName = name,
                            quantity = quantity,
                            buyPrice = buy,
                            sellPrice = sell,
                            deliveryFee = delivery
                        )
                    )
                }
                showForm = false
            }
        )
    }
}

@Composable
fun SaleCard(
    sale: Sale,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            sale.productName,
            color = Green,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text("Дата: ${sale.date}", color = Muted)
        Text("Количество: ${sale.quantity}", color = White)
        Text("Выручка: ${money(sale.revenue)} AZN", color = White)
        Text("Себестоимость: ${money(sale.cost)} AZN", color = White)
        Text("Прибыль товара: ${money(sale.productProfit)} AZN", color = Green)
        Text("Доставка: ${money(sale.deliveryFee)} AZN", color = White)

        Text(
            "Итого прибыль: ${money(sale.totalProfit)} AZN",
            color = Green,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            ) {
                Text("Изменить")
            }

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Red
                )
            ) {
                Text("Удалить")
            }
        }
    }
}

@Composable
fun SaleForm(
    products: List<Product>,
    initial: Sale?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Double, Double, Double) -> Unit
) {
    var selectedName by remember(initial, products) {
        mutableStateOf(
            initial?.productName ?: products.firstOrNull()?.name.orEmpty()
        )
    }

    val selectedProduct = products.firstOrNull {
        it.name == selectedName
    }

    var quantity by remember(initial) {
        mutableStateOf(initial?.quantity?.toString() ?: "1")
    }

    var buyPrice by remember(initial, selectedProduct) {
        mutableStateOf(
            initial?.buyPrice?.toString()
                ?: selectedProduct?.buyPrice?.toString().orEmpty()
        )
    }

    var sellPrice by remember(initial, selectedProduct) {
        mutableStateOf(
            initial?.sellPrice?.toString()
                ?: selectedProduct?.sellPrice?.toString().orEmpty()
        )
    }

    var delivery by remember(initial) {
        mutableStateOf(initial?.deliveryFee?.toString() ?: "0")
    }

    var expanded by remember { mutableStateOf(false) }

    val qty = quantity.toIntOrNull() ?: 0
    val buy = buyPrice.replace(",", ".").toDoubleOrNull() ?: 0.0
    val sell = sellPrice.replace(",", ".").toDoubleOrNull() ?: 0.0
    val deliveryAmount = delivery.replace(",", ".").toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = {
            Text(
                if (initial == null) "Новая продажа" else "Изменить продажу",
                color = Green
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedName.ifBlank { "Выбрать товар" }, color = Green)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        products.forEach { product ->
                            DropdownMenuItem(
                                text = { Text(product.name) },
                                onClick = {
                                    selectedName = product.name
                                    buyPrice = product.buyPrice.toString()
                                    sellPrice = product.sellPrice.toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                FormField(
                    "Количество",
                    quantity,
                    keyboardType = KeyboardType.Number
                ) {
                    quantity = it
                }

                FormField(
                    "Закупочная цена за единицу",
                    buyPrice,
                    keyboardType = KeyboardType.Decimal
                ) {
                    buyPrice = it
                }

                FormField(
                    "Цена продажи за единицу",
                    sellPrice,
                    keyboardType = KeyboardType.Decimal
                ) {
                    sellPrice = it
                }

                FormField(
                    "Доставка (AZN, если была)",
                    delivery,
                    keyboardType = KeyboardType.Decimal
                ) {
                    delivery = it
                }

                HorizontalDivider(color = Muted)

                Text("Выручка: ${money(sell * qty)} AZN", color = White)
                Text("Себестоимость: ${money(buy * qty)} AZN", color = White)
                Text(
                    "Прибыль товара: ${money((sell - buy) * qty)} AZN",
                    color = Green
                )
                Text(
                    "Общая прибыль с доставкой: ${
                        money((sell - buy) * qty + deliveryAmount)
                    } AZN",
                    color = Green,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (
                        selectedName.isNotBlank() &&
                        qty > 0 &&
                        buy >= 0 &&
                        sell >= 0 &&
                        deliveryAmount >= 0
                    ) {
                        onSave(
                            selectedName,
                            qty,
                            buy,
                            sell,
                            deliveryAmount
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
}

@Composable
fun StatisticsScreen(
    sales: List<Sale>,
    onResetStats: () -> Unit,
    onResetSales: () -> Unit
) {
    var showStatsDialog by remember { mutableStateOf(false) }
    var showSalesDialog by remember { mutableStateOf(false) }

    val revenue = sales.sumOf { it.revenue }
    val cost = sales.sumOf { it.cost }
    val productProfit = sales.sumOf { it.productProfit }
    val delivery = sales.sumOf { it.deliveryFee }
    val totalProfit = sales.sumOf { it.totalProfit }

    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text(
            "Статистика",
            color = Green,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Text("Продаж в текущей статистике: ${sales.size}", color = Muted)

        StatisticRow("Выручка от товаров", revenue)
        StatisticRow("Себестоимость товаров", cost)
        StatisticRow("Прибыль от товаров", productProfit)
        StatisticRow("Доход от доставки", delivery)

        HorizontalDivider(color = Muted)

        StatisticRow("Общая прибыль", totalProfit, true)

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = { showStatsDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Green,
                contentColor = Bg
            )
        ) {
            Text("Обнулить статистику")
        }

        OutlinedButton(
            onClick = { showSalesDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Red
            )
        ) {
            Text("Обнулить продажи")
        }

        Text(
            "Сброс статистики сохраняет историю продаж. " +
                "Удаление продаж стирает всю историю.",
            color = Muted,
            fontSize = 12.sp
        )
    }

    if (showStatsDialog) {
        AlertDialog(
            onDismissRequest = { showStatsDialog = false },
            containerColor = Panel,
            title = {
                Text("Обнулить статистику?", color = Green)
            },
            text = {
                Text(
                    "Показатели обнулятся. История продаж останется " +
                        "в разделе «Продажи»."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetStats()
                        showStatsDialog = false
                    }
                ) {
                    Text("Обнулить", color = Green)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatsDialog = false }) {
                    Text("Отмена", color = Muted)
                }
            }
        )
    }

    if (showSalesDialog) {
        AlertDialog(
            onDismissRequest = { showSalesDialog = false },
            containerColor = Panel,
            title = {
                Text("Удалить все продажи?", color = Red)
            },
            text = {
                Text(
                    "Вся история продаж будет удалена без возможности " +
                        "восстановления. Товары и долги останутся."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetSales()
                        showSalesDialog = false
                    }
                ) {
                    Text("Удалить всё", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSalesDialog = false }) {
                    Text("Отмена", color = Muted)
                }
            }
        )
    }
}


@Composable
fun StatisticRow(
    label: String,
    amount: Double,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = if (highlight) Green else White,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
        )

        Text(
            "${money(amount)} AZN",
            color = Green,
            fontWeight = FontWeight.Bold
        )
    }
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            color = Green,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(description, color = Muted, fontSize = 15.sp)
    }
}

