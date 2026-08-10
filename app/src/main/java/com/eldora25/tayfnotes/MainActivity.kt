package com.eldora25.tayfnotes

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.eldora25.tayfnotes.data.database.AppDatabase
import com.eldora25.tayfnotes.data.repository.FolderRepository
import com.eldora25.tayfnotes.data.repository.NoteRepository
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.ui.*
import com.eldora25.tayfnotes.ui.components.BottomNavigationBar
import com.eldora25.tayfnotes.ui.theme.EditorNeonIcon
import com.eldora25.tayfnotes.ui.theme.TayfNotesTheme
import com.eldora25.tayfnotes.ui.viewmodel.NoteViewModel
import com.eldora25.tayfnotes.ui.viewmodel.NoteViewModelFactory
import com.eldora25.tayfnotes.util.BackupImportHelper
import com.eldora25.tayfnotes.util.BackupPackageHelper
import com.eldora25.tayfnotes.util.BiometricHelper

sealed class Screen {
    object Main : Screen()
    object Folders : Screen()
    object Calendar : Screen()
    object More : Screen()
    object Archive : Screen()
    object Trash : Screen()
    object Settings : Screen()
    object ThemeSelection : Screen()
    data class EditNote(val note: Note? = null, val initialSketch: Boolean = false) : Screen()
}

class MainActivity : FragmentActivity() {
    
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val noteRepository by lazy { NoteRepository(database.noteDao()) }
    private val folderRepository by lazy { FolderRepository(database.folderDao()) }
    
    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(application, noteRepository, folderRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val currentTheme by noteViewModel.currentTheme.collectAsState()
            val isDarkModePref by noteViewModel.isDarkMode.collectAsState()
            val isBiometricEnabled by noteViewModel.isBiometricEnabled.collectAsState()
            
            var isAuthenticated by rememberSaveable(isBiometricEnabled) { 
                mutableStateOf(!isBiometricEnabled) 
            }

            val permissionsToRequest = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { /* Results */ }

            LaunchedEffect(Unit) {
                permissionLauncher.launch(permissionsToRequest)
            }

            LaunchedEffect(isBiometricEnabled) {
                if (isBiometricEnabled && !isAuthenticated) {
                    BiometricHelper.authenticate(
                        activity = this@MainActivity,
                        onSuccess = { isAuthenticated = true },
                        onError = { error ->
                            Toast.makeText(this@MainActivity, "Güvenlik Hatası: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            TayfNotesTheme(
                darkTheme = isDarkModePref ?: isSystemInDarkTheme(),
                currentTheme = currentTheme
            ) {
                if (isAuthenticated) {
                    MainAppContent()
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text("TayfNotes Kilitli", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MainAppContent() {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
        val notes by noteViewModel.notes.collectAsState()
        val archivedNotes by noteViewModel.archivedNotes.collectAsState()
        val trashedNotes by noteViewModel.trashedNotes.collectAsState()
        val searchQuery by noteViewModel.searchQuery.collectAsState()
        val folders by noteViewModel.allFolders.collectAsState()
        val isSyncing by noteViewModel.isSyncing.collectAsState()
        val currentTheme by noteViewModel.currentTheme.collectAsState()
        val isDarkModePref by noteViewModel.isDarkMode.collectAsState()
        val isBiometricEnabled by noteViewModel.isBiometricEnabled.collectAsState()
        val activeCloudProvider by noteViewModel.activeCloudProvider.collectAsState()
        
        val configuration = LocalConfiguration.current
        // Madde 2: Split 0.4 / 0.6 always active
        val isMasterDetail = true 
        var selectedNoteInMasterDetail by remember { mutableStateOf<Note?>(null) }

        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                BackupImportHelper.importBackup(this, it, 
                    onComplete = { Toast.makeText(this, "Yedek başarıyla yüklendi. Uygulamayı yeniden başlatın.", Toast.LENGTH_LONG).show() },
                    onError = { e -> Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show() }
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (currentScreen is Screen.EditNote) {
                val screen = currentScreen as Screen.EditNote
                BackHandler { currentScreen = Screen.Main }
                NoteEditorScreen(
                    note = screen.note,
                    folders = folders,
                    initialSketch = screen.initialSketch,
                    onBack = { currentScreen = Screen.Main },
                    onSave = { noteViewModel.saveNote(it) },
                    onDelete = { 
                        noteViewModel.trashNote(it.id)
                        currentScreen = Screen.Main
                    }
                )
            } else if (currentScreen is Screen.ThemeSelection) {
                BackHandler { currentScreen = Screen.More }
                ThemeSelectionScreen(
                    currentTheme = currentTheme,
                    isDarkMode = isDarkModePref,
                    onThemeSelected = { noteViewModel.setTheme(it) },
                    onDarkModeChanged = { noteViewModel.setDarkMode(it) },
                    onBack = { currentScreen = Screen.More }
                )
            } else {
                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(
                            currentScreen = currentScreen,
                            onScreenChange = { currentScreen = it },
                            onNotesClick = { noteViewModel.onFolderSelected(null) }
                        )
                    }
                ) { innerPadding ->
                    Row(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        // Madde 2: Left 0.4 column
                        Box(modifier = Modifier.weight(if (currentScreen is Screen.Main) 0.4f else 1f)) {
                            when (currentScreen) {
                                is Screen.Main -> MainScreen(
                                    notes = notes,
                                    searchQuery = searchQuery,
                                    onSearchQueryChanged = { noteViewModel.onSearchQueryChanged(it) },
                                    onAddNote = { currentScreen = Screen.EditNote() },
                                    onAddChecklist = { currentScreen = Screen.EditNote(note = Note(id = System.currentTimeMillis().toString(), title = "", content = "", type = com.eldora25.tayfnotes.shared.model.NoteType.CHECKLIST)) },
                                    onAddSketch = { currentScreen = Screen.EditNote(initialSketch = true) },
                                    onEditNote = { note -> 
                                        selectedNoteInMasterDetail = note
                                    }
                                )
                                is Screen.Folders -> FoldersScreen(
                                    folders = folders,
                                    onFolderClick = { folder ->
                                        noteViewModel.onFolderSelected(folder.id)
                                        currentScreen = Screen.Main
                                    },
                                    onAddFolder = { name, color -> noteViewModel.addFolder(name, color) },
                                    onUpdateFolder = { folder -> noteViewModel.updateFolder(folder) }
                                )
                                is Screen.Calendar -> CalendarScreen(
                                    notes = notes,
                                    onEditNote = { note -> currentScreen = Screen.EditNote(note) }
                                )
                                is Screen.More -> MoreScreen(onScreenChange = { currentScreen = it })
                                is Screen.Archive -> NoteListScreen(
                                    title = "Arşiv",
                                    notes = archivedNotes,
                                    onBack = { currentScreen = Screen.More },
                                    onEditNote = { note -> currentScreen = Screen.EditNote(note) }
                                )
                                is Screen.Trash -> NoteListScreen(
                                    title = "Çöp Kutusu",
                                    notes = trashedNotes,
                                    onBack = { currentScreen = Screen.More },
                                    onEditNote = { note -> currentScreen = Screen.EditNote(note) },
                                    onEmptyTrash = { noteViewModel.emptyTrash() }
                                )
                                is Screen.Settings -> SettingsScreen(
                                    onBack = { currentScreen = Screen.More },
                                    isSyncing = isSyncing,
                                    onSyncClick = { 
                                        if (activeCloudProvider == null) {
                                            Toast.makeText(this@MainActivity, "Lütfen önce bir sağlayıcı seçin.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            noteViewModel.syncData() 
                                        }
                                    },
                                    currentTheme = currentTheme,
                                    onThemeSelected = { noteViewModel.setTheme(it) },
                                    isDarkMode = isDarkModePref,
                                    onDarkModeChanged = { noteViewModel.setDarkMode(it) },
                                    isBiometricEnabled = isBiometricEnabled,
                                    onBiometricToggle = { noteViewModel.setBiometricEnabled(it) },
                                    activeCloudProvider = activeCloudProvider,
                                    onCloudProviderSelected = { noteViewModel.setCloudProvider(it) },
                                    onFullBackupClick = {
                                        noteViewModel.exportFullBackup { file ->
                                            BackupPackageHelper.shareBackup(this@MainActivity, file)
                                        }
                                    },
                                    onImportBackupClick = { importLauncher.launch("application/zip") }
                                )
                                else -> {}
                            }
                        }
                        
                        // Madde 2: Right 0.6 Detail Pane (only for MainScreen)
                        if (currentScreen is Screen.Main) {
                            VerticalDivider(modifier = Modifier.fillMaxHeight(), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            Box(modifier = Modifier.weight(0.6f)) {
                                DetailPane(
                                    note = selectedNoteInMasterDetail,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (selectedNoteInMasterDetail != null) {
                                    IconButton(
                                        onClick = { currentScreen = Screen.EditNote(selectedNoteInMasterDetail) },
                                        modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(16.dp)
                                    ) {
                                        EditorNeonIcon {
                                            Icon(Icons.Default.Edit, contentDescription = "Düzenle")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
