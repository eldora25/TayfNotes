package com.eldora25.tayfnotes.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.R
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException

@Composable
fun OneDriveAuthLauncher(
    onAuthSuccess: (String) -> Unit, // Başarılı girişte Token döner
    onAuthError: (String) -> Unit
) {
    val context = LocalContext.current
    var msalApp by remember { mutableStateOf<ISingleAccountPublicClientApplication?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }

    // OneDrive "App Folder" okuma/yazma izni
    val scopes = arrayOf("Files.ReadWrite.AppFolder")

    // Uygulama başlarken MSAL Client'ı oluştur (JSON dosyasını okuyarak)
    LaunchedEffect(Unit) {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            R.raw.auth_config_single_account,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication?) {
                    msalApp = application
                }
                override fun onError(exception: MsalException?) {
                    onAuthError("MSAL başlatılamadı: ${exception?.message}")
                }
            }
        )
    }

    Button(
        onClick = {
            if (msalApp == null || context !is Activity) return@Button
            isAuthenticating = true
            
            // Etkileşimli (Interactive) giriş ekranını başlat
            msalApp?.signIn(
                context, 
                "", 
                scopes, 
                object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                        isAuthenticating = false
                        val token = authenticationResult?.accessToken
                        if (token != null) {
                            onAuthSuccess(token)
                        } else {
                            onAuthError("Token alınamadı.")
                        }
                    }

                    override fun onError(exception: MsalException?) {
                        isAuthenticating = false
                        onAuthError("Giriş hatası: ${exception?.message}")
                    }

                    override fun onCancel() {
                        isAuthenticating = false
                        onAuthError("Giriş iptal edildi.")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        enabled = msalApp != null && !isAuthenticating,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D4)) // OneDrive Mavisi
    ) {
        Text(if (isAuthenticating) "Bağlanıyor..." else "OneDrive ile Bağlan", color = Color.White)
    }
}
