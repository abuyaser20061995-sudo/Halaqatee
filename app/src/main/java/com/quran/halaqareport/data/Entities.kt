package com.quran.halaqareport.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "halaqa_profile")
data class HalaqaProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 1,
    val appCustomTitle: String = "تقرير الحلقة",
    val halaqaName: String = "حلقة الإمام عاصم بن أبي النجود",
    val mosqueName: String = "جامع الهدى الكبير",
    val centerName: String = "مركز الفرقان لتحفيظ القرآن الكريم",
    val teacherName: String = "الشيخ عبد الرحمن المحمود",
    val supervisorName: String = "الشيخ محمد السالم",
    val directorName: String = "أ. خالد العبد الله",
    val targetAgeGroup: String = "المرحلة الإعدادية والثانوية",
    val startDate: String = "2026-01-01",
    val meetingDays: String = "السبت، الإثنين، الأربعاء",
    val startTime: String = "16:00",
    val endTime: String = "18:00",
    val targetStudentCount: Int = 35,
    val goals: String = "إتقان تلاوة وحفظ خمسة أجزاء مع التجويد",
    val generalLevel: String = "متوسط - متقدم",
    val adminNotes: String = "",
    val absentAlertThresholdDays: Int = 7,
    val darkThemeEnabled: Boolean = false,
    val pinCode: String = ""
)

@Entity(tableName = "student_groups")
data class StudentGroup(
    @PrimaryKey(autoGenerate = true) val groupId: Long = 0,
    val halaqaId: Long = 1,
    val groupName: String,
    val description: String = ""
)

@Entity(
    tableName = "students",
    indices = [Index("halaqaId"), Index("groupId"), Index("fullName")]
)
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val halaqaId: Long = 1,
    val studentCode: String,
    val fullName: String,
    val birthDateOrAge: String,
    val guardianName: String,
    val guardianPhone: String = "",
    val joinDate: String,
    val groupId: Long? = null,
    val groupNameCache: String = "عام",
    val quranicLevel: String = "متوسط",
    val readingLevel: String = "جيد جداً",
    val memorizationLevel: String = "جيد جداً",
    val tajweedLevel: String = "جيد",
    val previousMemorizationPages: Float = 0f,
    val lastReachedSurah: String = "الفاتحة",
    val lastReachedAyah: Int = 1,
    val teacherNotes: String = "",
    val status: String = "ACTIVE",
    val weeklyGoalPages: Float = 3.0f,
    val monthlyGoalPages: Float = 12.0f,
    val isSampleData: Boolean = false
)

@Entity(
    tableName = "sessions",
    indices = [Index(value = ["halaqaId", "sessionDate"], unique = true)]
)
data class HalaqaSession(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val halaqaId: Long = 1,
    val sessionDate: String,
    val status: String = "HELD",
    val cancelReason: String = "",
    val generalEvents: String = "",
    val teacherNotes: String = "",
    val isCompleted: Boolean = true,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val isSampleData: Boolean = false
)

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(entity = HalaqaSession::class, parentColumns = ["sessionId"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("sessionId"), Index("studentId"), Index(value = ["sessionId", "studentId"], unique = true)]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val attendanceId: Long = 0,
    val sessionId: Long,
    val sessionDate: String,
    val studentId: Long,
    val status: String,
    val reason: String = "",
    val dailyNote: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "recitation_records",
    foreignKeys = [
        ForeignKey(entity = HalaqaSession::class, parentColumns = ["sessionId"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("sessionId"), Index("studentId"), Index("recitationDate")]
)
data class RecitationRecord(
    @PrimaryKey(autoGenerate = true) val recitationId: Long = 0,
    val sessionId: Long,
    val studentId: Long,
    val recitationDate: String,
    val type: String,
    val surahNumber: Int,
    val surahName: String,
    val fromAyah: Int,
    val toAyah: Int,
    val fromPage: Int = 0,
    val toPage: Int = 0,
    val amountLabel: String = "صفحة واحدة",
    val pagesCountEquivalent: Float = 1.0f,
    val mistakesCount: Int = 0,
    val mistakeTypes: String = "",
    val scoreOutOf100: Int = 95,
    val ratingLabel: String = "ممتاز",
    val notes: String = "",
    val nextAssignmentText: String = ""
)

@Entity(
    tableName = "evaluations",
    foreignKeys = [
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("studentId")]
)
data class StudentEvaluation(
    @PrimaryKey(autoGenerate = true) val evaluationId: Long = 0,
    val studentId: Long,
    val evalDate: String,
    val memorizationQuality: Int = 90,
    val revisionMastery: Int = 90,
    val readingSoundness: Int = 90,
    val tajweedApplication: Int = 85,
    val attendanceCommitment: Int = 95,
    val homeworkPerformance: Int = 90,
    val discipline: Int = 95,
    val respectAndManners: Int = 100,
    val interaction: Int = 90,
    val overallScore: Int = 92,
    val overallGrade: String = "ممتاز",
    val constructiveNotes: String = ""
)

@Entity(
    tableName = "assignments",
    foreignKeys = [
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("studentId")]
)
data class AssignmentRecord(
    @PrimaryKey(autoGenerate = true) val assignmentId: Long = 0,
    val studentId: Long,
    val studentNameCache: String,
    val assignedDate: String,
    val dueDate: String,
    val requiredMemorization: String,
    val requiredRevision: String = "",
    val specialInstructions: String = "",
    val completionStatus: String = "PENDING"
)
