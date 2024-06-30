package com.etg.asr

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream

class FileReader {
    companion object {
        const val BYTE_BUFFER_SIZE = 384000 * 4 / 3 * 72
        val byteDataRaw = ByteArray(BYTE_BUFFER_SIZE)
        var mIndex = 0;

        const val REQUEST_CODE_OPEN_FILE = 1
    }

    fun readFromFile(context : Context) {
        // MediaStore을 통해 파일 URI를 검색
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.MIME_TYPE)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("record.pcm")

        val queryUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val cursor = context.contentResolver.query(
            queryUri,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            cursor.moveToFirst();
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idColumn)
                val uri = Uri.withAppendedPath(queryUri, id.toString())
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    inputStream?.read(byteDataRaw)
                    inputStream?.close()
                } catch (e: Exception) {
                    Log.e("FileOpen", "File not found: $uri", e)
                }
            } else {
                Log.e("FileOpen", "File not found in MediaStore")
            }
        } ?: run {
            Log.e("FileOpen", "Cursor is null")
        }
    }

    fun readFromFileDfa(context: Context) {
        // Direct File Access not working
    }

    fun readFromFileSaf(context: Context) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*" // Allow all file types, or specify a more specific MIME type if needed
        }
        (context as? Activity)?.startActivityForResult(intent, REQUEST_CODE_OPEN_FILE)
            ?: Log.e("FileOpen", "Context is not an Activity, cannot start SAF")

        intent.data.let { documentUri ->
            try {
                context.contentResolver.openInputStream(documentUri!!)?.use { inputStream ->
                    // Assuming byteDataRaw is a ByteArray defined elsewhere
                    inputStream.read(byteDataRaw)
                    Log.d("FileOpen", "File read successfully from SAF")
                }
            } catch (e: Exception) {
                Log.e("FileOpen", "Error reading file from SAF: ${e.message}")
            }
        }
    }

    fun getByteArrayChunk(chunkSize: Int): ByteArray {
        val byteArrayChunk = ByteArray(chunkSize)
        for (i in 1..chunkSize) {
            if (mIndex >= BYTE_BUFFER_SIZE) break

            // Remove 4th channel data
            if ((i % 7 == 0) || (i % 8 == 0)) {
                mIndex++ //////////////////////////////////***
                continue
            }

            byteArrayChunk[i] = byteDataRaw[mIndex++]
        }
        return byteArrayChunk
    }
}