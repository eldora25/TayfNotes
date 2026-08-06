import 'package:isar/isar.dart';
import 'package:path_provider/path_provider.dart';
import '../models/note.dart';

class DatabaseService {
  static late Isar isar;

  static Future<void> initialize() async {
    final dir = awaitgetApplicationSupportDirectory();
    isar = await Isar.open(
      [NoteSchema],
      directory: dir.path,
      inspector: true,
    );
  }

  static Future<List<Note>> getAllNotes() async {
    return await isar.notes.where().sortByUpdatedAtDesc().findAll();
  }

  static Future<void> saveNote(Note note) async {
    note.updatedAt = DateTime.now();
    await isar.writeTxn(() async {
      await isar.notes.put(note);
    });
  }

  static Future<void> deleteNote(Id id) async {
    await isar.writeTxn(() async {
      await isar.notes.delete(id);
    });
  }
}
