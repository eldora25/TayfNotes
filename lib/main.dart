import 'package:flutter/material.dart';
import 'models/note.dart';
import 'services/database_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await DatabaseService.initialize();
  runApp(const TayfNotesApp());
}

class TayfNotesApp extends StatelessWidget {
  const TayfNotesApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'TayfNotes Pro',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.amber, brightness: Brightness.light),
        useMaterial3: true,
      ),
      darkTheme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.amber, brightness: Brightness.dark),
        useMaterial3: true,
      ),
      themeMode: ThemeMode.system,
      home: const HomeScreen(),
    );
  }
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  late Future<List<Note>> _notesFuture;

  @override
  void initState() {
    super.initState();
    _loadNotes();
  }

  void _loadNotes() {
    setState(() {
      _notesFuture = DatabaseService.getAllNotes();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('TayfNotes (Evernote & ColorNote)'),
        actions: [
          IconButton(
            icon: const Icon(Icons.search),
            onPressed: () {
              // Arama ekranı veya filtresi entegre edilebilir
            },
          )
        ],
      ),
      body: FutureBuilder<List<Note>>(
        future: _notesFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          } else if (snapshot.hasError) {
            return Center(child: Text('Hata: ${snapshot.error}'));
          } else if (!snapshot.hasData || snapshot.data!.isEmpty) {
            return const Center(
              child: Text('Henüz not eklenmedi. Sağ alttan yeni not oluşturun!'),
            );
          }

          final notes = snapshot.data!;
          return GridView.builder(
            padding: const EdgeInsets.all(8),
            gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
              maxCrossAxisExtent: 250,
              mainAxisSpacing: 8,
              crossAxisSpacing: 8,
              childAspectRatio: 1,
            ),
            itemCount: notes.length,
            itemBuilder: (context, index) {
              final note = notes[index];
              return Card(
                color: note.colorHex.isNotEmpty 
                    ? Color(int.parse(note.colorHex.replaceFirst('#', '0xFF'))) 
                    : null,
                child: InkWell(
                  onTap: () async {
                    await Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => NoteEditScreen(note: note),
                      ),
                    );
                    _loadNotes();
                  },
                  child: Padding(
                    padding: const EdgeInsets.all(12.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          note.title.isNotEmpty ? note.title : 'İsimsiz',
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 8),
                        Expanded(
                          child: Text(
                            note.content,
                            maxLines: 5,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        Text(
                          note.notebook,
                          style: TextStyle(fontSize: 10, color: Colors.grey[700]),
                        )
                      ],
                    ),
                  ),
                ),
              );
            },
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        child: const Icon(Icons.add),
        onPressed: () async {
          await Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) => const NoteEditScreen(),
            ),
          );
          _loadNotes();
        },
      ),
    );
  }
}

class NoteEditScreen extends StatefulWidget {
  final Note? note;
  const NoteEditScreen({super.key, this.note});

  @override
  State<NoteEditScreen> createState() => _NoteEditScreenState();
}

class _NoteEditScreenState extends State<NoteEditScreen> {
  late TextEditingController _titleController;
  late TextEditingController _contentController;
  late String _notebook;
  late String _colorHex;

  @override
  void initState() {
    super.initState();
    _titleController = TextEditingController(text: widget.note?.title ?? '');
    _contentController = TextEditingController(text: widget.note?.content ?? '');
    _notebook = widget.note?.notebook ?? 'Genel';
    _colorHex = widget.note?.colorHex ?? '#FFF9C4'; // Varsayılan ColorNote sarısı
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.note == null ? 'Yeni Not' : 'Notu Düzenle'),
        actions: [
          IconButton(
            icon: const Icon(Icons.save),
            onPressed: () async {
              final note = widget.note ?? Note();
              note.title = _titleController.text;
              note.content = _contentController.text;
              note.createdAt = widget.note?.createdAt ?? DateTime.now();
              note.notebook = _notebook;
              note.colorHex = _colorHex;
              note.tags = [];
              
              await DatabaseService.saveNote(note);
              if (mounted) Navigator.pop(context);
            },
          )
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            TextField(
              controller: _titleController,
              decoration: const InputDecoration(hintText: 'Başlık', border: InputBorder.none),
              style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const Divider(),
            Expanded(
              child: TextField(
                controller: _contentController,
                decoration: const InputDecoration(hintText: 'Notunuzu yazın...', border: InputBorder.none),
                maxLines: null,
                expands: true,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
