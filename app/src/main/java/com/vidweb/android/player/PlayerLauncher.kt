package com.vidweb.android.player

import android.content.Context
import android.content.Intent

object PlayerLauncher {
    fun open(context: Context, paths: List<String>, index: Int) {
        val clean = paths.filter { it.isNotBlank() }
        if (clean.isEmpty()) return
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putStringArrayListExtra("paths", ArrayList(clean))
        intent.putExtra("index", index.coerceIn(0, clean.size - 1))
        context.startActivity(intent)
    }
}
