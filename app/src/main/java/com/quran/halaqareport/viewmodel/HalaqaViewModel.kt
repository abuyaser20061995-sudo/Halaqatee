package com.quran.halaqareport.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quran.halaqareport.data.*
import com.quran.halaqareport.util.ExportAndBackupManager
import com.quran.halaqareport.util.FullBackupPayload
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class SmartAlert(val title: String, val description: String, val severity: String)

data class DashboardMetrics(
    val totalStudents: Int = 0,
    val activeStudents: Int = 0,
    val todayPresent: Int = 0,
    val todayAbsent: Int = 0,
    val todayLate: Int = 0,
    val todayAttendancePercent: Int = 0,
    val todayRecitedStudentsCount: Int = 0,
    val todayNotRecitedCount: Int = 0,
    val todayTotalPagesMemorized: Float = 0f,
    val todayTotalPagesRevised: Float = 0f,
    val pendingAssignmentsCount: Int = 0,
    val overallAttendancePercent: Int = 0,
    val levelDistribution: Map<String, Int> = emptyMap(),
    val smartAlerts: List<SmartAlert> = emptyList()
)

class HalaqaViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).halaqaDao()
    val todayDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val profile: StateFlow<HalaqaProfile> = dao.getHalaqaProfile()
        .map { it ?: HalaqaProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HalaqaProfile())

    val students: StateFlow<List<Student>> = dao.getAllStudentsIncludingArchived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups: StateFlow<List<StudentGroup>> = dao.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<HalaqaSession>> = dao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceRecords: StateFlow<List<AttendanceRecord>> = dao.getAllAttendance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recitationRecords: StateFlow<List<RecitationRecord>> = dao.getAllRecitations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val evaluations: StateFlow<List<StudentEvaluation>> = dao.getAllEvaluations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assignments: StateFlow<List<AssignmentRecord>> = dao.getAllAssignments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun clearStatusMessage() { _statusMessage.value = null }
    fun postMessage(msg: String) { _statusMessage.value = msg }

    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        students, sessions, attendanceRecords, recitationRecords, assignments
    ) { stuList, sessList, attList, recList, assignList ->
        val activeList = stuList.filter { it.status == "ACTIVE" }
        val heldSessionIds = sessList.filter { it.status == "HELD" }.map { it.sessionId }.toSet()

        val todayAtt = attList.filter { it.sessionDate == todayDate }
        val presentToday = todayAtt.count { it.status == "PRESENT" }
        val lateToday = todayAtt.count { it.status == "LATE" }
        val absentToday = todayAtt.count { it.status.startsWith("ABSENT") }

        val todayAttPercent = if (todayAtt.isNotEmpty()) {
            ((presentToday + lateToday) * 100) / todayAtt.size
        } else 0

        val todayRecs = recList.filter { it.recitationDate == todayDate }
        val recitedStudentIdsToday = todayRecs.map { it.studentId }.toSet()
        val newPagesToday = todayRecs.filter { it.type == "NEW_MEM" }.sumOf { it.pagesCountEquivalent.toDouble() }.toFloat()
        val revPagesToday = todayRecs.filter { it.type != "NEW_MEM" }.sumOf { it.pagesCountEquivalent.toDouble() }.toFloat()

        val validAtt = attList.filter { it.sessionId in heldSessionIds }
        val overallAttPercent = if (validAtt.isNotEmpty()) {
            (validAtt.count { it.status == "PRESENT" || it.status == "LATE" } * 100) / validAtt.size
        } else 0

        val levelDist = activeList.groupingBy { it.quranicLevel }.eachCount()

        val alerts = mutableListOf<SmartAlert>()
        activeList.forEach { st ->
            val stAtt = validAtt.filter { it.studentId == st.id }
            val unexcusedAbsences = stAtt.count { it.status == "ABSENT_UNEXCUSED" }
            if (unexcusedAbsences >= 3) {
                alerts.add(SmartAlert("كثرة الغياب: ${st.fullName}", "سجل $unexcusedAbsences غيابات بدون عذر", "HIGH"))
            }

            val stRecs = recList.filter { it.studentId == st.id }
            if (stRecs.isEmpty() && heldSessionIds.size >= 2) {
                alerts.add(SmartAlert("تأخر تسميع: ${st.fullName}", "لم يسجل له أي تسميع حتى الآن", "MEDIUM"))
            } else if (stRecs.take(2).count { it.ratingLabel == "يحتاج إعادة" || it.scoreOutOf100 < 70 } == 2) {
                alerts.add(SmartAlert("تراجع الأداء: ${st.fullName}", "يحتاج إلى متابعة فردية وإعادة التسميع", "HIGH"))
            } else if (stRecs.count { it.scoreOutOf100 >= 95 } >= 3) {
                alerts.add(SmartAlert("إنجاز متميز: ${st.fullName}", "حقق درجة الامتياز في 3 جلسات متتالية ويستحق التكريم", "SUCCESS"))
            }
        }

        val overdueCount = assignList.count { it.completionStatus == "PENDING" || it.completionStatus == "NEEDS_REPEAT" }
        if (overdueCount > 0) {
            alerts.add(SmartAlert("واجبات قيد المتابعة", "يوجد $overdueCount تكليفات لم تنجز أو تحتاج إعادة", "MEDIUM"))
        }

        DashboardMetrics(
            totalStudents = stuList.count { it.status != "ARCHIVED" },
            activeStudents = activeList.size,
            todayPresent = presentToday,
            todayAbsent = absentToday,
            todayLate = lateToday,
            todayAttendancePercent = todayAttPercent,
            todayRecitedStudentsCount = recitedStudentIdsToday.size,
            todayNotRecitedCount = (activeList.size - recitedStudentIdsToday.size).coerceAtLeast(0),
            todayTotalPagesMemorized = newPagesToday,
            todayTotalPagesRevised = revPagesToday,
            pendingAssignmentsCount = overdueCount,
            overallAttendancePercent = overallAttPercent,
            levelDistribution = levelDist,
            smartAlerts = alerts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    fun saveProfile(newProfile: HalaqaProfile) {
        viewModelScope.launch {
            dao.saveHalaqaProfile(newProfile)
            postMessage("تم حفظ بيانات الحلقة والإعدادات بنجاح")
        }
    }

    fun saveStudent(student: Student) {
        viewModelScope.launch {
            dao.saveStudent(student)
            postMessage("تم حفظ ملف الطالب: ${student.fullName}")
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            dao.deleteStudent(student)
            postMessage("تم حذف الطالب نهائياً")
        }
    }

    fun archiveStudentsBatch(studentIds: List<Long>) {
        viewModelScope.launch {
            dao.updateStudentsStatusBatch(studentIds, "ARCHIVED")
            postMessage("تمت أرشفة الطلاب المحددين (${studentIds.size})")
        }
    }

    fun addGroup(name: String, desc: String) {
        viewModelScope.launch {
            dao.insertGroup(StudentGroup(groupName = name, description = desc))
            postMessage("تم إنشاء المجموعة: $name")
        }
    }

    fun moveStudentsToGroup(studentIds: List<Long>, group: StudentGroup) {
        viewModelScope.launch {
            dao.moveStudentsToGroupBatch(studentIds, group.groupId, group.groupName)
            postMessage("تم نقل الطلاب إلى مجموعة ${group.groupName}")
        }
    }

    fun saveCompleteSession(
        date: String,
        status: String,
        cancelReason: String,
        generalEvents: String,
        teacherNotes: String,
        attendanceMap: Map<Long, Pair<String, String>>
    ) {
        viewModelScope.launch {
            val existing = dao.getSessionByDate(date)
            val sessionId = dao.saveSession(
                HalaqaSession(
                    sessionId = existing?.sessionId ?: 0L,
                    sessionDate = date,
                    status = status,
                    cancelReason = cancelReason,
                    generalEvents = generalEvents,
                    teacherNotes = teacherNotes
                )
            )
            if (status == "HELD") {
                val records = attendanceMap.map { (stuId, pair) ->
                    AttendanceRecord(
                        sessionId = sessionId,
                        sessionDate = date,
                        studentId = stuId,
                        status = pair.first,
                        reason = pair.second
                    )
                }
                dao.saveAttendanceList(records)
            }
            postMessage("تم حفظ بيانات جلسة $date بنجاح")
        }
    }

    fun recordRecitation(
        date: String,
        student: Student,
        type: String,
        surah: SurahInfo,
        fromAyah: Int,
        toAyah: Int,
        amountLabel: String,
        pagesEquivalent: Float,
        mistakes: Int,
        mistakeTypes: String,
        score: Int,
        rating: String,
        notes: String,
        nextHomework: String
    ) {
        if (!QuranData.validateAyahRange(surah.number, fromAyah, toAyah)) {
            postMessage("خطأ: نطاق الآيات غير صحيح! سورة ${surah.name} عدد آياتها ${surah.maxAyahs}")
            return
        }
        viewModelScope.launch {
            val session = dao.getSessionByDate(date)
            val sessionId = session?.sessionId ?: dao.saveSession(HalaqaSession(sessionDate = date, status = "HELD"))

            dao.saveRecitation(
                RecitationRecord(
                    sessionId = sessionId,
                    studentId = student.id,
                    recitationDate = date,
                    type = type,
                    surahNumber = surah.number,
                    surahName = surah.name,
                    fromAyah = fromAyah,
                    toAyah = toAyah,
                    amountLabel = amountLabel,
                    pagesCountEquivalent = pagesEquivalent,
                    mistakesCount = mistakes,
                    mistakeTypes = mistakeTypes,
                    scoreOutOf100 = score,
                    ratingLabel = rating,
                    notes = notes,
                    nextAssignmentText = nextHomework
                )
            )
            if (type == "NEW_MEM") {
                dao.updateStudentLastReached(student.id, surah.name, toAyah)
            }
            if (nextHomework.isNotBlank()) {
                dao.saveAssignment(
                    AssignmentRecord(
                        studentId = student.id,
                        studentNameCache = student.fullName,
                        assignedDate = date,
                        dueDate = "الجلسة القادمة",
                        requiredMemorization = nextHomework
                    )
                )
            }
            postMessage("تم حفظ تسميع الطالب ${student.fullName}")
        }
    }

    fun saveEvaluation(evaluation: StudentEvaluation) {
        viewModelScope.launch {
            dao.saveEvaluation(evaluation)
            postMessage("تم حفظ التقييم التربوي بنجاح")
        }
    }

    fun buildWhatsAppParentsSummary(startDate: String, endDate: String): String {
        val prof = profile.value
        val sessInPeriod = sessions.value.filter { it.sessionDate in startDate..endDate && it.status == "HELD" }
        val attInPeriod = attendanceRecords.value.filter { it.sessionDate in startDate..endDate }
        val recInPeriod = recitationRecords.value.filter { it.recitationDate in startDate..endDate }

        val presentCount = attInPeriod.count { it.status == "PRESENT" || it.status == "LATE" }
        val attPercent = if (attInPeriod.isNotEmpty()) (presentCount * 100) / attInPeriod.size else 0
        val newPages = recInPeriod.filter { it.type == "NEW_MEM" }.sumOf { it.pagesCountEquivalent.toDouble() }
        val revPages = recInPeriod.filter { it.type != "NEW_MEM" }.sumOf { it.pagesCountEquivalent.toDouble() }

        return """
            🌿 *تقرير أولياء الأمور - ${prof.halaqaName}* 🌿
            📍 ${prof.mosqueName} | ${prof.centerName}
            🗓 الفترة: من $startDate إلى $endDate
            ━━━━━━━━━━━━━━━━━━
            ✅ عدد الجلسات المنعقدة: ${sessInPeriod.size}
            📊 نسبة الحضور العامة للحلقة: $attPercent%
            📖 إجمالي الحفظ الجديد المنجز: ${String.format(Locale.US, "%.1f", newPages)} صفحة
            🔄 إجمالي المراجعة المتقنة: ${String.format(Locale.US, "%.1f", revPages)} صفحة
            ━━━━━━━━━━━━━━━━━━
            💡 حفاظاً على خصوصية أبنائنا الطلاب، تُرسل التقييمات الفردية لكل ولي أمر على حدة.
            مع تحيات معلم الحلقة: ${prof.teacherName}
        """.trimIndent()
    }

    fun exportFullBackup(uri: Uri) {
        viewModelScope.launch {
            val payload = FullBackupPayload(
                exportDate = todayDate,
                profile = dao.getHalaqaProfileOnce(),
                students = dao.getAllStudentsOnce(),
                sessions = dao.getAllSessionsOnce(),
                attendance = dao.getAllAttendanceOnce(),
                recitations = dao.getAllRecitationsOnce(),
                evaluations = dao.getAllEvaluationsOnce(),
                assignments = dao.getAllAssignmentsOnce()
            )
            val ok = ExportAndBackupManager.writeBackupToUri(getApplication(), uri, payload)
            postMessage(if (ok) "تم تصدير النسخة الاحتياطية بنجاح" else "حدث خطأ أثناء التصدير")
        }
    }

    fun restoreFullBackup(uri: Uri) {
        viewModelScope.launch {
            val payload = ExportAndBackupManager.readBackupFromUri(getApplication(), uri)
            if (payload == null) {
                postMessage("خطأ: ملف النسخة الاحتياطية غير صالح!")
                return@launch
            }
            dao.clearAllSessions()
            dao.clearAllStudents()
            payload.profile?.let { dao.saveHalaqaProfile(it) }
            dao.insertStudentsList(payload.students)
            payload.sessions.forEach { dao.saveSession(it) }
            dao.saveAttendanceList(payload.attendance)
            payload.recitations.forEach { dao.saveRecitation(it) }
            payload.evaluations.forEach { dao.saveEvaluation(it) }
            payload.assignments.forEach { dao.saveAssignment(it) }
            postMessage("تمت استعادة البيانات بنجاح!")
        }
    }

    fun importStudentsCsv(uri: Uri) {
        viewModelScope.launch {
            val imported = ExportAndBackupManager.parseStudentsFromCsvUri(getApplication(), uri)
            if (imported.isNotEmpty()) {
                dao.insertStudentsList(imported)
                postMessage("تم استيراد ${imported.size} طالب بنجاح")
            } else {
                postMessage("تعذر قراءة ملف CSV")
            }
        }
    }

    fun loadLabeledSampleData() {
        viewModelScope.launch {
            val g1 = dao.insertGroup(StudentGroup(groupName = "مجموعة المهرة", description = "حفاظ 5 أجزاء فأكثر"))
            val s1 = dao.saveStudent(Student(studentCode = "DEMO-01", fullName = "[تجريبي] أسامة بن زيد", birthDateOrAge = "15", guardianName = "زيد الحارثة", guardianPhone = "0501112233", joinDate = "2026-01-10", groupId = g1, groupNameCache = "مجموعة المهرة", quranicLevel = "متقدم", lastReachedSurah = "الأنفال", lastReachedAyah = 40, isSampleData = true))
            val s2 = dao.saveStudent(Student(studentCode = "DEMO-02", fullName = "[تجريبي] معاذ بن جبل", birthDateOrAge = "16", guardianName = "جبل الأنصاري", guardianPhone = "0502223344", joinDate = "2026-01-10", groupId = g1, groupNameCache = "مجموعة المهرة", quranicLevel = "متقدم", lastReachedSurah = "آل عمران", lastReachedAyah = 120, isSampleData = true))
            val sessId = dao.saveSession(HalaqaSession(sessionDate = todayDate, status = "HELD", generalEvents = "جلسة تجريبية", isSampleData = true))
            dao.saveAttendanceList(listOf(
                AttendanceRecord(sessionId = sessId, sessionDate = todayDate, studentId = s1, status = "PRESENT"),
                AttendanceRecord(sessionId = sessId, sessionDate = todayDate, studentId = s2, status = "PRESENT")
            ))
            dao.saveRecitation(RecitationRecord(sessionId = sessId, studentId = s1, recitationDate = todayDate, type = "NEW_MEM", surahNumber = 8, surahName = "الأنفال", fromAyah = 1, toAyah = 40, amountLabel = "نصف حزب (5 ص)", pagesCountEquivalent = 5.0f, mistakesCount = 0, scoreOutOf100 = 99, ratingLabel = "ممتاز"))
            postMessage("تم إدراج بيانات تجريبية مميزة بعلامة [تجريبي]")
        }
    }

    fun deleteSampleDataOnly() {
        viewModelScope.launch {
            dao.deleteSampleSessions()
            dao.deleteSampleStudents()
            postMessage("تم حذف البيانات التجريبية فقط")
        }
    }
}
