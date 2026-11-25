package com.osintra.toolkit

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

data class WifiNet(
    val ssid: String,
    val bssid: String,
    val level: Int,
    val channel: Int,
    val band: String,
    val security: String
)

fun freqToChannel(f: Int): Int = when {
    f == 2484 -> 14
    f in 2412..2472 -> (f - 2407) / 5
    f in 5170..5895 -> (f - 5000) / 5
    f in 5955..7115 -> (f - 5950) / 5
    else -> 0
}

fun freqBand(f: Int): String = when {
    f < 3000 -> "2,4 GHz"
    f < 5900 -> "5 GHz"
    else -> "6 GHz"
}

fun securityOf(cap: String): String = when {
    cap.contains("SAE") -> "WPA3"
    cap.contains("WPA2") || cap.contains("RSN") -> "WPA2"
    cap.contains("WPA") -> "WPA"
    cap.contains("WEP") -> "WEP"
    else -> "Ouvert"
}

@Composable
fun ColumnScope.WifiScreen() {
    PermissionGate(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION),
        "Android exige l'autorisation de localisation pour lister les réseaux Wi-Fi. Rien n'est envoyé nulle part."
    ) {
        WifiContent()
    }
}

@SuppressLint("MissingPermission")
@Composable
fun ColumnScope.WifiContent() {
    val context = LocalContext.current
    val wifi = remember {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    var networks by remember { mutableStateOf(listOf<WifiNet>()) }
    var message by remember { mutableStateOf("") }

    fun readResults() {
        try {
            networks = wifi.scanResults.map { r ->
                val name = r.SSID
                WifiNet(
                    ssid = if (name.isNullOrEmpty()) "(réseau caché)" else name,
                    bssid = r.BSSID ?: "",
                    level = r.level,
                    channel = freqToChannel(r.frequency),
                    band = freqBand(r.frequency),
                    security = securityOf(r.capabilities ?: "")
                )
            }.sortedByDescending { it.level }
        } catch (e: SecurityException) {
            message = "Autorisation refusée."
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                readResults()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        readResults()
        onDispose { context.unregisterReceiver(receiver) }
    }

    Button(onClick = {
        if (!wifi.isWifiEnabled) {
            message = "Le Wi-Fi est éteint : active-le, puis relance."
        } else {
            val ok = wifi.startScan()
            message = if (ok) {
                "Scan en cours..."
            } else {
                "Android limite le nombre de scans : réessaie dans une minute (anciens résultats affichés)."
            }
        }
    }) {
        Text("Scanner les réseaux")
    }
    Spacer(Modifier.height(8.dp))
    if (message.isNotEmpty()) Text(message, fontSize = 14.sp)

    val on24 = networks.filter { it.band == "2,4 GHz" }
    if (on24.isNotEmpty()) {
        val best = listOf(1, 6, 11).minByOrNull { ch ->
            on24.count { kotlin.math.abs(it.channel - ch) <= 2 }
        }
        Text("Canal 2,4 GHz le moins encombré : $best", fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
    Text("${networks.size} réseau(x) trouvé(s)", fontSize = 14.sp)
    Spacer(Modifier.height(8.dp))
    LazyColumn(modifier = Modifier.weight(1f)) {
        items(networks) { n ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(n.ssid, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${n.level} dBm (${signalLabel(n.level)})  •  ${n.band}  •  canal ${n.channel}  •  ${n.security}",
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
