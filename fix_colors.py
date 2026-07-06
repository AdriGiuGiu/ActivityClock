import os

def fix_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements.items():
        content = content.replace(old, new)
        
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

dashboard_replacements = {
    "color.value.toLong().toInt()": "color.toArgb()",
    "selectedColor.value.toLong().toInt()": "selectedColor.toArgb()",
    "import androidx.compose.ui.graphics.Color": "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.toArgb",
    "Color.White": "MaterialTheme.colorScheme.onSurface",
    "Color.Gray": "MaterialTheme.colorScheme.onSurfaceVariant",
    "Color.Black": "MaterialTheme.colorScheme.onPrimary"
}

analytics_replacements = {
    "Color.White": "MaterialTheme.colorScheme.onSurface",
    "Color(0xFF222228)": "MaterialTheme.colorScheme.surfaceVariant",
    "Color(0xFF1E1E24)": "MaterialTheme.colorScheme.surfaceVariant"
}

fix_file(r"app\src\main\java\com\example\activityclock\ui\components\DashboardTabs.kt", dashboard_replacements)
fix_file(r"app\src\main\java\com\example\activityclock\ui\components\AnalyticsCharts.kt", analytics_replacements)
print("Files fixed successfully.")
