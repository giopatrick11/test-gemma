package com.example.gemmaphonetest

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.File
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private var engine: Engine? = null
    private lateinit var input: EditText
    private lateinit var analyze: Button
    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        input = EditText(this).apply {
            setText("Analyze this message: I cannot log in to my account even though my password is correct.")
            minLines = 3
            setSingleLine(false)
            hint = "Message to analyze"
        }
        analyze = Button(this).apply { text = "Analyze" }
        output = TextView(this).apply {
            text = "Place the model in the app's files directory, then tap Analyze."
            textSize = 16f
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(input)
            addView(analyze)
            addView(output)
        }
        setContentView(ScrollView(this).apply { addView(content) })
        analyze.setOnClickListener { runInference() }
    }

    private fun runInference() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) {
            output.text = "Enter a message first."
            return
        }
        analyze.isEnabled = false
        output.text = "Loading model / analyzing…"

        worker.execute {
            try {
                val model = File(filesDir, MODEL_FILE)
                if (!model.isFile) {
                    error("Model missing: ${model.absolutePath}")
                }
                if (engine == null) {
                    val start = SystemClock.elapsedRealtime()
                    val loaded = Engine(
                        EngineConfig(
                            modelPath = model.absolutePath,
                            backend = Backend.CPU(),
                            maxNumTokens = 1024,
                            cacheDir = cacheDir.absolutePath,
                        )
                    )
                    try {
                        loaded.initialize()
                        engine = loaded
                        Log.i(TAG, "Model loaded successfully; load time=${SystemClock.elapsedRealtime() - start} ms; bytes=${model.length()}")
                    } catch (error: Throwable) {
                        runCatching { loaded.close() }
                        throw error
                    }
                } else {
                    Log.i(TAG, "Model already loaded; load time=0 ms (reused)")
                }

                val prompt = """
                    Classify and summarize the customer support message below.
                    Reply in exactly two short lines: Category: ... and Summary: ...

                    $message
                """.trimIndent()
                val start = SystemClock.elapsedRealtime()
                val response = requireNotNull(engine).createConversation().use { conversation ->
                    conversation.sendMessage(prompt).toString()
                }
                Log.i(TAG, "Inference time=${SystemClock.elapsedRealtime() - start} ms")
                runOnUiThread { output.text = response.ifBlank { "Model returned an empty response." } }
            } catch (error: Throwable) {
                Log.e(TAG, "Gemma test failed", error)
                runOnUiThread { output.text = "Error: ${error.message ?: error.javaClass.simpleName}" }
            } finally {
                runOnUiThread { analyze.isEnabled = true }
            }
        }
    }

    override fun onDestroy() {
        worker.execute {
            engine?.close()
            engine = null
        }
        worker.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "GemmaPhoneTest"
        private const val MODEL_FILE = "gemma3-1b-it-int4.litertlm"
    }
}
