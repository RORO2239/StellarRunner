package com.stellar.runner

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import roro.stellar.Stellar
import kotlin.concurrent.thread

class ExecNoDisplay : ComponentActivity() {

    private var process: Process? = null
    private var executionThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        executionThread = thread {
            executeCommand(intent.getStringExtra("content").orEmpty())
        }
        
        finish()
    }

    private fun executeCommand(command: String) {
        runCatching {
            process = Stellar.newProcess(arrayOf("sh"), null, null)
            
            process?.outputStream?.use { output ->
                output.write("$command\n".toByteArray())
                output.flush()
            }
            
            process?.waitFor()
        }.onFailure { e ->
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        runCatching {
            process?.destroyProcess()
            executionThread?.interrupt()
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
