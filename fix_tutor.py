import re

with open('app/src/main/java/com/example/ui/screens/TutorScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('@Composable\n@Composable\nfun ChatBubble(', '@Composable\nfun ChatBubble(')
content = content.replace('@Composable\n@Composable', '@Composable')

with open('app/src/main/java/com/example/ui/screens/TutorScreen.kt', 'w') as f:
    f.write(content)
