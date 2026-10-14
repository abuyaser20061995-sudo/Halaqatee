package com.quran.halaqareport.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quran.halaqareport.viewmodel.HalaqaViewModel
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: HalaqaViewModel,
    onNavigateToQuickSession: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val students by viewModel.students.collectAsState()
    val recitations by viewModel.recitationRecords.collectAsState()

    var globalSearchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(profile.halaqaName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${profile.mosqueName} • ${profile.teacherName}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE5C168))
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onNavigateToQuickSession,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("بدء/تسجيل الجلسة اليومية (${viewModel.todayDate})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = globalSearchQuery,
                onValueChange = { globalSearchQuery = it },
                label = { Text("بحث شامل (اسم طالب، سورة، مجموعة...)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        if (globalSearchQuery.isNotBlank()) {
            val matchedStudents = students.filter {
                it.fullName.contains(globalSearchQuery, true) || it.groupNameCache.contains(globalSearchQuery, true)
            }
            val matchedRecs = recitations.filter {
                it.surahName.contains(globalSearchQuery, true) || it.recitationDate.contains(globalSearchQuery)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("نتائج البحث:", fontWeight = FontWeight.Bold)
                        matchedStudents.take(5).forEach { s ->
                            Text("• طالب: ${s.fullName} - آخر موضع: ${s.lastReachedSurah} (${s.lastReachedAyah})")
                        }
                        matchedRecs.take(5).forEach { r ->
                            Text("• تسميع: ${r.surahName} (${r.fromAyah}-${r.toAyah}) بتاريخ ${r.recitationDate}")
                        }
                    }
                }
            }
        }

        item {
            Text("ملخص الحلقة اليومي والعام", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatMetricCard("النشطون", "${metrics.activeStudents}/${metrics.totalStudents}", Icons.Default.Groups, Modifier.weight(1f))
                StatMetricCard("حضور اليوم", "${metrics.todayPresent} (${metrics.todayAttendancePercent}%)", Icons.Default.CheckCircle, Modifier.weight(1f))
                StatMetricCard("غياب وتأخر", "غ:${metrics.todayAbsent} | ت:${metrics.todayLate}", Icons.Default.Warning, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatMetricCard("سُمّع لهم", "${metrics.todayRecitedStudentsCount}", Icons.Default.MenuBook, Modifier.weight(1f))
                StatMetricCard("لم يسمعوا", "${metrics.todayNotRecitedCount}", Icons.Default.Schedule, Modifier.weight(1f))
                StatMetricCard("إنجاز اليوم", String.format(Locale.US, "%.1f ص", metrics.todayTotalPagesMemorized + metrics.todayTotalPagesRevised), Icons.Default.AutoGraph, Modifier.weight(1f))
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("مؤشرات الحفظ والمراجعة والانتظام", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    val memPages = metrics.todayTotalPagesMemorized
                    val revPages = metrics.todayTotalPagesRevised
                    val maxVal = (memPages + revPages).coerceAtLeast(1f)

                    Text("الحفظ الجديد ($memPages ص) مقابل المراجعة ($revPages ص):", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                    Canvas(modifier = Modifier.fillMaxWidth().height(22.dp)) {
                        val memWidth = (memPages / maxVal) * size.width
                        drawRect(color = Color(0xFF0D4F3C), size = Size(memWidth, size.height))
                        drawRect(color = Color(0xFFC69C45), topLeft = Offset(memWidth, 0f), size = Size(size.width - memWidth, size.height))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("نسبة الانتظام العامة للحلقة: ${metrics.overallAttendancePercent}%")
                    LinearProgressIndicator(
                        progress = { metrics.overallAttendancePercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                }
            }
        }

        item {
            Text("تنبيهات مهمة (${metrics.smartAlerts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(metrics.smartAlerts) { alert ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(alert.title, fontWeight = FontWeight.Bold)
                        Text(alert.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun StatMetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
