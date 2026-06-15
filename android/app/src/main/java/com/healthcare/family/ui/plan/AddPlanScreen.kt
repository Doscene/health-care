package com.healthcare.family.ui.plan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

private val departments = listOf(
    "心内科", "内分泌科", "肾内科", "神经内科", "骨科", "全科",
)

/**
 * 添加复诊计划页面，对接后端 Appointment 接口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlanScreen(
    onBack: () -> Unit,
    onPlanAdded: () -> Unit,
    viewModel: PlanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var hospital by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf("") }
    var isDeptExpanded by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf("") }
    var reminderDays by remember { mutableIntStateOf(3) }

    LaunchedEffect(state.addSuccess) {
        if (state.addSuccess) {
            viewModel.clearAddSuccess()
            onPlanAdded()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("添加复诊") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            ExposedDropdownMenuBox(
                expanded = isDeptExpanded,
                onExpandedChange = { isDeptExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedDept,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("科室") },
                    placeholder = { Text("请选择科室") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDeptExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = isDeptExpanded,
                    onDismissRequest = { isDeptExpanded = false },
                ) {
                    departments.forEach { dept ->
                        DropdownMenuItem(
                            text = { Text(dept) },
                            onClick = {
                                selectedDept = dept
                                isDeptExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = hospital,
                onValueChange = { hospital = it },
                label = { Text("医院") },
                placeholder = { Text("请输入医院名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("复诊日期") },
                placeholder = { Text("如 2026-06-20") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = reminderDays.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { reminderDays = it }
                },
                label = { Text("提前提醒（天）") },
                placeholder = { Text("3") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            state.errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.addPlan(hospital, selectedDept, date, reminderDays)
                },
                enabled = selectedDept.isNotBlank() && hospital.isNotBlank() && date.isNotBlank() && !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("添加复诊计划", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
