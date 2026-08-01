import re

with open('app/src/main/java/com/example/ui/screens/TutorScreen.kt', 'r') as f:
    content = f.read()

# Fix the concatenated first line by inserting newlines before "import " and "package "
first_line, rest = content.split('\n', 1)[0], content.split('\n', 1)[1:]

# Replace "import " with "\nimport " and "package " with "package \n"
fixed_first_line = re.sub(r'(import [a-zA-Z0-9.*]+)', r'\n\1\n', first_line)
fixed_first_line = fixed_first_line.replace('package com.example.ui.screens', 'package com.example.ui.screens\n')
# Clean up multiple newlines
fixed_first_line = re.sub(r'\n+', '\n', fixed_first_line)

content = fixed_first_line + '\n' + '\n'.join(rest)

with open('app/src/main/java/com/example/ui/screens/TutorScreen.kt', 'w') as f:
    f.write(content)
