import re

with open("app/src/main/java/com/example/ui/screens/LessonScreen.kt", "r") as f:
    content = f.read()

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

content = content.replace("val lessonState by viewModel.lessonState.collectAsStateWithLifecycle()", 
"val lessonState by viewModel.lessonState.collectAsStateWithLifecycle()\n" + color_vars)

with open("app/src/main/java/com/example/ui/screens/LessonScreen.kt", "w") as f:
    f.write(content)

