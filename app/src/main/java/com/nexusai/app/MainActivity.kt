package com.nexusai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Bg = Color(0xFF080B0A)
private val Panel = Color(0xFF111A15)
private val Green = Color(0xFF00E676)
private val TextMain = Color(0xFFE5F5EB)
private val Muted = Color(0xFF8BA99A)
private val Border = Color(0xFF244532)

data class ChatMessage(val text: String, val user: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 11, 10)
        window.navigationBarColor = android.graphics.Color.rgb(8, 11, 10)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Bg,
                    surface = Panel,
                    primary = Green,
                    onBackground = TextMain,
                    onSurface = TextMain
                )
            ) {
                NexusScreen()
            }
        }
    }
}

@Composable
private fun NexusScreen() {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun send() {
        val prompt = input.trim()
        if (prompt.isEmpty() || loading) return

        messages.add(ChatMessage(prompt, true))
        input = ""
        error = ""
        loading = true

        scope.launch {
            val result = withContext(Dispatchers.IO) { askNexus(prompt) }
            if (result.startsWith("ERRO:")) {
                error = result.removePrefix("ERRO:")
            } else {
                messages.add(ChatMessage(result, false))
            }
            loading = false
        }
    }

    LaunchedEffect(messages.size, loading) {
        val last = messages.size + if (loading) 0 else -1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Column(
        Modifier.fillMaxSize().background(Bg)
    ) {
        Row(
            Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).background(Color(0xFF0D2B1C), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Memory, null, tint = Green, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("NEXUS AI", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(Green, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("INTELLIGENCE SYSTEM", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                messages.clear()
                error = ""
            }) {
                Icon(Icons.Default.Add, "Nova conversa", tint = Green)
            }
        }

        HorizontalDivider(color = Border)

        if (messages.isEmpty()) {
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(82.dp).background(Color(0xFF0D2B1C), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Terminal, null, tint = Green, modifier = Modifier.size(43.dp))
                    }
                    Spacer(Modifier.height(25.dp))
                    Text("SISTEMA PRONTO", color = Green, fontSize = 11.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Como posso ajudar?", color = TextMain, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Sua inteligência. Seu espaço. Sua NEXUS.",
                        color = Muted, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(28.dp))
                    Row(
                        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Memory, null, tint = Green, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("NEXUS LOCAL", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Conexão com o modelo no dispositivo", color = Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                items(messages) { msg ->
                    if (msg.user) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text(
                                msg.text,
                                Modifier.widthIn(max = 310.dp)
                                    .background(Panel, RoundedCornerShape(18.dp))
                                    .padding(14.dp),
                                color = TextMain, fontSize = 15.sp, lineHeight = 23.sp
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier.size(30.dp).background(Color(0xFF0D2B1C), RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("N", color = Green, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(11.dp))
                            Text(
                                msg.text,
                                Modifier.weight(1f).padding(top = 3.dp),
                                color = TextMain, fontSize = 15.sp, lineHeight = 24.sp
                            )
                        }
                    }
                }
                if (loading) {
                    item {
                        Text("NEXUS AI está processando...", color = Green, fontSize = 12.sp)
                    }
                }
            }
        }

        if (error.isNotBlank()) {
            Text(error, color = Color(0xFFFF8A80), modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 12.sp)
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                .background(Panel, RoundedCornerShape(25.dp))
                .padding(start = 17.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = TextMain, fontSize = 15.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Green),
                decorationBox = { inner ->
                    Box {
                        if (input.isEmpty()) Text("Envie uma mensagem...", color = Muted, fontSize = 15.sp)
                        inner()
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() })
            )
            IconButton(
                onClick = { send() },
                enabled = input.isNotBlank() && !loading,
                modifier = Modifier.size(42.dp).background(
                    if (input.isNotBlank() && !loading) Green else Color(0xFF304137),
                    CircleShape
                )
            ) {
                Icon(Icons.Default.ArrowUpward, "Enviar", tint = Bg)
            }
        }

        Text(
            "A NEXUS AI pode cometer erros. Verifique informações importantes.",
            Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp),
            color = Muted, fontSize = 9.sp
        )
    }
}

private fun askNexus(message: String): String {
    var connection: HttpURLConnection? = null
    return try {
        connection = (URL("http://127.0.0.1:8000/chat").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 240000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        val body = JSONObject().put("message", message).toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream.bufferedReader().use { it.readText() }

        if (code !in 200..299) {
            "ERRO: O servidor respondeu HTTP $code."
        } else {
            val json = JSONObject(response)
            json.optString("response", json.optString("answer", "Não recebi uma resposta válida."))
        }
    } catch (e: Exception) {
        "ERRO: Não consegui conectar ao servidor NEXUS no Termux. Inicie o servidor e tente novamente. ${e.message ?: ""}"
    } finally {
        connection?.disconnect()
    }
}
