package com.healthcare.family.ui.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 食谱详情页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onBack: () -> Unit,
    viewModel: DietViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(recipeId) { viewModel.loadRecipeDetail(recipeId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.selectedRecipe?.name ?: "食谱详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val recipe = uiState.selectedRecipe
            if (recipe == null) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("食谱不存在", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 基本信息
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(recipe.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("热量: ${recipe.calories} kcal", style = MaterialTheme.typography.bodyMedium)
                                    Text("钠: ${recipe.sodium} mg", style = MaterialTheme.typography.bodyMedium)
                                    Text("GI: ${recipe.glycemicIndex}", style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("适合: ${recipe.suitableFor.joinToString("、")}", style = MaterialTheme.typography.bodySmall)
                                Text("份量: ${recipe.servings}人份", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // 食材
                    item {
                        Text("食材清单", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            val ingredients = recipe.ingredients as? List<*> ?: emptyList<Any>()
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (ingredients.isEmpty()) {
                                    Text("暂无食材信息", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                ingredients.forEach { ing ->
                                    val map = ing as? Map<*, *>
                                    if (map != null) {
                                        Text("• ${map["name"] ?: ""} ${map["amount"] ?: ""}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }

                    // 步骤
                    item {
                        Text("烹饪步骤", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            val steps = recipe.steps as? List<*> ?: emptyList<Any>()
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (steps.isEmpty()) {
                                    Text("暂无步骤信息", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                steps.forEachIndexed { index, step ->
                                    Text("${index + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
