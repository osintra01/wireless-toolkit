package com.osintra.toolkit

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

fun readNetInfo(cm: ConnectivityManager): List<String> {
    val net = cm.activeNetwork ?: return listOf("Aucune connexion active.")
    val caps = cm.getNetworkCapabilities(net)
    val props = cm.getLinkProperties(net)
    val out = mutableListOf<String>()
    val type = when {
        caps == null -> "Inconnu"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Données mobiles"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        else -> "Autre"
    }
    out.add("Connexion : $type")
    if (caps != null) {
        out.add("Débit descendant estimé : ${caps.linkDownstreamBandwidthKbps / 1000} Mb/s")
        out.add("Débit montant estimé : ${caps.linkUpstreamBandwidthKbps / 1000} Mb/s")
        val ok = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        out.add("Accès Internet : " + if (ok) "oui" else "non vérifié")
    }
    if (props != null) {
        props.linkAddresses.forEach { out.add("Adresse IP : " + (it.address.hostAddress ?: "?")) }
        props.dnsServers.forEach { out.add("Serveur DNS : " + (it.hostAddress ?: "?")) }
        val iface = props.interfaceName
        if (iface != null) out.add("Interface : $iface")
    }
    return out
}

fun tcpTest(host: String): List<String> {
    val out = mutableListOf<String>()
    val times = mutableListOf<Long>()
    try {
        val addr = InetAddress.getByName(host)
        out.add("Adresse : ${addr.hostAddress}")
        for (n in 1..4) {
            val start = System.nanoTime()
            try {
                Socket().use { s -> s.connect(InetSocketAddress(addr, 443), 3000) }
                val ms = (System.nanoTime() - start) / 1_000_000
                times.add(ms)
                out.add("Essai $n : $ms ms")
            } catch (e: Exception) {
                out.add("Essai $n : échec")
            }
        }
        if (times.isNotEmpty()) out.add("Moyenne : ${times.average().toInt()} ms")
    } catch (e: Exception) {
        out.add("Adresse introuvable : vérifie le nom du site et ta connexion.")
    }
    return out
}

@Composable
fun ColumnScope.NetworkScreen() {
    val context = LocalContext.current
    val cm = remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    var info by remember { mutableStateOf(readNetInfo(cm)) }
    var host by remember { mutableStateOf("google.com") }
    var results by remember { mutableStateOf(listOf<String>()) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            info.forEach { Text(it, fontSize = 15.sp) }
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(onClick = { info = readNetInfo(cm) }) { Text("Actualiser") }
    Spacer(Modifier.height(16.dp))
    Text("Test de réponse d'un site :", fontSize = 15.sp)
    OutlinedTextField(
        value = host,
        onValueChange = { host = it },
        label = { Text("Site (ex : google.com)") },
        singleLine = true
    )
    Spacer(Modifier.height(8.dp))
    Button(
        enabled = !running,
        onClick = {
            val target = host.trim()
            scope.launch {
                running = true
                results = listOf("Test en cours...")
                results = withContext(Dispatchers.IO) { tcpTest(target) }
                running = false
            }
        }
    ) { Text("Lancer le test") }
    Spacer(Modifier.height(8.dp))
    if (results.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                results.forEach { Text(it, fontSize = 15.sp) }
            }
        }
    }
}
