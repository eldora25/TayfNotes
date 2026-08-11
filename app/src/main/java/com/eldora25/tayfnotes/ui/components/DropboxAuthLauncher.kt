package com.eldora25.tayfnotes.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dropbox.core.android.Auth

@Composable
fun DropboxAuthLauncher(
    appKey: String,
    onAuthSuccess: (String) -> Unit, // Başarılı girişte Token döner
    onAuthError: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Kimlik doğrulamanın devam edip etmediğini takip eden state
    var isAuthenticating by remember { mutableStateOf(false) }

    // Yaşam döngüsünü dinle (Tarayıcıdan uygulamaya geri dönüldüğünde çalışır)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isAuthenticating) {
                // Uygulama tekrar ön plana geldiğinde Dropbox'tan token gelip gelmediğini kontrol et
                val accessToken = Auth.getOAuth2Token()
                
                if (accessToken != null) {
                    onAuthSuccess(accessToken)
                } else {
                    onAuthError()
                }
                isAuthenticating = false // İşlem bitti
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Button(
        onClick = {
            isAuthenticating = true
            // Dropbox giriş ekranını tetikler
            Auth.startOAuth2Authentication(context, appKey)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061FF))
    ) {
        Text("Dropbox ile Bağlan", color = Color.White)
    }
}
