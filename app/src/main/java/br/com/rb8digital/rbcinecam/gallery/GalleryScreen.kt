package br.com.rb8digital.rbcinecam.gallery

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun GalleryScreen(onBack: () -> Unit, onOpen: (Uri) -> Unit) {
    val context = LocalContext.current
    val repo = remember { MediaRepository(context) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    LaunchedEffect(Unit) { videos = repo.videos() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") }
            Text("RB CineCam • Galeria", style = MaterialTheme.typography.titleLarge)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(videos) { item ->
                ListItem(
                    headlineContent = { Text(item.name) },
                    supportingContent = { Text("${item.durationMs / 1000}s") },
                    modifier = Modifier.clickable { onOpen(item.uri) }
                )
                HorizontalDivider()
            }
        }
    }
}
