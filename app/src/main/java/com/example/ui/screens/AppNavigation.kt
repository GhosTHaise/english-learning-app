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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.VocabularyWord
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.TutorViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AppNavigation(viewModel: TutorViewModel) {
    val navController = rememberNavController()

    val navBarBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    val selectedIconColor = MaterialTheme.colorScheme.primary
    val unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            NavigationBar(
                containerColor = navBarBg,
                modifier = Modifier.background(Color.Transparent)
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Accueil") },
                    label = { Text("Accueil") },
                    selected = currentRoute == "home",
                    onClick = {
                        if (currentRoute != "home") {
                            navController.navigate("home") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedIconColor,
                        unselectedIconColor = unselectedIconColor,
                        selectedTextColor = selectedIconColor,
                        unselectedTextColor = unselectedIconColor,
                        indicatorColor = selectedIconColor.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Book, contentDescription = "Leçons") },
                    label = { Text("Leçons") },
                    selected = currentRoute == "lessons",
                    onClick = {
                        if (currentRoute != "lessons") {
                            navController.navigate("lessons") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedIconColor,
                        unselectedIconColor = unselectedIconColor,
                        selectedTextColor = selectedIconColor,
                        unselectedTextColor = unselectedIconColor,
                        indicatorColor = selectedIconColor.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = "Tuteur IA") },
                    label = { Text("Tuteur IA") },
                    selected = currentRoute == "tutor",
                    onClick = {
                        if (currentRoute != "tutor") {
                            navController.navigate("tutor") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedIconColor,
                        unselectedIconColor = unselectedIconColor,
                        selectedTextColor = selectedIconColor,
                        unselectedTextColor = unselectedIconColor,
                        indicatorColor = selectedIconColor.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Réglages") },
                    label = { Text("Réglages") },
                    selected = currentRoute == "settings",
                    onClick = {
                        if (currentRoute != "settings") {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedIconColor,
                        unselectedIconColor = unselectedIconColor,
                        selectedTextColor = selectedIconColor,
                        unselectedTextColor = unselectedIconColor,
                        indicatorColor = selectedIconColor.copy(alpha = 0.2f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            BackgroundOrbs()
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") { HomeScreen(viewModel, onNavigateToLessons = { navController.navigate("lessons") }) }
                composable("lessons") { LessonScreen(viewModel) }
                composable("tutor") { TutorScreen(viewModel) }
                composable("settings") { SettingsScreen(viewModel) }
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: TutorViewModel,
    onNavigateToLessons: () -> Unit = {}
) {
    val dailyWords by viewModel.dailyWords.collectAsStateWithLifecycle()
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()

    val textColor = MaterialTheme.colorScheme.onBackground
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryColor.copy(alpha = 0.15f))
                            .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF6366F1))))
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Bonjour, Voyageur", color = subTextColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Apprenti Voyageur", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .border(1.dp, primaryColor.copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥 ${progress?.currentStreak ?: 0}", color = Color(0xFFFB923C), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.padding(horizontal = 8.dp).width(1.dp).height(12.dp).background(subTextColor.copy(alpha = 0.3f)))
                        Text("💎 ${progress?.wordsLearnedCount ?: 0}", color = Color(0xFF60A5FA), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("LEÇON QUOTIDIENNE", color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Vocabulaire du Jour", color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Light)
                    Text("\"L'apprentissage du jour\"", color = subTextColor, fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onNavigateToLessons() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Continuer l'apprentissage", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        items(dailyWords) { word ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = word.english, color = textColor, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(text = word.french, color = subTextColor, fontSize = 16.sp)
                    }
                    IconButton(
                        onClick = { viewModel.markWordAsLearned(word) },
                        modifier = Modifier
                            .background(
                                if (word.isLearned) Color(0xFF22C55E).copy(alpha = 0.6f) 
                                else primaryColor.copy(alpha = 0.15f), 
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Filled.Check, 
                            contentDescription = "Marquer comme appris", 
                            tint = if (word.isLearned) Color.White else primaryColor
                        )
                    }
                }
            }
        }
    }
}

