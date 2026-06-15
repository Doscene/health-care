package com.healthcare.family.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

enum class UserRole { PATIENT, FAMILY }

enum class DiseaseType(val label: String) {
    HYPERTENSION("高血压"),
    DIABETES("糖尿病"),
    BOTH("双病"),
}

/**
 * 角色选择页面：我是患者 / 我是家人。
 */
@Composable
fun RoleSelectionScreen(
    onRoleConfirmed: (UserRole, String?) -> Unit,
    viewModel: RoleSelectionViewModel = hiltViewModel(),
) {
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    var selectedDisease by remember { mutableStateOf<DiseaseType?>(null) }
    var familyName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "请选择您的角色",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "我们将为您定制专属的健康管理方案",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 角色卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RoleCard(
                title = "我是患者",
                subtitle = "记录自己的健康数据",
                icon = Icons.Default.Favorite,
                isSelected = selectedRole == UserRole.PATIENT,
                onClick = { selectedRole = UserRole.PATIENT },
                modifier = Modifier.weight(1f),
            )
            RoleCard(
                title = "我是家人",
                subtitle = "关注家人的健康状况",
                icon = Icons.Default.Person,
                isSelected = selectedRole == UserRole.FAMILY,
                onClick = { selectedRole = UserRole.FAMILY },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 根据角色显示不同选项
        when (selectedRole) {
            UserRole.PATIENT -> {
                Text(
                    text = "请选择您的病种",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DiseaseType.entries.forEach { disease ->
                        FilterChip(
                            selected = selectedDisease == disease,
                            onClick = { selectedDisease = disease },
                            label = { Text(disease.label) },
                        )
                    }
                }
            }
            UserRole.FAMILY -> {
                OutlinedTextField(
                    value = familyName,
                    onValueChange = { familyName = it },
                    label = { Text("您的姓名") },
                    placeholder = { Text("请输入姓名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            null -> { /* 未选择 */ }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 确认按钮
        Button(
            onClick = {
                when (selectedRole) {
                    UserRole.PATIENT -> onRoleConfirmed(UserRole.PATIENT, selectedDisease?.label)
                    UserRole.FAMILY -> onRoleConfirmed(UserRole.FAMILY, familyName.ifEmpty { null })
                    null -> {}
                }
            },
            enabled = selectedRole != null && (
                (selectedRole == UserRole.PATIENT && selectedDisease != null) ||
                (selectedRole == UserRole.FAMILY && familyName.isNotBlank())
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("确认", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .height(160.dp)
            .selectable(selected = isSelected, onClick = onClick),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
