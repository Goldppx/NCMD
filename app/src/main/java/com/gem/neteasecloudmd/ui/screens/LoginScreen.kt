package com.gem.neteasecloudmd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gem.neteasecloudmd.api.NeteaseApiService
import com.gem.neteasecloudmd.api.SessionManager
import kotlinx.coroutines.launch

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
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                isLoading = true
                errorMessage = null
                scope.launch {
                    try {
                        val result = apiService.login(phone, password)
                        result.onSuccess { loginResult ->
                            val cookie = loginResult.cookie?.joinToString(";") ?: ""
                            sessionManager.saveLoginResult(loginResult, cookie)
                            onLoginSuccess()
                        }.onFailure { e ->
                            errorMessage = e.message ?: "登录失败"
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
    }
}
