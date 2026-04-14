package com.gem.neteasecloudmd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gem.neteasecloudmd.api.NeteaseApiService
import com.gem.neteasecloudmd.api.SessionManager
import kotlinx.coroutines.launch

enum class LoginMode {
    PASSWORD, CAPTCHA, COOKIE
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val apiService = remember { NeteaseApiService() }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var captcha by remember { mutableStateOf("") }
    var cookie by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCaptcha by remember { mutableStateOf(false) }
    var loginMode by remember { mutableStateOf(LoginMode.PASSWORD) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "NCMD",
            style = MaterialTheme.typography.headlineLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("手机号") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (loginMode) {
            LoginMode.PASSWORD -> {
                // 密码登录
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
                
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (phone.isBlank()) {
                            errorMessage = "请输入手机号"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val result = apiService.login(phone, password)
                                result.onSuccess { loginResult ->
                                    val cookieStr = loginResult.cookie?.joinToString(";") ?: ""
                                    sessionManager.saveLoginResult(loginResult, cookieStr)
                                    onLoginSuccess()
                                }.onFailure { e ->
                                    val msg = e.message ?: ""
                                    if (msg.contains("400") || msg.contains("安全验证")) {
                                        errorMessage = "需要安全验证，请发送短信验证码登录"
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "登录失败"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && phone.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("登录")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = {
                        if (phone.isBlank()) {
                            errorMessage = "请输入手机号"
                            return@TextButton
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val result = apiService.sendCaptcha(phone)
                                result.onSuccess {
                                    showCaptcha = true
                                    loginMode = LoginMode.CAPTCHA
                                }.onFailure { e ->
                                    errorMessage = e.message
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && phone.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("发送短信验证码登录")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { loginMode = LoginMode.COOKIE },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("使用Cookie登录")
                }
            }
            
            LoginMode.CAPTCHA -> {
                // 验证码登录
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = captcha,
                        onValueChange = { captcha = it },
                        label = { Text("验证码") },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (phone.isBlank()) {
                                errorMessage = "请输入手机号"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val result = apiService.verifyCaptchaAndLogin(phone, captcha)
                                    result.onSuccess { loginResult ->
                                        val cookieStr = loginResult.cookie?.joinToString(";") ?: ""
                                        sessionManager.saveLoginResult(loginResult, cookieStr)
                                        onLoginSuccess()
                                    }.onFailure { e ->
                                        errorMessage = e.message
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading && captcha.isNotBlank(),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("验证")
                    }
                }
                
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { loginMode = LoginMode.PASSWORD },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("返回账号密码登录")
                }
            }
            
            LoginMode.COOKIE -> {
                // Cookie登录
                OutlinedTextField(
                    value = cookie,
                    onValueChange = { cookie = it },
                    label = { Text("Cookie") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5,
                    placeholder = { Text("请输入Cookie") }
                )
                
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (cookie.isBlank()) {
                            errorMessage = "请输入Cookie"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val result = apiService.loginWithCookie(cookie)
                                result.onSuccess { loginResult ->
                                    sessionManager.saveLoginResult(loginResult, cookie)
                                    onLoginSuccess()
                                }.onFailure { e ->
                                    errorMessage = e.message
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && cookie.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Cookie登录")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { loginMode = LoginMode.PASSWORD },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("返回账号密码登录")
                }
            }
        }
    }
}
