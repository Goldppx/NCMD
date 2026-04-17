package com.gem.neteasecloudmd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gem.neteasecloudmd.R
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
    val resources = LocalResources.current
    val apiService = remember { NeteaseApiService(context) }
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
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 16.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.displayCutout),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.3f))
        
        Text(
            text = stringResource(R.string.main_title),
            style = MaterialTheme.typography.headlineLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(stringResource(R.string.login_phone_label)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (loginMode) {
            LoginMode.PASSWORD -> {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.login_password_label)) },
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
                            errorMessage = resources.getString(R.string.login_enter_phone)
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
                                    if (msg.contains("400") || msg.contains("security", ignoreCase = true)) {
                                        errorMessage = resources.getString(R.string.login_need_security_verify)
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: resources.getString(R.string.login_failed)
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
                        Text(stringResource(R.string.common_login))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = {
                        if (phone.isBlank()) {
                            errorMessage = resources.getString(R.string.login_enter_phone)
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
                    Text(stringResource(R.string.login_send_sms))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { loginMode = LoginMode.COOKIE },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.login_use_cookie))
                }
            }
            
            LoginMode.CAPTCHA -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = captcha,
                        onValueChange = { captcha = it },
                        label = { Text(stringResource(R.string.login_captcha_label)) },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (phone.isBlank()) {
                                errorMessage = resources.getString(R.string.login_enter_phone)
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
                        Text(stringResource(R.string.login_verify))
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
                    Text(stringResource(R.string.login_back_to_password))
                }
            }
            
            LoginMode.COOKIE -> {
                OutlinedTextField(
                    value = cookie,
                    onValueChange = { cookie = it },
                    label = { Text(stringResource(R.string.login_cookie_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5,
                    placeholder = { Text(stringResource(R.string.login_cookie_placeholder)) }
                )
                
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (cookie.isBlank()) {
                            errorMessage = resources.getString(R.string.login_enter_cookie)
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
                        Text(stringResource(R.string.login_cookie_login))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { loginMode = LoginMode.PASSWORD },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.login_back_to_password))
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(0.7f))
    }
}
