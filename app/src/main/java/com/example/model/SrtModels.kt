package com.example.model

data class SubtitleItem(
    val index: Int,
    val startTime: String?,
    val endTime: String?,
    val text: String
)

data class SrtError(
    val blockNumber: Int,
    val lineText: String,
    val errorMessage: String
)

enum class OutputMode {
    WITH_INDEX,    // 1: Hello
    WITHOUT_INDEX  // Hello
}

data class ExtractionState(
    val srtInput: String = "",
    val outputMode: OutputMode = OutputMode.WITHOUT_INDEX,
    val srtOutput: String = "",
    val subtitleCount: Int = 0,
    val lineCount: Int = 0,
    val errors: List<SrtError> = emptyList()
)

data class SyncState(
    val mainInput: String = "",
    val referenceSrtContent: String = "",
    val referenceFileName: String? = null,
    val srtOutput: String = "",
    val mainSubtitleCount: Int = 0,
    val mainLineCount: Int = 0,
    val refSubtitleCount: Int = 0,
    val refLineCount: Int = 0,
    val lineDifference: Int = 0,
    val infoMessage: String = "",
    val errors: List<SrtError> = emptyList()
)
