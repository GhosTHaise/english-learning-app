import re

with open('app/src/main/java/com/example/ui/screens/TutorScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("tts = TextToSpeech(context) {", "tts = TextToSpeech(context) { status ->")
content = content.replace("ActivityResultContracts.RequestPermission()\n    ) {", "ActivityResultContracts.RequestPermission()\n    ) { isGranted ->")
content = content.replace("viewModel.sendMessageToAI(text) {", "viewModel.sendMessageToAI(text) { response ->")
content = content.replace("items(TutorMode.values()) {", "items(TutorMode.values()) { mode ->")
content = content.replace("items(chatHistory) {", "items(chatHistory) { msg ->")

with open('app/src/main/java/com/example/ui/screens/TutorScreen.kt', 'w') as f:
    f.write(content)

