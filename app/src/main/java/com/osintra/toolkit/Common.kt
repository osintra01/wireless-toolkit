package com.osintra.toolkit

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

fun hasPerms(context: Context, perms: List<String>): Boolean =
    perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

fun signalLabel(rssi: Int): String = when {
    rssi >= -60 -> "très proche"
    rssi >= -75 -> "proche"
    else -> "loin"
}

@Composable
fun ColumnScope.PermissionGate(
    perms: List<String>,
    why: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasPerms(context, perms)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasPerms(context, perms)
    }
    if (granted) {
        content()
    } else {
        Text(why, fontSize = 15.sp)
        Button(onClick = { launcher.launch(perms.toTypedArray()) }) { Text("Autoriser") }
    }
}
