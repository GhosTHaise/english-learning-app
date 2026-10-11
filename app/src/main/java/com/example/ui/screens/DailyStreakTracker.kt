package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyPracticeLog
import com.example.data.UserProgress
import com.example.viewmodel.TutorViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class WeekDayInfo(
    val dayLabel: String,     // "L", "M", "M", "J", "V", "S", "D"
    val fullName: String,     // "Lundi", etc.
    val dateStr: String,      // "2026-10-11"
    val dayNumber: Int,       // 11
    val isToday: Boolean,
    val isPracticed: Boolean,
    val isPast: Boolean,
    val isFuture: Boolean,
    val practiceCount: Int = 0
)

data class StreakMilestone(
    val targetDays: Int,
    val title: String,
    val description: String,
    val badgeIcon: String
)

val streakMilestones = listOf(
    StreakMilestone(1, "Allumage", "Première flamme allumée", "🔥"),
    StreakMilestone(3, "Étincelle Constante", "3 jours d'assiduité", "⚡"),
    StreakMilestone(7, "Habitude d'Or", "1 semaine complète d'anglais", "🏆"),
    StreakMilestone(14, "Flamme de Diamant", "2 semaines d'immersion", "💎"),
    StreakMilestone(30, "Maître Bilingue", "1 mois consécutif d'excellence", "👑")
)

fun calculateWeekDays(
    todayDateStr: String,
    practiceLogs: List<DailyPracticeLog>
): List<WeekDayInfo> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.FRENCH)
    val cal = Calendar.getInstance()

    // Determine start of current week (Monday)
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
    cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)

    val list = mutableListOf<WeekDayInfo>()
    val dayInitials = listOf("L", "M", "M", "J", "V", "S", "D")

    val logMap = practiceLogs.associateBy { it.date }

    for (i in 0..6) {
        val dateString = sdf.format(cal.time)
        val dayNum = cal.get(Calendar.DAY_OF_MONTH)
        val isToday = dateString == todayDateStr
        val fullName = dayOfWeekFormat.format(cal.time).replaceFirstChar { it.uppercase() }

        val todayCal = Calendar.getInstance()
        val isPast = cal.before(todayCal) && !isToday
        val isFuture = cal.after(todayCal) && !isToday

        val log = logMap[dateString]
        val isPracticed = log != null && log.practiceCount > 0
        val count = log?.practiceCount ?: 0

        list.add(
            WeekDayInfo(
                dayLabel = dayInitials[i],
                fullName = fullName,
                dateStr = dateString,
                dayNumber = dayNum,
                isToday = isToday,
                isPracticed = isPracticed,
                isPast = isPast,
                isFuture = isFuture,
                practiceCount = count
            )
        )
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    return list
}

@Composable
fun DailyStreakCard(
    viewModel: TutorViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val userProgress by viewModel.userProgress.collectAsStateWithLifecycle()
    val practiceLogs by viewModel.dailyPracticeLogs.collectAsStateWithLifecycle()
    val celebrationMessage by viewModel.streakCelebrationMessage.collectAsStateWithLifecycle()

    val todayDateStr = remember { viewModel.getTodayDateString() }
    val isPracticedToday = practiceLogs.any { it.date == todayDateStr }

    var showDetailsDialog by remember { mutableStateOf(false) }
    var selectedDayInfo by remember { mutableStateOf<WeekDayInfo?>(null) }
    var showExpressChallenge by remember { mutableStateOf(false) }

    val currentStreak = userProgress?.currentStreak ?: 0
    val longestStreak = userProgress?.longestStreak ?: 0
    val totalPracticeDays = userProgress?.totalPracticeDays ?: practiceLogs.size

    val weekDays = remember(todayDateStr, practiceLogs) {
        calculateWeekDays(todayDateStr, practiceLogs)
    }

    // Next milestone computation
    val nextMilestone = streakMilestones.firstOrNull { it.targetDays > currentStreak } ?: streakMilestones.last()
    val prevTarget = streakMilestones.lastOrNull { it.targetDays <= currentStreak }?.targetDays ?: 0
    val progressToNext = if (nextMilestone.targetDays > prevTarget) {
        ((currentStreak - prevTarget).toFloat() / (nextMilestone.targetDays - prevTarget).toFloat()).coerceIn(0f, 1f)
    } else 1f

    // Flame pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "flameGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Streak Celebration Banner
    AnimatedVisibility(
        visible = celebrationMessage != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        celebrationMessage?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("streak_celebration_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF97316).copy(alpha = 0.2f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF97316))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearCelebrationMessage() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Fermer",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Main Daily Streak Card
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_streak_tracker_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPracticedToday) Color(0xFFF97316).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Fire Badge & Streak Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing animated flame box
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFF97316).copy(alpha = 0.35f),
                                        Color(0xFFEA580C).copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .scale(if (currentStreak > 0) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFFF97316),
                                            Color(0xFFDC2626)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "Flamme de Série",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SÉRIE QUOTIDIENNE",
                                color = Color(0xFFF97316),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (isPracticedToday) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "Validé",
                                        color = Color(0xFF22C55E),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (currentStreak > 0) "$currentStreak ${if (currentStreak > 1) "Jours" else "Jour"} d'affilée" else "Lancez votre série !",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Details Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        showDetailsDialog = true
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .testTag("streak_details_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Détails de la série",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPracticedToday) Color(0xFF22C55E).copy(alpha = 0.12f)
                        else Color(0xFFF59E0B).copy(alpha = 0.15f)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPracticedToday) Icons.Filled.CheckCircle else Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = if (isPracticedToday) Color(0xFF22C55E) else Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPracticedToday) {
                            "Flamme préservée pour aujourd'hui ! Revenez demain pour le jour ${currentStreak + 1}."
                        } else {
                            "Votre flamme est en jeu aujourd'hui ! Pratiquez pour la maintenir."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7-Day Interactive Calendar Strip
            Text(
                text = "SEMAINE EN COURS",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { day ->
                    WeekDayPill(
                        day = day,
                        onClick = {
                            selectedDayInfo = day
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Milestone Progress
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Prochain palier : ${nextMilestone.badgeIcon} ${nextMilestone.title}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$currentStreak / ${nextMilestone.targetDays}j",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF97316)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progressToNext },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFF97316),
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Row: Quick Practice or View Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        if (!isPracticedToday) {
                            viewModel.recordDailyPractice("Session d'anglais quotidienne")
                        } else {
                            showExpressChallenge = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("streak_practice_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPracticedToday) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF97316),
                        contentColor = if (isPracticedToday) MaterialTheme.colorScheme.onSurface else Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isPracticedToday) Icons.Filled.PlayArrow else Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPracticedToday) "S'entraîner à nouveau" else "Valider ma pratique",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        showDetailsDialog = true
                    },
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("streak_dashboard_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Historique", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Day Details Toast / Pop-up
    selectedDayInfo?.let { day ->
        AlertDialog(
            onDismissRequest = { selectedDayInfo = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (day.isPracticed) "🔥" else "📅")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(day.fullName)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Date : ${day.dateStr}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (day.isPracticed) {
                        Text(
                            text = "✅ Pratique complétée avec succès (${day.practiceCount} ${if (day.practiceCount > 1) "sessions" else "session"}).",
                            color = Color(0xFF22C55E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else if (day.isToday) {
                        Text(
                            text = "⏳ Pas encore pratiqué aujourd'hui. Faites un exercice ou prononcez un mot pour allumer la flamme !",
                            color = Color(0xFFF59E0B),
                            fontSize = 14.sp
                        )
                    } else if (day.isPast) {
                        Text(
                            text = "Aucune session enregistrée ce jour-là.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = "Journée à venir dans votre calendrier d'apprentissage.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDayInfo = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Detailed Streak History Dialog (Room database records)
    if (showDetailsDialog) {
        StreakDetailDialog(
            viewModel = viewModel,
            onDismiss = { showDetailsDialog = false }
        )
    }

    // Express Challenge Dialog
    if (showExpressChallenge) {
        ExpressChallengeDialog(
            viewModel = viewModel,
            onDismiss = { showExpressChallenge = false }
        )
    }
}

@Composable
fun WeekDayPill(
    day: WeekDayInfo,
    onClick: () -> Unit
) {
    val isHighlighted = day.isPracticed
    val isToday = day.isToday

    val containerColor = when {
        isHighlighted -> Color(0xFFF97316)
        isToday -> Color(0xFFF59E0B).copy(alpha = 0.2f)
        day.isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
    }

    val contentColor = when {
        isHighlighted -> Color.White
        isToday -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    val borderColor = when {
        isToday -> Color(0xFFF59E0B)
        isHighlighted -> Color(0xFFEA580C)
        else -> Color.Transparent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Text(
            text = day.dayLabel,
            fontSize = 11.sp,
            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isToday) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(containerColor)
                .border(if (isToday) 1.5.dp else 1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isHighlighted) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = "Pratiqué",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else if (isToday) {
                Text(
                    text = "${day.dayNumber}",
                    color = Color(0xFFF59E0B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "${day.dayNumber}",
                    color = contentColor,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun StreakDetailDialog(
    viewModel: TutorViewModel,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val userProgress by viewModel.userProgress.collectAsStateWithLifecycle()
    val practiceLogs by viewModel.dailyPracticeLogs.collectAsStateWithLifecycle()

    val currentStreak = userProgress?.currentStreak ?: 0
    val longestStreak = userProgress?.longestStreak ?: 0
    val totalPracticeDays = userProgress?.totalPracticeDays ?: practiceLogs.size
    val freezeCount = userProgress?.streakFreezeCount ?: 1

    var showSimulateOptions by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("streak_detail_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Tableau de Bord de Série",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Persisté dans Room Database",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Stats Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatBox(
                                label = "Série Actuelle",
                                value = "$currentStreak j",
                                icon = Icons.Filled.LocalFireDepartment,
                                color = Color(0xFFF97316),
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                label = "Record Personnel",
                                value = "$longestStreak j",
                                icon = Icons.Filled.EmojiEvents,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                label = "Total Jours",
                                value = "$totalPracticeDays j",
                                icon = Icons.Filled.CalendarMonth,
                                color = Color(0xFF3B82F6),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Shield & Freeze Feature
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bouclier de Série : $freezeCount actif",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Protège automatiquement votre série en cas d'oubli d'une journée.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Milestones List
                    item {
                        Text(
                            text = "PALIERS DE FLAMME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF97316),
                            letterSpacing = 1.sp
                        )
                    }

                    items(streakMilestones) { milestone ->
                        val isUnlocked = currentStreak >= milestone.targetDays
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUnlocked) Color(0xFFF97316).copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isUnlocked) Color(0xFFF97316).copy(alpha = 0.4f) else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(milestone.badgeIcon, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${milestone.targetDays} Jours — ${milestone.title}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isUnlocked) Color(0xFFF97316) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = milestone.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isUnlocked) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Débloqué",
                                        tint = Color(0xFF22C55E),
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${milestone.targetDays - currentStreak}j restants",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Room Database Practice Sessions Log
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HISTORIQUE DES SESSIONS (${practiceLogs.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    if (practiceLogs.isEmpty()) {
                        item {
                            Text(
                                text = "Aucune session enregistrée pour le moment. Effectuez votre premier exercice !",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(practiceLogs) { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = log.date,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = log.activitiesSummary,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${log.practiceCount} ${if (log.practiceCount > 1) "sessions" else "session"}",
                                            color = Color(0xFF22C55E),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Developer / Demonstration Simulation Tools
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showSimulateOptions = !showSimulateOptions },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.FitnessCenter,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Outils de Test & Simulation",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = if (showSimulateOptions) "Masquer" else "Afficher",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (showSimulateOptions) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Permet de tester instantanément la persistance Room pour 3 ou 7 jours consécutifs :",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.simulateConsecutiveStreak(3)
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Série 3j", fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.simulateConsecutiveStreak(7)
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Série 7j", fontSize = 11.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.resetStreakData()
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Reset", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Fermer")
                }
            }
        }
    }
}

@Composable
fun StatBox(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color)
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ExpressChallengeDialog(
    viewModel: TutorViewModel,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var completed by remember { mutableStateOf(false) }

    val samplePhrases = listOf(
        "I am practicing English every day to improve my fluency.",
        "Consistency is the key to mastering any new language.",
        "Every small effort today makes a big difference tomorrow."
    )
    val chosenPhrase = remember { samplePhrases.random() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⚡", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Défi Express de Série",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lisez cette phrase à voix haute pour valider votre session :",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF97316).copy(alpha = 0.12f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\"$chosenPhrase\"",
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!completed) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.recordDailyPractice("Défi Express : Lecture à voix haute")
                            completed = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
                    ) {
                        Text("C'est fait ! Valider la session 🔥", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "🎉 Bravo ! Session validée et enregistrée dans la base de données !",
                        color = Color(0xFF22C55E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Continuer")
                    }
                }
            }
        }
    }
}
