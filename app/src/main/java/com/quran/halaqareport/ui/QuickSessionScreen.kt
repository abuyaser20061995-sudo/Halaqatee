package com.quran.halaqareport.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quran.halaqareport.data.QuranData
import com.quran.halaqareport.data.Student
import com.quran.halaqareport.data.StudentEvaluation
import com.quran.halaqareport.data.SurahInfo
import com.quran.halaqareport.viewmodel.HalaqaViewModel

@Composable
fun QuickSessionScreen(viewModel: HalaqaViewModel) {
    val activeStudents by viewModel.students.collectAsState()
    val studentsList = activeStudents.filter { it.status == "ACTIVE" }

    var sessionDate by remember { mutableStateOf(viewModel.todayDate) }
    var isSessionCancelled by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    val attendanceState = remember { mutableStateMapOf<Long, Pair<String, String>>() }
    LaunchedEffect(studentsList) {
        studentsList.forEach { st ->
            if (!attendanceState.containsKey(st.id)) {
                attendanceState[st.id] = "PRESENT" to ""
            }
        }
    }

    var recitingStudent by remember { mutableStateOf<Student?>(null) }
    var evaluatingStudent by remember { mutableStateOf<Student?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = sessionDate,
                        onValueChange = { sessionDate = it },
                        label = { Text("تاريخ الجلسة") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = isSessionCancelled,
                        onClick = { isSessionCancelled = !isSessionCancelled },
                        label = { Text(if (isSessionCancelled) "ملغاة" else "منعقدة") }
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = {
                        studentsList.forEach { attendanceState[it.id] = "PRESENT" to "" }
                    }) {
                        Text("تحضير الجميع")
                    }
                    Button(onClick = {
                        viewModel.saveCompleteSession(
                            date = sessionDate,
                            status = if (isSessionCancelled) "CANCELLED" else "HELD",
                            cancelReason = cancelReason,
                            generalEvents = "",
                            teacherNotes = "",
                            attendanceMap = attendanceState.toMap()
                        )
                    }) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("حفظ الجلسة")
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (!isSessionCancelled) {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(studentsList, key = { it.id }) { student ->
                    val currentAtt = attendanceState[student.id] ?: ("PRESENT" to "")
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(student.fullName, fontWeight = FontWeight.Bold)
                                    Text("آخر موضع: ${student.lastReachedSurah} (${student.lastReachedAyah})", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Row {
                                    FilledTonalButton(onClick = { recitingStudent = student }) {
                                        Text("تسميع")
                                    }
                                    IconButton(onClick = { evaluatingStudent = student }) {
                                        Icon(Icons.Default.StarRate, contentDescription = "تقييم", tint = GoldAccent)
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val statuses = listOf(
                                    "PRESENT" to "حاضر",
                                    "LATE" to "متأخر",
                                    "EXCUSED_LEAVE" to "مستأذن",
                                    "ABSENT_EXCUSED" to "بعذر",
                                    "ABSENT_UNEXCUSED" to "غياب"
                                )
                                statuses.forEach { (code, label) ->
                                    FilterChip(
                                        selected = currentAtt.first == code,
                                        onClick = { attendanceState[student.id] = code to currentAtt.second },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    recitingStudent?.let { st ->
        RecitationEntryDialog(
            student = st,
            onDismiss = { recitingStudent = null },
            onSave = { type, surah, fromAyah, toAyah, amountLabel, pagesEq, mistakes, mistakeTypes, score, rating, notes, homework ->
                viewModel.recordRecitation(
                    date = sessionDate,
                    student = st,
                    type = type,
                    surah = surah,
                    fromAyah = fromAyah,
                    toAyah = toAyah,
                    amountLabel = amountLabel,
                    pagesEquivalent = pagesEq,
                    mistakes = mistakes,
                    mistakeTypes = mistakeTypes,
                    score = score,
                    rating = rating,
                    notes = notes,
                    nextHomework = homework
                )
                recitingStudent = null
            }
        )
    }

    evaluatingStudent?.let { st ->
        var score by remember { mutableStateOf("95") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { evaluatingStudent = null },
            title = { Text("تقييم الطالب: ${st.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = score, onValueChange = { score = it }, label = { Text("الدرجة العامة (من 100)") })
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات تربوية") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val s = score.toIntOrNull() ?: 95
                    viewModel.saveEvaluation(StudentEvaluation(studentId = st.id, evalDate = sessionDate, overallScore = s, constructiveNotes = notes))
                    evaluatingStudent = null
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { evaluatingStudent = null }) { Text("إلغاء") } }
        )
    }
}

@Composable
fun RecitationEntryDialog(
    student: Student,
    onDismiss: () -> Unit,
    onSave: (String, SurahInfo, Int, Int, String, Float, Int, String, Int, String, String, String) -> Unit
) {
    var recType by remember { mutableStateOf("NEW_MEM") }
    var surahSearch by remember { mutableStateOf(student.lastReachedSurah) }
    val matchedSurah = QuranData.surahs.find { it.name.contains(surahSearch.trim()) } ?: QuranData.surahs.first()

    var fromAyah by remember { mutableStateOf("1") }
    var toAyah by remember { mutableStateOf(matchedSurah.maxAyahs.toString()) }
    var selectedQuickIdx by remember { mutableIntStateOf(1) }
    var mistakesCount by remember { mutableStateOf("0") }
    var score by remember { mutableStateOf("95") }
    var rating by remember { mutableStateOf("ممتاز") }
    var nextHomework by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسميع: ${student.fullName}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = recType == "NEW_MEM", onClick = { recType = "NEW_MEM" }, label = { Text("حفظ جديد") })
                        FilterChip(selected = recType == "NEAR_REV", onClick = { recType = "NEAR_REV" }, label = { Text("مراجعة") })
                    }
                }
                item {
                    OutlinedTextField(
                        value = surahSearch,
                        onValueChange = { surahSearch = it },
                        label = { Text("السورة (${matchedSurah.name} - آياتها ${matchedSurah.maxAyahs})") }
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = fromAyah, onValueChange = { fromAyah = it }, label = { Text("من آية") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = toAyah, onValueChange = { toAyah = it }, label = { Text("إلى آية") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        QuranData.quickAmounts.take(3).forEachIndexed { idx, pair ->
                            FilterChip(
                                selected = selectedQuickIdx == idx,
                                onClick = { selectedQuickIdx = idx },
                                label = { Text(pair.first) }
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = mistakesCount, onValueChange = { mistakesCount = it }, label = { Text("الأخطاء") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = score, onValueChange = { score = it }, label = { Text("الدرجة") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    OutlinedTextField(value = nextHomework, onValueChange = { nextHomework = it }, label = { Text("الواجب القادم") })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val quick = QuranData.quickAmounts[selectedQuickIdx]
                onSave(
                    recType, matchedSurah,
                    fromAyah.toIntOrNull() ?: 1,
                    toAyah.toIntOrNull() ?: matchedSurah.maxAyahs,
                    quick.first, quick.second,
                    mistakesCount.toIntOrNull() ?: 0, "",
                    score.toIntOrNull() ?: 95, rating, "", nextHomework
                )
            }) { Text("حفظ التسميع") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
