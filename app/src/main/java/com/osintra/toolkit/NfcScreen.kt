package com.osintra.toolkit

import android.app.Activity
import android.content.Intent
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun recordText(r: NdefRecord): String {
    if (r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(NdefRecord.RTD_TEXT)) {
        val p = r.payload
        if (p.isNotEmpty()) {
            val lang = p[0].toInt() and 0x3F
            val charset = if ((p[0].toInt() and 0x80) == 0) Charsets.UTF_8 else Charsets.UTF_16
            return "texte « " + String(p, 1 + lang, p.size - 1 - lang, charset) + " »"
        }
    }
    val uri = try {
        r.toUri()
    } catch (e: Exception) {
        null
    }
    if (uri != null) return "lien " + uri.toString()
    return "données (${r.payload.size} octets)"
}

fun describeTag(tag: Tag): List<String> {
    val out = mutableListOf<String>()
    out.add("Identifiant (UID) : " + tag.id.joinToString(":") { "%02X".format(it) })
    out.add("Technologies : " + tag.techList.joinToString(", ") { it.substringAfterLast('.') })
    try {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            val msg = ndef.ndefMessage
            ndef.close()
            if (msg != null) {
                msg.records.forEachIndexed { i, r ->
                    out.add("Contenu ${i + 1} : " + recordText(r))
                }
            } else {
                out.add("Aucun message enregistré sur cette puce.")
            }
        } else {
            out.add("Pas de données lisibles (carte protégée ou format non standard).")
        }
    } catch (e: Exception) {
        out.add("Lecture impossible : ${e.message}")
    }
    return out
}

@Composable
fun ColumnScope.NfcScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val adapter: NfcAdapter? = remember { NfcAdapter.getDefaultAdapter(context) }
    var refresh by remember { mutableStateOf(0) }
    val enabled = remember(refresh) { adapter?.isEnabled == true }
    var lines by remember { mutableStateOf(listOf<String>()) }

    if (adapter == null) {
        Text("Ce téléphone n'a pas de NFC.", fontSize = 15.sp)
    } else if (!enabled) {
        Text("Le NFC est éteint. Active-le dans les réglages, puis reviens ici.", fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }) {
            Text("Ouvrir les réglages NFC")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { refresh = refresh + 1 }) { Text("J'ai activé, vérifier") }
    } else {
        DisposableEffect(Unit) {
            val cb = NfcAdapter.ReaderCallback { tag ->
                val result = describeTag(tag)
                activity?.runOnUiThread { lines = result }
            }
            if (activity != null) {
                adapter.enableReaderMode(
                    activity,
                    cb,
                    NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V,
                    null
                )
            }
            onDispose {
                if (activity != null) adapter.disableReaderMode(activity)
            }
        }
        Text("Prêt : approche une carte ou une étiquette du dos du téléphone.", fontSize = 15.sp)
        Spacer(Modifier.height(12.dp))
        if (lines.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    lines.forEach { Text(it, fontSize = 15.sp) }
                }
            }
        }
    }
}
