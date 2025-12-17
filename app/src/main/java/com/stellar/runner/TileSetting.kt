package com.stellar.runner

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.stellar.runner.ui.theme.StellarRunnerTheme

class TileSetting : ComponentActivity() {

    private val prefs: SharedPreferences by lazy { 
        getSharedPreferences("tile", Context.MODE_PRIVATE) 
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)

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

                TileSettingScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TileSettingScreen() {
        var command by remember { mutableStateOf(prefs.getString("content", "").orEmpty()) }
        var switchMode by remember { mutableStateOf(prefs.getBoolean("switch", false)) }
        var offCommand by remember { mutableStateOf(prefs.getString("content1", "").orEmpty()) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("磁帖设置") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.Close, "关闭")
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "点击快速设置磁帖后会后台执行设置的命令",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("点击磁帖执行的命令") },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    placeholder = { Text("例如: pm grant com.example.app android.permission.WRITE_SECURE_SETTINGS") }
                )

                FilledTonalButton(
                    onClick = {
                        prefs.edit { putString("content", command) }
                        Toast.makeText(this@TileSetting, "已保存命令", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("保存命令")
                }

                HorizontalDivider()

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("开关模式", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "启用后可设置磁帖关闭时执行的命令",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = switchMode,
                            onCheckedChange = {
                                switchMode = it
                                prefs.edit { putBoolean("switch", it) }
                            }
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = switchMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = offCommand,
                            onValueChange = { offCommand = it },
                            label = { Text("磁帖关闭时执行的命令") },
                            leadingIcon = { Icon(Icons.Default.Stop, null) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            placeholder = { Text("例如: pm revoke com.example.app android.permission.WRITE_SECURE_SETTINGS") }
                        )

                        FilledTonalButton(
                            onClick = {
                                prefs.edit { putString("content1", offCommand) }
                                Toast.makeText(this@TileSetting, "已保存关闭命令", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("保存关闭命令")
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = { finish() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("完成")
                }
            }
        }
    }
}
