from pathlib import Path
p=Path('app/src/main/java/br/com/rb8digital/rbcinecam/ui/CameraScreen09.kt')
s=p.read_text(encoding='utf-8')
old='''                Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(start = 72.dp, end = 82.dp)) {'''
new='''                Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Color.Black.copy(alpha = .93f)).padding(start = 72.dp, end = 82.dp)) {'''
assert old in s, 'top bar anchor missing'
s=s.replace(old,new,1)
s=s.replace('var scopeMode by remember { mutableStateOf(ScopeMode09.RGB) }','var scopeMode by remember { mutableStateOf(ScopeMode09.OFF) }',1)
s=s.replace('Modifier.width(62.dp).fillMaxHeight().background(Color.Black.copy(alpha = .30f)).padding(4.dp)','Modifier.width(46.dp).fillMaxHeight().background(Color.Black.copy(alpha = .25f)).padding(3.dp)',1)
s=s.replace('Box(Modifier.align(Alignment.CenterEnd).padding(end = 64.dp))','Box(Modifier.align(Alignment.CenterEnd).padding(end = 48.dp))',1)
s=s.replace('padding(start = 74.dp, end = 146.dp, bottom = 4.dp)','padding(start = 64.dp, end = 124.dp, bottom = 4.dp)',1)
s=s.replace('Modifier.padding(horizontal=2.dp).clip(RoundedCornerShape(7.dp))','Modifier.padding(horizontal=3.dp).clip(RoundedCornerShape(9.dp))',1)
s=s.replace('Modifier.padding(horizontal=7.dp, vertical=4.dp)','Modifier.padding(horizontal=8.dp, vertical=7.dp)',1)
p.write_text(s,encoding='utf-8')
b=Path('app/build.gradle.kts'); t=b.read_text(); assert 'versionName = "0.16.0-alpha"' in t
t=t.replace('versionCode = 17','versionCode = 18').replace('versionName = "0.16.0-alpha"','versionName = "0.17.0-alpha"');b.write_text(t)
print('RB CineCam 0.17 HUD polish applied')
