package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.TutorViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun TutorScreen(viewModel: TutorViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

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
            startListening(speechRecognizer, { isListening = it }, { text -> inputText = text })
        } else {
            Toast.makeText(context, "Permission refusée", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pratique Conversationnelle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.3f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("HORS-LIGNE DISPONIBLE", color = Color(0xFF4ADE80), fontSize = 10.sp, letterSpacing = 1.sp)
            }
        }
        
        if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = false
        ) {
            items(chatHistory) { msg ->
                ChatBubble(msg)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isLoading) {
                item {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        if (isListening) {
                            speechRecognizer.stopListening()
                            isListening = false
                        } else {
                            startListening(speechRecognizer, { isListening = it }, { text -> inputText = text })
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isListening) androidx.compose.ui.graphics.SolidColor(Color.Red.copy(alpha = 0.5f)) 
                        else androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF2563EB)))
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                    contentDescription = "Microphone",
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Parlez ou tapez...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.2f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = CircleShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    val text = inputText
                    inputText = ""
                    viewModel.sendMessageToAI(text) { response ->
                        tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                })
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    val text = inputText
                    inputText = ""
                    viewModel.sendMessageToAI(text) { response ->
                        tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Envoyer", tint = Color.White)
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(if (isUser) RoundedCornerShape(24.dp, 24.dp, 4.dp, 24.dp) else RoundedCornerShape(24.dp, 24.dp, 24.dp, 4.dp))
                .background(if (isUser) androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF3B82F6))) else androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.1f)))
                .border(1.dp, Color.White.copy(alpha = 0.1f), if (isUser) RoundedCornerShape(24.dp, 24.dp, 4.dp, 24.dp) else RoundedCornerShape(24.dp, 24.dp, 24.dp, 4.dp))
                .padding(16.dp)
        ) {
            Text(text = msg.text, color = Color.White)
        }
    }
}

private fun startListening(
    speechRecognizer: SpeechRecognizer,
    setListening: (Boolean) -> Unit,
    onResult: (String) -> Unit
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
    }

    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { setListening(true) }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { setListening(false) }
        override fun onError(error: Int) { setListening(false) }
        override fun onResults(results: Bundle?) {
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
