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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.eldora25.tayfnotes.BuildConfig
import com.eldora25.tayfnotes.data.database.AppDatabase
import com.eldora25.tayfnotes.data.repository.FolderRepository
import com.eldora25.tayfnotes.data.repository.NoteRepository
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType
import com.eldora25.tayfnotes.ui.*
import com.eldora25.tayfnotes.ui.components.BottomNavigationBar
import com.eldora25.tayfnotes.ui.components.NavigationDrawerContent
import com.eldora25.tayfnotes.ui.theme.EditorNeonIcon
import com.eldora25.tayfnotes.ui.theme.TayfNotesTheme
import com.eldora25.tayfnotes.ui.viewmodel.NoteViewModel
import com.eldora25.tayfnotes.ui.viewmodel.NoteViewModelFactory
import com.eldora25.tayfnotes.util.BackupImportHelper
import com.eldora25.tayfnotes.util.BackupPackageHelper
import com.eldora25.tayfnotes.util.BiometricHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

sealed class Screen {
    object Main : Screen()
    object Folders : Screen()
    object Calendar : Screen()
    object Archive : Screen()
    object Trash : Screen()
    object Settings : Screen()
    object ThemeSelection : Screen()
    data class EditNote(val note: Note? = null, val initialSketch: Boolean = false) : Screen()
    data class WebClipper(val url: String, val title: String? = null, val content: String? = null) : Screen()
    object InternalBrowser : Screen()
    data class Placeholder(val title: String) : Screen()
    data class DetailNote(val note: Note) : Screen()
    data class List(val type: NoteType?) : Screen() // Madde 3: Filtreleme
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
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }

        LaunchedEffect(Unit) {
            sharedUrlFlow.collect { url ->
                currentScreen = Screen.WebClipper(url)
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                NavigationDrawerContent(
                    currentScreen = currentScreen,
                    onScreenSelected = { screen ->
                        currentScreen = screen
                        scope.launch { drawerState.close() }
                    },
                    onPlaceholderSelected = { title ->
                        currentScreen = Screen.Placeholder(title)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        ) {
            ContentArea(
                currentScreen = currentScreen,
                onScreenChange = { currentScreen = it },
                onMenuClick = { scope.launch { drawerState.open() } }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ContentArea(
        currentScreen: Screen,
        onScreenChange: (Screen) -> Unit,
        onMenuClick: () -> Unit
    ) {
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
        val currentFontFamily by noteViewModel.currentFontFamily.collectAsState()

        var selectedNoteInMasterDetail by remember { mutableStateOf<Note?>(null) }
        var sortType by remember { mutableStateOf(SortType.DATE_MODIFIED) }
        var showSortMenu by remember { mutableStateOf(false) }
        
        // Madde 3: Navigasyon ve Filtreleme
        val currentFilter = remember(currentScreen) {
            when (currentScreen) {
                is Screen.Main -> null // Combined/All
                is Screen.Folders -> null 
                // Add more logic here if needed for specific types
                else -> null
            }
        }

        val filteredNotes = remember(notes, currentScreen) {
            when (currentScreen) {
                is Screen.Main -> notes // Combined
                is Screen.List -> {
                    val targetType = (currentScreen as Screen.List).type
                    if (targetType == null) notes
                    else notes.filter { it.type == targetType }
                }
                else -> notes
            }
        }

        val isWideScreen = LocalConfiguration.current.screenWidthDp > 600

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
                BackHandler { onScreenChange(Screen.Main) }
                NoteEditorScreen(
                    note = screen.note,
                    folders = folders,
                    initialSketch = screen.initialSketch,
                    fontSize = currentFontSize,
                    fontFamily = currentFontFamily,
                    onBack = { onScreenChange(Screen.Main) },
                    onSave = { noteViewModel.saveNote(it) },
                    onDelete = { noteViewModel.trashNote(it.id); onScreenChange(Screen.Main) }
                )
            } else if (currentScreen is Screen.DetailNote) {
                val screen = currentScreen as Screen.DetailNote
                BackHandler { onScreenChange(Screen.Main) }
                NoteDetailScreen(
                    note = screen.note,
                    fontSize = currentFontSize,
                    fontFamily = currentFontFamily,
                    onBack = { onScreenChange(Screen.Main) },
                    onEdit = { onScreenChange(Screen.EditNote(screen.note)) },
                    onDelete = { noteViewModel.trashNote(screen.note.id) }
                )
            } else if (currentScreen is Screen.WebClipper) {
                val screen = currentScreen as Screen.WebClipper
                BackHandler { onScreenChange(Screen.Main) }
                WebClipperScreen(
                    url = screen.url,
                    folders = folders,
                    predefinedTitle = screen.title,
                    predefinedContent = screen.content,
                    onSave = { 
                        noteViewModel.saveNote(it)
                        onScreenChange(Screen.Main) 
                    },
                    onCancel = { onScreenChange(Screen.Main) }
                )
            } else if (currentScreen is Screen.Placeholder) {
                val screen = currentScreen as Screen.Placeholder
                BackHandler { onScreenChange(Screen.Main) }
                PlaceholderScreen(title = screen.title, onBack = { onScreenChange(Screen.Main) })
            } else if (currentScreen is Screen.InternalBrowser) {
                BackHandler { onScreenChange(Screen.Main) }
                InternalWebBrowserScreen(
                    onBack = { onScreenChange(Screen.Main) },
                    onClipContent = { title, url, text ->
                        onScreenChange(Screen.WebClipper(url, title, text))
                    }
                )
            } else if (currentScreen is Screen.ThemeSelection) {
                BackHandler { onScreenChange(Screen.Main) }
                ThemeSelectionScreen(
                    currentTheme = currentTheme,
                    isDarkMode = isDarkModePref,
                    onThemeSelected = { noteViewModel.setTheme(it) },
                    onDarkModeChanged = { noteViewModel.setDarkMode(it) },
                    onBack = { onScreenChange(Screen.Main) }
                )
            } else if (currentScreen == Screen.Main || currentScreen is Screen.List) {
                MainScreen(
                    notes = filteredNotes,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { noteViewModel.onSearchQueryChanged(it) },
                    onAddNote = { onScreenChange(Screen.EditNote()) },
                    onAddChecklist = { onScreenChange(Screen.EditNote(note = Note(id = System.currentTimeMillis().toString(), title = "", content = "", type = com.eldora25.tayfnotes.shared.model.NoteType.CHECKLIST))) },
                    onAddSketch = { onScreenChange(Screen.EditNote(initialSketch = true)) },
                    onEditNote = { onScreenChange(Screen.EditNote(it)) },
                    onMoveNote = { f, t -> noteViewModel.updateNotePosition(f, t) },
                    onDeleteNote = { noteViewModel.trashNote(it.id) },
                    onArchiveNote = { id, arc -> noteViewModel.archiveNote(id, arc) },
                    onUndoDelete = { noteViewModel.restoreNote(it) },
                    onNoteClick = { 
                        if (isWideScreen) selectedNoteInMasterDetail = it
                        else onScreenChange(Screen.DetailNote(it))
                    },
                    selectedNoteId = selectedNoteInMasterDetail?.id,
                    fontSize = currentFontSize,
                    fontFamily = currentFontFamily,
                    onMenuClick = onMenuClick
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        is Screen.Folders -> FoldersScreen(
                            folders = folders,
                            onFolderClick = { noteViewModel.onFolderSelected(it.id); onScreenChange(Screen.Main) },
                            onAddFolder = { n, c -> noteViewModel.addFolder(n, c) },
                            onUpdateFolder = { noteViewModel.updateFolder(it) },
                            onMenuClick = onMenuClick
                        )
                        is Screen.Calendar -> CalendarScreen(
                            notes = notes,
                            onEditNote = { onScreenChange(Screen.EditNote(it)) },
                            onMenuClick = onMenuClick
                        )
                        is Screen.Archive -> NoteListScreen(
                            title = "Arşiv",
                            notes = archivedNotes,
                            onBack = { onScreenChange(Screen.Main) },
                            onEditNote = { onScreenChange(Screen.EditNote(it)) },
                            onUnarchiveNote = { noteViewModel.archiveNote(it, false) },
                            onMenuClick = onMenuClick
                        )
                        is Screen.Trash -> NoteListScreen(
                            title = "Çöp", 
                            notes = trashedNotes, 
                            onBack = { onScreenChange(Screen.Main) }, 
                            onEditNote = { onScreenChange(Screen.EditNote(it)) }, 
                            onRestoreNote = { noteViewModel.restoreNote(it) },
                            onBulkRestore = { noteViewModel.bulkRestoreNotes(it) },
                            onBulkDelete = { noteViewModel.bulkPermanentlyDeleteNotes(it) },
                            onEmptyTrash = { noteViewModel.emptyTrash() },
                            onMenuClick = onMenuClick
                        )
                        is Screen.Settings -> SettingsScreen(
                            onBack = { onScreenChange(Screen.Main) },
                            isSyncing = isSyncing,
                            activeCloudProvider = activeCloudProvider,
                            onConnectDropbox = { token ->
                                noteViewModel.setDropboxToken(token)
                                noteViewModel.startDropboxSync(this@MainActivity, token)
                                Toast.makeText(this@MainActivity, "Dropbox Bağlandı ve Senkronizasyon Başladı", Toast.LENGTH_SHORT).show()
                            },
                            onConnectOneDrive = { token ->
                                noteViewModel.setOneDriveToken(token)
                                Toast.makeText(this@MainActivity, "OneDrive Bağlandı", Toast.LENGTH_SHORT).show()
                            },
                            onDisconnectCloud = { noteViewModel.setCloudProvider(null) },
                            currentTheme = currentTheme,
                            onThemeSelected = { noteViewModel.setTheme(it) },
                            isDarkMode = isDarkModePref,
                            onDarkModeChanged = { noteViewModel.setDarkMode(it) },
                            currentFontSize = currentFontSize,
                            onFontSizeChanged = { noteViewModel.setFontSize(it) },
                            currentFontFamily = currentFontFamily,
                            onFontFamilyChanged = { noteViewModel.setFontFamily(it) },
                            onAuthSuccess = { email ->
                                noteViewModel.setCloudProvider("Google Drive")
                                noteViewModel.startGoogleDriveSync(this@MainActivity, email)
                                Toast.makeText(this@MainActivity, "Bağlandı: $email", Toast.LENGTH_SHORT).show()
                            },
                            onAuthError = { errorMsg ->
                                Toast.makeText(this@MainActivity, "Hata: $errorMsg", Toast.LENGTH_SHORT).show()
                            },
                            isBiometricEnabled = isBiometricEnabled,
                            onBiometricToggle = { noteViewModel.setBiometricEnabled(it) },
                            onFullBackupClick = { noteViewModel.exportFullBackup { BackupPackageHelper.shareBackup(this@MainActivity, it) } },
                            onImportBackupClick = { importLauncher.launch("application/zip") },
                            onMenuClick = onMenuClick
                        )
                        else -> {}
                    }
                }
            }
        }
    }
}
