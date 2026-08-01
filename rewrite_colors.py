import re

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # We will safely replace Color.White.copy(...) first
    content = re.sub(r'Color\.White\.copy\(alpha = ([0-9.]+)f\)', r'androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = \1f)', content)
    
    # Then standalone Color.White
    content = content.replace('Color.White', 'androidx.compose.material3.MaterialTheme.colorScheme.onSurface')
    
    # Hex colors
    content = content.replace('Color(0xFFA5B4FC)', 'androidx.compose.material3.MaterialTheme.colorScheme.primary')
    content = content.replace('Color(0xFF6366F1)', 'androidx.compose.material3.MaterialTheme.colorScheme.primary')
    content = content.replace('Color(0xFF2563EB)', 'androidx.compose.material3.MaterialTheme.colorScheme.primary')
    content = content.replace('Color(0xFF93C5FD)', 'androidx.compose.material3.MaterialTheme.colorScheme.primary')
    content = content.replace('Color(0xFF818CF8)', 'androidx.compose.material3.MaterialTheme.colorScheme.primary')
    
    # For dark hex colors, use primaryContainer
    content = content.replace('Color(0xFF312E81)', 'androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer')
    content = content.replace('Color(0xFF1E1B4B)', 'androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer')
    
    # Success/Error colors
    content = content.replace('Color(0xFFEF4444)', 'androidx.compose.material3.MaterialTheme.colorScheme.error')
    content = content.replace('Color(0xFFDC2626)', 'androidx.compose.material3.MaterialTheme.colorScheme.error')
    content = content.replace('Color(0xFFF87171)', 'androidx.compose.material3.MaterialTheme.colorScheme.error')
    
    content = content.replace('Color(0xFF4ADE80)', 'androidx.compose.ui.graphics.Color(0xFF22C55E)')
    content = content.replace('Color(0xFF16A34A)', 'androidx.compose.ui.graphics.Color(0xFF22C55E)')
    
    content = content.replace('Color(0xFFFBBF24)', 'androidx.compose.ui.graphics.Color(0xFFD97706)')
    
    # Fix buttons where text color should be onPrimary
    # We will just replace it where containerColor is primary
    
    with open(filepath, 'w') as f:
        f.write(content)

process_file('app/src/main/java/com/example/ui/screens/LessonScreen.kt')

# TutorScreen.kt has already some messed up state? No, I deleted the bad lines.
with open('app/src/main/java/com/example/ui/screens/TutorScreen.kt', 'r') as f:
    tutor_content = f.read()

tutor_content = re.sub(r'Color\.White\.copy\(alpha = ([0-9.]+)f\)', r'androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = \1f)', tutor_content)
tutor_content = tutor_content.replace('Color.White', 'androidx.compose.material3.MaterialTheme.colorScheme.onSurface')
# Fix Hex colors in TutorScreen too
tutor_content = tutor_content.replace('Color(0xFFA5B4FC)', 'androidx.compose.material3.MaterialTheme.colorScheme.primary')
tutor_content = tutor_content.replace('Color(0xFF6366F1)', 'androidx.compose.material3.MaterialTheme.colorScheme.primary')

with open('app/src/main/java/com/example/ui/screens/TutorScreen.kt', 'w') as f:
    f.write(tutor_content)

print("Done")
