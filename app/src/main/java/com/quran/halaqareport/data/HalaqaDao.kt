package com.quran.halaqareport.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HalaqaDao {
    @Query("SELECT * FROM halaqa_profile WHERE id = 1")
    fun getHalaqaProfile(): Flow<HalaqaProfile?>

    @Query("SELECT * FROM halaqa_profile WHERE id = 1")
    suspend fun getHalaqaProfileOnce(): HalaqaProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHalaqaProfile(profile: HalaqaProfile)

    @Query("SELECT * FROM student_groups ORDER BY groupName ASC")
    fun getAllGroups(): Flow<List<StudentGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: StudentGroup): Long

    @Query("SELECT * FROM students ORDER BY fullName ASC")
    fun getAllStudentsIncludingArchived(): Flow<List<Student>>

    @Query("SELECT * FROM students")
    suspend fun getAllStudentsOnce(): List<Student>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStudent(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentsList(students: List<Student>)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("UPDATE students SET status = :newStatus WHERE id IN (:studentIds)")
    suspend fun updateStudentsStatusBatch(studentIds: List<Long>, newStatus: String)

    @Query("UPDATE students SET groupId = :groupId, groupNameCache = :groupName WHERE id IN (:studentIds)")
    suspend fun moveStudentsToGroupBatch(studentIds: List<Long>, groupId: Long, groupName: String)

    @Query("UPDATE students SET lastReachedSurah = :surah, lastReachedAyah = :ayah WHERE id = :studentId")
    suspend fun updateStudentLastReached(studentId: Long, surah: String, ayah: Int)

    @Query("SELECT * FROM sessions ORDER BY sessionDate DESC")
    fun getAllSessions(): Flow<List<HalaqaSession>>

    @Query("SELECT * FROM sessions")
    suspend fun getAllSessionsOnce(): List<HalaqaSession>

    @Query("SELECT * FROM sessions WHERE sessionDate = :date LIMIT 1")
    suspend fun getSessionByDate(date: String): HalaqaSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: HalaqaSession): Long

    @Query("SELECT * FROM attendance_records ORDER BY sessionDate DESC")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllAttendanceOnce(): List<AttendanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAttendanceList(records: List<AttendanceRecord>)

    @Query("SELECT * FROM recitation_records ORDER BY recitationDate DESC, recitationId DESC")
    fun getAllRecitations(): Flow<List<RecitationRecord>>

    @Query("SELECT * FROM recitation_records")
    suspend fun getAllRecitationsOnce(): List<RecitationRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecitation(recitation: RecitationRecord): Long

    @Query("SELECT * FROM evaluations ORDER BY evalDate DESC")
    fun getAllEvaluations(): Flow<List<StudentEvaluation>>

    @Query("SELECT * FROM evaluations")
    suspend fun getAllEvaluationsOnce(): List<StudentEvaluation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEvaluation(evaluation: StudentEvaluation)

    @Query("SELECT * FROM assignments ORDER BY dueDate ASC")
    fun getAllAssignments(): Flow<List<AssignmentRecord>>

    @Query("SELECT * FROM assignments")
    suspend fun getAllAssignmentsOnce(): List<AssignmentRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAssignment(assignment: AssignmentRecord)

    @Query("DELETE FROM students WHERE isSampleData = 1")
    suspend fun deleteSampleStudents()

    @Query("DELETE FROM sessions WHERE isSampleData = 1")
    suspend fun deleteSampleSessions()

    @Query("DELETE FROM students")
    suspend fun clearAllStudents()

    @Query("DELETE FROM sessions")
    suspend fun clearAllSessions()
}
