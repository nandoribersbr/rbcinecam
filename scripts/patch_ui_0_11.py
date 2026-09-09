from pathlib import Path

p = Path('app/src/main/java/br/com/rb8digital/rbcinecam/ui/CameraScreen.kt')
s = p.read_text(encoding='utf-8')

old = '''            else -> Row(
                Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                LeftRail('''
new = '''            else -> Column(
                Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // HUD is a real fixed strip ABOVE the camera body. The preview can never cover it.
                TopHud(mode, recording, status, elapsedSeconds, freeSpace, audioLevel)
                Row(Modifier.weight(1f).fillMaxWidth()) {
                LeftRail('''
if old not in s:
    raise SystemExit('camera root layout marker not found')
s = s.replace(old, new, 1)

old = '''                Column(Modifier.weight(1f).fillMaxHeight()) {
                    TopHud(mode, recording, status, elapsedSeconds, freeSpace, audioLevel)
                    Box(Modifier.weight(1f).fillMaxWidth()) {'''
new = '''                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {'''
if old not in s:
    raise SystemExit('nested HUD marker not found')
s = s.replace(old, new, 1)

# Close the new camera-body Row before closing the outer Column branch.
marker = '''                }
            }
        }
    }
}

@Composable
private fun AudioMeterPanel'''
replacement = '''                }
                }
            }
        }
    }
}

@Composable
private fun AudioMeterPanel'''
if marker not in s:
    raise SystemExit('camera body closing marker not found')
s = s.replace(marker, replacement, 1)

# Make the HUD a compact, fixed-height technical strip and keep every item on one line.
old = '''    Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {'''
new = '''    Row(Modifier.fillMaxWidth().height(44.dp).background(Color(0xFF090B0E)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {'''
if old not in s:
    raise SystemExit('TopHud row marker not found')
s = s.replace(old, new, 1)

# Compact labels for narrow landscape screens; critical status remains at the right.
s = s.replace('Text("CineCam", fontWeight = FontWeight.Bold, fontSize = 18.sp)', 'Text("CineCam", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)')
s = s.replace('Text("RB ", color = Red, fontWeight = FontWeight.Black, fontSize = 18.sp)', 'Text("RB ", color = Red, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1)')
s = s.replace('"VÍDEO • FHD • 30 FPS • MP4"', '"FHD30 • 180° • MP4"')
s = s.replace('fontSize = 13.sp)', 'fontSize = 11.sp, maxLines = 1)', 1)
s = s.replace('Text("Livre $free", color = Color.LightGray, fontSize = 11.sp)', 'Text(free, color = Color.LightGray, fontSize = 10.sp, maxLines = 1)')

p.write_text(s, encoding='utf-8')

# Build identity is patched in CI so this APK is unmistakably newer than 0.10.1.
b = Path('app/build.gradle.kts')
g = b.read_text(encoding='utf-8')
g = g.replace('versionCode = 11', 'versionCode = 12')
g = g.replace('versionName = "0.10.1-alpha"', 'versionName = "0.11.0-alpha"')
b.write_text(g, encoding='utf-8')

print('RB CineCam 0.11 structural HUD patch applied')
