package com.eldora25.tayfnotes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.eldora25.tayfnotes.data.database.AppDatabase
import com.eldora25.tayfnotes.data.repository.FolderRepository
import com.eldora25.tayfnotes.data.repository.NoteRepository
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType
import com.eldora25.tayfnotes.ui.*
import com.eldora25.tayfnotes.ui.theme.TayfNotesTheme
import com.eldora25.tayfnotes.ui.theme.TayfTheme
import com.eldora25.tayfnotes.ui.viewmodel.NoteViewModel
import com.eldora25.tayfnotes.ui.viewmodel.NoteViewModelFactory
import com.eldora25.tayfnotes.util.BackupPackageHelper
import com.eldora25.tayfnotes.util.BiometricHelper
import kotlinx.coroutines.launch

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
            val context = LocalContext.current
            val currentTheme by noteViewModel.currentTheme.collectAsState()
            val isDarkModePref by noteViewModel.isDarkMode.collectAsState()
            val isBiometricEnabled by noteViewModel.isBiometricEnabled.collectAsState()
            val currentFontFamily by noteViewModel.currentFontFamily.collectAsState()
            
            // Auto-lock logic restored but with safe defaults
            var isAuthenticated by rememberSaveable(isBiometricEnabled) { 
                val isAvailable = BiometricHelper.isBiometricAvailable(context)
                mutableStateOf(!isBiometricEnabled || !isAvailable) 
            }

            key(currentTheme, isDarkModePref, currentFontFamily) {
                TayfNotesTheme(
                    darkTheme = isDarkModePref ?: isSystemInDarkTheme(),
                    currentTheme = currentTheme,
                    defaultFontFamily = currentFontFamily
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (isAuthenticated) {
                            MainAppContent()
                        } else {
                            LockedScreen(onAuthenticate = { isAuthenticated = true })
                        }
                    }
                }
            }

            LaunchedEffect(isBiometricEnabled, isAuthenticated) {
                if (isBiometricEnabled && !isAuthenticated && BiometricHelper.isBiometricAvailable(context)) {
                    BiometricHelper.authenticate(
                        activity = this@MainActivity,
                        onSuccess = { isAuthenticated = true },
                        onError = { error -> Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        }
    }

    @Composable
    fun LockedScreen(onAuthenticate: () -> Unit) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("TayfNotes Kilitli", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    BiometricHelper.authenticate(
                        activity = this@MainActivity,
                        onSuccess = onAuthenticate,
                        onError = { error -> Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show() }
                    )
                }) {
                    Text("Giriş Yap")
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

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                com.eldora25.tayfnotes.ui.components.NavigationDrawerContent(
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

        Surface(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                is Screen.Main, is Screen.List -> {
                    val filteredNotes = remember(notes, currentScreen) {
                        if (currentScreen is Screen.List) {
                            notes.filter { it.type == currentScreen.type }
                        } else {
                            notes
                        }
                    }
                    MainScreen(
                        notes = filteredNotes,
                        searchQuery = searchQuery,
                        onSearchQueryChanged = { noteViewModel.onSearchQueryChanged(it) },
                        onAddNote = { onScreenChange(Screen.EditNote()) },
                        onAddChecklist = { onScreenChange(Screen.EditNote(note = Note(id = System.currentTimeMillis().toString(), title = "", content = "", type = NoteType.CHECKLIST))) },
                        onAddSketch = { onScreenChange(Screen.EditNote(initialSketch = true)) },
                        onEditNote = { onScreenChange(Screen.EditNote(it)) },
                        onMoveNote = { f, t -> noteViewModel.updateNotePosition(f, t) },
                        onDeleteNote = { noteViewModel.trashNote(it.id) },
                        onArchiveNote = { id, arc -> noteViewModel.archiveNote(id, arc) },
                        onUndoDelete = { noteViewModel.restoreNote(it) },
                        onNoteClick = { onScreenChange(Screen.DetailNote(it)) },
                        fontSize = currentFontSize,
                        fontFamily = currentFontFamily,
                        onMenuClick = onMenuClick
                    )
                }
                is Screen.EditNote -> {
                    val screen = currentScreen as Screen.EditNote
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
                }
                is Screen.DetailNote -> {
                    val note = (currentScreen as Screen.DetailNote).note
                    NoteDetailScreen(
                        note = note,
                        fontSize = currentFontSize,
                        fontFamily = currentFontFamily,
                        onEdit = { onScreenChange(Screen.EditNote(note)) },
                        onDelete = { noteViewModel.trashNote(note.id); onScreenChange(Screen.Main) },
                        onBack = { onScreenChange(Screen.Main) }
                    )
                }
                is Screen.Settings -> {
                    SettingsScreen(
                        onBack = { onScreenChange(Screen.Main) },
                        isSyncing = isSyncing,
                        activeCloudProvider = activeCloudProvider,
                        onConnectDropbox = { token ->
                            noteViewModel.setDropboxToken(token)
                            noteViewModel.startDropboxSync(this, token)
                        },
                        onConnectOneDrive = { /* OneDrive */ },
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
                            noteViewModel.startGoogleDriveSync(this, email)
                        },
                        onAuthError = { error -> Toast.makeText(this, error, Toast.LENGTH_SHORT).show() },
                        isBiometricEnabled = isBiometricEnabled,
                        onBiometricToggle = { noteViewModel.setBiometricEnabled(it) },
                        onFullBackupClick = { noteViewModel.exportFullBackup { BackupPackageHelper.shareBackup(this, it) } },
                        onImportBackupClick = { /* importLauncher.launch("application/zip") */ },
                        onMenuClick = onMenuClick
                    )
                }
                is Screen.Folders -> FoldersScreen(
                    folders = folders,
                    onFolderClick = { noteViewModel.onFolderSelected(it.id); onScreenChange(Screen.Main) },
                    onAddFolder = { n, c -> noteViewModel.addFolder(n, c) },
                    onUpdateFolder = { noteViewModel.updateFolder(it) },
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
                is Screen.Placeholder -> {
                    val screen = currentScreen as Screen.Placeholder
                    PlaceholderScreen(title = screen.title, onBack = { onScreenChange(Screen.Main) })
                }
                is Screen.Info -> InfoScreen(onMenuClick = onMenuClick)
                is Screen.InternalBrowser -> InternalWebBrowserScreen(
                    onBack = { onScreenChange(Screen.Main) },
                    onClipContent = { title, url, text ->
                        onScreenChange(Screen.WebClipper(url, title, text))
                    }
                )
                is Screen.WebClipper -> {
                    val screen = currentScreen as Screen.WebClipper
                    WebClipperScreen(
                        url = screen.url,
                        folders = folders,
                        onSave = { noteViewModel.saveNote(it); onScreenChange(Screen.Main) },
                        onCancel = { onScreenChange(Screen.Main) },
                        predefinedTitle = screen.title,
                        predefinedContent = screen.content
                    )
                }
                else -> { /* Unknown */ }
            }
        }
    }
}
