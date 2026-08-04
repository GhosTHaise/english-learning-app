package com.example.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.viewmodel.TutorViewModel
import com.example.viewmodel.ChatMessage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import com.example.viewmodel.TutorMode
import android.widget.Toast
import androidx.compose.material.icons.Icons
import android.speech.SpeechRecognizer
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import android.speech.RecognitionListener
import androidx.compose.foundation.clickable
import android.os.Bundle
import android.content.Intent
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Send
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.MicOff
import android.speech.RecognizerIntent
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import java.util.Locale
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape

@Composable
fun TutorScreen(viewModel: TutorViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    
    var inputText by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var rmsDb by remember { mutableStateOf(0f) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var playbackRate by remember { mutableStateOf(1.0f) }
    
    val listState = rememberLazyListState()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    // Auto-scroll when new messages arrive
    LaunchedEffect(chatHistory.size, isLoading) {
        if (chatHistory.isNotEmpty()) {
            val maxIndex = if (isLoading) chatHistory.size else chatHistory.size - 1
            if (maxIndex >= 0) {
                listState.animateScrollToItem(maxIndex)
            }
        }
    }

    // TTS Init
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer.destroy()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening(
                speechRecognizer = speechRecognizer,
                setListening = { isListening = it },
                onRmsChange = { rmsDb = it },
                onResult = { text ->
                    inputText = text
                    viewModel.sendMessageToAI(text) { response ->
                        tts?.setSpeechRate(playbackRate)
                        tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            )
        } else {
            Toast.makeText(context, "Permission microphone refusée", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tuteur IA Vocal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "Pratiquez votre accent en temps réel",
                    fontSize = 11.sp,
                    color = subTextColor
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color(0xFF22C55E).copy(alpha = 0.2f))
                    .border(1.dp, androidx.compose.ui.graphics.Color(0xFF22C55E).copy(alpha = 0.3f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF4ADE80), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GEMINI LIVE", color = androidx.compose.ui.graphics.Color(0xFF4ADE80), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Tutor Mode Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            items(TutorMode.values()) { mode ->
                val isSelected = mode == selectedMode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            1.dp,
                            if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.selectMode(mode) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = mode.title,
                        color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.onPrimary else subTextColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Live Audio Spectrogram Visualizer Card
        SpectrogramVisualizer(
            isListening = isListening,
            rmsDb = rmsDb,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Chat History List
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatHistory) { msg ->
                ChatBubble(
                    msg = msg,
                    onReplay = { text ->
                        tts?.setSpeechRate(playbackRate)
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                )
            }
            if (isLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Le tuteur génère sa réponse...",
                            color = subTextColor,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Speed Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Vitesse audio :", color = subTextColor.copy(alpha = 0.7f), fontSize = 11.sp)
            Spacer(modifier = Modifier.width(8.dp))
            listOf(0.8f to "0.8x Lent", 1.0f to "1.0x Normal", 1.25f to "1.25x Rapide").forEach { (rate, label) ->
                val isSelected = playbackRate == rate
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else subTextColor.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { playbackRate = rate }
                )
            }
        }

        // Bottom Input Control Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Microphone Button
            IconButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        if (isListening) {
                            speechRecognizer.stopListening()
                            isListening = false
                        } else {
                            startListening(
                                speechRecognizer = speechRecognizer,
                                setListening = { isListening = it },
                                onRmsChange = { rmsDb = it },
                                onResult = { text ->
                                    inputText = text
                                    viewModel.sendMessageToAI(text) { response ->
                                        tts?.setSpeechRate(playbackRate)
                                        tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
                                    }
                                }
                            )
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (isListening) androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444)) 
                        else androidx.compose.ui.graphics.Brush.linearGradient(listOf(androidx.compose.material3.MaterialTheme.colorScheme.primary, androidx.compose.material3.MaterialTheme.colorScheme.primary))
                    )
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            ) {
                Icon(
                    if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                    contentDescription = "Microphone",
                    tint = textColor
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            // Text Input Field
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Tapez votre réponse en anglais...", fontSize = 13.sp, color = subTextColor.copy(alpha = 0.7f)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = CircleShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    val text = inputText
                    if (text.isNotBlank()) {
                        inputText = ""
                        viewModel.sendMessageToAI(text) { response ->
                            tts?.setSpeechRate(playbackRate)
                            tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    }
                })
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            IconButton(
                onClick = {
                    val text = inputText
                    if (text.isNotBlank()) {
                        inputText = ""
                        viewModel.sendMessageToAI(text) { response ->
                            tts?.setSpeechRate(playbackRate)
                            tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Envoyer", tint = textColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ChatBubble(
    msg: ChatMessage,
    onReplay: (String) -> Unit
) {

    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    val isUser = msg.role == "user"
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        if (isUser) RoundedCornerShape(22.dp, 22.dp, 4.dp, 22.dp)
                        else RoundedCornerShape(22.dp, 22.dp, 22.dp, 4.dp)
                    )
                    .background(
                        if (isUser) androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(androidx.compose.material3.MaterialTheme.colorScheme.primary, androidx.compose.material3.MaterialTheme.colorScheme.primary)
                        ) else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        if (isUser) RoundedCornerShape(22.dp, 22.dp, 4.dp, 22.dp)
                        else RoundedCornerShape(22.dp, 22.dp, 22.dp, 4.dp)
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isUser) "Vous" else "Tuteur IA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) androidx.compose.material3.MaterialTheme.colorScheme.onPrimary else androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )

                        if (!isUser) {
                            Icon(
                                imageVector = Icons.Filled.VolumeUp,
                                contentDescription = "Réécouter",
                                tint = textColor.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onReplay(msg.text) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = msg.text,
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

private fun startListening(
    speechRecognizer: SpeechRecognizer,
    setListening: (Boolean) -> Unit,
    onRmsChange: (Float) -> Unit,
    onResult: (String) -> Unit
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {
            onRmsChange(rmsdB)
        }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            setListening(false)
        }
        override fun onResults(results: Bundle?) {
            setListening(false)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onResult(matches[0])
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })
    
    speechRecognizer.startListening(intent)
}

