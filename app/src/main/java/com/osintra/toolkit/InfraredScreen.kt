package com.osintra.toolkit

import android.content.Context
import android.hardware.ConsumerIrManager
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun buildPattern(headerMark: Int, headerSpace: Int, bytes: List<Int>): IntArray {
    val p = ArrayList<Int>()
    p.add(headerMark)
    p.add(headerSpace)
    for (b in bytes) {
        for (i in 0 until 8) {
            p.add(560)
            p.add(if (((b shr i) and 1) == 1) 1690 else 560)
        }
    }
    p.add(560)
    return p.toIntArray()
}

fun necPattern(addr: Int, cmd: Int): IntArray =
    buildPattern(9000, 4500, listOf(addr and 0xFF, addr.inv() and 0xFF, cmd and 0xFF, cmd.inv() and 0xFF))

fun samsungPattern(addr: Int, cmd: Int): IntArray =
    buildPattern(4500, 4500, listOf(addr and 0xFF, addr and 0xFF, cmd and 0xFF, cmd.inv() and 0xFF))

@Composable
fun ColumnScope.InfraredScreen() {
    val context = LocalContext.current
    val ir = remember { context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager }
    var message by remember { mutableStateOf("") }
    var addrText by remember { mutableStateOf("04") }
    var cmdText by remember { mutableStateOf("08") }

    if (ir == null || !ir.hasIrEmitter()) {
        Text(
            "Ton téléphone n'a pas d'émetteur infrarouge. Ce module ne peut donc pas fonctionner ici.",
            fontSize = 15.sp
        )
    } else {
        Text("Pointe le haut du téléphone vers ta télé, puis appuie.", fontSize = 15.sp)
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            ir.transmit(38000, necPattern(0x04, 0x08))
            message = "Signal LG envoyé."
        }) { Text("LG : marche / arrêt") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            ir.transmit(38000, samsungPattern(0x07, 0x02))
            message = "Signal Samsung envoyé."
        }) { Text("Samsung : marche / arrêt") }
        Spacer(Modifier.height(16.dp))
        Text("Code personnalisé (protocole NEC, valeurs en hexadécimal) :", fontSize = 14.sp)
        OutlinedTextField(
            value = addrText,
            onValueChange = { addrText = it },
            label = { Text("Adresse (ex : 04)") },
            singleLine = true
        )
        OutlinedTextField(
            value = cmdText,
            onValueChange = { cmdText = it },
            label = { Text("Commande (ex : 08)") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val a = addrText.trim().toIntOrNull(16)
            val c = cmdText.trim().toIntOrNull(16)
            if (a == null || c == null) {
                message = "Valeurs invalides : utilise des chiffres et lettres de 0 à F."
            } else {
                ir.transmit(38000, necPattern(a, c))
                message = "Signal envoyé."
            }
        }) { Text("Envoyer") }
        Spacer(Modifier.height(8.dp))
        if (message.isNotEmpty()) Text(message, fontSize = 14.sp)
    }
}
