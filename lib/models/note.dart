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

  // ColorNote tarzı renk etiketi (HEX string örn: #FFF59D)
  late String colorHex;

  // Evernote tarzı kategorizasyon ve etiketler
  late String notebook; 
  
  late List<String> tags;

  // Checklist desteği (ColorNote özelliği)
  bool isChecklist = false;
  
  List<String> checklistItems = []; // "Yapıldı_GorevAdi" veya "Yapılmadı_GorevAdi" şeklinde tutulabilir

  bool isPinned = false;
  bool isArchived = false;
  bool isLocked = false;
  
  String? password;
}
