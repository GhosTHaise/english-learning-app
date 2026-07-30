package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.Repository
import com.example.data.UserProgress
import com.example.data.VocabularyWord
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
        
    val userProgress: StateFlow<UserProgress?> = repository.userProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

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

    private fun initDatabase() {
        viewModelScope.launch {
            val words = listOf(
                VocabularyWord(english = "Hello", french = "Bonjour"),
                VocabularyWord(english = "Thank you", french = "Merci"),
                VocabularyWord(english = "Please", french = "S'il vous plaît"),
                VocabularyWord(english = "Goodbye", french = "Au revoir"),
                VocabularyWord(english = "Yes", french = "Oui"),
                VocabularyWord(english = "No", french = "Non"),
                VocabularyWord(english = "How are you?", french = "Comment allez-vous ?"),
                VocabularyWord(english = "Good morning", french = "Bonjour"),
                VocabularyWord(english = "Good night", french = "Bonne nuit"),
                VocabularyWord(english = "Water", french = "Eau")
            )
            repository.insertInitialWordsIfEmpty(words)
            
            // Check streak
            repository.userProgress.collect { progress ->
                if (progress == null) {
                    repository.saveUserProgress(UserProgress())
                } else {
                    val lastLogin = progress.lastLoginTimestamp
                    val now = System.currentTimeMillis()
                    // Simple streak logic: if more than 24h passed, check if it's the next day
                    val cal1 = Calendar.getInstance().apply { timeInMillis = lastLogin }
                    val cal2 = Calendar.getInstance().apply { timeInMillis = now }
                    
                    if (cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR)) {
                        // Different day
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
