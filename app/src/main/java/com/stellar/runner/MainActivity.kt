package com.stellar.runner

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.stellar.runner.ui.theme.StellarRunnerTheme
import roro.stellar.Stellar
import roro.stellar.StellarHelper

class MainActivity : ComponentActivity() {

    private val sp: SharedPreferences by lazy { getSharedPreferences("data", Context.MODE_PRIVATE) }
    private val activityManager: ActivityManager by lazy { getSystemService()!! }
    private val clipboardManager: ClipboardManager by lazy { getSystemService()!! }
    
    private var stellarRunning = mutableStateOf(true)
    private var stellarAuthorized = mutableStateOf(false)

    private val stellarPermissionListener = Stellar.OnRequestPermissionResultListener { _, _, granted ->
        checkStellarStatus()
        if (granted) {
            Toast.makeText(this, "Stellar授权成功", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (sp.getBoolean("first", true)) {
            showPrivacyDialog()
        }

        activityManager.appTasks.firstOrNull()?.setExcludeFromRecents(sp.getBoolean("hide", true))

        Stellar.addRequestPermissionResultListener(stellarPermissionListener)
        checkStellarStatus()

        setContent {
            StellarRunnerTheme {
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = !isSystemInDarkTheme()
                val backgroundColor = MaterialTheme.colorScheme.background

                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = useDarkIcons
                    )
                }

                MainScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        var showQuickCommand by remember { mutableStateOf(false) }
        var showSettings by remember { mutableStateOf(false) }
        var refreshTrigger by remember { mutableStateOf(0) }
        
        val primaryColor = MaterialTheme.colorScheme.primary
        val errorColor = MaterialTheme.colorScheme.error
        
        val stellarStatus = remember(stellarRunning.value, stellarAuthorized.value) {
            when {
                !stellarRunning.value -> "未运行"
                !stellarAuthorized.value -> "未授权"
                else -> "已激活"
            }
        }
        
        val stellarColor = remember(stellarRunning.value, stellarAuthorized.value) {
            if (stellarRunning.value && stellarAuthorized.value) {
                primaryColor
            } else {
                errorColor
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = stellarColor.copy(alpha = 0.2f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable { checkStellarStatus() },
                                    contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                        imageVector = if (stellarRunning.value && stellarAuthorized.value) {
                                            Icons.Default.CheckCircle
                                        } else {
                                            Icons.Default.Warning
                                        },
                                        contentDescription = null,
                                        tint = stellarColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            
                            Column {
                                Text(
                                    "Stellar Runner",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Stellar $stellarStatus",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = stellarColor
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, "设置")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showQuickCommand = true },
                    icon = { Icon(Icons.Default.PlayArrow, "快速执行") },
                    text = { Text("快速执行") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                CommandListScreen(refreshTrigger)
            }
        }

        if (showQuickCommand) {
            QuickCommandDialog(onDismiss = { showQuickCommand = false })
        }

        if (showSettings) {
            SettingsDialog(
                onDismiss = { showSettings = false },
                onRefresh = { refreshTrigger++ }
            )
        }
    }

    @Composable
    fun CommandListScreen(refreshTrigger: Int) {
        var displayCount by remember { mutableStateOf(20) }
        val listState = rememberLazyGridState()

        LaunchedEffect(listState) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                .collect { lastVisibleIndex ->
                    if (lastVisibleIndex != null && lastVisibleIndex >= displayCount - 4) {
                        displayCount += 20
                    }
                }
        }

        LaunchedEffect(refreshTrigger) {
            displayCount = 20
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(displayCount, key = { it }) { index ->
                CommandCard(index, onRefresh = { /* 触发刷新 */ })
            }
        }
    }

    @Composable
    fun CommandCard(index: Int, onRefresh: () -> Unit) {
        val prefs = getSharedPreferences(index.toString(), Context.MODE_PRIVATE)
        var showEditDialog by remember { mutableStateOf(false) }
        var showExecuteDialog by remember { mutableStateOf(false) }
        var content by remember { mutableStateOf(prefs.getString("content", "").orEmpty()) }
        var name by remember { mutableStateOf(prefs.getString("name", "").orEmpty()) }
        val hasContent = content.isNotEmpty()

        LaunchedEffect(Unit) {
            content = prefs.getString("content", "").orEmpty()
            name = prefs.getString("name", "").orEmpty()
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .animateContentSize(),
            onClick = { showEditDialog = true },
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (hasContent) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (name.isNotEmpty()) name else "未命名",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasContent) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (content.isNotEmpty()) {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "点击添加",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }

                FilledIconButton(
                    onClick = {
                        when {
                            !hasContent -> showEditDialog = true
                            stellarRunning.value && stellarAuthorized.value -> showExecuteDialog = true
                            else -> checkStellarStatus()
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (hasContent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (hasContent) Icons.Default.PlayArrow else Icons.Default.Add,
                        contentDescription = if (hasContent) "运行" else "添加"
                    )
                }
            }
        }

        if (showEditDialog) {
            EditCommandDialog(
                prefs = prefs,
                initialName = name,
                initialContent = content,
                onDismiss = { showEditDialog = false },
                onSave = { newName, newContent ->
                    name = newName
                    content = newContent
                }
            )
        }

        if (showExecuteDialog) {
            ExecuteCommandDialog(
                command = content,
                onDismiss = { showExecuteDialog = false }
            )
        }
    }

    @Composable
    fun EditCommandDialog(
        prefs: SharedPreferences,
        initialName: String,
        initialContent: String,
        onDismiss: () -> Unit,
        onSave: (String, String) -> Unit
    ) {
        var name by remember { mutableStateOf(initialName) }
        var content by remember { mutableStateOf(initialContent) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("编辑命令") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("命令标题") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("命令内容") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        prefs.edit {
                            putString("name", name)
                            putString("content", content)
                        }
                        onSave(name, content)
                        onDismiss()
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ExecuteCommandDialog(command: String, onDismiss: () -> Unit) {
        var output by remember { mutableStateOf("执行中...\n") }
        var exitValue by remember { mutableStateOf<Int?>(null) }
        var executionTime by remember { mutableStateOf<Float?>(null) }
        var isRunning by remember { mutableStateOf(true) }
        val scrollState = rememberScrollState()

        LaunchedEffect(command) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                try {
                    val process = roro.stellar.Stellar.newProcess(arrayOf("sh"), null, null)
                    
                    process?.outputStream?.use { out ->
                        out.write("$command\n".toByteArray())
                        out.flush()
                    }

                    val outputBuilder = StringBuilder()
                    val errorBuilder = StringBuilder()

                    val outputThread = Thread {
                        process?.inputStream?.bufferedReader()?.use { reader ->
                            reader.lineSequence().forEach { line ->
                                outputBuilder.append("$line\n")
                                kotlinx.coroutines.runBlocking {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        output = outputBuilder.toString()
                                    }
                                }
                            }
                        }
                    }

                    val errorThread = Thread {
                        process?.errorStream?.bufferedReader()?.use { reader ->
                            reader.lineSequence().forEach { line ->
                                errorBuilder.append("[ERROR] $line\n")
                                kotlinx.coroutines.runBlocking {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        output = outputBuilder.toString() + errorBuilder.toString()
                                    }
                                }
                            }
                        }
                    }

                    outputThread.start()
                    errorThread.start()

                    process?.waitFor()
                    outputThread.join()
                    errorThread.join()

                    val exit = process?.exitValue() ?: -1
                    val time = (System.currentTimeMillis() - startTime) / 1000f

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        exitValue = exit
                        executionTime = time
                        isRunning = false
                        if (outputBuilder.isEmpty() && errorBuilder.isEmpty()) {
                            output = "命令执行完成，无输出"
                        }
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        output += "\n[ERROR] 错误: ${e.message}"
                        isRunning = false
                    }
                }
            }
        }

        LaunchedEffect(output) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }

        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 标题栏
                    Surface(
                        color = if (isRunning) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else if (exitValue == 0) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isRunning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (exitValue == 0) {
                                            Icons.Default.CheckCircle
                                        } else {
                                            Icons.Default.Error
                                        },
                                        contentDescription = null,
                                        tint = if (exitValue == 0) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        },
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                
                                Column {
                                    Text(
                                        text = if (isRunning) "执行中..." else "执行完成",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRunning) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else if (exitValue == 0) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        }
                                    )
                                    if (exitValue != null && executionTime != null) {
                                        Text(
                                            "退出值: $exitValue · 用时: %.2fs".format(executionTime),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (exitValue == 0) {
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            } else {
                                                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                            }
                                        )
                                    }
                                }
                            }
                            
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = if (isRunning) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else if (exitValue == 0) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                            }
                        }
                    }

                    // 命令显示
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = command,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 输出区域
                    Text(
                        text = "输出",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(16.dp)
                        ) {
                            if (output.isEmpty() || output == "执行中...\n") {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "等待输出...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text = output,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    // 底部按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledTonalButton(
                            onClick = onDismiss,
                            enabled = !isRunning
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("完成")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun QuickCommandDialog(onDismiss: () -> Unit) {
        var command by remember { mutableStateOf("") }
        var showExecute by remember { mutableStateOf(false) }

        if (!showExecute) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("快速执行") },
                text = {
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text("输入命令") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { Text("例如: ls -la") },
                        minLines = 3
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (command.isNotEmpty()) {
                                showExecute = true
                            }
                        },
                        enabled = command.isNotEmpty()
                    ) {
                        Text("执行")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            )
        } else {
            ExecuteCommandDialog(
                command = command,
                onDismiss = onDismiss
            )
        }
    }

    @Composable
    fun SettingsDialog(onDismiss: () -> Unit, onRefresh: () -> Unit) {
        var hideFromRecents by remember { mutableStateOf(sp.getBoolean("hide", true)) }
        var showExport by remember { mutableStateOf(false) }
        var showImport by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("设置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("隐藏最近任务", fontWeight = FontWeight.Medium)
                            Text(
                                "在任务列表中隐藏此应用",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = hideFromRecents,
                            onCheckedChange = {
                                hideFromRecents = it
                                sp.edit { putBoolean("hide", it) }
                                activityManager.appTasks.firstOrNull()?.setExcludeFromRecents(it)
                            }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showExport = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("导出")
                        }

                        OutlinedButton(
                            onClick = { showImport = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("导入")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        )

        if (showExport) {
            ExportDialog(onDismiss = { showExport = false })
        }

        if (showImport) {
            ImportDialog(
                onDismiss = { showImport = false },
                onSuccess = onRefresh
            )
        }
    }

    @Composable
    fun ExportDialog(onDismiss: () -> Unit) {
        val exportText = remember {
            buildString {
                var index = 0
                while (index < 1000) {
                    val prefs = getSharedPreferences(index.toString(), Context.MODE_PRIVATE)
                    val content = prefs.getString("content", "").orEmpty()
                    val name = prefs.getString("name", "").orEmpty()
                    
                    if (content.isNotEmpty()) {
                        append("$index.${name.ifEmpty { "空" }}\n$content\n\n")
                    }
                    index++
                }
            }
        }
        val scrollState = rememberScrollState()

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("导出备份") },
            text = {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = exportText.ifEmpty { "没有可导出的命令" },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setPrimaryClip(ClipData.newPlainText("c", exportText))
                        Toast.makeText(this@MainActivity, "已复制至剪切板", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                ) {
                    Text("复制")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        )
    }

    @Composable
    fun ImportDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
        var importText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("导入备份") },
            text = {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = { Text("粘贴备份内容") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    placeholder = { Text("粘贴从导出功能复制的内容") },
                    minLines = 6
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (importText.isEmpty()) {
                            Toast.makeText(this@MainActivity, "导入内容为空", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }

                        var position = 0
                        var currentName = ""
                        
                        importText.lines().forEach { line ->
                            when {
                                line.isEmpty() -> return@forEach
                                line.matches(Regex("\\d+\\..*")) -> {
                                    currentName = line.substringAfter(".")
                                }
                                else -> {
                                    while (true) {
                                        val prefs = getSharedPreferences(position.toString(), Context.MODE_PRIVATE)
                                        if (prefs.getString("content", "").orEmpty().isEmpty()) {
                                            prefs.edit {
                                                putString("name", currentName)
                                                putString("content", line)
                                            }
                                            break
                                        }
                                        position++
                                    }
                                }
                            }
                        }
                        
                        Toast.makeText(this@MainActivity, "导入成功！", Toast.LENGTH_SHORT).show()
                        onSuccess()
                        onDismiss()
                    },
                    enabled = importText.isNotEmpty()
                ) {
                    Text("导入")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }

    private fun showPrivacyDialog() {
        Toast.makeText(this, "首次使用，请查看隐私政策", Toast.LENGTH_LONG).show()
        sp.edit { putBoolean("first", false) }
    }

    private fun checkStellarStatus() {
        stellarRunning.value = true
        stellarAuthorized.value = false

        when {
            !StellarHelper.isManagerInstalled(this) -> {
                stellarRunning.value = false
                Toast.makeText(this, "Stellar未安装", Toast.LENGTH_SHORT).show()
            }
            !Stellar.pingBinder() -> {
                stellarRunning.value = false
                Toast.makeText(this, "Stellar未运行", Toast.LENGTH_SHORT).show()
            }
            else -> {
                runCatching {
                    if (!Stellar.checkSelfPermission("stellar")) {
                        Stellar.requestPermission("stellar", 0)
                    } else {
                        stellarAuthorized.value = true
                    }
                }.onFailure { e ->
                    Toast.makeText(this, "权限检查失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        Stellar.removeRequestPermissionResultListener(stellarPermissionListener)
        super.onDestroy()
    }
}
