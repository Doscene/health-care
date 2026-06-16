package com.healthcare.family.ui.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * 服药日历页面
 * 显示月视图，带状态颜色标记
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationCalendarScreen(
    onBack: () -> Unit,
    viewModel: MedicationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(currentMonth) {
        viewModel.loadCalendarData(currentMonth.year, currentMonth.monthValue)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("服药日历") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            // 月份切换
            MonthSelector(
                currentMonth = currentMonth,
                onPrevious = { currentMonth = currentMonth.minusMonths(1) },
                onNext = { currentMonth = currentMonth.plusMonths(1) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 星期标题
            WeekdayHeader()

            Spacer(modifier = Modifier.height(8.dp))

            // 日历网格
            CalendarGrid(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                calendarData = uiState.calendarData,
                onDateSelected = { selectedDate = it },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 图例
            Legend()

            Spacer(modifier = Modifier.height(16.dp))

            // 选中日期的详情
            selectedDate?.let { date ->
                DayDetailCard(
                    date = date,
                    records = uiState.calendarData[date.toString()] ?: emptyList(),
                )
            }
        }
    }
}

/**
 * 月份选择器
 */
@Composable
private fun MonthSelector(
    currentMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
        }

        Text(
            text = "${currentMonth.year}年${currentMonth.monthValue}月",
            style = MaterialTheme.typography.titleLarge,
        )

        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
        }
    }
}

/**
 * 星期标题
 */
@Composable
private fun WeekdayHeader() {
    val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(modifier = Modifier.fillMaxWidth()) {
        weekdays.forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 日历网格
 */
@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    calendarData: Map<String, List<MedicationCalendarRecord>>,
    onDateSelected: (LocalDate) -> Unit,
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1=Monday, 7=Sunday

    // 生成日期列表
    val days = mutableListOf<LocalDate?>()

    // 添加上个月的填充日期
    for (i in 1 until firstDayOfWeek) {
        days.add(null)
    }

    // 添加本月的日期
    for (day in 1..lastDayOfMonth.dayOfMonth) {
        days.add(currentMonth.atDay(day))
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(days) { date ->
            if (date != null) {
                val dateString = date.toString()
                val records = calendarData[dateString] ?: emptyList()
                val isToday = date == LocalDate.now()
                val isSelected = date == selectedDate

                CalendarDay(
                    date = date,
                    records = records,
                    isToday = isToday,
                    isSelected = isSelected,
                    onClick = { onDateSelected(date) },
                )
            } else {
                // 空白填充
                Box(modifier = Modifier.aspectRatio(1f))
            }
        }
    }
}

/**
 * 日历日期单元格
 */
@Composable
private fun CalendarDay(
    date: LocalDate,
    records: List<MedicationCalendarRecord>,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val statusColor = when {
        records.isEmpty() -> Color.Transparent
        records.all { it.status == "taken" } -> Color(0xFF4CAF50) // 全部服用 - 绿色
        records.any { it.status == "missed" } -> Color(0xFFF44336) // 有漏服 - 红色
        records.any { it.status == "pending" } -> Color(0xFFFF9800) // 有未服用 - 橙色
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    isToday -> MaterialTheme.colorScheme.secondaryContainer
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isToday) 2.dp else 0.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    isToday -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )

            // 状态指示点
            if (records.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
            }
        }
    }
}

/**
 * 图例
 */
@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        LegendItem(color = Color(0xFF4CAF50), label = "全部服用")
        LegendItem(color = Color(0xFFFF9800), label = "待服用")
        LegendItem(color = Color(0xFFF44336), label = "漏服")
    }
}

/**
 * 图例项
 */
@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 日期详情卡片
 */
@Composable
private fun DayDetailCard(
    date: LocalDate,
    records: List<MedicationCalendarRecord>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${date.monthValue}月${date.dayOfMonth}日 服药记录",
                style = MaterialTheme.typography.titleMedium,
            )

            if (records.isEmpty()) {
                Text(
                    text = "当日无服药计划",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                records.forEach { record ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = record.medicationName,
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        Text(
                            text = when (record.status) {
                                "taken" -> "已服用"
                                "missed" -> "漏服"
                                "pending" -> "待服用"
                                else -> record.status
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (record.status) {
                                "taken" -> Color(0xFF4CAF50)
                                "missed" -> Color(0xFFF44336)
                                "pending" -> Color(0xFFFF9800)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 服药日历记录
 */
data class MedicationCalendarRecord(
    val medicationName: String,
    val status: String, // taken, missed, pending
    val scheduledTime: String,
)
