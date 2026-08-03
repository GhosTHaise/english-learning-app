package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.TutorApp
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.CompletedLesson
import com.example.data.Repository
import com.example.data.UserProgress
import com.example.data.VocabularyWord
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
data class LessonVocab(
    val english: String = "",
    val french: String = "",
    val phonetic: String = "",
    val example: String = ""
)

@Serializable
data class DialogueLine(
    val speaker: String = "Person",
    val text: String = "",
    val translation: String = ""
)

@Serializable
data class LessonData(
    val title: String = "",
    val level: String = "Intermediate",
    val summary: String = "",
    val vocabulary: List<LessonVocab> = emptyList(),
    val grammarRule: String = "",
    val dialogue: List<DialogueLine> = emptyList(),
    val quizQuestion: String = "",
    val quizOptions: List<String> = emptyList(),
    val correctOptionIndex: Int = 0,
    val quizExplanation: String = ""
)

sealed class LessonUiState {
    object Idle : LessonUiState()
    object Loading : LessonUiState()
    data class Success(val lesson: LessonData) : LessonUiState()
    data class Error(val message: String) : LessonUiState()
}

enum class TutorMode(val title: String, val description: String, val prompt: String) {
    CASUAL(
        "Ami Conversationnel",
        "Echanges naturels et quotidien",
        "You are an AI English tutor for a French student. Keep responses short, encouraging, natural, and friendly. Correct minor grammar mistakes naturally."
    ),
    BUSINESS(
        "Business & Carrière",
        "Vocabulaire professionnel et entretiens",
        "You are an executive English coach. Focus on professional vocabulary, formal expressions, email etiquette, and interview preparation. Correct errors precisely."
    ),
    GRAMMAR(
        "Phonétique & Grammaire",
        "Explications détaillées et corrections",
        "You are a master English linguistics tutor. Analyze the user's sentence structure, highlight grammar or pronunciation tips in detail, and explain nuances in French if needed."
    )
}

data class ChatMessage(
    val role: String,
    val text: String,
    val fluencyScore: Int? = null,
    val feedback: String? = null
)

class TutorViewModel(private val repository: Repository) : ViewModel() {
    val dailyWords: StateFlow<List<VocabularyWord>> = repository.dailyWords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    val historyWords: StateFlow<List<VocabularyWord>> = repository.getHistoryWords(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    val userProgress: StateFlow<UserProgress?> = repository.userProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val completedLessons: StateFlow<List<CompletedLesson>> = repository.completedLessons
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        saveThemeMode(mode)
    }

    private fun loadThemeMode(): ThemeMode {
        return try {
            val prefs = TutorApp.instance.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val saved = prefs.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
            ThemeMode.valueOf(saved)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    private fun saveThemeMode(mode: ThemeMode) {
        try {
            val prefs = TutorApp.instance.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            prefs.edit().putString("theme_mode", mode.name).apply()
        } catch (_: Exception) {}
    }

    fun clearCompletedLessons() {
        viewModelScope.launch {
            repository.clearCompletedLessons()
        }
    }


    private val _selectedMode = MutableStateFlow(TutorMode.CASUAL)
    val selectedMode = _selectedMode.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory = _chatHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _lastSpeechAnalysis = MutableStateFlow<String?>(null)
    val lastSpeechAnalysis = _lastSpeechAnalysis.asStateFlow()

    private val _lessonState = MutableStateFlow<LessonUiState>(LessonUiState.Idle)
    val lessonState: StateFlow<LessonUiState> = _lessonState.asStateFlow()

    fun selectMode(mode: TutorMode) {
        _selectedMode.value = mode
    }

    init {
        initDatabase()
    }

    private var databaseInitialized = false

    private fun initDatabase() {
        if (databaseInitialized) return
        databaseInitialized = true
        
        viewModelScope.launch {
            // Check streak
            repository.userProgress.collect { progress ->
                if (progress == null) {
                    repository.saveUserProgress(UserProgress(lastLoginTimestamp = System.currentTimeMillis()))
                    generateDailyVocabulary()
                } else {
                    val lastLogin = progress.lastLoginTimestamp
                    val now = System.currentTimeMillis()
                    // Simple streak logic: if more than 24h passed, check if it's the next day
                    val cal1 = Calendar.getInstance().apply { timeInMillis = lastLogin }
                    val cal2 = Calendar.getInstance().apply { timeInMillis = now }
                    
                    if (cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR) || cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR)) {
                        // Different day
                        generateDailyVocabulary()
                        if (cal2.get(Calendar.DAY_OF_YEAR) - cal1.get(Calendar.DAY_OF_YEAR) == 1 ||
                            (cal2.get(Calendar.YEAR) > cal1.get(Calendar.YEAR))) {
                            // Consecutive day
                            repository.saveUserProgress(progress.copy(currentStreak = progress.currentStreak + 1, lastLoginTimestamp = now))
                        } else {
                            // Streak broken
                            repository.saveUserProgress(progress.copy(currentStreak = 1, lastLoginTimestamp = now))
                        }
                    }
                }
            }
        }
    }

    fun markWordAsLearned(word: VocabularyWord) {
        viewModelScope.launch {
            repository.updateWord(word.copy(isLearned = true))
            val progress = userProgress.value ?: UserProgress()
            repository.saveUserProgress(progress.copy(wordsLearnedCount = progress.wordsLearnedCount + 1))
        }
    }

    fun sendMessageToAI(text: String, onAiResponse: (String) -> Unit) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val userMsg = ChatMessage(role = "user", text = text)
            _chatHistory.update { it + userMsg }

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                _error.value = "Gemini API Key is missing. Please configure it in AI Studio Secrets."
                _isLoading.value = false
                return@launch
            }

            try {
                // Convert history for Gemini API
                val apiContents = _chatHistory.value.map { msg ->
                    Content(role = if (msg.role == "ai") "model" else "user", parts = listOf(Part(text = msg.text)))
                }
                
                val modePrompt = _selectedMode.value.prompt
                val systemInstruction = Content(
                    parts = listOf(Part(text = "$modePrompt Keep responses under 4 sentences."))
                )

                val request = GenerateContentRequest(
                    contents = apiContents,
                    systemInstruction = systemInstruction
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from AI."
                
                val aiMsg = ChatMessage(role = "ai", text = responseText)
                _chatHistory.update { it + aiMsg }
                onAiResponse(responseText)
                
            } catch (e: Exception) {
                _error.value = "Failed to connect to AI: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    
    private fun generateDailyVocabulary() {
        viewModelScope.launch {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                val words = listOf(
                    VocabularyWord(english = "Hello", french = "Bonjour"),
                    VocabularyWord(english = "Thank you", french = "Merci"),
                    VocabularyWord(english = "Please", french = "S'il vous plaît"),
                    VocabularyWord(english = "Goodbye", french = "Au revoir"),
                    VocabularyWord(english = "Yes", french = "Oui")
                )
                repository.insertWords(words)
                return@launch
            }
            
            try {
                val promptText = """
                    Generate 5 useful daily English vocabulary words for a French speaker.
                    Return ONLY a valid JSON array of objects without markdown blocks, with these exact fields:
                    [
                        {"english": "word1", "french": "traduction1"}
                    ]
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                    generationConfig = GenerationConfig(responseModalities = listOf("TEXT"))
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val aiText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                
                val cleanedJson = aiText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val jsonArray = org.json.JSONArray(cleanedJson)
                val newWords = mutableListOf<VocabularyWord>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    newWords.add(VocabularyWord(
                        english = obj.optString("english", ""),
                        french = obj.optString("french", ""),
                        isLearned = false
                    ))
                }
                
                if (newWords.isNotEmpty()) {
                    repository.insertWords(newWords)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun generateLesson(topic: String, level: String = "Intermediate") {
        viewModelScope.launch {
            _lessonState.value = LessonUiState.Loading

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                _lessonState.value = LessonUiState.Success(getFallbackLesson(topic, level))
                return@launch
            }

            try {
                val promptText = """
                    Create a structured English lesson for a French speaker on the topic: "$topic" at level "$level".
                    Return ONLY a valid JSON object without markdown code blocks, with these exact fields:
                    {
                      "title": "Lesson Title in English",
                      "level": "$level",
                      "summary": "Short 2-sentence summary of the lesson goal in French.",
                      "vocabulary": [
                        {"english": "word1", "french": "traduction1", "phonetic": "/phonetic/", "example": "Example sentence in English."}
                      ],
                      "grammarRule": "Explanation of a key grammar pattern or useful phrase structure in French.",
                      "dialogue": [
                        {"speaker": "Alex", "text": "English line 1", "translation": "French translation 1"},
                        {"speaker": "Sarah", "text": "English line 2", "translation": "French translation 2"}
                      ],
                      "quizQuestion": "A multiple choice question testing this lesson in English.",
                      "quizOptions": ["Option A", "Option B", "Option C", "Option D"],
                      "correctOptionIndex": 0,
                      "quizExplanation": "Explanation of the correct answer in French."
                    }
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                    systemInstruction = Content(parts = listOf(Part(text = "You are an expert English language educator. Always return clean valid JSON format matching the requested schema.")))
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

                val cleanedJson = cleanJsonString(rawText)
                val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
                val lessonData = json.decodeFromString<LessonData>(cleanedJson)

                _lessonState.value = LessonUiState.Success(lessonData)
            } catch (e: Exception) {
                _lessonState.value = LessonUiState.Success(getFallbackLesson(topic, level))
            }
        }
    }

    private fun cleanJsonString(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```")
        }
        return cleaned.trim()
    }

    private fun getFallbackLesson(topic: String, level: String): LessonData {
        return LessonData(
            title = "Mastering $topic ($level)",
            level = level,
            summary = "Cette leçon vous apprend les expressions clés et le vocabulaire indispensable pour aborder le sujet \"$topic\" avec aisance et confiance.",
            vocabulary = listOf(
                LessonVocab(
                    english = "Could I have...",
                    french = "Puis-je avoir...",
                    phonetic = "/kʊd aɪ hæv/",
                    example = "Could I have a table for two, please?"
                ),
                LessonVocab(
                    english = "I would like to order",
                    french = "Je souhaiterais commander",
                    phonetic = "/aɪ wʊd laɪk tuː ˈɔːrdər/",
                    example = "I would like to order the daily special."
                ),
                LessonVocab(
                    english = "What do you recommend?",
                    french = "Que me recommandez-vous ?",
                    phonetic = "/wɒt duː juː ˌrekəˈmend/",
                    example = "What do you recommend for dessert?"
                ),
                LessonVocab(
                    english = "The check, please",
                    french = "L'addition, s'il vous plaît",
                    phonetic = "/ðə tʃek pliːz/",
                    example = "Excuse me, could we get the check, please?"
                )
            ),
            grammarRule = "Pour faire une demande polie en anglais, privilégiez l'utilisation de 'Could I...' ou 'I would like...' plutôt que 'I want...', qui est souvent perçu comme trop direct.",
            dialogue = listOf(
                DialogueLine(
                    speaker = "Waiter",
                    text = "Good evening! Are you ready to order?",
                    translation = "Bonsoir ! Êtes-vous prêts à commander ?"
                ),
                DialogueLine(
                    speaker = "Customer",
                    text = "Yes, I would like the grilled salmon with vegetables.",
                    translation = "Oui, je voudrais le saumon grillé avec des légumes."
                ),
                DialogueLine(
                    speaker = "Waiter",
                    text = "Excellent choice! Would you like anything to drink?",
                    translation = "Excellent choix ! Désirez-vous quelque chose à boire ?"
                ),
                DialogueLine(
                    speaker = "Customer",
                    text = "Just a bottle of sparkling water, please.",
                    translation = "Juste une bouteille d'eau pétillante, s'il vous plaît."
                )
            ),
            quizQuestion = "Quelle tournure est la plus polie pour passer une commande en anglais ?",
            quizOptions = listOf(
                "I want a coffee now.",
                "Give me coffee.",
                "I would like a coffee, please.",
                "Coffee for me."
            ),
            correctOptionIndex = 2,
            quizExplanation = "'I would like...' est la formule standard de politesse pour exprimer un souhait ou passer une commande."
        )
    }

    fun saveLessonCompletion(
        lessonTitle: String,
        topic: String,
        level: String,
        score: Int,
        maxScore: Int = 100,
        status: String = "COMPLETED"
    ) {
        viewModelScope.launch {
            val lesson = CompletedLesson(
                lessonTitle = lessonTitle,
                topic = topic,
                level = level,
                score = score,
                maxScore = maxScore,
                status = status,
                timestamp = System.currentTimeMillis()
            )
            repository.saveCompletedLesson(lesson)

            // Update user progress count
            val currentProgress = userProgress.value ?: UserProgress(id = 1, currentStreak = 1, lastLoginTimestamp = System.currentTimeMillis(), wordsLearnedCount = 0)
            repository.saveUserProgress(currentProgress.copy(wordsLearnedCount = currentProgress.wordsLearnedCount + 1))
        }
    }
}

class TutorViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TutorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TutorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
