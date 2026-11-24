package com.osintra.toolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Module(val emoji: String, val title: String, val description: String)

val modules = listOf(
    Module("📶", "Bluetooth", "Scanne les appareils Bluetooth autour de toi et affiche leur nom et la force du signal."),
    Module("📡", "Wi-Fi", "Analyse les réseaux Wi-Fi proches : signal, canal et sécurité."),
    Module("💳", "NFC", "Lis les puces NFC compatibles (cartes, badges, étiquettes)."),
    Module("📺", "Infrarouge", "Télécommande universelle, si ton téléphone a un émetteur infrarouge."),
    Module("🌐", "Réseau", "Infos sur ta connexion, ping et tests de débit.")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App()
                }
            }
        }
    }
}

@Composable
fun App() {
    var selected by remember { mutableStateOf<Module?>(null) }
    BackHandler(enabled = selected != null) { selected = null }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
        val current = selected
        if (current == null) {
            HomeScreen { selected = it }
        } else {
            DetailScreen(current) { selected = null }
        }
    }
}

@Composable
fun HomeScreen(onOpen: (Module) -> Unit) {
    Text("Wireless Toolkit", fontSize = 30.sp, fontWeight = FontWeight.Bold)
    Text("Choisis un outil pour commencer", fontSize = 16.sp)
    Spacer(Modifier.height(20.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        modules.forEach { m ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(m) }) {
                Row(modifier = Modifier.padding(20.dp)) {
                    Text(m.emoji, fontSize = 32.sp)
                    Spacer(Modifier.padding(8.dp))
                    Text(m.title, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun DetailScreen(module: Module, onBack: () -> Unit) {
    Text("${module.emoji} ${module.title}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Text(module.description, fontSize = 16.sp)
    Spacer(Modifier.height(12.dp))
    Text("Ce module sera activé à la prochaine étape.", fontSize = 14.sp)
    Spacer(Modifier.height(24.dp))
    Button(onClick = onBack) { Text("Retour") }
}
