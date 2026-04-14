package com.gem.neteasecloudmd

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.gem.neteasecloudmd.api.NeteaseApiService
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.ui.theme.NeteaseCloudMDTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeteaseCloudMDTheme {
                LoginScreen(
                    onBack = { finish() },
                    onLoginSuccess = {
                        setResult(RESULT_OK)
                        finish()
                    },
                    context = this
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    context: android.content.Context
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var captcha by remember { mutableStateOf("") }
    var cookie by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCaptcha by remember { mutableStateOf(false) }
    var loginMode by remember { mutableStateOf("password") } // "password", "captcha", "cookie"
    
    val apiService = remember { NeteaseApiService() }
    val sessionManager = remember { SessionManager(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Login") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (showCaptcha) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = captcha,
                            onValueChange = { captcha = it },
                            label = { Text("Captcha") },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                isLoading = true
                                CoroutineScope(Dispatchers.Main).launch {
                                    val result = withContext(Dispatchers.IO) {
                                        apiService.verifyCaptchaAndLogin(phone, captcha)
                                    }
                                    isLoading = false
                                    result.fold(
                                        onSuccess = { loginResult ->
                                            val cookie = loginResult.cookie?.joinToString(";") ?: ""
                                            val profile = loginResult.profile
                                            sessionManager.saveLoginResult(
                                                com.gem.neteasecloudmd.api.LoginResult(
                                                    code = loginResult.code,
                                                    profile = profile,
                                                    cookie = loginResult.cookie
                                                ),
                                                cookie
                                            )
                                            Toast.makeText(context, "Welcome ${profile?.nickname ?: "User"}!", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess()
                                        },
                                        onFailure = { e ->
                                            errorMessage = e.message
                                        }
                                    )
                                }
                            },
                            enabled = !isLoading && captcha.isNotBlank(),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Verify")
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = androidx.compose.ui.graphics.Color.Red,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if (phone.isBlank()) {
                                errorMessage = "Please enter phone"
                                return@Button
                            }
                            
                            isLoading = true
                            errorMessage = null
                            
                            CoroutineScope(Dispatchers.Main).launch {
                                val result = withContext(Dispatchers.IO) {
                                    apiService.login(phone, password)
                                }
                                
                                isLoading = false
                                
                                result.fold(
                                    onSuccess = { loginResult ->
                                        val cookie = loginResult.cookie?.joinToString(";") ?: ""
                                        val profile = loginResult.profile
                                        sessionManager.saveLoginResult(
                                            com.gem.neteasecloudmd.api.LoginResult(
                                                code = loginResult.code,
                                                profile = profile,
                                                cookie = loginResult.cookie
                                            ),
                                            cookie
                                        )
                                        Toast.makeText(context, "Welcome ${profile?.nickname ?: "User"}!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    },
                                    onFailure = { e ->
                                        val msg = e.message ?: ""
                                        if (msg.contains("400") || msg.contains("安全验证")) {
                                            errorMessage = "需要安全验证，请发送短信验证码登录"
                                        } else {
                                            errorMessage = msg
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && phone.isNotBlank() && password.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                        Text("Login")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = {
                            if (phone.isBlank()) {
                                errorMessage = "Please enter phone"
                                return@TextButton
                            }
                            
                            isLoading = true
                            errorMessage = null
                            
                            CoroutineScope(Dispatchers.Main).launch {
                                val result = withContext(Dispatchers.IO) {
                                    apiService.sendCaptcha(phone)
                                }
                                
                                isLoading = false
                                
                                result.fold(
                                    onSuccess = {
                                        showCaptcha = true
                                        Toast.makeText(context, "验证码已发送", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { e ->
                                        errorMessage = e.message
                                    }
                                )
                            }
                        },
                        enabled = !isLoading && phone.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("发送短信验证码登录")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = { loginMode = "cookie" },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("使用Cookie登录")
                    }
                }
                
                // Cookie 登录模式
                if (loginMode == "cookie") {
                    OutlinedTextField(
                        value = cookie,
                        onValueChange = { cookie = it },
                        label = { Text("Cookie") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = false,
                        minLines = 3,
                        maxLines = 5
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = Color.Red,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if (cookie.isBlank()) {
                                errorMessage = "请输入Cookie"
                                return@Button
                            }
                            
                            isLoading = true
                            errorMessage = null
                            
                            CoroutineScope(Dispatchers.Main).launch {
                                val result = withContext(Dispatchers.IO) {
                                    apiService.loginWithCookie(cookie)
                                }
                                
                                isLoading = false
                                
                                result.fold(
                                    onSuccess = { loginResult ->
                                        sessionManager.saveLoginResult(loginResult, cookie)
                                        Toast.makeText(context, "Welcome ${loginResult.profile?.nickname ?: "User"}!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    },
                                    onFailure = { e ->
                                        errorMessage = e.message
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && cookie.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                color = Color.White
                            )
                        }
                        Text("Cookie登录")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = { loginMode = "password" },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("返回账号密码登录")
                    }
                }
            }
        }
    }
}
