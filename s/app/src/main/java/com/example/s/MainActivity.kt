package com.example.s

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1976D2),
                    secondary = Color(0xFF388E3C),
                    tertiary = Color(0xFFD32F2F)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FinancialCalculatorApp()
                }
            }
        }
    }
}

// Модель одной строки графика платежей
data class PaymentRow(
    val month: Int,
    val payment: Double,
    val principal: Double,
    val interest: Double,
    val remainingBalance: Double
)

// Модель итогового результата расчета
data class CalculationResult(
    val monthlyPaymentFirst: Double,
    val totalPaid: Double,
    val totalInterest: Double,
    val monthsCount: Int,
    val schedule: List<PaymentRow>,
    val errorMessage: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialCalculatorApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ипотека", "Кредитная карта")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Калькулятор Кредитов", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White
            )
        )

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        if (selectedTab == 0) {
            MortgageScreen()
        } else {
            CreditCardScreen()
        }
    }
}

@Composable
fun MortgageScreen() {
    var propertyPrice by remember { mutableStateOf("6000000") }
    var downPayment by remember { mutableStateOf("1200000") }
    var years by remember { mutableStateOf("20") }
    var interestRate by remember { mutableStateOf("16.5") }

    val result = remember(propertyPrice, downPayment, years, interestRate) {
        calculateMortgage(
            price = propertyPrice.toDoubleOrNull() ?: 0.0,
            down = downPayment.toDoubleOrNull() ?: 0.0,
            years = years.toIntOrNull() ?: 0,
            annualRate = interestRate.toDoubleOrNull() ?: 0.0
        )
    }

    CalculatorLayout(
        inputs = {
            NumberInputField("Стоимость недвижимости (₽)", propertyPrice) { propertyPrice = it }
            NumberInputField("Первоначальный взнос (₽)", downPayment) { downPayment = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    NumberInputField("Срок (лет)", years) { years = it }
                }
                Box(modifier = Modifier.weight(1f)) {
                    NumberInputField("Ставка (%)", interestRate) { interestRate = it }
                }
            }
        },
        result = result
    )
}

@Composable
fun CreditCardScreen() {
    var debt by remember { mutableStateOf("150000") }
    var interestRate by remember { mutableStateOf("32.9") }
    var monthlyPayment by remember { mutableStateOf("7500") }

    val result = remember(debt, interestRate, monthlyPayment) {
        calculateCreditCard(
            debt = debt.toDoubleOrNull() ?: 0.0,
            annualRate = interestRate.toDoubleOrNull() ?: 0.0,
            fixedPayment = monthlyPayment.toDoubleOrNull() ?: 0.0
        )
    }

    CalculatorLayout(
        inputs = {
            NumberInputField("Сумма долга по карте (₽)", debt) { debt = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    NumberInputField("Годовая ставка (%)", interestRate) { interestRate = it }
                }
                Box(modifier = Modifier.weight(1f)) {
                    NumberInputField("Ваш платеж в мес. (₽)", monthlyPayment) { monthlyPayment = it }
                }
            }
        },
        result = result
    )
}

@Composable
fun CalculatorLayout(
    inputs: @Composable ColumnScope.() -> Unit,
    result: CalculationResult
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = inputs
                )
            }
        }

        if (result.errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = result.errorMessage,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else if (result.schedule.isNotEmpty()) {
            item {
                SummaryCard(result)
            }

            item {
                ChartsCard(result)
            }

            item {
                Text(
                    text = "График платежей (по месяцам)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                TableHeader()
            }

            items(result.schedule) { row ->
                TableRow(row)
                HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun SummaryCard(result: CalculationResult) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Итоги расчета", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            SummaryRow("Ежемесячный платеж:", "${result.monthlyPaymentFirst.roundToInt()} ₽")
            SummaryRow("Срок погашения:", "${result.monthsCount} мес. (${String.format("%.1f", result.monthsCount / 12.0)} лет)")
            SummaryRow("Переплата по процентам:", "${result.totalInterest.roundToInt()} ₽", color = Color(0xFFD32F2F))
            SummaryRow("Общая сумма выплат:", "${result.totalPaid.roundToInt()} ₽", weight = FontWeight.Bold)
        }
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    color: Color = Color.Unspecified,
    weight: FontWeight = FontWeight.SemiBold
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.DarkGray)
        Text(text = value, fontWeight = weight, color = color)
    }
}

@Composable
fun ChartsCard(result: CalculationResult) {
    val principalTotal = max(0.0, result.totalPaid - result.totalInterest)
    val principalRatio = if (result.totalPaid > 0) (principalTotal / result.totalPaid).toFloat() else 0f

    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Визуализация", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Text("Структура выплат (Долг / Проценты)", fontSize = 14.sp, color = Color.Gray)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                val widthPrincipal = size.width * principalRatio
                drawRect(
                    color = Color(0xFF4CAF50),
                    topLeft = Offset(0f, 0f),
                    size = Size(widthPrincipal, size.height)
                )
                drawRect(
                    color = Color(0xFFE53935),
                    topLeft = Offset(widthPrincipal, 0f),
                    size = Size(size.width - widthPrincipal, size.height)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem(Color(0xFF4CAF50), "Тело: ${(principalRatio * 100).roundToInt()}%")
                LegendItem(Color(0xFFE53935), "Проценты: ${((1 - principalRatio) * 100).roundToInt()}%")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("График снижения остатка долга", fontSize = 14.sp, color = Color.Gray)
            BalanceLineChart(schedule = result.schedule, initialBalance = principalTotal)
        }
    }
}

@Composable
fun BalanceLineChart(schedule: List<PaymentRow>, initialBalance: Double) {
    if (schedule.isEmpty() || initialBalance <= 0) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFFF2F4F7), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxMonth = schedule.size.toFloat()
            val maxBalance = initialBalance.toFloat()

            for (i in 0..2) {
                val y = size.height * (i / 2f)
                drawLine(
                    color = Color.LightGray,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val path = Path()
            path.moveTo(0f, 0f)

            schedule.forEach { row ->
                val x = (row.month / maxMonth) * size.width
                val y = size.height - ((row.remainingBalance.toFloat() / maxBalance) * size.height)
                path.lineTo(x, y.coerceIn(0f, size.height))
            }

            drawPath(
                path = path,
                color = Color(0xFF1976D2),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        Text(
            text = "${initialBalance.roundToInt()} ₽",
            fontSize = 10.sp,
            color = Color.DarkGray,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Text(
            text = "0 ₽ (${schedule.size} мес.)",
            fontSize = 10.sp,
            color = Color.DarkGray,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun NumberInputField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.all { it.isDigit() || it == '.' }) {
                onValueChange(newValue)
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE3F2FD))
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text("Мес", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("Платеж", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
        Text("Долг", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
        Text("Проценты", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
        Text("Остаток", modifier = Modifier.weight(1.1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
    }
}

@Composable
fun TableRow(row: PaymentRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Text("${row.month}", modifier = Modifier.weight(0.5f), fontSize = 12.sp)
        Text("${row.payment.roundToInt()}", modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.End)
        Text("${row.principal.roundToInt()}", modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color(0xFF2E7D32), textAlign = TextAlign.End)
        Text("${row.interest.roundToInt()}", modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color(0xFFC62828), textAlign = TextAlign.End)
        Text("${row.remainingBalance.roundToInt()}", modifier = Modifier.weight(1.1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
    }
}

// ================= МАТЕМАТИЧЕСКАЯ ЛОГИКА =================

fun calculateMortgage(price: Double, down: Double, years: Int, annualRate: Double): CalculationResult {
    val principal = price - down
    if (principal <= 0) return CalculationResult(0.0, 0.0, 0.0, 0, emptyList(), "Сумма кредита должна быть больше 0")
    if (years <= 0 || years > 50) return CalculationResult(0.0, 0.0, 0.0, 0, emptyList(), "Укажите корректный срок (1–50 лет)")
    if (annualRate <= 0) return CalculationResult(0.0, 0.0, 0.0, 0, emptyList(), "Ставка должна быть больше 0%")

    val months = years * 12
    val monthlyRate = annualRate / 12.0 / 100.0
    val factor = (1 + monthlyRate).pow(months)
    val annuityPayment = principal * (monthlyRate * factor) / (factor - 1)

    var balance = principal
    val schedule = mutableListOf<PaymentRow>()
    var totalInterest = 0.0

    for (m in 1..months) {
        val interest = balance * monthlyRate
        val principalPart = annuityPayment - interest
        balance = max(0.0, balance - principalPart)
        totalInterest += interest

        schedule.add(
            PaymentRow(
                month = m,
                payment = annuityPayment,
                principal = principalPart,
                interest = interest,
                remainingBalance = balance
            )
        )
    }

    return CalculationResult(
        monthlyPaymentFirst = annuityPayment,
        totalPaid = principal + totalInterest,
        totalInterest = totalInterest,
        monthsCount = months,
        schedule = schedule
    )
}

fun calculateCreditCard(debt: Double, annualRate: Double, fixedPayment: Double): CalculationResult {
    if (debt <= 0) return CalculationResult(0.0, 0.0, 0.0, 0, emptyList(), "Введите сумму долга больше 0")
    if (annualRate <= 0) return CalculationResult(0.0, 0.0, 0.0, 0, emptyList(), "Введите корректную ставку")

    val monthlyRate = annualRate / 12.0 / 100.0
    val firstMonthInterest = debt * monthlyRate

    if (fixedPayment <= firstMonthInterest) {
        return CalculationResult(
            0.0, 0.0, 0.0, 0, emptyList(),
            "Платеж слишком мал! В первый месяц начисляется ${firstMonthInterest.roundToInt()} ₽ процентов. Увеличьте ежемесячный платеж, иначе долг никогда не погасится."
        )
    }

    var balance = debt
    var month = 0
    var totalInterest = 0.0
    var totalPaid = 0.0
    val schedule = mutableListOf<PaymentRow>()

    while (balance > 0.01 && month < 600) {
        month++
        val interest = balance * monthlyRate
        val actualPayment = if (balance + interest <= fixedPayment) balance + interest else fixedPayment
        val principalPart = actualPayment - interest
        balance = max(0.0, balance - principalPart)

        totalInterest += interest
        totalPaid += actualPayment

        schedule.add(
            PaymentRow(
                month = month,
                payment = actualPayment,
                principal = principalPart,
                interest = interest,
                remainingBalance = balance
            )
        )
    }

    return CalculationResult(
        monthlyPaymentFirst = fixedPayment,
        totalPaid = totalPaid,
        totalInterest = totalInterest,
        monthsCount = month,
        schedule = schedule
    )
}
