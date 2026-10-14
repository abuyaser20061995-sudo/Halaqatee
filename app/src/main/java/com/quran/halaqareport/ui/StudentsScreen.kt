package com.quran.halaqareport.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quran.halaqareport.data.Student
import com.quran.halaqareport.util.ExportAndBackupManager
import com.quran.halaqareport.viewmodel.HalaqaViewModel

@Composable
fun StudentsScreen(viewModel: HalaqaViewModel) {
    val context = LocalContext.current
    val allStudents by viewModel.students.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val recitations by viewModel.recitationRecords.collectAsState()
    val attendance by viewModel.attendanceRecords.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ACTIVE") }
    val selectedIds = remember { mutableStateListOf<Long>() }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var viewingProfileStudent by remember { mutableStateOf<Student?>(null) }
    var showAddGroupDialog by remember { mutableStateOf(false) }

    val csvImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.importStudentsCsv(it) }
    }

    val filteredStudents = allStudents
        .filter { if (selectedStatusFilter == "ALL") true else it.status == selectedStatusFilter }
        .filter { it.fullName.contains(searchQuery, true) || it.groupNameCache.contains(searchQuery, true) }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { editingStudent = null; showAddEditDialog = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("إضافة طالب")
            }
            OutlinedButton(onClick = { showAddGroupDialog = true }) {
                Text("+ مجموعة")
            }
            IconButton(onClick = { csvImportLauncher.launch("text/*") }) {
                Icon(Icons.Default.UploadFile, contentDescription = "استيراد CSV")
            }
            IconButton(onClick = {
                val file = ExportAndBackupManager.exportStudentsToCsv(context, filteredStudents)
                ExportAndBackupManager.shareFile(context, file, "text/csv", "تصدير CSV")
            }) {
                Icon(Icons.Default.Download, contentDescription = "تصدير CSV")
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("ابحث باسم الطالب أو المجموعة...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = selectedStatusFilter == "ACTIVE", onClick = { selectedStatusFilter = "ACTIVE" }, label = { Text("النشطون") })
            FilterChip(selected = selectedStatusFilter == "PAUSED", onClick = { selectedStatusFilter = "PAUSED" }, label = { Text("متوقف") })
            FilterChip(selected = selectedStatusFilter == "ARCHIVED", onClick = { selectedStatusFilter = "ARCHIVED" }, label = { Text("الأرشيف") })
        }

        if (selectedIds.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("محدد: ${selectedIds.size}", fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        viewModel.archiveStudentsBatch(selectedIds.toList())
                        selectedIds.clear()
                    }) { Text("أرشفة المحددين") }
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredStudents, key = { it.id }) { student ->
                val isSelected = student.id in selectedIds
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { viewingProfileStudent = student },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked -> if (checked) selectedIds.add(student.id) else selectedIds.remove(student.id) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(student.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("المجموعة: ${student.groupNameCache} | المستوى: ${student.quranicLevel}", style = MaterialTheme.typography.bodySmall)
                            Text("آخر موضع: سورة ${student.lastReachedSurah} (${student.lastReachedAyah})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { editingStudent = student; showAddEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل")
                        }
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        StudentFormDialog(
            initial = editingStudent,
            groups = groups.map { it.groupName },
            onDismiss = { showAddEditDialog = false },
            onSave = { st -> viewModel.saveStudent(st); showAddEditDialog = false }
        )
    }

    if (showAddGroupDialog) {
        var gName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text("مجموعة جديدة") },
            text = { OutlinedTextField(value = gName, onValueChange = { gName = it }, label = { Text("اسم المجموعة") }) },
            confirmButton = {
                Button(onClick = {
                    if (gName.isNotBlank()) { viewModel.addGroup(gName, ""); showAddGroupDialog = false }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showAddGroupDialog = false }) { Text("إلغاء") } }
        )
    }

    viewingProfileStudent?.let { st ->
        val stRecs = recitations.filter { it.studentId == st.id }
        val stAtt = attendance.filter { it.studentId == st.id }
        val presentRatio = if (stAtt.isNotEmpty()) (stAtt.count { it.status == "PRESENT" || it.status == "LATE" } * 100) / stAtt.size else 100
        val totalNewPages = stRecs.filter { it.type == "NEW_MEM" }.sumOf { it.pagesCountEquivalent.toDouble() }

        AlertDialog(
            onDismissRequest = { viewingProfileStudent = null },
            title = { Text("ملف الطالب: ${st.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ولي الأمر: ${st.guardianName} (${st.guardianPhone})")
                    Text("نسبة الحضور: $presentRatio% | إجمالي الحفظ: $totalNewPages صفحة")
                    Text("آخر موضع: سورة ${st.lastReachedSurah} الآية ${st.lastReachedAyah}", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = { Button(onClick = { viewingProfileStudent = null }) { Text("إغلاق") } },
            dismissButton = {
                TextButton(onClick = { viewModel.deleteStudent(st); viewingProfileStudent = null }) {
                    Text("حذف", color = Color.Red)
                }
            }
        )
    }
}

@Composable
fun StudentFormDialog(
    initial: Student?,
    groups: List<String>,
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit
) {
    var fullName by remember { mutableStateOf(initial?.fullName ?: "") }
    var age by remember { mutableStateOf(initial?.birthDateOrAge ?: "14") }
    var guardian by remember { mutableStateOf(initial?.guardianName ?: "") }
    var phone by remember { mutableStateOf(initial?.guardianPhone ?: "") }
    var groupName by remember { mutableStateOf(initial?.groupNameCache ?: groups.firstOrNull() ?: "عام") }
    var level by remember { mutableStateOf(initial?.quranicLevel ?: "متوسط") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة طالب جديد" else "تعديل بيانات الطالب") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("الاسم الكامل *") }) }
                item { OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("العمر") }) }
                item { OutlinedTextField(value = guardian, onValueChange = { guardian = it }, label = { Text("اسم ولي الأمر") }) }
                item { OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("هاتف ولي الأمر") }) }
                item { OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("المجموعة") }) }
                item { OutlinedTextField(value = level, onValueChange = { level = it }, label = { Text("المستوى (مبتدئ / متوسط / متقدم)") }) }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (fullName.isNotBlank()) {
                    onSave(
                        (initial ?: Student(
                            studentCode = "ST-${(100..999).random()}",
                            fullName = fullName,
                            birthDateOrAge = age,
                            guardianName = guardian,
                            joinDate = "2026-10-14"
                        )).copy(
                            fullName = fullName,
                            birthDateOrAge = age,
                            guardianName = guardian,
                            guardianPhone = phone,
                            groupNameCache = groupName,
                            quranicLevel = level
                        )
                    )
                }
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
