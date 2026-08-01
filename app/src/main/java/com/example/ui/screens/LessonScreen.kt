package com.example.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.LessonData
import com.example.viewmodel.LessonUiState
import com.example.viewmodel.TutorViewModel
import java.util.Locale

private val presetTopics = listOf(
    "🍽️ Restaurant & Café",
    "💼 Business & Entretien",
    "✈️ Voyage & Transport",
    "🤝 Small Talk & Réseau",
    "🛒 Shopping & Achats"
)

private val levelOptions = listOf(
    "A1-A2 Débutant" to "Beginner",
    "B1-B2 Intermédiaire" to "Intermediate",
    "C1-C2 Avancé" to "Advanced"
)

@Composable
fun LessonScreen(viewModel: TutorViewModel) {
    val lessonState by viewModel.lessonState.collectAsStateWithLifecycle()
    val completedLessons by viewModel.completedLessons.collectAsStateWithLifecycle()
    var selectedTopic by remember { mutableStateOf(presetTopics[0]) }
    var selectedLevel by remember { mutableStateOf("Intermediate") }
    var customTopicText by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // Text To Speech
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Trigger initial lesson on launch if idle
    LaunchedEffect(Unit) {
        if (lessonState is LessonUiState.Idle) {
            viewModel.generateLesson("Restaurant & Ordering Food", selectedLevel)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Book,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Leçons d'Anglais IA",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Générées sur-mesure par Gemini",
                        fontSize = 11.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GEMINI 3.5", color = androidx.compose.material3.MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Level Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                levelOptions.forEach { (label, levelCode) ->
                    val isSelected = selectedLevel == levelCode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF4F46E5) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            .border(1.dp, if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .clickable {
                                selectedLevel = levelCode
                                val topicName = if (showCustomInput && customTopicText.isNotBlank()) customTopicText else selectedTopic
                                viewModel.generateLesson(topicName, selectedLevel)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Preset Topics Bar
        item {
            Column {
                Text(
                    text = "SÉLECTIONNER UN THÈME",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetTopics) { topic ->
                        val isSelected = selectedTopic == topic && !showCustomInput
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                .border(1.dp, if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                .clickable {
                                    showCustomInput = false
                                    selectedTopic = topic
                                    viewModel.generateLesson(topic, selectedLevel)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = topic,
                                color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    item {
                        // Custom Topic Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (showCustomInput) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                .border(1.dp, if (showCustomInput) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                .clickable { showCustomInput = !showCustomInput }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Autre sujet...", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // Custom Topic Input Field
        if (showCustomInput) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customTopicText,
                        onValueChange = { customTopicText = it },
                        placeholder = { Text("Ex: Vocabulaire médical, négociation...", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (customTopicText.isNotBlank()) {
                                viewModel.generateLesson(customTopicText, selectedLevel)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Générer", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Main Content Area based on LessonUiState
        when (val state = lessonState) {
            is LessonUiState.Loading -> {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Génération de la leçon...",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "L'IA prépare votre vocabulaire, grammaire et quiz",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            is LessonUiState.Error -> {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Error, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Erreur lors du chargement", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text(state.message, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.generateLesson(selectedTopic, selectedLevel) },
                                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Réessayer")
                            }
                        }
                    }
                }
            }

            is LessonUiState.Success -> {
                val lesson = state.lesson

                // Lesson Hero Header Card
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lesson.title,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.3f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(lesson.level, color = androidx.compose.material3.MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = lesson.summary,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Section 1: Vocabulaire Clé
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "1. VOCABULAIRE ESSENTIEL",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        lesson.vocabulary.forEach { item ->
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.english,
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            if (item.phonetic.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = item.phonetic,
                                                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        Text(
                                            text = item.french,
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            fontSize = 13.sp
                                        )

                                        if (item.example.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "\"${item.example}\"",
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Light
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            tts?.speak(item.english, TextToSpeech.QUEUE_FLUSH, null, null)
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    ) {
                                        Icon(
                                            Icons.Filled.VolumeUp,
                                            contentDescription = "Ecouter",
                                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Règle de Grammaire / Astuce
                if (lesson.grammarRule.isNotBlank()) {
                    item {
                        Column {
                            Text(
                                text = "2. ASTUCE GRAMMAIRE & STRUCTURE",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                                androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                            )
                                        )
                                    )
                                    .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Filled.Lightbulb,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color(0xFFD97706),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = lesson.grammarRule,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Dialogue Pratique
                if (lesson.dialogue.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "3. DIALOGUE IMMERSIF",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )

                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    lesson.dialogue.forEach { line ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = line.speaker,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = line.text,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = line.translation,
                                                    fontSize = 12.sp,
                                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    tts?.speak(line.text, TextToSpeech.QUEUE_FLUSH, null, null)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.VolumeUp,
                                                    contentDescription = "Écouter la ligne",
                                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Quiz Interactive Component
                if (lesson.quizQuestion.isNotBlank() && lesson.quizOptions.isNotEmpty()) {
                    item {
                        QuizWidget(
                            question = lesson.quizQuestion,
                            options = lesson.quizOptions,
                            correctIndex = lesson.correctOptionIndex,
                            explanation = lesson.quizExplanation,
                            onQuizCompleted = { userCorrect ->
                                val score = if (userCorrect) 100 else 50
                                viewModel.saveLessonCompletion(
                                    lessonTitle = lesson.title,
                                    topic = selectedTopic,
                                    level = lesson.level,
                                    score = score,
                                    maxScore = 100,
                                    status = if (userCorrect) "RÉUSSI" else "À REVOIR"
                                )
                            }
                        )
                    }
                }
            }

            else -> {}
        }

        // Section 5: Completed Lessons History (Room Database)
        if (completedLessons.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HISTORIQUE DES LEÇONS (${completedLessons.size})",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        val avgScore = completedLessons.map { it.score }.average().toInt()
                        Text(
                            text = "Score moyen: $avgScore%",
                            color = androidx.compose.ui.graphics.Color(0xFF22C55E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    completedLessons.take(5).forEach { completed ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = completed.lessonTitle,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${completed.topic} • ${completed.level}",
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (completed.score >= 80) androidx.compose.ui.graphics.Color(0xFF22C55E).copy(alpha = 0.3f)
                                            else Color(0xFFD97706).copy(alpha = 0.3f)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${completed.score}/${completed.maxScore}",
                                        color = if (completed.score >= 80) androidx.compose.ui.graphics.Color(0xFF22C55E) else androidx.compose.ui.graphics.Color(0xFFD97706),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizWidget(
    question: String,
    options: List<String>,
    correctIndex: Int,
    explanation: String,
    onQuizCompleted: (userIsCorrect: Boolean) -> Unit = {}
) {
    var selectedOption by remember(question) { mutableStateOf<Int?>(null) }
    var isSubmitted by remember(question) { mutableStateOf(false) }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = "4. QUIZ DE VALIDATION",
            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = question,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                options.forEachIndexed { index, option ->
                    val isSelected = selectedOption == index
                    val isCorrect = index == correctIndex

                    val containerColor = when {
                        !isSubmitted && isSelected -> Color(0xFF4F46E5).copy(alpha = 0.5f)
                        isSubmitted && isCorrect -> androidx.compose.ui.graphics.Color(0xFF22C55E).copy(alpha = 0.4f)
                        isSubmitted && isSelected && !isCorrect -> androidx.compose.material3.MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                        else -> androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    }

                    val borderColor = when {
                        !isSubmitted && isSelected -> androidx.compose.material3.MaterialTheme.colorScheme.primary
                        isSubmitted && isCorrect -> androidx.compose.ui.graphics.Color(0xFF22C55E)
                        isSubmitted && isSelected && !isCorrect -> androidx.compose.material3.MaterialTheme.colorScheme.error
                        else -> androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(containerColor)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable(enabled = !isSubmitted) { selectedOption = index }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { if (!isSubmitted) selectedOption = index },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    unselectedColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isSubmitted) {
                    Button(
                        onClick = {
                            if (selectedOption != null) {
                                isSubmitted = true
                                onQuizCompleted(selectedOption == correctIndex)
                            }
                        },
                        enabled = selectedOption != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            disabledContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Vérifier ma réponse", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    val userIsCorrect = selectedOption == correctIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (userIsCorrect) androidx.compose.ui.graphics.Color(0xFF22C55E).copy(alpha = 0.2f)
                                else androidx.compose.material3.MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (userIsCorrect) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = if (userIsCorrect) androidx.compose.ui.graphics.Color(0xFF22C55E) else androidx.compose.material3.MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (userIsCorrect) "Bravo ! Réponse correcte !" else "Dommage !",
                                    fontWeight = FontWeight.Bold,
                                    color = if (userIsCorrect) androidx.compose.ui.graphics.Color(0xFF22C55E) else androidx.compose.material3.MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp
                                )
                            }
                            if (explanation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = explanation,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
