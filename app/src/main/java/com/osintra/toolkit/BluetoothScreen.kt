package com.osintra.toolkit

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BleDevice(val name: String, val address: String, val rssi: Int)

fun bluetoothPerms(): List<String> =
    if (Build.VERSION.SDK_INT >= 31) {
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

@Composable
fun ColumnScope.BluetoothScreen() {
    PermissionGate(
        bluetoothPerms(),
        "Android demande ton autorisation pour chercher les appareils Bluetooth proches."
    ) {
        BluetoothContent()
    }
}

@SuppressLint("MissingPermission")
@Composable
fun ColumnScope.BluetoothContent() {
    val context = LocalContext.current
    val adapter: BluetoothAdapter? = remember {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }
    val devices = remember { mutableStateMapOf<String, BleDevice>() }
    var scanning by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val callback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val address = result.device.address
                var name: String? = null
                try {
                    name = result.device.name
                } catch (e: SecurityException) {
                }
                if (name == null) name = result.scanRecord?.deviceName
                devices[address] = BleDevice(name ?: "(sans nom)", address, result.rssi)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                adapter?.bluetoothLeScanner?.stopScan(callback)
            } catch (e: Exception) {
            }
        }
    }

    if (adapter == null) {
        Text("Ce téléphone n'a pas de Bluetooth.", fontSize = 15.sp)
    } else {
        Button(onClick = {
            if (scanning) {
                try {
                    adapter.bluetoothLeScanner?.stopScan(callback)
                } catch (e: Exception) {
                }
                scanning = false
            } else if (!adapter.isEnabled) {
                message = "Le Bluetooth est éteint : active-le dans les réglages rapides, puis relance."
            } else {
                devices.clear()
                try {
                    adapter.bluetoothLeScanner.startScan(callback)
                    scanning = true
                    message = ""
                } catch (e: Exception) {
                    message = "Impossible de lancer le scan : ${e.message}"
                }
            }
        }) {
            Text(if (scanning) "Arrêter le scan" else "Lancer le scan")
        }
        Spacer(Modifier.height(8.dp))
        if (message.isNotEmpty()) Text(message, fontSize = 14.sp)
        Text("${devices.size} appareil(s) trouvé(s)", fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(devices.values.sortedByDescending { it.rssi }) { d ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(d.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("${d.address}  •  ${d.rssi} dBm  •  ${signalLabel(d.rssi)}", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
