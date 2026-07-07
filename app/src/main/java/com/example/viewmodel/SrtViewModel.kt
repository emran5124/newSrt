package com.example.viewmodel

import androidx.lifecycle.ViewModel
import com.example.model.ExtractionState
import com.example.model.OutputMode
import com.example.model.SyncState
import com.example.model.SubtitleItem
import com.example.model.SrtError
import com.example.util.SrtParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SrtViewModel : ViewModel() {

    private val _extractionState = MutableStateFlow(ExtractionState())
    val extractionState: StateFlow<ExtractionState> = _extractionState.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // ==========================================
    // EXTRACTOR ACTIONS (TAB 1)
    // ==========================================

    fun onSrtInputChanged(input: String) {
        _extractionState.update { state ->
            val errorsList = mutableListOf<SrtError>()
            val parsed = SrtParser.parseSrt(input, errorsList)

            val output = generateExtractedText(parsed, state.outputMode)
            val subCount = parsed.size
            val rawLineCount = input.lines().filter { it.isNotBlank() }.size

            state.copy(
                srtInput = input,
                srtOutput = output,
                subtitleCount = subCount,
                lineCount = rawLineCount,
                errors = errorsList
            )
        }
    }

    fun onOutputModeChanged(mode: OutputMode) {
        _extractionState.update { state ->
            val errorsList = mutableListOf<SrtError>()
            val parsed = SrtParser.parseSrt(state.srtInput, errorsList)
            val output = generateExtractedText(parsed, mode)

            state.copy(
                outputMode = mode,
                srtOutput = output
            )
        }
    }

    fun clearExtractionInput() {
        _extractionState.update { state ->
            state.copy(
                srtInput = "",
                subtitleCount = 0,
                lineCount = 0,
                errors = emptyList()
            )
        }
        recalculateExtractionOutput()
    }

    fun clearExtractionOutput() {
        _extractionState.update { state ->
            state.copy(
                srtOutput = ""
            )
        }
    }

    private fun recalculateExtractionOutput() {
        _extractionState.update { state ->
            val errorsList = mutableListOf<SrtError>()
            val parsed = SrtParser.parseSrt(state.srtInput, errorsList)
            val output = generateExtractedText(parsed, state.outputMode)
            state.copy(
                srtOutput = output,
                subtitleCount = parsed.size,
                errors = errorsList
            )
        }
    }

    private fun generateExtractedText(
        subtitles: List<SubtitleItem>,
        mode: OutputMode
    ): String {
        if (subtitles.isEmpty()) return ""
        val sb = StringBuilder()
        for (sub in subtitles) {
            when (mode) {
                OutputMode.WITH_INDEX -> {
                    sb.append(sub.index).append(": ").append(sub.text).append("\n")
                }
                OutputMode.WITHOUT_INDEX -> {
                    sb.append(sub.text).append("\n")
                }
            }
        }
        return sb.toString().trimEnd()
    }

    // ==========================================
    // SYNC/CREATOR ACTIONS (TAB 2)
    // ==========================================

    fun onMainInputChanged(input: String) {
        _syncState.update { state ->
            val dialogues = SrtParser.parseInputTextToDialogues(input)
            val refSubs = if (state.referenceSrtContent.isNotEmpty()) {
                SrtParser.parseSrt(state.referenceSrtContent)
            } else {
                emptyList()
            }

            val (alignedSubs, message) = SrtParser.alignSrt(dialogues, refSubs)
            val outputText = SrtParser.srtListToString(alignedSubs)

            val inputSubCount = dialogues.size
            val inputLineCount = input.lines().filter { it.isNotBlank() }.size
            val refSubCount = refSubs.size
            val refLineCount = state.referenceSrtContent.lines().filter { it.isNotBlank() }.size

            state.copy(
                mainInput = input,
                srtOutput = outputText,
                mainSubtitleCount = inputSubCount,
                mainLineCount = inputLineCount,
                refSubtitleCount = refSubCount,
                refLineCount = refLineCount,
                lineDifference = inputSubCount - refSubCount,
                infoMessage = message
            )
        }
    }

    fun onReferenceFileLoaded(content: String, fileName: String) {
        _syncState.update { state ->
            val dialogues = SrtParser.parseInputTextToDialogues(state.mainInput)
            val refErrors = mutableListOf<SrtError>()
            val refSubs = SrtParser.parseSrt(content, refErrors)

            val (alignedSubs, message) = SrtParser.alignSrt(dialogues, refSubs)
            val outputText = SrtParser.srtListToString(alignedSubs)

            val inputSubCount = dialogues.size
            val inputLineCount = state.mainInput.lines().filter { it.isNotBlank() }.size
            val refSubCount = refSubs.size
            val refLineCount = content.lines().filter { it.isNotBlank() }.size

            state.copy(
                referenceSrtContent = content,
                referenceFileName = fileName,
                srtOutput = outputText,
                mainSubtitleCount = inputSubCount,
                mainLineCount = inputLineCount,
                refSubtitleCount = refSubCount,
                refLineCount = refLineCount,
                lineDifference = inputSubCount - refSubCount,
                infoMessage = message,
                errors = refErrors
            )
        }
    }

    fun removeReferenceFile() {
        _syncState.update { state ->
            val dialogues = SrtParser.parseInputTextToDialogues(state.mainInput)
            val (alignedSubs, message) = SrtParser.alignSrt(dialogues, emptyList())
            val outputText = SrtParser.srtListToString(alignedSubs)

            state.copy(
                referenceSrtContent = "",
                referenceFileName = null,
                srtOutput = outputText,
                refSubtitleCount = 0,
                refLineCount = 0,
                lineDifference = dialogues.size,
                infoMessage = "فایل مرجع حذف شد. زمان‌بندی خودکار برای تمام خطوط اعمال شد.",
                errors = emptyList()
            )
        }
    }

    fun clearSyncInput() {
        _syncState.update { state ->
            state.copy(
                mainInput = "",
                mainSubtitleCount = 0,
                mainLineCount = 0
            )
        }
        recalculateSyncOutput()
    }

    fun clearSyncOutput() {
        _syncState.update { state ->
            state.copy(
                srtOutput = ""
            )
        }
    }

    private fun recalculateSyncOutput() {
        _syncState.update { state ->
            val dialogues = SrtParser.parseInputTextToDialogues(state.mainInput)
            val refSubs = if (state.referenceSrtContent.isNotEmpty()) {
                SrtParser.parseSrt(state.referenceSrtContent)
            } else {
                emptyList()
            }

            val (alignedSubs, message) = SrtParser.alignSrt(dialogues, refSubs)
            val outputText = SrtParser.srtListToString(alignedSubs)

            state.copy(
                srtOutput = outputText,
                mainSubtitleCount = dialogues.size,
                lineDifference = dialogues.size - refSubs.size,
                infoMessage = message
            )
        }
    }
}
