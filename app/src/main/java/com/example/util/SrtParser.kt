package com.example.util

import com.example.model.SubtitleItem
import com.example.model.SrtError
import java.util.Locale

object SrtParser {

    /**
     * Parses SRT text into a list of SubtitleItem.
     * Also detects formatting errors.
     */
    fun parseSrt(srtContent: String, errorsList: MutableList<SrtError>? = null): List<SubtitleItem> {
        val items = mutableListOf<SubtitleItem>()
        if (srtContent.isBlank()) return items

        val normalized = srtContent.replace("\r\n", "\n").replace("\r", "\n")
        // Split into blocks by double newline (or multiple newlines)
        val blocks = normalized.split(Regex("\\n\\s*\\n"))

        var runningIndex = 1
        for ((blockIdx, block) in blocks.withIndex()) {
            val lines = block.trim().lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue

            val currentBlockNum = blockIdx + 1

            // Find timing line (contains -->)
            val timingIndex = lines.indexOfFirst { it.contains("-->") }
            if (timingIndex == -1) {
                // Formatting error: No timing line found
                errorsList?.add(
                    SrtError(
                        blockNumber = currentBlockNum,
                        lineText = lines.firstOrNull() ?: "",
                        errorMessage = "خط ریتم (زمان‌بندی) یافت نشد (باید شامل --> باشد)."
                    )
                )
                // Fallback: use all lines as text
                val text = lines.joinToString(" ")
                items.add(
                    SubtitleItem(
                        index = runningIndex,
                        startTime = null,
                        endTime = null,
                        text = text
                    )
                )
                runningIndex++
                continue
            }

            val timingLine = lines[timingIndex]
            val timeParts = timingLine.split("-->").map { it.trim() }
            val startTime = timeParts.getOrNull(0)
            val endTime = timeParts.getOrNull(1)

            // Validate timestamps
            if (startTime != null && !isValidTimestamp(startTime)) {
                errorsList?.add(
                    SrtError(
                        blockNumber = currentBlockNum,
                        lineText = timingLine,
                        errorMessage = "فرمت زمان شروع نادرست است: '$startTime'"
                    )
                )
            }
            if (endTime != null && !isValidTimestamp(endTime)) {
                errorsList?.add(
                    SrtError(
                        blockNumber = currentBlockNum,
                        lineText = timingLine,
                        errorMessage = "فرمت زمان پایان نادرست است: '$endTime'"
                    )
                )
            }

            // Extract text
            val textLines = lines.subList(timingIndex + 1, lines.size)
            val text = textLines.joinToString(" ")

            // Extract explicit index
            val indexStr = if (timingIndex > 0) lines[timingIndex - 1] else null
            val explicitIndex = indexStr?.toIntOrNull()
            if (indexStr != null && explicitIndex == null) {
                errorsList?.add(
                    SrtError(
                        blockNumber = currentBlockNum,
                        lineText = indexStr,
                        errorMessage = "شماره ردیف نامعتبر است: '$indexStr'"
                    )
                )
            }

            val finalIndex = explicitIndex ?: runningIndex
            items.add(
                SubtitleItem(
                    index = finalIndex,
                    startTime = startTime,
                    endTime = endTime,
                    text = text
                )
            )
            runningIndex++
        }
        return items
    }

    private fun isValidTimestamp(timestamp: String): Boolean {
        val clean = timestamp.trim().replace('.', ',')
        return clean.matches(Regex("^\\d{1,2}:\\d{2}:\\d{2},\\d{3}$"))
    }

    fun parseTimestampToMillis(timestamp: String): Long {
        try {
            val clean = timestamp.trim().replace('.', ',')
            val parts = clean.split(":")
            if (parts.size < 3) return 0L
            val hours = parts[0].toLongOrNull() ?: 0L
            val minutes = parts[1].toLongOrNull() ?: 0L
            val secondsParts = parts[2].split(",")
            val seconds = secondsParts.getOrNull(0)?.toLongOrNull() ?: 0L
            val millis = secondsParts.getOrNull(1)?.toLongOrNull() ?: 0L

            return (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
        } catch (e: Exception) {
            return 0L
        }
    }

    fun formatMillisToTimestamp(millis: Long): String {
        val m = Math.max(0L, millis)
        val ms = m % 1000
        val s = (m / 1000) % 60
        val min = (m / (1000 * 60)) % 60
        val hr = m / (1000 * 60 * 60)
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hr, min, s, ms)
    }

    /**
     * Parses input plain text/numbered text/SRT to raw dialogues.
     */
    fun parseInputTextToDialogues(input: String): List<String> {
        val normalized = input.replace("\r\n", "\n").replace("\r", "\n").trim()
        if (normalized.isEmpty()) return emptyList()

        if (normalized.contains("-->")) {
            return parseSrt(normalized).map { it.text }
        }

        val lines = normalized.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val dialogues = mutableListOf<String>()
        for (line in lines) {
            val match = Regex("^\\s*(\\d+)\\s*:(.*)$").matchEntire(line)
            if (match != null) {
                dialogues.add(match.groupValues[2].trim())
            } else {
                dialogues.add(line)
            }
        }
        return dialogues
    }

    /**
     * Aligns dialogues with reference subtitles.
     */
    fun alignSrt(
        dialogues: List<String>,
        referenceSubtitles: List<SubtitleItem>
    ): Pair<List<SubtitleItem>, String> {
        val alignedList = mutableListOf<SubtitleItem>()
        val n = dialogues.size
        val m = referenceSubtitles.size

        var lastEndTimeMs = 0L

        for (i in 0 until n) {
            val dialogue = dialogues[i]
            val ref = referenceSubtitles.getOrNull(i)

            val startTime: String
            val endTime: String

            if (ref != null && ref.startTime != null && ref.endTime != null) {
                startTime = ref.startTime
                endTime = ref.endTime
                lastEndTimeMs = parseTimestampToMillis(endTime)
            } else {
                val autoStartMs = lastEndTimeMs + 1000L
                val autoEndMs = autoStartMs + 2000L
                startTime = formatMillisToTimestamp(autoStartMs)
                endTime = formatMillisToTimestamp(autoEndMs)
                lastEndTimeMs = autoEndMs
            }

            alignedList.add(
                SubtitleItem(
                    index = i + 1,
                    startTime = startTime,
                    endTime = endTime,
                    text = dialogue
                )
            )
        }

        val infoMessage = if (m == 0) {
            "توجه: فایل مرجع انتخاب نشده است یا فاقد زمان‌بندی معتبر است. برای کل $n خط زمان‌بندی خودکار ایجاد شد."
        } else if (n > m) {
            "توجه: تعداد خطوط ورودی ($n) بیشتر از زمان‌بندی‌های مرجع ($m) است. برای ${n - m} خط اضافی زمان‌بندی خودکار ایجاد شد."
        } else if (n < m) {
            "توجه: تعداد خطوط ورودی ($n) کمتر از زمان‌بندی‌های مرجع ($m) است. فقط $n زیرنویس تولید شد."
        } else {
            "تعداد خطوط ورودی و مرجع کاملاً منطبق است ($n)."
        }

        return Pair(alignedList, infoMessage)
    }

    /**
     * Converts SubtitleItem list to SRT text representation.
     */
    fun srtListToString(subtitles: List<SubtitleItem>): String {
        val sb = StringBuilder()
        for (sub in subtitles) {
            sb.append(sub.index).append("\n")
            sb.append(sub.startTime ?: "00:00:00,000").append(" --> ").append(sub.endTime ?: "00:00:00,000").append("\n")
            sb.append(sub.text).append("\n\n")
        }
        return sb.toString().trimEnd() + "\n"
    }
}
