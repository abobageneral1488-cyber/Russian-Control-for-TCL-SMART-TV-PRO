package com.tvmanager.launcher

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.io.File

/** Все файлы лежат в папке приложения: /sdcard/Android/data/com.tvmanager.launcher/files/ */
object Store {
    fun dir(c: Context): File {
        val d = c.getExternalFilesDir(null) ?: c.filesDir
        d.mkdirs()
        return d
    }

    fun sub(c: Context, name: String): File {
        val d = File(dir(c), name)
        d.mkdirs()
        return d
    }

    fun file(c: Context, folder: String, name: String?): File? {
        if (name.isNullOrEmpty()) return null
        val f = File(sub(c, folder), name)
        return if (f.isFile) f else null
    }

    fun list(c: Context, folder: String): List<String> {
        val ok = setOf("mp4", "mkv", "webm", "mov", "3gp")
        val files = sub(c, folder).listFiles() ?: emptyArray<File>()
        return files.filter { it.isFile && it.extension.lowercase() in ok }.map { it.name }.sorted()
    }

    fun prefs(c: Context): SharedPreferences = c.getSharedPreferences("tvm", Context.MODE_PRIVATE)

    fun config(c: Context): JSONObject =
        try {
            JSONObject(File(dir(c), "config.json").readText())
        } catch (e: Exception) {
            JSONObject()
        }
}
