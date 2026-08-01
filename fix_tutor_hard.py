import re

with open('app/src/main/java/com/example/ui/screens/TutorScreen.kt', 'r') as f:
    content = f.read()

# First line is a huge mess. Let's find the start of the first @Composable.
parts = content.split('@Composable', 1)
if len(parts) == 2:
    imports_mess = parts[0]
    rest = '@Composable' + parts[1]
    
    # Extract all imports using regex
    packages = re.findall(r'package [a-zA-Z0-9_.]+', imports_mess)
    imports = re.findall(r'import [a-zA-Z0-9_.*]+', imports_mess)
    
    # unique imports
    imports = list(set(imports))
    
    clean_header = packages[0] + '\n\n' + '\n'.join(imports) + '\n\n'
    
    content = clean_header + rest
else:
    print("Failed to find @Composable")

with open('app/src/main/java/com/example/ui/screens/TutorScreen.kt', 'w') as f:
    f.write(content)

