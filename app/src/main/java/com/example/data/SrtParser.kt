package com.example.data

import android.util.Log
import java.io.File
import java.io.InputStream

data class SubtitleEntry(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

object SrtParser {
    fun parse(file: File): List<SubtitleEntry> {
        return try {
            if (file.exists()) {
                parse(file.inputStream())
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SrtParser", "Error reading file $file: $e")
            emptyList()
        }
    }

    fun parse(inputStream: InputStream): List<SubtitleEntry> {
        val entries = mutableListOf<SubtitleEntry>()
        try {
            val content = inputStream.bufferedReader().use { it.readText() }
            // Normalize line endings and split by empty lines (often \n\n or \r\n\r\n)
            val blocks = content.replace("\r\n", "\n").split("\n\n")

            for (block in blocks) {
                val lines = block.trim().split("\n")
                if (lines.size >= 2) {
                    // Line 0: ID (or index)
                    // Line 1: Timing "00:01:20,000 --> 00:01:23,500"
                    // Line 2+: Subtitle text
                    val timingLine = lines[1]
                    if (timingLine.contains("-->")) {
                        try {
                            val times = timingLine.split("-->")
                            if (times.size == 2) {
                                val startMs = parseTimeToMs(times[0])
                                val endMs = parseTimeToMs(times[1])
                                val textLines = lines.subList(2, lines.size).filter { it.isNotBlank() }
                                val text = textLines.joinToString("\n")
                                if (startMs >= 0 && endMs > startMs && text.isNotBlank()) {
                                    entries.add(SubtitleEntry(startMs, endMs, text))
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("SrtParser", "Failed to parse bock timing: '$timingLine' / error: $e")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SrtParser", "Error parsing SRT stream", e)
        }
        return entries
    }

    private fun parseTimeToMs(timeStr: String): Long {
        val trimmed = timeStr.trim()
        val parts = trimmed.split(":")
        if (parts.size < 3) return -1L
        
        val hours = parts[0].toLongOrNull() ?: 0L
        val minutes = parts[1].toLongOrNull() ?: 0L
        
        val secondsPart = parts[2]
        // Seconds and milliseconds are separated by ',' or '.'
        val secParts = secondsPart.split(",", ".")
        val seconds = secParts[0].toLongOrNull() ?: 0L
        val millis = if (secParts.size > 1) {
            val mStr = secParts[1].padEnd(3, '0').take(3)
            mStr.toLongOrNull() ?: 0L
        } else {
            0L
        }
        
        return (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
    }
}
