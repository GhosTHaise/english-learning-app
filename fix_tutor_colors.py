import re

with open("app/src/main/java/com/example/ui/screens/TutorScreen.kt", "r") as f:
    content = f.read()

color_vars = """
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
"""

content = content.replace("val viewModel: TutorViewModel = viewModel()", 
"val viewModel: TutorViewModel = viewModel()\n" + color_vars)
# Oh wait, TutorScreen doesn't instantiate viewModel like that.
# Let's just put it after `val error by viewModel.error.collectAsStateWithLifecycle()`

content = content.replace("val error by viewModel.error.collectAsStateWithLifecycle()", 
"val error by viewModel.error.collectAsStateWithLifecycle()\n" + color_vars)


# Replace Color.White inside TutorScreen
content = content.replace("Color.White.copy(alpha = 0.6f)", "subTextColor")
content = content.replace("Color.White.copy(alpha = 0.7f)", "subTextColor")
content = content.replace("Color.White.copy(alpha = 0.5f)", "subTextColor.copy(alpha = 0.7f)")
content = content.replace("Color.White.copy(alpha = 0.8f)", "textColor.copy(alpha = 0.9f)")
content = content.replace("Color.White.copy(alpha = 0.9f)", "textColor")
content = content.replace("Color.White", "textColor")

content = content.replace("textColor.copy(alpha = 0.08f)", "MaterialTheme.colorScheme.surfaceVariant")
content = content.replace("textColor.copy(alpha = 0.12f)", "MaterialTheme.colorScheme.outlineVariant")
content = content.replace("textColor.copy(alpha = 0.15f)", "MaterialTheme.colorScheme.outlineVariant")
content = content.replace("textColor.copy(alpha = 0.1f)", "MaterialTheme.colorScheme.surfaceVariant")
content = content.replace("textColor.copy(alpha = 0.2f)", "MaterialTheme.colorScheme.outline")
content = content.replace("textColor.copy(alpha = 0.05f)", "MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)")


# Let's fix ChatBubble.
# ChatBubble is a separate Composable at the bottom of TutorScreen.kt
# We need to add color_vars to ChatBubble as well.
content = content.replace("fun ChatBubble(\n    msg: ChatMessage,\n    onReplay: (String) -> Unit\n) {",
"fun ChatBubble(\n    msg: ChatMessage,\n    onReplay: (String) -> Unit\n) {\n" + color_vars.replace("val textColor", "val textColor1").replace("textColor", "textColor1"))
# it's messy. Let's just use `MaterialTheme.colorScheme.onSurface` directly.

content = content.replace("val textColor1", "val textColor")

with open("app/src/main/java/com/example/ui/screens/TutorScreen.kt", "w") as f:
    f.write(content)
