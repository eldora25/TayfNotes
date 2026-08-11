package com.eldora25.tayfnotes

import android.Manifest
import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.eldora25.tayfnotes.BuildConfig
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
import kotlinx.coroutines.flow.MutableSharedFlow

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
    data class WebClipper(val url: String, val title: String? = null, val content: String? = null) : Screen()
    object InternalBrowser : Screen()
}

class MainActivity : FragmentActivity() {
    
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val noteRepository by lazy { NoteRepository(database.noteDao()) }
    private val folderRepository by lazy { FolderRepository(database.folderDao()) }
    
    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(application, noteRepository, folderRepository)
    }

    private val sharedUrlFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEND == intent.action && "text/plain" == intent.type) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                val url = text.split("\\s+".toRegex()).find { it.startsWith("http") }
                if (url != null) {
                    sharedUrlFlow.tryEmit(url)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        
        setContent {
            val currentTheme by noteViewModel.currentTheme.collectAsState()
            val isDarkModePref by noteViewModel.isDarkMode.collectAsState()
            val isBiometricEnabled by noteViewModel.isBiometricEnabled.collectAsState()
            
            var isAuthenticated by rememberSaveable(isBiometricEnabled) { 
                mutableStateOf(!isBiometricEnabled) 
            }

            LaunchedEffect(isBiometricEnabled) {
                if (isBiometricEnabled && !isAuthenticated) {
                    BiometricHelper.authenticate(
                        activity = this@MainActivity,
                        onSuccess = { isAuthenticated = true },
                        onError = { error -> Toast.makeText(this@MainActivity, "Güvenlik: $error", Toast.LENGTH_SHORT).show() }
                    )
                }
            }

            TayfNotesTheme(
                darkTheme = isDarkModePref ?: isSystemInDarkTheme(),
                currentTheme = currentTheme
            ) {
                if (isAuthenticated) MainAppContent()
                else Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(contentAlignment = Alignment.Center) { Text("TayfNotes Kilitli", style = MaterialTheme.typography.headlineMedium) }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
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
        val currentFontSize by noteViewModel.currentFontSize.collectAsState()
        
        var selectedNoteInMasterDetail by remember { mutableStateOf<Note?>(null) }
        var sortType by remember { mutableStateOf(SortType.DATE_MODIFIED) }
        var showSortMenu by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            sharedUrlFlow.collect { url ->
                currentScreen = Screen.WebClipper(url)
            }
        }

        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                BackupImportHelper.importBackup(this, it, 
                    onComplete = { Toast.makeText(this, "Yedek yüklendi.", Toast.LENGTH_LONG).show() },
                    onError = { e -> Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show() }
                )
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (currentScreen is Screen.EditNote) {
                val screen = currentScreen as Screen.EditNote
                BackHandler { currentScreen = Screen.Main }
                NoteEditorScreen(
                    note = screen.note,
                    folders = folders,
                    initialSketch = screen.initialSketch,
                    fontSize = currentFontSize,
                    onBack = { currentScreen = Screen.Main },
                    onSave = { noteViewModel.saveNote(it) },
                    onDelete = { noteViewModel.trashNote(it.id); currentScreen = Screen.Main }
                )
            } else if (currentScreen is Screen.WebClipper) {
                val screen = currentScreen as Screen.WebClipper
                BackHandler { currentScreen = Screen.Main }
                WebClipperScreen(
                    url = screen.url,
                    folders = folders,
                    predefinedTitle = screen.title,
                    predefinedContent = screen.content,
                    onSave = { 
                        noteViewModel.saveNote(it)
                        currentScreen = Screen.Main 
                    },
                    onCancel = { currentScreen = Screen.Main }
                )
            } else if (currentScreen is Screen.InternalBrowser) {
                BackHandler { currentScreen = Screen.More }
                InternalWebBrowserScreen(
                    onBack = { currentScreen = Screen.More },
                    onClipContent = { title, url, text ->
                        // Dahili tarayıcıdan kırpılan veriyi WebClipperScreen'e pasla
                        currentScreen = Screen.WebClipper(url, title, text)
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
            } else if (currentScreen == Screen.Main) {
                MainScreen(
                    notes = notes,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { noteViewModel.onSearchQueryChanged(it) },
                    onAddNote = { currentScreen = Screen.EditNote() },
                    onAddChecklist = { currentScreen = Screen.EditNote(note = Note(id = System.currentTimeMillis().toString(), title = "", content = "", type = com.eldora25.tayfnotes.shared.model.NoteType.CHECKLIST)) },
                    onAddSketch = { currentScreen = Screen.EditNote(initialSketch = true) },
                    onEditNote = { currentScreen = Screen.EditNote(it) },
                    onMoveNote = { f, t -> /* noteViewModel.moveNote(f, t) */ },
                    onDeleteNote = { noteViewModel.trashNote(it.id) },
                    selectedNoteId = selectedNoteInMasterDetail?.id,
                    bottomBar = {
                        BottomNavigationBar(currentScreen, { currentScreen = it }, { noteViewModel.onFolderSelected(null) })
                    }
                )
            } else {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { 
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("TayfNotes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                                    Text("buildv01.${BuildConfig.BUILD_NO} Tayfun YAMAK©", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            actions = {
                                if (currentScreen == Screen.Folders) {
                                    IconButton(onClick = { showSortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, null) }
                                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                        DropdownMenuItem(text = { Text("Alfabetik") }, onClick = { sortType = SortType.ALPHABETICAL; showSortMenu = false })
                                        DropdownMenuItem(text = { Text("Renge Göre") }, onClick = { sortType = SortType.COLOR; showSortMenu = false })
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        BottomNavigationBar(currentScreen, { currentScreen = it }, { noteViewModel.onFolderSelected(null) })
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        when (currentScreen) {
                            is Screen.Folders -> FoldersScreen(folders, { noteViewModel.onFolderSelected(it.id); currentScreen = Screen.Main }, { n, c -> noteViewModel.addFolder(n, c) }, { noteViewModel.updateFolder(it) })
                            is Screen.Calendar -> CalendarScreen(notes, { currentScreen = Screen.EditNote(it) })
                            is Screen.More -> MoreScreen { currentScreen = it }
                            is Screen.Archive -> NoteListScreen("Arşiv", archivedNotes, { currentScreen = Screen.More }, { currentScreen = Screen.EditNote(it) })
                            is Screen.Trash -> NoteListScreen("Çöp", trashedNotes, { currentScreen = Screen.More }, { currentScreen = Screen.EditNote(it) }, { noteViewModel.emptyTrash() })
                            is Screen.Settings -> SettingsScreen(
                                onBack = { currentScreen = Screen.More },
                                isSyncing = isSyncing,
                                activeCloudProvider = activeCloudProvider,
                                onConnectGoogleDrive = { noteViewModel.setCloudProvider("Google Drive") }, // Placeholder for real OAuth
                                onConnectDropbox = { token ->
                                    noteViewModel.setDropboxToken(token)
                                    Toast.makeText(this@MainActivity, "Dropbox Bağlandı", Toast.LENGTH_SHORT).show()
                                },
                                onDisconnectCloud = { noteViewModel.setCloudProvider(null) },
                                currentTheme = currentTheme,
                                onThemeSelected = { noteViewModel.setTheme(it) },
                                isDarkMode = isDarkModePref,
                                onDarkModeChanged = { noteViewModel.setDarkMode(it) },
                                currentFontSize = currentFontSize,
                                onFontSizeChanged = { noteViewModel.setFontSize(it) },
                                onAuthSuccess = { email ->
                                    noteViewModel.setCloudProvider("Google Drive")
                                    Toast.makeText(this@MainActivity, "Bağlandı: $email", Toast.LENGTH_SHORT).show()
                                },
                                onAuthError = { e ->
                                    Toast.makeText(this@MainActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                },
                                isBiometricEnabled = isBiometricEnabled,
                                onBiometricToggle = { noteViewModel.setBiometricEnabled(it) },
                                onFullBackupClick = { noteViewModel.exportFullBackup { BackupPackageHelper.shareBackup(this@MainActivity, it) } },
                                onImportBackupClick = { importLauncher.launch("application/zip") }
                            )
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
