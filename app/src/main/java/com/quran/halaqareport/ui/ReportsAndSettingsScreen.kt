package com.quran.halaqareport.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quran.halaqareport.util.ExportAndBackupManager
import com.quran.halaqareport.viewmodel.HalaqaViewModel
import java.util.Locale

@Composable
fun ReportsAndSettingsScreen(viewModel: HalaqaViewModel) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val students by viewModel.students.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val attendance by viewModel.attendanceRecords.collectAsState()
    val recitations by viewModel.recitationRecords.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var startDate by remember { mutableStateOf("2026-01-01") }
    var endDate by remember { mutableStateOf(viewModel.todayDate) }
    var teacherRecommendations by remember { mutableStateOf("التركيز على مراجعة الأجزاء الخمسة الأولى.") }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> uri?.let { viewModel.exportFullBackup(it) } }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.restoreFullBackup(it) } }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("التقارير (PDF/واتساب)") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("الإعدادات والنسخ الاحتياطي") })
        }

        if (selectedTab == 0) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("من تاريخ") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("إلى تاريخ") }, modifier = Modifier.weight(1f))
                    }
                }

                item {
                    OutlinedTextField(
                        value = teacherRecommendations,
                        onValueChange = { teacherRecommendations = it },
                        label = { Text("توصيات المعلم في التقرير") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Button(
                        onClick = {
                            val activeSt = students.filter { it.status == "ACTIVE" }
                            val periodAtt = attendance.filter { it.sessionDate in startDate..endDate }
                            val periodRec = recitations.filter { it.recitationDate in startDate..endDate }

                            val rows = activeSt.mapIndexed { idx, st ->
                                val stAtt = periodAtt.filter { it.studentId == st.id }
                                val attRate = if (stAtt.isNotEmpty()) "${(stAtt.count { it.status == "PRESENT" || it.status == "LATE" } * 100) / stAtt.size}%" else "100%"
                                val stNewPages = periodRec.filter { it.studentId == st.id && it.type == "NEW_MEM" }.sumOf { it.pagesCountEquivalent.toDouble() }
                                val stRevPages = periodRec.filter { it.studentId == st.id && it.type != "NEW_MEM" }.sumOf { it.pagesCountEquivalent.toDouble() }
                                listOf(
                                    "${idx + 1}", st.fullName, st.groupNameCache, attRate,
                                    String.format(Locale.US, "%.1f ص", stNewPages),
                                    String.format(Locale.US, "%.1f ص", stRevPages),
                                    "${st.lastReachedSurah} (${st.lastReachedAyah})"
                                )
                            }

                            val totalNew = periodRec.filter { it.type == "NEW_MEM" }.sumOf { it.pagesCountEquivalent.toDouble() }
                            val totalRev = periodRec.filter { it.type != "NEW_MEM" }.sumOf { it.pagesCountEquivalent.toDouble() }

                            val pdfFile = ExportAndBackupManager.generateAndSharePdfReport(
                                context = context,
                                profile = profile,
                                reportTitle = "تقرير أداء الحلقة - ${profile.halaqaName}",
                                periodSubtitle = "الفترة: من $startDate إلى $endDate",
                                summaryMetrics = listOf(
                                    "الطلاب النشطون" to "${activeSt.size}",
                                    "الجلسات المنعقدة" to "${sessions.count { it.sessionDate in startDate..endDate && it.status == "HELD" }}",
                                    "الحفظ الجديد" to String.format(Locale.US, "%.1f ص", totalNew),
                                    "المراجعة" to String.format(Locale.US, "%.1f ص", totalRev),
                                    "المستوى" to profile.generalLevel,
                                    "الإشراف" to profile.supervisorName
                                ),
                                tableHeaders = listOf("م", "اسم الطالب", "المجموعة", "الحضور", "حفظ جديد", "مراجعة", "آخر موضع"),
                                tableRows = rows,
                                footerNotes = teacherRecommendations
                            )
                            ExportAndBackupManager.shareFile(context, pdfFile, "application/pdf", "مشاركة تقرير PDF")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("تصدير تقرير PDF رسمي (A4)")
                    }
                }

                item {
                    OutlinedButton(
                        onClick = {
                            val summaryText = viewModel.buildWhatsAppParentsSummary(startDate, endDate)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, summaryText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "إرسال ملخص واتساب"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("مشاركة ملخص أولياء الأمور عبر واتساب")
                    }
                }
            }
        } else {
            var halaqaName by remember(profile) { mutableStateOf(profile.halaqaName) }
            var mosqueName by remember(profile) { mutableStateOf(profile.mosqueName) }
            var teacherName by remember(profile) { mutableStateOf(profile.teacherName) }
            var darkTheme by remember(profile) { mutableStateOf(profile.darkThemeEnabled) }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text("النسخ الاحتياطي والاستعادة", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { createBackupLauncher.launch("Halaqa_Backup.json") }, modifier = Modifier.weight(1f)) {
                            Text("تصدير نسخة")
                        }
                        OutlinedButton(onClick = { restoreBackupLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) {
                            Text("استعادة نسخة")
                        }
                    }
                }

                item {
                    Text("البيانات التجريبية", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { viewModel.loadLabeledSampleData() }) { Text("إدراج بيانات تجريبية") }
                        OutlinedButton(onClick = { viewModel.deleteSampleDataOnly() }) { Text("حذف التجريبية", color = Color.Red) }
                    }
                }

                item { OutlinedTextField(value = halaqaName, onValueChange = { halaqaName = it }, label = { Text("اسم الحلقة") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = mosqueName, onValueChange = { mosqueName = it }, label = { Text("اسم المسجد") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = teacherName, onValueChange = { teacherName = it }, label = { Text("اسم المعلم") }, modifier = Modifier.fillMaxWidth()) }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الوضع الليلي الداكن")
                        Switch(checked = darkTheme, onCheckedChange = { darkTheme = it })
                    }
                }

                item {
                    Button(
                        onClick = { viewModel.saveProfile(profile.copy(halaqaName = halaqaName, mosqueName = mosqueName, teacherName = teacherName, darkThemeEnabled = darkTheme)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("حفظ الإعدادات") }
                }
            }
        }
    }
}
