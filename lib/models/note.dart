import 'package:isar/isar.dart';

part 'note.g.dart';

@collection
class Note {
  Id id = Isar.autoIncrement;

  @Index(type: IndexType.value)
  late String title;

  late String content;

  late DateTime createdAt;

  late DateTime updatedAt;

  // ColorNote tarzı renk etiketi (HEX)
  late String colorHex;

  // Evernote tarzı kategorizasyon
  late String notebook; 
  
  late List<String> tags;

  // Checklist desteği
  bool isChecklist = false;
  List<String> checklistItems = [];

  bool isPinned = false;
  bool isArchived = false;

  // Güvenlik ve Şifreleme Özellikleri
  bool isLocked = false;
  String? passwordHash;
  
  // Akıllı Hatırlatıcı
  DateTime? reminderDate;
}
