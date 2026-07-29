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

data class ChatMessage(val role: String, val text: String)

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

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory = _chatHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

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
                
                val systemInstruction = Content(
                    parts = listOf(Part(text = "You are an AI English tutor for a French-speaking student. Keep your responses short, encouraging, and helpful. Correct their English gently if they make mistakes, and primarily respond in English but you can explain in French if they seem confused. Keep it conversational."))
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
