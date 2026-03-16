package com.example.offermatrix.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AssetUtils {
    private const val TAG = "AssetUtils"

    /**
     * Copies a file from assets to the internal files directory if it doesn't exist.
     * Returns the absolute path of the copied file, or null if failed.
     */
    suspend fun copyAssetToFilesDir(context: Context, assetName: String): String? = withContext(Dispatchers.IO) {
        val outFile = File(context.filesDir, assetName)
        if (outFile.exists()) {
            Log.d(TAG, "File already exists: ${outFile.absolutePath}")
            return@withContext outFile.absolutePath
        }

        try {
            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(outFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d(TAG, "Copied asset $assetName to ${outFile.absolutePath}")
            return@withContext outFile.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy asset $assetName", e)
            return@withContext null
        }
    }
}
