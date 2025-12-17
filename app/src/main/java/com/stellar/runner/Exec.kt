package com.stellar.runner

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.stellar.runner.ui.theme.StellarRunnerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import roro.stellar.Stellar
import kotlin.concurrent.thread

class Exec : ComponentActivity() {

    private var process: Process? = null
    private var executionThread: Thread? = null
    private var outputThread: Thread? = null
    private var errorThread: Thread? = null
    private var shouldStop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val command = intent.getStringExtra("content").orEmpty()

        setContent {
            StellarRunnerTheme {
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = !isSystemInDarkTheme()

                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = useDarkIcons
                    )
                }

                ExecScreen(command)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ExecScreen(command: String) {
        var output by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("执行中...") }
        var isRunning by remember { mutableStateOf(true) }
        var exitValue by remember { mutableStateOf<Int?>(null) }
        var executionTime by remember { mutableStateOf<Float?>(null) }
        val scope = rememberCoroutineScope()
        val scrollState = rememberScrollState()

        LaunchedEffect(command) {
            scope.launch {
                executeCommand(
                    command = command,
                    onOutput = { output += it },
                    onComplete = { exit, time ->
                        exitValue = exit
                        executionTime = time
                        status = "执行完毕"
                        isRunning = false
                    }
                )
            }
        }

        LaunchedEffect(output) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(status, style = MaterialTheme.typography.titleMedium)
                            if (exitValue != null && executionTime != null) {
                                Text(
                                    "返回值: $exitValue | 用时: %.2fs".format(executionTime),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (exitValue == 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.Close, "关闭")
                        }
                    },
                    actions = {
                        if (isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 16.dp),
                                strokeWidth = 2.dp
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
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            command,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                    ) {
                        if (output.isEmpty()) {
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun executeCommand(
        command: String,
        onOutput: (String) -> Unit,
        onComplete: (Int, Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val startTime = System.currentTimeMillis()

            process = Stellar.newProcess(arrayOf("sh"), null, null)
            
            process?.outputStream?.use { output ->
                output.write("$command\n".toByteArray())
                output.flush()
            }

            executionThread = thread {
                readOutputStream { onOutput(it) }
            }

            errorThread = thread {
                readErrorStream { onOutput(it) }
            }

            process?.waitFor()

            val exitVal = process?.exitValue() ?: -1
            val execTime = (System.currentTimeMillis() - startTime) / 1000f

            withContext(Dispatchers.Main) {
                onComplete(exitVal, execTime)
            }
        }.onFailure { e ->
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onOutput("\n[ERROR] 错误: ${e.message}\n")
            }
        }
    }

    private fun readOutputStream(onOutput: (String) -> Unit) {
        runCatching {
            process?.inputStream?.bufferedReader()?.use { reader ->
                reader.lineSequence().forEach { line ->
                    if (shouldStop) return@use
                    onOutput(if (line.isEmpty()) "\n" else "$line\n")
                }
            }
        }
    }

    private fun readErrorStream(onOutput: (String) -> Unit) {
        runCatching {
            process?.errorStream?.bufferedReader()?.use { reader ->
                reader.lineSequence().forEach { line ->
                    if (shouldStop) return@use
                    onOutput(if (line.isEmpty()) "\n" else "[ERROR] $line\n")
                }
            }
        }
    }

    override fun onDestroy() {
        shouldStop = true
        
        runCatching {
            process?.destroyProcess()
            executionThread?.interrupt()
            outputThread?.interrupt()
            errorThread?.interrupt()
        }

        super.onDestroy()
    }

    private fun Process.destroyProcess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            destroyForcibly()
        } else {
            destroy()
        }
    }
}
