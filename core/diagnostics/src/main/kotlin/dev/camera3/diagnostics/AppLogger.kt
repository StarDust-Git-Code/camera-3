package dev.camera3.diagnostics

import android.content.Context
import android.util.Log
import dev.camera3.model.LogEvent
import dev.camera3.model.LogLevel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/** Application-scoped writer. Camera callbacks only enqueue bounded work. */
class AppLogger(context: Context, scope: CoroutineScope) {
    private sealed interface Command {
        data class Write(val event: LogEvent) : Command
        data class Clear(val done: CompletableDeferred<Unit>) : Command
        data class Export(val header: String, val done: CompletableDeferred<File>) : Command
    }
    private val directory = File(context.filesDir, "logs")
    private val exports = File(context.cacheDir, "exports")
    private val queue = Channel<Command>(256)
    private val sequence = AtomicLong()
    private val dropped = AtomicLong()
    private val _events = MutableStateFlow<List<LogEvent>>(emptyList())
    val events = _events.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            directory.mkdirs()
            for (command in queue) {
                try {
                    when (command) {
                        is Command.Write -> {
                            val lost = dropped.getAndSet(0)
                            if (lost > 0) append(LogEvent(sequence.incrementAndGet(), System.currentTimeMillis(),
                                LogLevel.WARNING, "Logger", "$lost events dropped because the log queue was full"))
                            append(command.event)
                        }
                        is Command.Clear -> {
                            directory.listFiles()?.filter { it.name.startsWith("camera-") }?.forEach {
                                check(it.delete()) { "Could not delete a log file" }
                            }
                            _events.value = emptyList()
                            command.done.complete(Unit)
                        }
                        is Command.Export -> {
                            exports.mkdirs()
                            exports.listFiles()?.filter { it.name.startsWith("camera3-") }?.forEach { it.delete() }
                            val file = File(exports, "camera3-${System.currentTimeMillis()}.txt")
                            file.bufferedWriter().use { writer ->
                                writer.appendLine(command.header)
                                writer.appendLine("\nRecent application logs (no media or serial numbers):")
                                directory.listFiles()?.filter { it.name.startsWith("camera-") }
                                    ?.sortedByDescending { it.name }?.forEach { log ->
                                        log.bufferedReader().useLines { lines -> lines.forEach { writer.appendLine(it) } }
                                    }
                            }
                            command.done.complete(file)
                        }
                    }
                } catch (error: Exception) {
                    Log.e("Camera3/Logger", "Log storage operation failed", error)
                    when (command) {
                        is Command.Clear -> command.done.completeExceptionally(error)
                        is Command.Export -> command.done.completeExceptionally(error)
                        is Command.Write -> _events.value = (_events.value + command.event).takeLast(500)
                    }
                }
            }
        }
    }

    fun log(level: LogLevel, tag: String, message: String, error: Throwable? = null) {
        // Bounded strings and no user-media URIs/paths in routine logs.
        val detail = (message + (error?.let { "\n${it.javaClass.simpleName}: ${it.message}\n${it.stackTrace.take(8).joinToString("\n")}" } ?: "")).take(6000)
        val priority = when (level) {
            LogLevel.DEBUG -> Log.DEBUG; LogLevel.INFO -> Log.INFO
            LogLevel.WARNING -> Log.WARN; LogLevel.ERROR -> Log.ERROR
        }
        Log.println(priority, "Camera3/$tag", detail)
        if (queue.trySend(Command.Write(LogEvent(sequence.incrementAndGet(), System.currentTimeMillis(), level, tag, detail))).isFailure) {
            dropped.incrementAndGet()
        }
    }

    suspend fun clear() {
        val done = CompletableDeferred<Unit>()
        queue.send(Command.Clear(done)); done.await()
    }
    suspend fun export(header: String): File {
        val done = CompletableDeferred<File>()
        queue.send(Command.Export(header, done)); return done.await()
    }

    private fun append(event: LogEvent) {
        _events.value = (_events.value + event).takeLast(500)
        val active = File(directory, "camera-0.log")
        if (active.length() >= 1_048_576) {
            File(directory, "camera-2.log").let { if (it.exists()) check(it.delete()) }
            File(directory, "camera-1.log").let { if (it.exists()) check(it.renameTo(File(directory, "camera-2.log"))) }
            check(active.renameTo(File(directory, "camera-1.log")))
        }
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(event.timeMillis))
        active.appendText("$timestamp ${event.level} ${event.tag}: ${event.message}\n")
    }
}
