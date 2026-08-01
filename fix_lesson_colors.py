import re

with open("app/src/main/java/com/example/ui/screens/LessonScreen.kt", "r") as f:
    content = f.read()

# First, insert semantic color variables at the beginning of LessonScreen and QuizWidget
# LessonScreen
color_vars = """
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val errorColor = MaterialTheme.colorScheme.error
    val successColor = androidx.compose.ui.graphics.Color(0xFF22C55E)
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
"""

# add it after `val uiState by viewModel.lessonUiState.collectAsStateWithLifecycle()`
content = content.replace("val uiState by viewModel.lessonUiState.collectAsStateWithLifecycle()", 
"val uiState by viewModel.lessonUiState.collectAsStateWithLifecycle()\n" + color_vars)

# Do the same for QuizWidget
content = content.replace("fun QuizWidget(", "fun QuizWidget(\n    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,\n    subTextColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,\n    primaryColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,\n    successColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF22C55E),\n    errorColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.error,\n")

# Now replace specific hardcoded colors
# Colors that act as "white text"
content = content.replace("Color.White.copy(alpha = 0.6f)", "subTextColor")
content = content.replace("Color.White.copy(alpha = 0.7f)", "subTextColor")
content = content.replace("Color.White.copy(alpha = 0.5f)", "subTextColor.copy(alpha = 0.7f)")
content = content.replace("Color.White.copy(alpha = 0.8f)", "textColor.copy(alpha = 0.9f)")
content = content.replace("Color.White.copy(alpha = 0.9f)", "textColor")
content = content.replace("Color.White", "textColor")

# Background whites / lines
content = content.replace("textColor.copy(alpha = 0.08f)", "MaterialTheme.colorScheme.surfaceVariant")
content = content.replace("textColor.copy(alpha = 0.12f)", "MaterialTheme.colorScheme.outlineVariant")
content = content.replace("textColor.copy(alpha = 0.1f)", "MaterialTheme.colorScheme.surfaceVariant")
content = content.replace("textColor.copy(alpha = 0.2f)", "MaterialTheme.colorScheme.outline")
content = content.replace("textColor.copy(alpha = 0.05f)", "MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)")
content = content.replace("textColor.copy(alpha = 0.4f)", "MaterialTheme.colorScheme.outline")


# Hex colors:
content = content.replace("Color(0xFFA5B4FC)", "primaryColor")
content = content.replace("Color(0xFF6366F1)", "primaryColor")
content = content.replace("Color(0xFF2563EB)", "primaryColor")
content = content.replace("Color(0xFF93C5FD)", "primaryColor")
content = content.replace("Color(0xFF312E81)", "primaryContainer")
content = content.replace("Color(0xFF1E1B4B)", "primaryContainer")
content = content.replace("Color(0xFF818CF8)", "primaryColor")
content = content.replace("Color(0xFFFBBF24)", "androidx.compose.ui.graphics.Color(0xFFD97706)")
content = content.replace("Color(0xFFEF4444)", "errorColor")
content = content.replace("Color(0xFF4ADE80)", "successColor")
content = content.replace("Color(0xFF16A34A)", "successColor")
content = content.replace("Color(0xFFDC2626)", "errorColor")
content = content.replace("Color(0xFFF87171)", "errorColor")
content = content.replace("Color(0xFFD97706)", "androidx.compose.ui.graphics.Color(0xFFD97706)")

# Fix button colors where textColor shouldn't be used (needs onPrimary)
content = content.replace("textColor,", "MaterialTheme.colorScheme.onPrimary,") # Rough regex, better to use regex
content = re.sub(r'Button\([\s\S]*?Text\([^)]*textColor[^)]*\)', lambda m: m.group(0).replace("textColor", "MaterialTheme.colorScheme.onPrimary"), content)

with open("app/src/main/java/com/example/ui/screens/LessonScreen.kt", "w") as f:
    f.write(content)

print("Done")
