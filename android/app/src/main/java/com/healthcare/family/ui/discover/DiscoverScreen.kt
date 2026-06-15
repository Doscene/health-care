package com.healthcare.family.ui.discover

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 发现页面：知识文章、社区入口、健康工具。
 */
@Composable
fun DiscoverScreen(@Suppress("UNUSED_PARAMETER") onNavigate: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "发现",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        // 功能入口
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DiscoverCard(
                    icon = Icons.Default.HealthAndSafety,
                    title = "健康知识",
                    subtitle = "高血压/糖尿病科普",
                    color = Color(0xFFEFF6FF),
                    onClick = { },
                    modifier = Modifier.weight(1f),
                )
                DiscoverCard(
                    icon = Icons.Default.Lightbulb,
                    title = "并发症风险",
                    subtitle = "10年心血管评估",
                    color = Color(0xFFFFFBEB),
                    onClick = { },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DiscoverCard(
                    icon = Icons.Default.Group,
                    title = "社区小组",
                    subtitle = "病友交流互助",
                    color = Color(0xFFFDF2F8),
                    onClick = { },
                    modifier = Modifier.weight(1f),
                )
                DiscoverCard(
                    icon = Icons.AutoMirrored.Filled.Article,
                    title = "用药指南",
                    subtitle = "常见药物说明",
                    color = Color(0xFFF0FDF4),
                    onClick = { },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 知识文章列表
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("热门文章", style = MaterialTheme.typography.titleLarge)
        }

        val articles = listOf(
            Triple("高血压患者每日饮食建议", "健康知识", "了解低钠饮食的重要性"),
            Triple("糖尿病患者的运动指南", "健康知识", "科学运动，控制血糖"),
            Triple("如何正确测量血压", "用药指南", "测量血压的正确姿势和时间"),
            Triple("降压药什么时候吃最好", "用药指南", "服药时间对药效的影响"),
            Triple("家庭急救常识", "健康知识", "突发情况的应急处理"),
        )

        articles.forEach { (title, category, desc) ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(end = 8.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
