package com.eldora25.tayfnotes.ui.components

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

@Composable
fun GoogleDriveAuthLauncher(
    onAuthSuccess: (String) -> Unit, // Başarılı girişte kullanıcının E-posta adresini dönecek
    onAuthError: (String) -> Unit
) {
    val context = LocalContext.current
    var isAuthenticating by remember { mutableStateOf(false) }
    val webClientId = stringResource(id = R.string.default_web_client_id)

    // Google Sign-In İstemcisini Ayarla (Drive Scope'u ile birlikte)
    val googleSignInClient = remember(context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE)) // Uygulama klasörüne erişim izni
            .requestIdToken(webClientId)
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    // Seçim Ekranından Dönen Sonucu Yakalayan Launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isAuthenticating = false
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                val email = account?.email
                if (email != null) {
                    onAuthSuccess(email) // E-postayı yakaladık, bunu WorkManager'a göndereceğiz
                } else {
                    onAuthError("Hesap e-postası alınamadı.")
                }
            } catch (e: Exception) {
                onAuthError("Giriş başarısız: ${e.localizedMessage}")
            }
        } else {
            onAuthError("Giriş iptal edildi.")
        }
    }

    Button(
        onClick = {
            isAuthenticating = true
            signInLauncher.launch(googleSignInClient.signInIntent)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        enabled = !isAuthenticating,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)) // Google Drive Yeşili
    ) {
        Text(
            text = if (isAuthenticating) "Bağlanıyor..." else "Google Drive ile Bağlan",
            color = Color.White
        )
    }
}
