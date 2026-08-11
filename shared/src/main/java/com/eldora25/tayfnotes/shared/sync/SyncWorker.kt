package com.eldora25.tayfnotes.shared.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Gerekli parametreleri WorkManager'dan al
            val token = inputData.getString("DROPBOX_TOKEN") ?: return@withContext Result.failure()
            val notesJson = inputData.getString("NOTES_JSON") ?: "[]"

            // 2. Dropbox İstemcisini (Client) Hazırla
            val config = DbxRequestConfig.newBuilder("tayfnotes/v1").build()
            val client = DbxClientV2(config, token)

            // 3. Veriyi InputStream'e dönüştür
            val inputStream = ByteArrayInputStream(notesJson.toByteArray(Charsets.UTF_8))

            // 4. Dropbox'taki "App Folder" içine yükle
            client.files().uploadBuilder("/tayfnotes_backup.json")
                .withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(inputStream)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
