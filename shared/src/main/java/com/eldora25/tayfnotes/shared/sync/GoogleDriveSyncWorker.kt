package com.eldora25.tayfnotes.shared.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.eldora25.tayfnotes.shared.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

class GoogleDriveSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. SettingsScreen'den gelen Google Hesabı E-postasını al
            val accountEmail = inputData.getString("GOOGLE_ACCOUNT_EMAIL") ?: return@withContext Result.failure()
            val localNotesJson = inputData.getString("NOTES_JSON") ?: "[]"

            // 2. Google Drive İstemcisini Hazırla
            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext, listOf(DriveScopes.DRIVE_FILE)
            ).apply { selectedAccountName = accountEmail }

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("TayfNotes").build()

            val fileName = "tayfnotes_backup.json"

            // 3. Drive'da dosya var mı kontrol et (Sadece uygulamamızın oluşturduğu dosyalar içinde)
            val fileList = driveService.files().list()
                .setQ("name='$fileName' and trashed=false")
                .setSpaces("drive")
                .execute()

            val existingFile = fileList.files.firstOrNull()

            // 4. Yerel verileri çözümle
            val localNotes = try { Json.decodeFromString<List<Note>>(localNotesJson) } catch (e: Exception) { emptyList() }
            var remoteNotes: List<Note> = emptyList()

            // 5. Dosya varsa indir ve uzak verileri çözümle
            if (existingFile != null) {
                val outputStream = ByteArrayOutputStream()
                driveService.files().get(existingFile.id).executeMediaAndDownloadTo(outputStream)
                val remoteJson = outputStream.toString("UTF-8")
                try { remoteNotes = Json.decodeFromString(remoteJson) } catch (e: Exception) { }
            }

            // 6. Verileri Birleştir (Tarihe Göre)
            val mergedNotesMap = mutableMapOf<String, Note>()
            remoteNotes.forEach { mergedNotesMap[it.id] = it }
            localNotes.forEach { localNote ->
                val existingRemote = mergedNotesMap[localNote.id]
                if (existingRemote == null || localNote.lastModified > existingRemote.lastModified) {
                    mergedNotesMap[localNote.id] = localNote
                }
            }

            val finalMergedJson = Json.encodeToString(mergedNotesMap.values.toList())
            val byteArrayContent = ByteArrayContent.fromString("application/json", finalMergedJson)

            // 7. Birleştirilmiş veriyi Drive'a yükle (Var olanı güncelle veya yeni oluştur)
            if (existingFile != null) {
                driveService.files().update(existingFile.id, null, byteArrayContent).execute()
            } else {
                val fileMetadata = com.google.api.services.drive.model.File().apply { name = fileName }
                driveService.files().create(fileMetadata, byteArrayContent).execute()
            }

            // 8. Sonucu ViewModel'a dön
            val outputData = Data.Builder().putString("MERGED_NOTES_JSON", finalMergedJson).build()
            Result.success(outputData)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
