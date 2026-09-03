package ir.coffevista.vista_native.features.profile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import java.time.LocalDate

data class JalaliDate(val year: Int, val month: Int, val day: Int) {
    val displayValue: String
        get() = "%04d/%02d/%02d".format(year, month, day).toPersianDigits()
}

@Composable
fun JalaliDatePickerDialog(
    initialValue: String,
    onDismissRequest: () -> Unit,
    onDateSelected: (JalaliDate) -> Unit,
) {
    val today = rememberJalaliToday()
    val initial = parseJalaliDate(initialValue) ?: today
    var year by rememberSaveable { mutableIntStateOf(initial.year) }
    var month by rememberSaveable { mutableIntStateOf(initial.month) }
    var day by rememberSaveable { mutableIntStateOf(initial.day) }
    val maxDay = jalaliMonthLength(year, month)
    if (day > maxDay) day = maxDay

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("انتخاب تاریخ تولد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تاریخ را بر اساس تقویم شمسی انتخاب کنید.")
                JalaliPickerRow(
                    label = "سال",
                    values = (1200..today.year).toList().asReversed(),
                    selected = year,
                    labelFor = Int::toString,
                    onSelected = { year = it },
                )
                JalaliPickerRow(
                    label = "ماه",
                    values = (1..12).toList(),
                    selected = month,
                    labelFor = { PERSIAN_MONTHS[it - 1] },
                    onSelected = { month = it },
                )
                JalaliPickerRow(
                    label = "روز",
                    values = (1..maxDay).toList(),
                    selected = day,
                    labelFor = Int::toString,
                    onSelected = { day = it },
                )
            }
        },
        confirmButton = {
            Button(onClick = { onDateSelected(JalaliDate(year, month, day)) }) { Text("تأیید") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text("انصراف") }
        },
    )
}

@Composable
private fun JalaliPickerRow(
    label: String,
    values: List<Int>,
    selected: Int,
    labelFor: (Int) -> String,
    onSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(values, key = { it }) { value ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelected(value) },
                    label = { Text(labelFor(value).toPersianDigits()) },
                )
            }
        }
    }
}

@Composable
fun rememberJalaliToday(): JalaliDate = remember {
    val today = LocalDate.now()
    gregorianToJalali(today.year, today.monthValue, today.dayOfMonth)
}

fun parseJalaliDate(value: String): JalaliDate? {
    val parts = value.normalizeDigits().split('/', '-').mapNotNull(String::toIntOrNull)
    if (parts.size != 3) return null
    val (year, month, day) = parts
    return JalaliDate(year, month, day).takeIf {
        year in 1200..1500 && month in 1..12 && day in 1..jalaliMonthLength(year, month)
    }
}

fun jalaliToGregorianIso(value: String): String? {
    val jalali = parseJalaliDate(value) ?: return null
    val jy = jalali.year
    val jm = jalali.month
    val jd = jalali.day
    var jYear = jy - 979
    var jMonth = jm - 1
    var jDay = jd - 1
    var days = 365 * jYear + (jYear / 33) * 8 + ((jYear % 33 + 3) / 4) + 78 + jDay
    days += if (jMonth < 6) jMonth * 31 else jMonth * 30 + 6
    var gYear = 1600 + 400 * (days / 146097)
    days %= 146097
    var leap = true
    if (days >= 36525) {
        days--
        gYear += 100 * (days / 36524)
        days %= 36524
        if (days >= 365) days++ else leap = false
    }
    gYear += 4 * (days / 1461)
    days %= 1461
    if (days >= 366) {
        leap = false
        days--
        gYear += days / 365
        days %= 365
    }
    val monthDays = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var gMonth = 0
    while (gMonth < 11 && days >= monthDays[gMonth]) days -= monthDays[gMonth++]
    return "%04d-%02d-%02d".format(gYear, gMonth + 1, days + 1)
}

fun gregorianIsoToJalaliDisplay(value: String): String? {
    val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return null
    return gregorianToJalali(date.year, date.monthValue, date.dayOfMonth).displayValue
}

fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
    val gregorianMonthDays = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val normalizedYear = gy - 1600
    val normalizedMonth = gm - 1
    val normalizedDay = gd - 1
    var dayNumber = 365 * normalizedYear + (normalizedYear + 3) / 4 -
        (normalizedYear + 99) / 100 + (normalizedYear + 399) / 400
    for (monthIndex in 0 until normalizedMonth) dayNumber += gregorianMonthDays[monthIndex]
    if (normalizedMonth > 1 && (gy % 4 == 0 && (gy % 100 != 0 || gy % 400 == 0))) dayNumber++
    dayNumber += normalizedDay

    var jalaliDayNumber = dayNumber - 79
    val cycle = jalaliDayNumber / 12053
    jalaliDayNumber %= 12053
    var jalaliYear = 979 + 33 * cycle + 4 * (jalaliDayNumber / 1461)
    jalaliDayNumber %= 1461
    if (jalaliDayNumber >= 366) {
        jalaliYear += (jalaliDayNumber - 1) / 365
        jalaliDayNumber = (jalaliDayNumber - 1) % 365
    }
    val jalaliMonth = if (jalaliDayNumber < 186) 1 + jalaliDayNumber / 31 else 7 + (jalaliDayNumber - 186) / 30
    val jalaliDay = if (jalaliDayNumber < 186) 1 + jalaliDayNumber % 31 else 1 + (jalaliDayNumber - 186) % 30
    return JalaliDate(jalaliYear, jalaliMonth, jalaliDay)
}

fun jalaliMonthLength(year: Int, month: Int): Int = when (month) {
    in 1..6 -> 31
    in 7..11 -> 30
    12 -> if (isJalaliLeapYear(year)) 30 else 29
    else -> 0
}

fun isJalaliLeapYear(year: Int): Boolean {
    val remainder = (year - 474).floorMod(2820) + 474
    return ((remainder + 38) * 682 % 2816) < 682
}

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus

fun String.normalizeDigits(): String = buildString(length) {
    this@normalizeDigits.forEach { character ->
        append(
            when (character) {
                '۰' -> '0'; '۱' -> '1'; '۲' -> '2'; '۳' -> '3'; '۴' -> '4'
                '۵' -> '5'; '۶' -> '6'; '۷' -> '7'; '۸' -> '8'; '۹' -> '9'
                else -> character
            },
        )
    }
}

fun String.toPersianDigits(): String = buildString(length) {
    this@toPersianDigits.forEach { character ->
        append(
            when (character) {
                '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'
                '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'
                else -> character
            },
        )
    }
}

val PERSIAN_MONTHS = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
)
