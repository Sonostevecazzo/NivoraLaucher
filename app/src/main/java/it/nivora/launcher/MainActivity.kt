package it.nivora.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class LaunchableApp(val label: String, val packageName: String, val icon: Drawable)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NivoraApp() }
    }
}

@Composable
private fun NivoraApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nivora_preferences", 0) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
    var query by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                .mapNotNull { info ->
                    try {
                        LaunchableApp(info.loadLabel(pm).toString(), info.activityInfo.packageName, info.loadIcon(pm))
                    } catch (_: Exception) { null }
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
        }
        loading = false
    }

    val visibleApps = apps.filter {
        it.label.contains(query.trim(), ignoreCase = true) &&
            (!favoritesOnly || favorites.contains(it.packageName))
    }.sortedWith(compareByDescending<LaunchableApp> { favorites.contains(it.packageName) }.thenBy { it.label.lowercase() })

    val bg = if (dark) Color(0xFF080A0B) else Color(0xFFF6F6F2)
    val fg = if (dark) Color(0xFFF2F2EE) else Color(0xFF191B19)
    val muted = if (dark) Color(0xFF9A9D9A) else Color(0xFF6D716D)

    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xFFB8E4C8), background = bg, surface = bg, onBackground = fg, onSurface = fg)
        else lightColorScheme(primary = Color(0xFF246B4B), background = bg, surface = bg, onBackground = fg, onSurface = fg)) {
        Surface(Modifier.fillMaxSize(), color = bg) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("NIVORA", color = muted, fontSize = 12.sp, letterSpacing = 4.sp)
                        Text("Le tue app.", color = fg, fontSize = 32.sp, fontWeight = FontWeight.Light)
                    }
                    IconButton(onClick = { dark = !dark; prefs.edit().putBoolean("dark", dark).apply() }) {
                        Text(if (dark) "☼" else "☾", color = fg, fontSize = 24.sp)
                    }
                    IconButton(onClick = { favoritesOnly = !favoritesOnly }) {
                        Text(if (favoritesOnly) "★" else "☆", color = fg, fontSize = 23.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cerca un'app…", color = muted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cerca") },
                    singleLine = true, shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.height(16.dp))
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    visibleApps.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (favoritesOnly) "Nessun preferito.\nTieni premuta un'app per aggiungerla." else "Nessuna app trovata.", color = muted)
                    }
                    else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 28.dp)) {
                        items(visibleApps, key = { it.packageName }) { app ->
                            AppRow(app, favorites.contains(app.packageName), fg, muted,
                                onClick = {
                                    val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                    if (launch != null) {
                                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        try { context.startActivity(launch) } catch (_: Exception) {
                                            Toast.makeText(context, "Impossibile aprire l'app", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onLongClick = {
                                    val next = favorites.toMutableSet()
                                    if (!next.add(app.packageName)) next.remove(app.packageName)
                                    favorites = next.toSet()
                                    prefs.edit().putStringSet("favorites", next).apply()
                                    Toast.makeText(context, if (next.contains(app.packageName)) "Aggiunta ai preferiti" else "Rimossa dai preferiti", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
                Text("Tieni premuta un'app per aggiungerla ai preferiti", color = muted, fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 12.dp).align(Alignment.CenterHorizontally))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(app: LaunchableApp, favorite: Boolean, fg: Color, muted: Color, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
            update = { it.setImageDrawable(app.icon) }, modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(18.dp))
        Text(app.label, color = fg, fontSize = 19.sp, modifier = Modifier.weight(1f))
        if (favorite) Text("★", color = muted, fontSize = 15.sp)
    }
}
