package com.example.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OutputMode
import com.example.model.ExtractionState
import com.example.model.SyncState
import com.example.model.SubtitleItem
import com.example.model.SrtError
import com.example.viewmodel.SrtViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SrtApp(viewModel: SrtViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("استخراج متن از SRT", "ساخت و اصلاح SRT")

    val showSnackbar: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ابزار زیرنویس SRT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ویرایش، استخراج متن و همگام‌سازی کاملاً آفلاین",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Force RTL for tab titles so Persian looks correct
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            modifier = Modifier.testTag("tab_$index")
                        )
                    }
                }
            }

            // Body Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> ExtractorTab(viewModel, context, clipboardManager, showSnackbar)
                    1 -> CreatorTab(viewModel, context, clipboardManager, showSnackbar)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExtractorTab(
    viewModel: SrtViewModel,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    showSnackbar: (String) -> Unit
) {
    val state by viewModel.extractionState.collectAsState()
    val scrollState = rememberScrollState()

    // SAF File Saver for TXT
    val saveTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            writeTextToUri(context, it, state.srtOutput)
            showSnackbar("فایل با موفقیت ذخیره شد")
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Description Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "اطلاعات",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "متن کامل فایل SRT را در کادر زیر وارد کنید تا دیالوگ‌های آن فوراً استخراج شوند.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Input TextArea Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "ورودی (متن SRT)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = state.srtInput,
                        onValueChange = { viewModel.onSrtInputChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("srt_input_field"),
                        placeholder = {
                            Text(
                                "نمونه ورودی:\n1\n00:00:01,000 --> 00:00:03,000\nسلام دنیا\n...",
                                style = TextStyle(textDirection = TextDirection.ContentOrRtl)
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            textDirection = TextDirection.ContentOrRtl
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input actions row
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stats
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "تعداد زیرنویس: ${state.subtitleCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "خطوط متن: ${state.lineCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    val clipboardText = clipboardManager.getText()?.text ?: ""
                                    if (clipboardText.isNotEmpty()) {
                                        viewModel.onSrtInputChanged(clipboardText)
                                        showSnackbar("متن از حافظه موقت جای‌گذاری شد")
                                    } else {
                                        showSnackbar("حافظه موقت خالی است")
                                    }
                                },
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                modifier = Modifier.testTag("paste_input_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "جای‌گذاری",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("جای‌گذاری", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.clearExtractionInput()
                                    showSnackbar("ورودی پاک شد")
                                },
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                modifier = Modifier.testTag("clear_input_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "پاک کردن",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("پاک کردن ورودی", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Mode Toggle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "فرمت خروجی دیالوگ‌ها",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            onClick = { viewModel.onOutputModeChanged(OutputMode.WITH_INDEX) },
                            selected = state.outputMode == OutputMode.WITH_INDEX,
                            modifier = Modifier.weight(1f).testTag("mode_with_index")
                        ) {
                            Text("همراه با شماره (1: Hello)", fontSize = 12.sp)
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            onClick = { viewModel.onOutputModeChanged(OutputMode.WITHOUT_INDEX) },
                            selected = state.outputMode == OutputMode.WITHOUT_INDEX,
                            modifier = Modifier.weight(1f).testTag("mode_without_index")
                        ) {
                            Text("فقط متن دیالوگ‌ها (Hello)", fontSize = 12.sp)
                        }
                    }
                }
            }

            // SRT Error Card
            AnimatedVisibility(
                visible = state.errors.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                OutlinedCard(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "خطای ساختاری",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "خطاهای فرمت SRT یافت شده (${state.errors.size} خطا)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        state.errors.take(5).forEach { error ->
                            Text(
                                text = "• بخش ${error.blockNumber}: ${error.errorMessage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                        if (state.errors.size > 5) {
                            Text(
                                text = "... و ${state.errors.size - 5} خطای دیگر",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Output TextArea Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "خروجی (فقط خواندنی)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (state.srtOutput.isEmpty()) {
                        // Empty State
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = "خالی",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "منتظر ورود اطلاعات...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = state.srtOutput,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .testTag("srt_output_field"),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                textDirection = TextDirection.ContentOrRtl
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Output actions row
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearExtractionOutput()
                                showSnackbar("خروجی پاک شد")
                            },
                            enabled = state.srtOutput.isNotEmpty(),
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                            modifier = Modifier.testTag("clear_output_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "پاک کردن",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("پاک کردن خروجی", fontSize = 11.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    if (state.srtOutput.isNotEmpty()) {
                                        clipboardManager.setText(AnnotatedString(state.srtOutput))
                                        showSnackbar("متن خروجی در حافظه کپی شد")
                                    }
                                },
                                enabled = state.srtOutput.isNotEmpty(),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                modifier = Modifier.testTag("copy_output_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "کپی",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("کپی خروجی", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    if (state.srtOutput.isNotEmpty()) {
                                        saveTxtLauncher.launch("dialogues_extracted.txt")
                                    }
                                },
                                enabled = state.srtOutput.isNotEmpty(),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("save_output_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "ذخیره",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ذخیره به صورت TXT", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatorTab(
    viewModel: SrtViewModel,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    showSnackbar: (String) -> Unit
) {
    val state by viewModel.syncState.collectAsState()
    val scrollState = rememberScrollState()

    // SAF Reference File Opener
    val openSrtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val content = readTextFromUri(context, it)
            val fileName = getFileNameFromUri(context, it) ?: "reference.srt"
            viewModel.onReferenceFileLoaded(content, fileName)
            showSnackbar("فایل مرجع بارگذاری شد: $fileName")
        }
    }

    // SAF Aligned SRT File Saver
    val saveSrtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            writeTextToUri(context, it, state.srtOutput)
            showSnackbar("فایل زیرنویس با موفقیت ذخیره شد")
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Instructions Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "راهنما",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "یک متن ساده یا شماره‌گذاری شده بنویسید و فایل زمان‌بندی مرجع را انتخاب کنید تا با هم ادغام شوند.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Reference File Section Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "فایل زیرنویس مرجع (زمان‌بندی)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.referenceFileName != null) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "انتخاب شده",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = state.referenceFileName ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${state.refSubtitleCount} ردیف زمان‌بندی",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            IconButton(
                                onClick = {
                                    viewModel.removeReferenceFile()
                                    showSnackbar("فایل مرجع حذف شد")
                                },
                                modifier = Modifier.testTag("remove_ref_file")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "حذف فایل مرجع",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Text(
                                text = "هیچ فایلی انتخاب نشده است.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    openSrtLauncher.launch(arrayOf("*/*"))
                                },
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.testTag("select_ref_file")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "انتخاب فایل",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("انتخاب فایل SRT مرجع", fontSize = 11.sp)
                            }
                        }
                    }

                    // Show Warning if Ref File has errors
                    AnimatedVisibility(
                        visible = state.errors.isNotEmpty() && state.referenceFileName != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "هشدار ساختار",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "هشدار: فایل مرجع شامل ${state.errors.size} خطای ساختاری است که بر زمان‌بندی تأثیر می‌گذارد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Input TextArea Card (Dialogues/Texts)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "متن جدید (ساده، شماره‌گذاری شده یا SRT)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = state.mainInput,
                        onValueChange = { viewModel.onMainInputChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("creator_input_field"),
                        placeholder = {
                            Text(
                                "دیالوگ‌های خود را وارد کنید:\nیک دیالوگ در هر خط\nیا با ساختار:\n1: دیالوگ اول\n2: دیالوگ دوم\n...",
                                style = TextStyle(textDirection = TextDirection.ContentOrRtl)
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            textDirection = TextDirection.ContentOrRtl
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input actions row
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stats
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "تعداد دیالوگ‌ها: ${state.mainSubtitleCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (state.referenceFileName != null) {
                                val diffText = if (state.lineDifference > 0) {
                                    "+${state.lineDifference}"
                                } else {
                                    "${state.lineDifference}"
                                }
                                Text(
                                    text = "اختلاف خطوط: $diffText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (state.lineDifference != 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (state.lineDifference != 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        // Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    val clipboardText = clipboardManager.getText()?.text ?: ""
                                    if (clipboardText.isNotEmpty()) {
                                        viewModel.onMainInputChanged(clipboardText)
                                        showSnackbar("متن از حافظه موقت جای‌گذاری شد")
                                    } else {
                                        showSnackbar("حافظه موقت خالی است")
                                    }
                                },
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                modifier = Modifier.testTag("paste_creator_input_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "جای‌گذاری",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("جای‌گذاری", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.clearSyncInput()
                                    showSnackbar("ورودی پاک شد")
                                },
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                modifier = Modifier.testTag("clear_creator_input_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "پاک کردن",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("پاک کردن ورودی", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Info and Warning Banner
            AnimatedVisibility(
                visible = state.infoMessage.isNotEmpty() && state.mainInput.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                OutlinedCard(
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.lineDifference != 0) {
                            MaterialTheme.colorScheme.warningContainer()
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (state.lineDifference != 0) {
                                MaterialTheme.colorScheme.warningBorder()
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state.lineDifference != 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = "اطلاعات همگام‌سازی",
                            tint = if (state.lineDifference != 0) {
                                MaterialTheme.colorScheme.warningTint()
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = state.infoMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.lineDifference != 0) {
                                MaterialTheme.colorScheme.onWarningContainer()
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Aligned Output Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "زیرنویس همگام‌سازی شده (خروجی SRT)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (state.srtOutput.trim().isEmpty() || state.mainInput.trim().isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = "خالی",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "منتظر ورود اطلاعات...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = state.srtOutput,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .testTag("creator_output_field"),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                textDirection = TextDirection.Ltr // Subtitles timestamps must stay Ltr
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Output actions row
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearSyncOutput()
                                showSnackbar("خروجی پاک شد")
                            },
                            enabled = state.srtOutput.trim().isNotEmpty() && state.mainInput.trim().isNotEmpty(),
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                            modifier = Modifier.testTag("clear_creator_output_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "پاک کردن",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("پاک کردن خروجی", fontSize = 11.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    if (state.srtOutput.isNotEmpty()) {
                                        clipboardManager.setText(AnnotatedString(state.srtOutput))
                                        showSnackbar("متن خروجی در حافظه کپی شد")
                                    }
                                },
                                enabled = state.srtOutput.trim().isNotEmpty() && state.mainInput.trim().isNotEmpty(),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                modifier = Modifier.testTag("copy_creator_output_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "کپی",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("کپی خروجی", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    if (state.srtOutput.isNotEmpty()) {
                                        saveSrtLauncher.launch("aligned_subtitle.srt")
                                    }
                                },
                                enabled = state.srtOutput.trim().isNotEmpty() && state.mainInput.trim().isNotEmpty(),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("save_creator_output_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "ذخیره",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ذخیره به صورت SRT", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// ==========================================
// UTILITY FUNCTIONS
// ==========================================

private fun writeTextToUri(context: Context, uri: Uri, text: String) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(text.toByteArray(Charsets.UTF_8))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun readTextFromUri(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.readText()
            }
        } ?: ""
    } catch (e: Exception) {
        ""
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

// Helper styling extensions for custom colors
@Composable
private fun ColorScheme.warningContainer(): Color {
    return if (!androidx.compose.foundation.isSystemInDarkTheme()) {
        Color(0xFFFFF8E1) // light warm amber
    } else {
        Color(0xFF3E2723) // dark warm amber/brown
    }
}

@Composable
private fun ColorScheme.warningBorder(): Color {
    return Color(0xFFFFB300)
}

@Composable
private fun ColorScheme.warningTint(): Color {
    return Color(0xFFFF8F00)
}

@Composable
private fun ColorScheme.onWarningContainer(): Color {
    return if (!androidx.compose.foundation.isSystemInDarkTheme()) {
        Color(0xFF5D4037)
    } else {
        Color(0xFFFFECB3)
    }
}
