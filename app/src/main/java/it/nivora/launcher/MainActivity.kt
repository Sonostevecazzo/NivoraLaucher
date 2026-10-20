package it.nivora.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class LaunchableApp(val label: String, val packageName: String, val icon: Drawable)

private data class LauncherThemePreset(
    val name: String, val background: Color, val backgroundBottom: Color,
    val foreground: Color, val muted: Color, val panel: Color, val accent: Color, val dark: Boolean
)

// Palette ideas inspired by named appearance packs, without copying another app's artwork.
private val launcherThemePresets = listOf(
    LauncherThemePreset("Glass Mono", Color(0xFF0B0D0F), Color(0xFF171A1E), Color(0xFFF1F2F2), Color(0xFFA2A7AC), Color(0xFF292D32), Color(0xFFE5E7E9), true),
    LauncherThemePreset("Obsidian", Color(0xFF050607), Color(0xFF101214), Color(0xFFF2F3F4), Color(0xFF8D9399), Color(0xFF1C2024), Color(0xFFC9CDD1), true),
    LauncherThemePreset("Frosted Silver", Color(0xFFDDE1E5), Color(0xFFF7F8F9), Color(0xFF171A1D), Color(0xFF626970), Color(0xFFFFFFFF), Color(0xFF454B51), false),
    LauncherThemePreset("Graphite", Color(0xFF1B1E22), Color(0xFF292D32), Color(0xFFF2F3F4), Color(0xFFB2B7BC), Color(0xFF363B41), Color(0xFFD4D8DC), true),
    LauncherThemePreset("Midnight", Color(0xFF101722), Color(0xFF1B2A3C), Color(0xFFF1F5FA), Color(0xFFA9B7C7), Color(0xFF29394B), Color(0xFFC0D9F4), true),
    LauncherThemePreset("Sage", Color(0xFF151B17), Color(0xFF25332A), Color(0xFFF0F4F0), Color(0xFFB0BDB3), Color(0xFF304036), Color(0xFFC5D9C8), true),
    LauncherThemePreset("Bronze", Color(0xFF1C1714), Color(0xFF30241D), Color(0xFFF5EEE7), Color(0xFFC4B2A3), Color(0xFF3A2D25), Color(0xFFE2C0A1), true),
    LauncherThemePreset("Pearl", Color(0xFFF0EEE9), Color(0xFFE2DFD8), Color(0xFF1C1C1A), Color(0xFF6D6B65), Color(0xFFFFFFFF), Color(0xFF55524B), false),
    LauncherThemePreset("Personalizzato", Color(0xFF090A0A), Color(0xFF090A0A), Color(0xFFF4F4F0), Color(0xFFB8BEBA), Color(0xFF202424), Color(0xFFD71920), true)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NivoraApp() }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NivoraApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nivora_preferences", 0) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
    var grid by remember { mutableStateOf(prefs.getBoolean("grid", true)) }
    var showLabels by remember { mutableStateOf(prefs.getBoolean("labels", true)) }
    var compact by remember { mutableStateOf(prefs.getBoolean("compact", false)) }
    var columns by remember { mutableIntStateOf(prefs.getInt("columns", 4).coerceIn(3, 5)) }
    var iconSize by remember { mutableIntStateOf(prefs.getInt("iconSize", 50).coerceIn(40, 64)) }
    var fontChoice by remember { mutableStateOf(prefs.getString("fontChoice", "System") ?: "System") }
    var wallpaper by remember { mutableStateOf(prefs.getString("wallpaper", "OLED") ?: "OLED") }
    var accentChoice by remember { mutableStateOf(prefs.getString("accentChoice", "Red") ?: "Red") }
    var themePreset by remember { mutableStateOf(prefs.getString("themePreset", "Glass Mono") ?: "Glass Mono") }
    var glassEffect by remember { mutableStateOf(prefs.getBoolean("glassEffect", true)) }
    var showBrand by remember { mutableStateOf(prefs.getBoolean("showBrand", true)) }
    var query by remember { mutableStateOf("") }
    var showAllApps by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf(prefs.getString("sortBy", "Name") ?: "Name") }
    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var clock by remember { mutableStateOf(LocalDateTime.now()) }

    suspend fun loadApps(): List<LaunchableApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                try { LaunchableApp(info.loadLabel(pm).toString(), info.activityInfo.packageName, info.loadIcon(pm)) }
                catch (_: Exception) { null }
            }.distinctBy { it.packageName }
    }

    LaunchedEffect(Unit) {
        apps = loadApps().sortedBy { it.label.lowercase() }
        loading = false
    }
    LaunchedEffect(Unit) {
        while (true) { clock = LocalDateTime.now(); delay(30_000) }
    }

    val filteredApps = apps.filter {
        (it.label.contains(query.trim(), true) || it.packageName.contains(query.trim(), true)) &&
            (!favoritesOnly || favorites.contains(it.packageName))
    }
    val visibleApps = when (sortBy) {
        "Recenti" -> filteredApps.sortedWith(compareByDescending<LaunchableApp> { favorites.contains(it.packageName) }.thenByDescending { it.label.lowercase() })
        "Preferiti" -> filteredApps.sortedWith(compareByDescending<LaunchableApp> { favorites.contains(it.packageName) }.thenBy { it.label.lowercase() })
        else -> filteredApps.sortedBy { it.label.lowercase() }
    }
    val activePreset = launcherThemePresets.firstOrNull { it.name == themePreset && it.name != "Personalizzato" }
    val bg = activePreset?.background ?: when (wallpaper) {
        "Graphite" -> Color(0xFF202326)
        "Warm" -> Color(0xFFF0E9DE)
        "Paper" -> Color(0xFFF7F7F2)
        else -> if (dark) Color(0xFF090A0A) else Color(0xFFF3F3EF)
    }
    val fg = activePreset?.foreground ?: if (wallpaper == "Warm" || wallpaper == "Paper" || (!dark && wallpaper == "OLED")) Color(0xFF111212) else Color(0xFFF4F4F0)
    val muted = activePreset?.muted ?: if (fg == Color(0xFF111212)) Color(0xFF555955) else Color(0xFFB8BEBA)
    val panel = activePreset?.panel ?: if (fg == Color(0xFF111212)) Color(0xFFE2DED5) else Color(0xFF202424)
    val accent = activePreset?.accent ?: when (accentChoice) { "Blue" -> Color(0xFF4B9EFF); "Green" -> Color(0xFF68D391); "Amber" -> Color(0xFFFFB547); else -> Color(0xFFD71920) }
    val themeIsDark = activePreset?.dark ?: (fg != Color(0xFF111212))
    val selectedFont = when (fontChoice) {
        "Mono iOS", "Mono" -> FontFamily.Monospace
        "Serif iOS", "Serif" -> FontFamily.Serif
        "Cursive" -> FontFamily.Cursive
        "Rounded iOS", "Rounded" -> FontFamily(Typeface.create("sans-serif-rounded", Typeface.NORMAL))
        "Condensed" -> FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
        "iOS Style" -> FontFamily(Typeface.create("sans-serif", Typeface.NORMAL))
        "iOS Light" -> FontFamily(Typeface.create("sans-serif-light", Typeface.NORMAL))
        "iOS Medium" -> FontFamily(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        else -> FontFamily.Default
    }
    val customTypography = Typography().let { base -> base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = selectedFont), displayMedium = base.displayMedium.copy(fontFamily = selectedFont),
        displaySmall = base.displaySmall.copy(fontFamily = selectedFont), headlineLarge = base.headlineLarge.copy(fontFamily = selectedFont),
        headlineMedium = base.headlineMedium.copy(fontFamily = selectedFont), headlineSmall = base.headlineSmall.copy(fontFamily = selectedFont),
        titleLarge = base.titleLarge.copy(fontFamily = selectedFont), titleMedium = base.titleMedium.copy(fontFamily = selectedFont),
        titleSmall = base.titleSmall.copy(fontFamily = selectedFont), bodyLarge = base.bodyLarge.copy(fontFamily = selectedFont),
        bodyMedium = base.bodyMedium.copy(fontFamily = selectedFont), bodySmall = base.bodySmall.copy(fontFamily = selectedFont),
        labelLarge = base.labelLarge.copy(fontFamily = selectedFont), labelMedium = base.labelMedium.copy(fontFamily = selectedFont),
        labelSmall = base.labelSmall.copy(fontFamily = selectedFont)
    ) }

    MaterialTheme(colorScheme = if (themeIsDark) darkColorScheme(primary = accent, background = bg, surface = panel, onBackground = fg, onSurface = fg)
        else lightColorScheme(primary = accent, background = bg, surface = panel, onBackground = fg, onSurface = fg), typography = customTypography) {
        Surface(Modifier.fillMaxSize(), color = bg) {
            val backdrop = activePreset?.let { preset ->
                Brush.verticalGradient(listOf(preset.background, preset.backgroundBottom))
            } ?: when (wallpaper) {
                "Gradient" -> Brush.verticalGradient(listOf(if (dark) Color(0xFF242833) else Color(0xFFE5E9F2), bg, if (dark) Color(0xFF090A0A) else Color(0xFFF3F3EF)))
                "Sunset" -> Brush.verticalGradient(listOf(Color(0xFF4B253F), Color(0xFF1A202C), Color(0xFF090A0A)))
                else -> Brush.verticalGradient(listOf(bg, bg))
            }
            Column(Modifier.fillMaxSize().background(backdrop).statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (showBrand) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
                            Text("N I V O R A", color = fg, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Impostazioni", tint = fg)
                    }
                }

                if (!showAllApps) {
                    Spacer(Modifier.height(28.dp))
                    Text(clock.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ITALIAN)).uppercase(Locale.ITALIAN),
                        color = muted, fontSize = 11.sp, letterSpacing = 2.sp)
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp, bottom = 22.dp)) {
                        Text(clock.format(DateTimeFormatter.ofPattern("HH:mm")), color = fg, fontSize = 66.sp,
                            fontWeight = if (fontChoice == "iOS Style" || fontChoice == "Rounded iOS") FontWeight.Normal else FontWeight.Light,
                            fontFamily = if (fontChoice == "Mono iOS") FontFamily.Monospace else selectedFont,
                            letterSpacing = if (fontChoice == "iOS Style") (-2).sp else (-3).sp, lineHeight = 70.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("●", color = accent, fontSize = 9.sp, modifier = Modifier.padding(bottom = 15.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                        Text("PREFERITE", color = muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("${favorites.size} SALVATE", color = muted, fontSize = 10.sp, letterSpacing = 1.sp)
                    }
                } else {
                    Row(Modifier.padding(top = 20.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("APPLICAZIONI", color = fg, fontSize = 22.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Light)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { grid = !grid; prefs.edit().putBoolean("grid", grid).apply() }) {
                            Icon(if (grid) Icons.Default.ViewList else Icons.Default.GridView, contentDescription = "Cambia disposizione", tint = fg)
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth()
                        .height(if (showAllApps) 46.dp else 38.dp)
                        .glassSurface(panel, fg, glassEffect, RoundedCornerShape(18.dp))
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Cerca app", tint = muted, modifier = Modifier.size(if (showAllApps) 19.dp else 16.dp))
                    Spacer(Modifier.width(9.dp))
                    BasicTextField(value = query, onValueChange = { query = it; if (it.isNotBlank()) showAllApps = true },
                        singleLine = true, textStyle = TextStyle(color = fg, fontSize = if (showAllApps) 14.sp else 12.sp),
                        cursorBrush = SolidColor(accent), modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text(if (showAllApps) "Cerca un'app o un pacchetto" else "Cerca…", color = muted, fontSize = if (showAllApps) 14.sp else 12.sp)
                            inner()
                        })
                    if (query.isNotEmpty()) Text("×", color = muted, fontSize = 20.sp,
                        modifier = Modifier.combinedClickable(onClick = { query = "" }, onLongClick = { query = "" }).padding(horizontal = 4.dp))
                }
                Spacer(Modifier.height(10.dp))

                if (showAllApps) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !favoritesOnly, onClick = { favoritesOnly = false }, label = { Text("Tutte") })
                        FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = true }, label = { Text("Preferite") })
                        Spacer(Modifier.weight(1f))
                        var sortExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { sortExpanded = true }) { Text(sortBy.uppercase(), fontSize = 10.sp, color = accent) }
                            DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                                listOf("Nome", "Preferiti", "Recenti").forEach { choice ->
                                    DropdownMenuItem(text = { Text(choice) }, onClick = {
                                        sortBy = choice
                                        prefs.edit().putString("sortBy", choice).apply()
                                        sortExpanded = false
                                    })
                                }
                            }
                        }
                    }
                }

                if (loading) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                } else if (visibleApps.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(if (favoritesOnly) "NESSUNA APP PREFERITA" else "NESSUNA APP TROVATA", color = muted, fontSize = 11.sp, letterSpacing = 1.sp)
                        if (favoritesOnly) {
                            TextButton(onClick = { showAllApps = true; favoritesOnly = false }) { Text("MOSTRA TUTTE", color = accent) }
                        }
                    }
                } else if (grid) {
                    LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp)) {
                        gridItems(visibleApps, key = { it.packageName }) { app ->
                            AppTile(app, favorites.contains(app.packageName), showLabels, fg, accent, compact, iconSize, fontChoice,
                                onClick = { launchApp(context, app) },
                                onLongClick = {
                                    favorites = toggleFavorite(favorites, app.packageName, prefs)
                                    Toast.makeText(context, if (favorites.contains(app.packageName)) "Preferito aggiunto" else "Preferito rimosso", Toast.LENGTH_SHORT).show()
                                })
                        }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(visibleApps, key = { it.packageName }) { app ->
                            AppListRow(app, favorites.contains(app.packageName), fg, muted, accent,
                                onClick = { launchApp(context, app) },
                                onLongClick = {
                                    favorites = toggleFavorite(favorites, app.packageName, prefs)
                                    Toast.makeText(context, if (favorites.contains(app.packageName)) "Preferito aggiunto" else "Preferito rimosso", Toast.LENGTH_SHORT).show()
                                })
                        }
                    }
                }

                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BottomAction("HOME", !showAllApps, accent, fg, panel, glassEffect) { showAllApps = false; query = ""; favoritesOnly = true }
                    BottomAction("APP", showAllApps, accent, fg, panel, glassEffect) { showAllApps = true; query = ""; favoritesOnly = false }
                }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            containerColor = panel,
            title = { Text("PERSONALIZZA", color = fg, letterSpacing = 2.sp, fontSize = 15.sp) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).heightIn(max = 430.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThemePresetChoice("Tema preimpostato", themePreset, launcherThemePresets) {
                        themePreset = it
                        prefs.edit().putString("themePreset", it).apply()
                    }
                    SettingSwitch("Effetto vetro", glassEffect) {
                        glassEffect = it
                        prefs.edit().putBoolean("glassEffect", it).apply()
                    }
                    SettingSwitch("Mostra logo Nivora", showBrand) {
                        showBrand = it
                        prefs.edit().putBoolean("showBrand", it).apply()
                    }
                    SettingSwitch("Griglia di icone", grid) { grid = it; prefs.edit().putBoolean("grid", it).apply() }
                    SettingSwitch("Mostra nomi app", showLabels) { showLabels = it; prefs.edit().putBoolean("labels", it).apply() }
                    SettingSwitch("Griglia compatta", compact) { compact = it; prefs.edit().putBoolean("compact", it).apply() }
                    SettingChoice("Colonne", "$columns", "3", "4", "5") { columns = it.toInt(); prefs.edit().putInt("columns", columns).apply() }
                    SettingChoice("Dimensione icone", "$iconSize dp", "40", "50", "60") { iconSize = it.toInt(); prefs.edit().putInt("iconSize", it.toInt()).apply() }
                    SettingChoice("Carattere", fontChoice, "System", "iOS Style", "iOS Light", "iOS Medium", "Rounded iOS", "Mono iOS", "Serif iOS", "Condensed", "Rounded", "Mono", "Serif", "Cursive") { fontChoice = it; prefs.edit().putString("fontChoice", it).apply() }
                    if (themePreset == "Personalizzato") {
                        SettingSwitch("Tema scuro", dark) { dark = it; prefs.edit().putBoolean("dark", it).apply() }
                        SettingChoice("Sfondo", wallpaper, "OLED", "Graphite", "Warm", "Paper", "Gradient", "Sunset") { wallpaper = it; prefs.edit().putString("wallpaper", it).apply() }
                        SettingChoice("Colore accento", accentChoice, "Red", "Blue", "Green", "Amber") { accentChoice = it; prefs.edit().putString("accentChoice", it).apply() }
                    }
                    Text("Stili ispirati alla personalizzazione per palette e sfondi di Revolut: monocromi sobri come base, con varianti Midnight, Sage e Bronze. Ispirazione cromatica, non copie di schermate o grafiche. I widget Android nativi e gli sfondi dalla galleria restano funzioni da completare.",
                        color = muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("FATTO", color = accent) } }
        )
    }
}

@Composable
private fun ThemePresetChoice(label: String, selected: String, presets: List<LauncherThemePreset>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        Box {
            TextButton(onClick = { expanded = true }) { Text(selected, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 400.dp)) {
                presets.forEach { preset ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(32.dp).clip(CircleShape)
                                    .background(Brush.verticalGradient(listOf(preset.accent, preset.background)))
                                    .border(1.dp, preset.foreground.copy(alpha = 0.24f), CircleShape))
                                Column(Modifier.weight(1f)) {
                                    Text(preset.name, fontSize = 13.sp)
                                    Text(if (preset.name == "Personalizzato") "COLORI MANUALI" else if (preset.dark) "DARK · GLASS" else "LIGHT · GLASS",
                                        fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.7.sp)
                                }
                                if (selected == preset.name) Text("✓", color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        onClick = { onChange(preset.name); expanded = false }
                    )
                }
            }
        }
    }
}

private fun Modifier.glassSurface(base: Color, foreground: Color, enabled: Boolean, shape: Shape): Modifier {
    val clipped = this.clip(shape)
    return if (enabled) {
        clipped
            .background(Brush.verticalGradient(listOf(base.copy(alpha = 0.78f), base.copy(alpha = 0.48f))))
            .border(1.dp, Brush.verticalGradient(listOf(foreground.copy(alpha = 0.26f), foreground.copy(alpha = 0.05f))), shape)
    } else {
        clipped.background(base)
    }
}

private fun launchApp(context: android.content.Context, app: LaunchableApp) {
    val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
    if (launch != null) {
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(launch) } catch (_: Exception) {
            Toast.makeText(context, "Impossibile aprire l'app", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun toggleFavorite(current: Set<String>, packageName: String, prefs: android.content.SharedPreferences): Set<String> {
    val next = current.toMutableSet()
    if (!next.add(packageName)) next.remove(packageName)
    prefs.edit().putStringSet("favorites", next).apply()
    return next.toSet()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(app: LaunchableApp, favorite: Boolean, showLabel: Boolean, fg: Color, accent: Color, compact: Boolean, iconSize: Int, fontChoice: String,
                    onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .padding(vertical = if (compact) 1.dp else 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopEnd) {
            AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                update = { it.setImageDrawable(app.icon) }, modifier = Modifier.size(if (compact) iconSize.coerceAtMost(46).dp else iconSize.dp))
            if (favorite) Box(Modifier.padding(top = 1.dp, end = 1.dp).size(6.dp).clip(CircleShape).background(accent))
        }
        if (showLabel) {
            Spacer(Modifier.height(7.dp))
            Text(app.label, color = fg, fontSize = if (compact) 9.sp else 10.sp, maxLines = 1, textAlign = TextAlign.Center, lineHeight = 12.sp,
                letterSpacing = if (fontChoice == "iOS Style") (-0.15).sp else 0.sp, fontWeight = if (fontChoice == "iOS Style") FontWeight.Medium else FontWeight.Normal)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListRow(app: LaunchableApp, favorite: Boolean, fg: Color, muted: Color, accent: Color, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
            update = { it.setImageDrawable(app.icon) }, modifier = Modifier.size(36.dp))
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, color = fg, fontSize = 15.sp, maxLines = 1)
            Text(app.packageName, color = muted, fontSize = 10.sp, maxLines = 1)
        }
        if (favorite) Text("●", color = accent, fontSize = 8.sp)
        else Text("↗", color = muted, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomAction(label: String, selected: Boolean, accent: Color, fg: Color, panel: Color, glassEffect: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val tint = if (selected) panel else panel.copy(alpha = 0.24f)
    Row(Modifier.fillMaxWidth(0.48f).glassSurface(tint, fg, glassEffect, shape)
        .combinedClickable(onClick = onClick, onLongClick = {}).padding(vertical = 13.dp), horizontalArrangement = Arrangement.Center) {
        Text(label, color = if (selected) accent else fg, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingChoice(label: String, value: String, vararg choices: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        Box {
            TextButton(onClick = { expanded = true }) { Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                choices.forEach { choice -> DropdownMenuItem(text = { Text(choice) }, onClick = { onChange(choice); expanded = false }) }
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFD71920)))
    }
}
