package com.quran.halaqareport.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.quran.halaqareport.data.*
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FullBackupPayload(
    val version: Int = 1,
    val exportDate: String,
    val profile: HalaqaProfile?,
    val students: List<Student>,
    val sessions: List<HalaqaSession>,
    val attendance: List<AttendanceRecord>,
    val recitations: List<RecitationRecord>,
    val evaluations: List<StudentEvaluation>,
    val assignments: List<AssignmentRecord>
)

object ExportAndBackupManager {

    fun generateAndSharePdfReport(
        context: Context,
        profile: HalaqaProfile,
        reportTitle: String,
        periodSubtitle: String,
        summaryMetrics: List<Pair<String, String>>,
        tableHeaders: List<String>,
        tableRows: List<List<String>>,
        footerNotes: String
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(13, 79, 60)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val rightHeaderPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val normalRightPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val tableHeaderBgPaint = Paint().apply {
            color = Color.rgb(13, 79, 60)
            style = Paint.Style.FILL
        }

        val tableHeaderTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val goldLinePaint = Paint().apply {
            color = Color.rgb(198, 156, 69)
            strokeWidth = 2.5f
        }

        fun drawHeader(c: Canvas) {
            c.drawText("المركز: ${profile.centerName}", pageWidth - 35f, 40f, rightHeaderPaint)
            c.drawText("المسجد: ${profile.mosqueName}", pageWidth - 35f, 56f, rightHeaderPaint)
            c.drawText("الحلقة: ${profile.halaqaName}", pageWidth - 35f, 72f, rightHeaderPaint)

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            c.drawText("المعلم: ${profile.teacherName}", 200f, 40f, rightHeaderPaint)
            c.drawText("المشرف: ${profile.supervisorName}", 200f, 56f, rightHeaderPaint)
            c.drawText("تاريخ الإصدار: $today", 200f, 72f, rightHeaderPaint)

            c.drawText(reportTitle, pageWidth / 2f, 105f, titlePaint)
            c.drawText(periodSubtitle, pageWidth / 2f, 123f, normalRightPaint.apply { textAlign = Paint.Align.CENTER })
            normalRightPaint.textAlign = Paint.Align.RIGHT

            c.drawLine(35f, 135f, pageWidth - 35f, 135f, goldLinePaint)
        }

        drawHeader(canvas)
        var currentY = 160f

        val boxWidth = (pageWidth - 70f) / 3f
        summaryMetrics.chunked(3).forEach { rowMetrics ->
            rowMetrics.forEachIndexed { idx, pair ->
                val rightEdge = pageWidth - 35f - (idx * boxWidth)
                val leftEdge = rightEdge - boxWidth + 8f
                canvas.drawRect(leftEdge, currentY, rightEdge, currentY + 36f, borderPaint)
                canvas.drawText("${pair.first}: ${pair.second}", rightEdge - 10f, currentY + 22f, rightHeaderPaint)
            }
            currentY += 44f
        }

        currentY += 12f
        val usableWidth = pageWidth - 70f
        val colWidth = usableWidth / tableHeaders.size.coerceAtLeast(1)

        fun drawTableHeader(c: Canvas, y: Float) {
            c.drawRect(35f, y, pageWidth - 35f, y + 26f, tableHeaderBgPaint)
            tableHeaders.forEachIndexed { index, headerText ->
                val cellRight = pageWidth - 35f - (index * colWidth) - 8f
                c.drawText(headerText, cellRight, y + 17f, tableHeaderTextPaint)
            }
        }

        drawTableHeader(canvas, currentY)
        currentY += 26f

        tableRows.forEach { row ->
            if (currentY > pageHeight - 90f) {
                canvas.drawText("صفحة $pageNumber", pageWidth / 2f, pageHeight - 25f, normalRightPaint)
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(canvas)
                currentY = 155f
                drawTableHeader(canvas, currentY)
                currentY += 26f
            }

            canvas.drawRect(35f, currentY, pageWidth - 35f, currentY + 24f, borderPaint)
            row.forEachIndexed { index, cellText ->
                val cellRight = pageWidth - 35f - (index * colWidth) - 8f
                val safeText = if (cellText.length > 26) cellText.take(24) + ".." else cellText
                canvas.drawText(safeText, cellRight, currentY + 16f, normalRightPaint)
            }
            currentY += 24f
        }

        currentY += 25f
        if (currentY < pageHeight - 80f) {
            canvas.drawText("ملاحظات وتوصيات المعلم: $footerNotes", pageWidth - 35f, currentY, rightHeaderPaint)
            canvas.drawText("توقيع معلم الحلقة: ____________________", pageWidth - 35f, currentY + 35f, rightHeaderPaint)
            canvas.drawText("اعتماد إدارة المعهد: ____________________", 230f, currentY + 35f, rightHeaderPaint)
        }
        canvas.drawText("صفحة $pageNumber", pageWidth / 2f, pageHeight - 25f, normalRightPaint)

        pdfDocument.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val pdfFile = File(reportsDir, "Halaqa_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return pdfFile
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun exportStudentsToCsv(context: Context, students: List<Student>): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val csvFile = File(reportsDir, "Students_Export_${System.currentTimeMillis()}.csv")
        FileOutputStream(csvFile).use { fos ->
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val writer = fos.bufferedWriter(Charsets.UTF_8)
            writer.write("الرقم التعريفي,الاسم الكامل,العمر,ولي الأمر,هاتف ولي الأمر,المجموعة,المستوى القرآني,آخر سورة,آخر آية,الحالة\n")
            students.forEach { s ->
                writer.write("${s.studentCode},\"${s.fullName}\",${s.birthDateOrAge},\"${s.guardianName}\",${s.guardianPhone},\"${s.groupNameCache}\",${s.quranicLevel},${s.lastReachedSurah},${s.lastReachedAyah},${s.status}\n")
            }
            writer.flush()
        }
        return csvFile
    }

    fun parseStudentsFromCsvUri(context: Context, uri: Uri): List<Student> {
        val result = mutableListOf<Student>()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            var lineIndex = 0
            reader.forEachLine { rawLine ->
                val line = rawLine.trim().removePrefix("\uFEFF")
                if (line.isNotEmpty() && lineIndex > 0) {
                    val parts = line.split(",").map { it.trim().removeSurrounding("\"") }
                    if (parts.size >= 2) {
                        result.add(
                            Student(
                                studentCode = parts.getOrNull(0)?.ifBlank { "S-${100 + lineIndex}" } ?: "S-$lineIndex",
                                fullName = parts[1],
                                birthDateOrAge = parts.getOrNull(2) ?: "14",
                                guardianName = parts.getOrNull(3) ?: "",
                                guardianPhone = parts.getOrNull(4) ?: "",
                                joinDate = today,
                                groupNameCache = parts.getOrNull(5) ?: "عام",
                                quranicLevel = parts.getOrNull(6) ?: "متوسط",
                                lastReachedSurah = parts.getOrNull(7) ?: "البقرة",
                                lastReachedAyah = parts.getOrNull(8)?.toIntOrNull() ?: 1
                            )
                        )
                    }
                }
                lineIndex++
            }
        }
        return result
    }

    fun writeBackupToUri(context: Context, uri: Uri, payload: FullBackupPayload): Boolean {
        return try {
            val jsonString = Gson().toJson(payload)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun readBackupFromUri(context: Context, uri: Uri): FullBackupPayload? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val json = input.bufferedReader(Charsets.UTF_8).readText()
                val payload = Gson().fromJson(json, FullBackupPayload::class.java)
                if (payload != null && payload.students != null && payload.sessions != null) payload else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
