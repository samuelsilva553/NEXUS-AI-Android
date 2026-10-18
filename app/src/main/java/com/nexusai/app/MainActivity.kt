package com.nexusai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Bg = Color(0xFF0A0A0A)
private val Panel = Color(0xFF121212)
private val Green = Color(0xFF00FF88)
private val Emerald = Color(0xFF00B86B)
private val TextMain = Color(0xFFF4FFF8)
private val Muted = Color(0xFF89968F)
private val Border = Color(0xFF26382F)

private enum class Page(val label: String) {
    HOME("Início"), CHAT("Chat"), IMAGES("Imagens"),
    HISTORY("Histórico"), SETTINGS("Ajustes")
}

data class ChatMessage(val text: String, val user: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(10, 10, 10)
        window.navigationBarColor = android.graphics.Color.rgb(10, 10, 10)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Bg,
                    surface = Panel,
                    primary = Green,
                    onPrimary = Bg,
                    onBackground = TextMain,
                    onSurface = TextMain
                )
            ) {
                NexusApp()
            }
        }
    }
}

@Composable
private fun NexusLogo(modifier: Modifier = Modifier, rounded: Boolean = true) {
    ImageLogo(modifier = modifier, rounded = rounded)
}

@Composable
private fun ImageLogo(modifier: Modifier = Modifier, rounded: Boolean = true) {
    androidx.compose.foundation.Image(
        painter = painterResource(R.drawable.nexus_logo),
        contentDescription = "Logo NEXUS",
        modifier = if (rounded) modifier.clip(RoundedCornerShape(18.dp)) else modifier,
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun NexusApp() {
    var page by remember { mutableStateOf(Page.HOME) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val history = remember { mutableStateListOf<String>() }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var keepHistory by remember { mutableStateOf(true) }
    var showAbout by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun sendMessage(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty() || loading) return

        messages.add(ChatMessage(prompt, true))
        if (keepHistory) history.add(prompt)
        input = ""
        error = ""
        page = Page.CHAT
        loading = true

        scope.launch {
            val result = withContext(Dispatchers.IO) { askNexus(prompt) }
            if (result.startsWith("ERRO:")) {
                error = result.removePrefix("ERRO:")
            } else {
                messages.add(ChatMessage(result, false))
                if (keepHistory) history.add(result)
            }
            loading = false
        }
    }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF101310),
                contentColor = Green,
                tonalElevation = 0.dp
            ) {
                val icons = listOf(
                    Icons.Default.Home,
                    Icons.Default.ChatBubbleOutline,
                    Icons.Default.AutoAwesome,
                    Icons.Default.History,
                    Icons.Default.Settings
                )
                Page.values().forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = page == item,
                        onClick = { page = item; error = "" },
                        icon = {
                            Icon(icons[index], contentDescription = item.label)
                        },
                        label = {
                            Text(item.label, fontSize = 10.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Green,
                            selectedTextColor = Green,
                            indicatorColor = Color(0xFF173B29),
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0B100D), Bg, Bg)
                    )
                )
                .padding(padding)
        ) {
            Header(
                onNewChat = {
                    messages.clear()
                    error = ""
                    page = Page.HOME
                }
            )

            when (page) {
                Page.HOME -> {
                    HomeScreen(
                        input = input,
                        onInputChange = { input = it },
                        onSend = { sendMessage(input) },
                        onSuggestion = { sendMessage(it) }
                    )
                }

                Page.CHAT -> {
                    ChatScreen(
                        messages = messages,
                        input = input,
                        onInputChange = { input = it },
                        loading = loading,
                        error = error,
                        onSend = { sendMessage(input) }
                    )
                }

                Page.IMAGES -> {
                    ImagesScreen()
                }

                Page.HISTORY -> {
                    HistoryScreen(
                        history = history,
                        onSelect = { item ->
                            if (item.isNotBlank()) {
                                input = item
                                page = Page.HOME
                            }
                        },
                        onClear = { history.clear() }
                    )
                }

                Page.SETTINGS -> {
                    SettingsScreen(
                        keepHistory = keepHistory,
                        onHistoryChange = { keepHistory = it },
                        onAbout = { showAbout = true }
                    )
                }
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            containerColor = Panel,
            title = { Text("NEXUS AI", color = Green) },
            text = {
                Text(
                    "Assistente local criado por Samuca. " +
                        "O modelo é executado no Termux deste dispositivo.",
                    color = TextMain
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("Fechar", color = Green)
                }
            }
        )
    }
}

@Composable
private fun Header(onNewChat: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NexusLogo(Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "NEXUS AI",
                color = TextMain,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingDot()
                Spacer(Modifier.width(6.dp))
                Text(
                    "SISTEMA INTELIGENTE",
                    color = Muted,
                    fontSize = 9.sp,
                    letterSpacing = 1.3.sp
                )
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onNewChat) {
            Icon(Icons.Default.Add, "Nova conversa", tint = Green)
        }
    }
    HorizontalDivider(color = Border.copy(alpha = 0.7f))
}

@Composable
private fun PulsingDot() {
    val transition = rememberInfiniteTransition(label = "nexusPulse")
    val scale by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )
    Box(
        Modifier.size(7.dp).scale(scale)
            .background(Green, CircleShape)
    )
}

@Composable
private fun HomeScreen(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestion: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                NexusLogo(Modifier.size(180.dp), rounded = false)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "SUA IDEIA. NOSSA CONEXÃO.",
                Modifier.fillMaxWidth(),
                color = Green,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Olá, vamos criar\nalgo extraordinário?",
                color = TextMain,
                fontSize = 29.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Converse, explore ideias e programe com a NEXUS.",
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }

        item {
            MessageInput(
                input = input,
                onInputChange = onInputChange,
                onSend = onSend,
                placeholder = "Pergunte ao NEXUS..."
            )
        }

        item {
            Text(
                "EXPLORE",
                color = Muted,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            SuggestionCard(
                icon = Icons.Default.Code,
                title = "Programação",
                subtitle = "Crie e corrija seus códigos",
                onClick = { onSuggestion("Ajude-me a criar um programa e explique o código.") }
            )
        }
        item {
            SuggestionCard(
                icon = Icons.Default.Lightbulb,
                title = "Novas ideias",
                subtitle = "Transforme uma ideia em projeto",
                onClick = { onSuggestion("Me dê uma ideia de projeto tecnológico e explique como começar.") }
            )
        }
        item {
            SuggestionCard(
                icon = Icons.Default.School,
                title = "Estudar",
                subtitle = "Aprenda com explicações claras",
                onClick = { onSuggestion("Como você pode me ajudar a estudar?") }
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp)
                .background(Color(0xFF123322), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Green)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        Icon(Icons.Default.ArrowForward, null, tint = Green)
    }
}

@Composable
private fun ChatScreen(
    messages: List<ChatMessage>,
    input: String,
    onInputChange: (String) -> Unit,
    loading: Boolean,
    error: String,
    onSend: () -> Unit
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(messages.size, loading) {
        val last = messages.size + if (loading) 0 else -1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        "Conversa nova",
                        color = Muted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            items(messages) { msg ->
                if (msg.user) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            msg.text,
                            Modifier.widthIn(max = 310.dp)
                                .background(
                                    Brush.linearGradient(listOf(Green, Emerald)),
                                    RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = 18.dp,
                                        bottomEnd = 5.dp
                                    )
                                )
                                .padding(14.dp),
                            color = Bg,
                            fontSize = 15.sp,
                            lineHeight = 23.sp
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.Top) {
                        NexusLogo(Modifier.size(34.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            msg.text,
                            Modifier.weight(1f)
                                .background(Panel, RoundedCornerShape(15.dp))
                                .border(1.dp, Border, RoundedCornerShape(15.dp))
                                .padding(14.dp),
                            color = TextMain,
                            fontSize = 15.sp,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            if (loading) {
                item {
                    Row(
                        Modifier.fillMaxWidth()
                            .background(Panel, RoundedCornerShape(15.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PulsingDot()
                        Spacer(Modifier.width(10.dp))
                        Text("NEXUS está pensando...", color = Green, fontSize = 13.sp)
                    }
                }
            }
        }

        if (error.isNotBlank()) {
            Text(
                error,
                color = Color(0xFFFF8A80),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                fontSize = 12.sp
            )
        }

        MessageInput(
            input = input,
            onInputChange = onInputChange,
            onSend = onSend,
            placeholder = "Pergunte ao NEXUS..."
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun MessageInput(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String
) {
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .border(1.dp, Border, RoundedCornerShape(27.dp))
            .background(Panel, RoundedCornerShape(27.dp))
            .padding(start = 17.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f).padding(vertical = 11.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TextMain,
                fontSize = 15.sp
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Green),
            decorationBox = { inner ->
                Box {
                    if (input.isEmpty()) {
                        Text(placeholder, color = Muted, fontSize = 14.sp)
                    }
                    inner()
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
        )

        IconButton(
            onClick = onSend,
            enabled = input.isNotBlank(),
            modifier = Modifier.size(43.dp)
                .background(
                    if (input.isNotBlank()) Green else Color(0xFF304137),
                    CircleShape
                )
        ) {
            Icon(Icons.Default.ArrowUpward, "Enviar", tint = Bg)
        }
    }
}

@Composable
private fun ImagesScreen() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(100.dp)
                .background(Color(0xFF123322), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                null,
                tint = Green,
                modifier = Modifier.size(50.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Geração de imagens",
            color = TextMain,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "O espaço para criar imagens está preparado. " +
                "O modelo local atual é de texto e ainda não gera imagens.",
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "RECURSO EM DESENVOLVIMENTO",
            color = Green,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun HistoryScreen(
    history: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Histórico", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Mensagens desta sessão", color = Muted, fontSize = 12.sp)
            }
            TextButton(onClick = onClear) {
                Text("Limpar", color = Green)
            }
        }
        Spacer(Modifier.height(18.dp))

        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, tint = Muted, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Nenhuma mensagem salva", color = Muted)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history.reversed()) { item ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Panel)
                            .clickable { onSelect(item) }
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, null, tint = Green)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            item,
                            Modifier.weight(1f),
                            color = TextMain,
                            maxLines = 3,
                            fontSize = 14.sp
                        )
                        Icon(Icons.Default.ChevronRight, null, tint = Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    keepHistory: Boolean,
    onHistoryChange: (Boolean) -> Unit,
    onAbout: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Configurações", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Personalize sua experiência", color = Muted, fontSize = 13.sp)

        Row(
            Modifier.fillMaxWidth()
                .background(Panel, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.History, null, tint = Green)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Histórico da sessão", color = TextMain, fontWeight = FontWeight.Medium)
                Text("Manter mensagens enquanto o app está aberto", color = Muted, fontSize = 11.sp)
            }
            Switch(
                checked = keepHistory,
                onCheckedChange = onHistoryChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Bg, checkedTrackColor = Green)
            )
        }

        Row(
            Modifier.fillMaxWidth()
                .background(Panel, RoundedCornerShape(16.dp))
                .clickable(onClick = onAbout)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, null, tint = Green)
            Spacer(Modifier.width(12.dp))
            Text("Sobre a NEXUS AI", Modifier.weight(1f), color = TextMain)
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }

        Row(
            Modifier.fillMaxWidth()
                .background(Panel, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Memory, null, tint = Green)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Motor de IA local", color = TextMain, fontWeight = FontWeight.Medium)
                Text("Conectado ao servidor do Termux", color = Muted, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            "NEXUS AI • Versão 1.0.0",
            Modifier.fillMaxWidth(),
            color = Muted,
            fontSize = 11.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
