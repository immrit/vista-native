package ir.coffevista.vista_native.features.profile.settings.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class FlutterFaqItem(
    val category: String,
    val question: String,
    val answer: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FAQScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var searchQuery by remember { mutableStateOf("") }
    val expanded = remember { mutableStateMapOf<Int, Boolean>() }
    val filteredItems = remember(searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) flutterFaqItems else flutterFaqItems.filter { item ->
            item.question.contains(query, ignoreCase = true) ||
                item.answer.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LocalLayoutDirection.current) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "سوالات متداول",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                        }
                    },
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("جستجو در سوالات...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Text("×", fontSize = 24.sp) } }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                if (searchQuery.isEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    ) {
                        items(flutterFaqCategories) { category ->
                            // Flutter renders category chips as informational (not selected/filtering).
                            AssistChip(onClick = {}, label = { Text(category) })
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filteredItems, key = { it.question }) { item ->
                        val sourceIndex = flutterFaqItems.indexOf(item)
                        FlutterFaqCard(
                            item = item,
                            expanded = expanded[sourceIndex] == true,
                            onToggle = { expanded[sourceIndex] = expanded[sourceIndex] != true },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlutterFaqCard(item: FlutterFaqItem, expanded: Boolean, onToggle: () -> Unit) {
    val presentation = faqPresentation(item.category)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.clickable(onClick = onToggle).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(presentation.color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(presentation.icon, contentDescription = null, tint = presentation.color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.question, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp))
                    Text(item.category, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp))
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Text(
                    text = item.answer,
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
                    textAlign = TextAlign.Justify,
                )
            }
        }
    }
}

private data class FaqPresentation(val icon: ImageVector, val color: Color)

private fun faqPresentation(category: String) = when (category) {
    "حساب کاربری" -> FaqPresentation(Icons.Default.Lock, Color(0xFF2196F3))
    "چت و پیام‌رسانی" -> FaqPresentation(Icons.Default.Chat, Color(0xFF9C27B0))
    "پست و محتوا" -> FaqPresentation(Icons.Default.AddCircle, Color(0xFF009688))
    "امنیت" -> FaqPresentation(Icons.Default.Shield, Color(0xFF4CAF50))
    "موزیک" -> FaqPresentation(Icons.Default.MusicNote, Color(0xFF9C27B0))
    "جستجو" -> FaqPresentation(Icons.Default.Search, Color(0xFF3F51B5))
    "استوری" -> FaqPresentation(Icons.Default.CameraAlt, Color(0xFFE91E63))
    "آفلاین" -> FaqPresentation(Icons.Default.OfflineBolt, Color(0xFF757575))
    "محدودیت‌ها" -> FaqPresentation(Icons.Default.Info, Color(0xFF2196F3))
    "مشکلات فنی" -> FaqPresentation(Icons.Default.Speed, Color(0xFFFF9800))
    "ویژگی‌های خاص" -> FaqPresentation(Icons.Default.Verified, Color(0xFFFFC107))
    else -> FaqPresentation(Icons.Default.SupportAgent, Color(0xFF009688))
}
