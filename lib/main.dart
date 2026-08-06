import 'package:flutter/material.dart';
import 'models/note.dart';
import 'services/database_service.dart';
import 'services/security_service.dart';
import 'services/reminder_service.dart';
import 'widgets/checklist_widget.dart';

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
                    if (note.isLocked) {
                      // Kilitli not kontrolü
                      String enteredPass = '';
                      final unlocked = await showDialog<bool>(
                        context: context,
                        builder: (context) => AlertDialog(
                          title: const Text('Şifreli Not'),
                          content: TextField(
                            obscureText: true,
                            decoration: const InputDecoration(hintText: 'Şifreyi girin'),
                            onChanged: (val) => enteredPass = val,
                          ),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.pop(context, false),
                              child: const Text('İptal'),
                            ),
                            ElevatedButton(
                              onPressed: () {
                                if (SecurityService.verifyPassword(enteredPass, note.passwordHash ?? '')) {
                                  Navigator.pop(context, true);
                                } else {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(content: Text('Hatalı şifre!')),
                                  );
                                }
                              },
                              child: const Text('Aç'),
                            ),
                          ],
                        ),
                      );
                      if (unlocked != true) return;
                    }

                    if (!context.mounted) return;
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
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                note.title.isNotEmpty ? note.title : 'İsimsiz',
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            if (note.isLocked) const Icon(Icons.lock, size: 16),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Expanded(
                          child: Text(
                            note.isChecklist ? 'Yapılacaklar Listesi' : note.content,
                            maxLines: 4,
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
  late bool _isChecklist;
  late List<String> _checklistItems;
  late bool _isLocked;
  String? _passwordHash;
  DateTime? _reminderDate;

  @override
  void initState() {
    super.initState();
    _titleController = TextEditingController(text: widget.note?.title ?? '');
    _contentController = TextEditingController(text: widget.note?.content ?? '');
    _notebook = widget.note?.notebook ?? 'Genel';
    _colorHex = widget.note?.colorHex ?? '#FFF9C4';
    _isChecklist = widget.note?.isChecklist ?? false;
    _checklistItems = widget.note?.checklistItems ?? [];
    _isLocked = widget.note?.isLocked ?? false;
    _passwordHash = widget.note?.passwordHash;
    _reminderDate = widget.note?.reminderDate;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.note == null ? 'Yeni Not' : 'Notu Düzenle'),
        actions: [
          IconButton(
            icon: Icon(_isLocked ? Icons.lock : Icons.lock_open),
            onPressed: () async {
              if (!_isLocked) {
                String pass = '';
                final setPass = await showDialog<bool>(
                  context: context,
                  builder: (context) => AlertDialog(
                    title: const Text('Notu Şifrele'),
                    content: TextField(
                      obscureText: true,
                      decoration: const InputDecoration(hintText: 'Şifre belirleyin'),
                      onChanged: (val) => pass = val,
                    ),
                    actions: [
                      TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('İptal')),
                      ElevatedButton(
                        onPressed: () {
                          if (pass.isNotEmpty) {
                            setState(() {
                              _isLocked = true;
                              _passwordHash = SecurityService.hashPassword(pass);
                            });
                            Navigator.pop(context, true);
                          }
                        },
                        child: const Text('Koru'),
                      ),
                    ],
                  ),
                );
                if (setPass == true) {
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Not şifrelendi!')));
                }
              } else {
                setState(() {
                  _isLocked = false;
                  _passwordHash = null;
                });
                ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Şifre kaldırıldı.')));
              }
            },
          ),
          IconButton(
            icon: const Icon(Icons.alarm),
            onPressed: () async {
              final date = await ReminderService.selectReminderDate(context);
              if (date != null) {
                setState(() {
                  _reminderDate = date;
                });
                if (!context.mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('Hatırlatıcı ayarlandı: ${date.toString()}'))
                );
              }
            },
          ),
          IconButton(
            icon: const Icon(Icons.save),
            onPressed: () async {
              final note = widget.note ?? Note();
              note.title = _titleController.text;
              note.content = _contentController.text;
              note.createdAt = widget.note?.createdAt ?? DateTime.now();
              note.notebook = _notebook;
              note.colorHex = _colorHex;
              note.isChecklist = _isChecklist;
              note.checklistItems = _checklistItems;
              note.isLocked = _isLocked;
              note.passwordHash = _passwordHash;
              note.reminderDate = _reminderDate;
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
            Row(
              children: [
                ChoiceChip(
                  label: const Text('Normal Not'),
                  selected: !_isChecklist,
                  onSelected: (val) => setState(() => _isChecklist = false),
                ),
                const SizedBox(width: 10),
                ChoiceChip(
                  label: const Text('Yapılacaklar (Checklist)'),
                  selected: _isChecklist,
                  onSelected: (val) => setState(() => _isChecklist = true),
                ),
              ],
            ),
            const Divider(),
            Expanded(
              child: _isChecklist
                  ? ChecklistWidget(
                      items: _checklistItems,
                      onChanged: (updatedItems) {
                        _checklistItems = updatedItems;
                      },
                    )
                  : TextField(
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
