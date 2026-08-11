package com.eldora25.tayfnotes.shared.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import com.eldora25.tayfnotes.shared.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val token = inputData.getString("DROPBOX_TOKEN") ?: return@withContext Result.failure()
            val localNotesJson = inputData.getString("NOTES_JSON") ?: "[]"

            val config = DbxRequestConfig.newBuilder("tayfnotes/v1").build()
            val client = DbxClientV2(config, token)
            val filePath = "/tayfnotes_backup.json"

            // 1. Yerel notları çözümle
            val localNotes = try {
                Json.decodeFromString<List<Note>>(localNotesJson)
            } catch (e: Exception) {
                emptyList()
            }

            // 2. Dropbox'taki mevcut yedeği indirmeyi dene
            var remoteNotes: List<Note> = emptyList()
            try {
                val outputStream = ByteArrayOutputStream()
                client.files().download(filePath).download(outputStream)
                val remoteJson = outputStream.toString("UTF-8")
                remoteNotes = Json.decodeFromString(remoteJson)
            } catch (e: Exception) {
                // Dosya henüz yoksa (ilk senkronizasyon) hata yoksayılır
            }

            // 3. Yerel ve Bulut verilerini birleştir (Merge by ID & lastModified)
            val mergedNotesMap = mutableMapOf<String, Note>()
            
            // Önce uzak verileri haritaya ekle
            remoteNotes.forEach { mergedNotesMap[it.id] = it }
            
            // Yerel verileri karşılaştırarak ekle (Yerel veri daha yeniyse üzerine yaz)
            localNotes.forEach { localNote ->
                val existingRemote = mergedNotesMap[localNote.id]
                if (existingRemote == null || localNote.lastModified > existingRemote.lastModified) {
                    mergedNotesMap[localNote.id] = localNote
                }
            }

            val finalMergedNotes = mergedNotesMap.values.toList()
            val finalMergedJson = Json.encodeToString(finalMergedNotes)

            // 4. Birleştirilmiş en güncel veriyi tekrar Dropbox'a yükle
            val inputStream = ByteArrayInputStream(finalMergedJson.toByteArray(Charsets.UTF_8))
            client.files().uploadBuilder(filePath)
                .withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(inputStream)

            // 5. Birleştirilen güncel veriyi ViewModel'a geri gönder
            val outputData = Data.Builder()
                .putString("MERGED_NOTES_JSON", finalMergedJson)
                .build()

            Result.success(outputData)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
