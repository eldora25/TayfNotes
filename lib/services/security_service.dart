import 'dart:convert';
import 'package:crypto/crypto.dart';

class SecurityService {
  // Şifre metnini SHA-256 ve basit bir tabanlı güvenlik katmanı ile hashleme / doğrulama
  static String hashPassword(String password) {
    final bytes = utf8.encode(password);
    final digest = sha256.convert(bytes);
    return digest.toString();
  }

  static bool verifyPassword(String inputPassword, String storedHash) {
    return hashPassword(inputPassword) == storedHash;
  }
}
