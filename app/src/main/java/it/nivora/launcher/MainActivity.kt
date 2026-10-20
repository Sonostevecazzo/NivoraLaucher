package it.nivora.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

private data class ThemePreset(
    val name: String,
    val background: Color,
    val foreground: Color,
    val muted: Color,
    val panel: Color,
    val accent: Color,
    val glow: Color,
    val light: Boolean = false
)

private val NivoraThemes = listOf(
    ThemePreset("Glass Mono", Color(0xFF0B0D0F), Color(0xFFF1F2F2), Color(0xFFA5AAAD), Color(0xFF202326), Color(0xFFE9EBEC), Color(0xFF30363B)),
    ThemePreset("Obsidian", Color(0xFF050506), Color(0xFFF5F5F5), Color(0xFF929498), Color(0xFF17181A), Color(0xFFF0F0F0), Color(0xFF202124)),
    ThemePreset("Arctic", Color(0xFFE9EFF2), Color(0xFF151B20), Color(0xFF66717A), Color(0xFFD4E0E6), Color(0xFF283740), Color(0xFFF9FCFF), true),
    ThemePreset("Graphite", Color(0xFF202326), Color(0xFFEDEEEF), Color(0xFFADB1B4), Color(0xFF303438), Color(0xFFF0F1F1), Color(0xFF3B4146)),
    ThemePreset("Sand", Color(0xFFEFE8DC), Color(0xFF25221D), Color(0xFF726B62), Color(0xFFE1D7C7), Color(0xFF383027), Color(0xFFFFF8ED), true),
    ThemePreset("Sage", Color(0xFF1D2420), Color(0xFFE4EEE6), Color(0xFFA0B0A4), Color(0xFF2B3730), Color(0xFFDCE9DF), Color(0xFF35463B)),
    ThemePreset("Plum", Color(0xFF211B26), Color(0xFFF1E8F3), Color(0xFFBBAAC1), Color(0xFF342A39), Color(0xFFEDE0F0), Color(0xFF413047)),
    ThemePreset("Blue Pulse", Color(0xFF111E2D), Color(0xFFE8F1FB), Color(0xFF9EB4CB), Color(0xFF22344A), Color(0xFFC8E0FB), Color(0xFF29435E))
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
    var themeChoice by remember { mutableStateOf(prefs.getString("themeChoice", "Glass Mono") ?: "Glass Mono") }
    var showLogo by remember { mutableStateOf(prefs.getBoolean("showLogo", true)) }
    var monoIcons by remember { mutableStateOf(prefs.getBoolean("monoIcons", true)) }
    var glassEnabled by remember { mutableStateOf(prefs.getBoolean("glassEnabled", true)) }
    var grid by remember { mutableStateOf(prefs.getBoolean("grid", true)) }
    var showLabels by remember { mutableStateOf(prefs.getBoolean("labels", true)) }
    var compact by remember { mutableStateOf(prefs.getBoolean("compact", false)) }
    var columns by remember { mutableIntStateOf(prefs.getInt("columns", 4).coerceIn(3, 5)) }
    var iconSize by remember { mutableIntStateOf(prefs.getInt("iconSize", 50).coerceIn(40, 64)) }
    var fontChoice by remember { mutableStateOf(prefs.getString("fontChoice", "System") ?: "System") }
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
    val selectedTheme = NivoraThemes.firstOrNull { it.name == themeChoice } ?: NivoraThemes.first()
    val bg = selectedTheme.background
    val fg = selectedTheme.foreground
    val muted = selectedTheme.muted
    val accent = selectedTheme.accent
    val panel = if (glassEnabled) selectedTheme.panel.copy(alpha = 0.56f) else selectedTheme.panel
    val glassBorder = if (selectedTheme.light) Color.White.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.16f)
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

    MaterialTheme(colorScheme = if (!selectedTheme.light) darkColorScheme(primary = accent, background = bg, surface = panel, onBackground = fg, onSurface = fg)
        else lightColorScheme(primary = accent, background = bg, surface = panel, onBackground = fg, onSurface = fg), typography = customTypography) {
        Surface(Modifier.fillMaxSize(), color = bg) {
            val backdrop = Brush.verticalGradient(listOf(selectedTheme.glow.copy(alpha = if (glassEnabled) 0.72f else 0.28f), bg, bg))
            Column(Modifier.fillMaxSize().background(backdrop).statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (showLogo) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
                            Text("N I V O R A", color = fg, fontSize = 11.sp, letterSpacing = 2.2.sp, fontWeight = FontWeight.SemiBold)
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

                val searchShape = RoundedCornerShape(15.dp)
                Row(
                    Modifier.fillMaxWidth().clip(searchShape).background(panel)
                        .then(if (glassEnabled) Modifier.border(1.dp, glassBorder, searchShape) else Modifier)
                        .padding(horizontal = 12.dp, vertical = if (showAllApps) 11.dp else 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Cerca app", tint = muted, modifier = Modifier.size(if (showAllApps) 19.dp else 17.dp))
                    Spacer(Modifier.width(9.dp))
                    BasicTextField(value = query, onValueChange = { query = it; if (it.isNotBlank()) showAllApps = true },
                        singleLine = true, textStyle = TextStyle(color = fg, fontSize = if (showAllApps) 14.sp else 13.sp),
                        cursorBrush = SolidColor(accent), modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text(if (showAllApps) "Cerca un'app" else "Cerca", color = muted, fontSize = if (showAllApps) 14.sp else 13.sp)
                            inner()
                        })
                    if (query.isNotEmpty()) Text("×", color = muted, fontSize = 20.sp,
                        modifier = Modifier.combinedClickable(onClick = { query = "" }, onLongClick = { query = "" }).padding(horizontal = 4.dp))
                }
                Spacer(Modifier.height(if (showAllApps) 12.dp else 8.dp))

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
                            AppTile(app, favorites.contains(app.packageName), showLabels, fg, compact, iconSize, fontChoice,
                                monoIcons, accent, panel, glassEnabled, glassBorder,
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
                            AppListRow(app, favorites.contains(app.packageName), fg, muted, monoIcons, accent, panel, glassEnabled, glassBorder,
                                onClick = { launchApp(context, app) },
                                onLongClick = {
                                    favorites = toggleFavorite(favorites, app.packageName, prefs)
                                    Toast.makeText(context, if (favorites.contains(app.packageName)) "Preferito aggiunto" else "Preferito rimosso", Toast.LENGTH_SHORT).show()
                                })
                        }
                    }
                }

                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BottomAction("HOME", !showAllApps, accent, fg, panel) { showAllApps = false; query = ""; favoritesOnly = true }
                    BottomAction("APP", showAllApps, accent, fg, panel) { showAllApps = true; query = ""; favoritesOnly = false }
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
                    Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingChoice("Tema preimpostato", themeChoice, "Glass Mono", "Obsidian", "Arctic", "Graphite", "Sand", "Sage", "Plum", "Blue Pulse") {
                        themeChoice = it
                        prefs.edit().putString("themeChoice", it).apply()
                    }
                    SettingSwitch("Effetto vetro", glassEnabled) { glassEnabled = it; prefs.edit().putBoolean("glassEnabled", it).apply() }
                    SettingSwitch("Icone monocromatiche", monoIcons) { monoIcons = it; prefs.edit().putBoolean("monoIcons", it).apply() }
                    SettingSwitch("Mostra logo Nivora", showLogo) { showLogo = it; prefs.edit().putBoolean("showLogo", it).apply() }
                    SettingSwitch("Griglia di icone", grid) { grid = it; prefs.edit().putBoolean("grid", it).apply() }
                    SettingSwitch("Mostra nomi app", showLabels) { showLabels = it; prefs.edit().putBoolean("labels", it).apply() }
                    SettingSwitch("Griglia compatta", compact) { compact = it; prefs.edit().putBoolean("compact", it).apply() }
                    SettingChoice("Colonne", "$columns", "3", "4", "5") { columns = it.toInt(); prefs.edit().putInt("columns", columns).apply() }
                    SettingChoice("Dimensione icone", "$iconSize dp", "40", "50", "60") { iconSize = it.toInt(); prefs.edit().putInt("iconSize", iconSize).apply() }
                    SettingChoice("Carattere", fontChoice, "System", "iOS Style", "iOS Light", "iOS Medium", "Rounded iOS", "Mono iOS", "Serif iOS", "Condensed", "Rounded", "Mono", "Serif", "Cursive") { fontChoice = it; prefs.edit().putString("fontChoice", it).apply() }
                    Text("Preset originali ispirati alla personalizzazione dei temi di app come Revolut e ai temi pronti all'uso di Niagara. Effetto vetro leggero, palette coerente e icone desaturate. I widget Android nativi, gli sfondi dalla galleria e i pacchetti icone restano funzioni da completare.",
                        color = muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("FATTO", color = accent) } }
        )
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
private fun AppTile(app: LaunchableApp, favorite: Boolean, showLabel: Boolean, fg: Color, compact: Boolean, iconSize: Int, fontChoice: String,
                    monoIcons: Boolean, accent: Color, panel: Color, glassEnabled: Boolean, glassBorder: Color,
                    onClick: () -> Unit, onLongClick: () -> Unit) {
    val tileShape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().clip(tileShape).background(if (glassEnabled) panel else Color.Transparent)
        .then(if (glassEnabled) Modifier.border(1.dp, glassBorder, tileShape) else Modifier)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .padding(vertical = if (compact) 7.dp else 9.dp, horizontal = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopEnd) {
            AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                update = {
                    it.setImageDrawable(app.icon)
                    it.colorFilter = if (monoIcons) ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) else null
                }, modifier = Modifier.size(if (compact) iconSize.coerceAtMost(46).dp else iconSize.dp))
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
private fun AppListRow(app: LaunchableApp, favorite: Boolean, fg: Color, muted: Color, monoIcons: Boolean, accent: Color,
                       panel: Color, glassEnabled: Boolean, glassBorder: Color, onClick: () -> Unit, onLongClick: () -> Unit) {
    val rowShape = RoundedCornerShape(15.dp)
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(rowShape).background(if (glassEnabled) panel else Color.Transparent)
        .then(if (glassEnabled) Modifier.border(1.dp, glassBorder, rowShape) else Modifier)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
            update = {
                it.setImageDrawable(app.icon)
                it.colorFilter = if (monoIcons) ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) else null
            }, modifier = Modifier.size(36.dp))
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
private fun BottomAction(label: String, selected: Boolean, accent: Color, fg: Color, panel: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(0.48f).clip(RoundedCornerShape(14.dp)).background(if (selected) panel else Color.Transparent)
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
