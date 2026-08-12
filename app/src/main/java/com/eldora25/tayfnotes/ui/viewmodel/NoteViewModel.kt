package com.eldora25.tayfnotes.ui.viewmodel

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asFlow
import androidx.work.*
import com.eldora25.tayfnotes.data.repository.FolderRepository
import com.eldora25.tayfnotes.data.repository.NoteRepository
import com.eldora25.tayfnotes.shared.model.ChecklistItem
import com.eldora25.tayfnotes.shared.model.Folder
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.sync.DropboxProvider
import com.eldora25.tayfnotes.shared.sync.GoogleDriveProvider
import com.eldora25.tayfnotes.shared.sync.SyncManager
import com.eldora25.tayfnotes.shared.sync.SyncWorker
import com.eldora25.tayfnotes.shared.sync.GoogleDriveSyncWorker
import com.eldora25.tayfnotes.ui.theme.TayfTheme
import com.eldora25.tayfnotes.util.AlarmHelper
import com.eldora25.tayfnotes.util.BackupPackageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore(name = "settings")

data class NoteFilter(
    val folderId: String?,
    val query: String,
    val archives: Set<String>,
    val trash: Set<String>
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class NoteViewModel(
    private val application: Application,
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository
) : AndroidViewModel(application) {

    private val dataStore = application.dataStore
    private val syncManager = SyncManager()
    
    private val appInstanceId = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)

    private val THEME_KEY = stringPreferencesKey("app_theme")
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val BIOMETRIC_KEY = booleanPreferencesKey("biometric_lock")
    private val CLOUD_PROVIDER_KEY = stringPreferencesKey("cloud_provider")
    private val ARCHIVE_IDS_KEY = stringPreferencesKey("archive_ids")
    private val TRASH_IDS_KEY = stringPreferencesKey("trash_ids")
    private val FONT_SIZE_KEY = floatPreferencesKey("font_size")
    private val FONT_FAMILY_KEY = stringPreferencesKey("font_family")
    private val DROPBOX_TOKEN_KEY = stringPreferencesKey("dropbox_token")
    private val ONEDRIVE_TOKEN_KEY = stringPreferencesKey("onedrive_token")

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    val currentTheme: StateFlow<TayfTheme> = dataStore.data
        .map { pref -> 
            val themeName = pref[THEME_KEY] ?: TayfTheme.MIDNIGHT.name
            try {
                TayfTheme.valueOf(themeName)
            } catch (e: Exception) {
                TayfTheme.MIDNIGHT
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TayfTheme.MIDNIGHT)

    val isDarkMode: StateFlow<Boolean?> = dataStore.data
        .map { pref -> pref[DARK_MODE_KEY] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isBiometricEnabled: StateFlow<Boolean> = dataStore.data
        .map { pref -> pref[BIOMETRIC_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentFontSize: StateFlow<Float> = dataStore.data
        .map { pref -> pref[FONT_SIZE_KEY] ?: 16f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16f)

    val currentFontFamily: StateFlow<String> = dataStore.data
        .map { pref -> pref[FONT_FAMILY_KEY] ?: "Roboto" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Roboto")

    val activeCloudProvider: StateFlow<String?> = dataStore.data
        .map { pref -> pref[CLOUD_PROVIDER_KEY] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val archiveIds: StateFlow<Set<String>> = dataStore.data
        .map { pref -> pref[ARCHIVE_IDS_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    
    private val trashIds: StateFlow<Set<String>> = dataStore.data
        .map { pref -> pref[TRASH_IDS_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val allFolders: StateFlow<List<Folder>> = combine(folderRepository.allFolders, noteRepository.allNotes, archiveIds, trashIds) { folders, notes, archives, trash ->
        folders.map { folder ->
            folder.copy(noteCount = notes.count { it.folderId == folder.id && !trash.contains(it.id) && !archives.contains(it.id) })
        }.sortedBy { it.position }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = combine(_selectedFolderId, _searchQuery, archiveIds, trashIds) { folderId, query, archives, trash ->
        NoteFilter(folderId, query, archives, trash)
    }.flatMapLatest { filter ->
        noteRepository.allNotes.map { all ->
            all.filter { note ->
                val isArchived = filter.archives.contains(note.id)
                val isTrashed = filter.trash.contains(note.id)
                val matchesFolder = if (filter.folderId != null) note.folderId == filter.folderId else true
                val matchesSearch = if (filter.query.isNotEmpty()) note.title.contains(filter.query, ignoreCase = true) || note.content.contains(filter.query, ignoreCase = true) else true
                !isArchived && !isTrashed && matchesFolder && matchesSearch
            }.sortedBy { it.position }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val archivedNotes: StateFlow<List<Note>> = combine(noteRepository.allNotes, archiveIds) { all, archives ->
        all.filter { archives.contains(it.id) }.sortedByDescending { it.lastModified }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<Note>> = combine(noteRepository.allNotes, trashIds) { all, trash ->
        all.filter { trash.contains(it.id) }.sortedByDescending { it.lastModified }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun onFolderSelected(folderId: String?) { _selectedFolderId.value = folderId }

    fun setTheme(theme: TayfTheme) {
        viewModelScope.launch { dataStore.edit { it[THEME_KEY] = theme.name } }
    }

    fun setDarkMode(enabled: Boolean?) {
        viewModelScope.launch { dataStore.edit { if (enabled == null) it.remove(DARK_MODE_KEY) else it[DARK_MODE_KEY] = enabled } }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[BIOMETRIC_KEY] = enabled } }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch { dataStore.edit { it[FONT_SIZE_KEY] = size } }
    }

    fun setFontFamily(family: String) {
        viewModelScope.launch { dataStore.edit { it[FONT_FAMILY_KEY] = family } }
    }

    fun setCloudProvider(providerName: String?) {
        viewModelScope.launch { dataStore.edit { if (providerName == null) it.remove(CLOUD_PROVIDER_KEY) else it[CLOUD_PROVIDER_KEY] = providerName } }
    }

    fun importMergedNotes(mergedJson: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mergedNotes = Json.decodeFromString<List<Note>>(mergedJson)
                noteRepository.updateNotes(mergedNotes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setDropboxToken(token: String) {
        viewModelScope.launch {
            dataStore.edit { it[DROPBOX_TOKEN_KEY] = token }
            setCloudProvider("Dropbox")
        }
    }

    fun setOneDriveToken(token: String) {
        viewModelScope.launch {
            dataStore.edit { it[ONEDRIVE_TOKEN_KEY] = token }
            setCloudProvider("OneDrive")
        }
    }

    fun startDropboxSync(context: Context, dropboxToken: String) {
        viewModelScope.launch {
            val currentNotes = notes.value
            val notesJson = Json.encodeToString(currentNotes)

            val inputData = Data.Builder()
                .putString("DROPBOX_TOKEN", dropboxToken)
                .putString("NOTES_JSON", notesJson)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL, 
                    WorkRequest.MIN_BACKOFF_MILLIS, 
                    TimeUnit.MILLISECONDS
                )
                .build()

            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                "DropboxSyncWork",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )

            // Listen for sync result to update local DB
            workManager.getWorkInfoByIdLiveData(syncRequest.id).asFlow().collectLatest { workInfo ->
                if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                    val mergedJson = workInfo.outputData.getString("MERGED_NOTES_JSON")
                    if (mergedJson != null) {
                        try {
                            val mergedNotes = Json.decodeFromString<List<Note>>(mergedJson)
                            mergedNotes.forEach { note ->
                                noteRepository.insert(note) 
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    fun startGoogleDriveSync(context: Context, accountEmail: String) {
        viewModelScope.launch {
            val currentNotes = notes.value
            val notesJson = Json.encodeToString(currentNotes)

            val inputData = Data.Builder()
                .putString("GOOGLE_ACCOUNT_EMAIL", accountEmail)
                .putString("NOTES_JSON", notesJson)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<GoogleDriveSyncWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL, 
                    WorkRequest.MIN_BACKOFF_MILLIS, 
                    TimeUnit.MILLISECONDS
                )
                .build()

            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                "GoogleDriveSyncWork",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )

            workManager.getWorkInfoByIdLiveData(syncRequest.id).asFlow().collectLatest { workInfo ->
                if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                    val mergedJson = workInfo.outputData.getString("MERGED_NOTES_JSON")
                    if (mergedJson != null) {
                        try {
                            val mergedNotes = Json.decodeFromString<List<Note>>(mergedJson)
                            mergedNotes.forEach { note ->
                                noteRepository.insert(note) 
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    fun saveNote(note: Note) {
        viewModelScope.launch {
            noteRepository.insert(note.copy(lastModified = System.currentTimeMillis()))
            if (note.reminderTimestamp != null) AlarmHelper.scheduleReminder(application, note)
            else AlarmHelper.cancelReminder(application, note.id)
        }
    }

    fun updateNotePosition(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentList = notes.value.toMutableList()
            if (fromIndex in currentList.indices && toIndex in currentList.indices) {
                val movedNote = currentList.removeAt(fromIndex)
                currentList.add(toIndex, movedNote)
                
                val updatedList = currentList.mapIndexed { index, note ->
                    note.copy(position = index)
                }
                noteRepository.updateNotes(updatedList)
            }
        }
    }

    fun updateFolderPosition(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentList = allFolders.value.toMutableList()
            if (fromIndex in currentList.indices && toIndex in currentList.indices) {
                val movedFolder = currentList.removeAt(fromIndex)
                currentList.add(toIndex, movedFolder)
                currentList.forEachIndexed { index, folder ->
                    folderRepository.insert(folder.copy(position = index))
                }
            }
        }
    }

    fun trashNote(noteId: String) {
        viewModelScope.launch {
            dataStore.edit { pref ->
                val current = pref[TRASH_IDS_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toMutableSet() ?: mutableSetOf()
                current.add(noteId)
                pref[TRASH_IDS_KEY] = current.joinToString(",")
            }
        }
    }

    fun restoreNote(noteId: String) {
        viewModelScope.launch {
            dataStore.edit { pref ->
                val current = pref[TRASH_IDS_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toMutableSet() ?: mutableSetOf()
                current.remove(noteId)
                pref[TRASH_IDS_KEY] = current.joinToString(",")
            }
        }
    }

    fun bulkRestoreNotes(noteIds: Set<String>) {
        viewModelScope.launch {
            dataStore.edit { pref ->
                val current = pref[TRASH_IDS_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toMutableSet() ?: mutableSetOf()
                current.removeAll(noteIds)
                pref[TRASH_IDS_KEY] = current.joinToString(",")
            }
        }
    }

    fun bulkPermanentlyDeleteNotes(noteIds: Set<String>) {
        viewModelScope.launch {
            val all = noteRepository.allNotes.first()
            noteIds.forEach { id ->
                all.find { it.id == id }?.let { noteRepository.delete(it) }
            }
            dataStore.edit { pref ->
                val current = pref[TRASH_IDS_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toMutableSet() ?: mutableSetOf()
                current.removeAll(noteIds)
                pref[TRASH_IDS_KEY] = current.joinToString(",")
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val currentTrash = trashIds.value
            currentTrash.forEach { id ->
                noteRepository.allNotes.first().find { it.id == id }?.let { noteRepository.delete(it) }
            }
            dataStore.edit { it.remove(TRASH_IDS_KEY) }
        }
    }

    fun addFolder(name: String, colorHex: String) {
        viewModelScope.launch { folderRepository.insert(Folder(id = System.currentTimeMillis().toString(), name = name, colorHex = colorHex)) }
    }
    
    fun updateFolder(folder: Folder) { viewModelScope.launch { folderRepository.insert(folder.copy(lastModified = System.currentTimeMillis())) } }

    fun syncData() {
        viewModelScope.launch {
            _isSyncing.value = true
            val provider = when(activeCloudProvider.value) {
                "Google Drive" -> GoogleDriveProvider()
                "Dropbox" -> DropboxProvider()
                else -> null
            }
            syncManager.setProvider(provider)
            syncManager.syncNotes(notes.value, appInstanceId)
            _isSyncing.value = false
        }
    }

    fun exportFullBackup(onSuccess: (File) -> Unit) {
        BackupPackageHelper.createFullBackup(application, { onSuccess(it) }, {})
    }

    fun checklistToJson(items: List<ChecklistItem>): String = Json.encodeToString(items)
    fun jsonToChecklist(json: String): List<ChecklistItem> = try { Json.decodeFromString(json) } catch (_: Exception) { emptyList() }
}

class NoteViewModelFactory(
    private val application: Application,
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(application, noteRepository, folderRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
