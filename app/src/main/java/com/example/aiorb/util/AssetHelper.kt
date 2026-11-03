package com.example.aiorb.util

import android.content.Context
import java.io.File
import java.io.InputStream

object AssetHelper {
    /**
     * Safely opens an asset file stream, supporting standard Android runtime
     * and Robolectric JVM test environments (including non-ASCII/Unicode filenames).
     */
    fun openAsset(context: Context, relativePath: String): InputStream {
        try {
            return context.assets.open(relativePath)
        } catch (_: Exception) {
            // Attempt listing matching files in the folder
            val dir = relativePath.substringBeforeLast('/', "")
            val fileName = relativePath.substringAfterLast('/')
            try {
                val list = context.assets.list(dir)
                val matched = list?.firstOrNull { it.trim() == fileName.trim() || it.equals(fileName, ignoreCase = true) }
                if (matched != null) {
                    val fullPath = if (dir.isNotEmpty()) "$dir/$matched" else matched
                    return context.assets.open(fullPath)
                }
            } catch (_: Exception) {}

            // JVM / Robolectric direct file lookup
            val candidateDirs = listOf(
                File("app/src/main/assets", relativePath),
                File("src/main/assets", relativePath),
                File("app/build/intermediates/assets/debug/mergeDebugAssets", relativePath)
            )
            for (candidate in candidateDirs) {
                if (candidate.exists()) {
                    return candidate.inputStream()
                }
            }
            // If still not found, re-attempt default open to surface standard exception
            return context.assets.open(relativePath)
        }
    }
}
