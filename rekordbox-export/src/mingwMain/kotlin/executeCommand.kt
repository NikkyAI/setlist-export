import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import platform.posix.FILE
import platform.posix.chdir
import platform.posix.execvp
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.pid_t
import platform.posix.popen

import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
fun executeCommand(
    command: String,
    trim: Boolean = true,
    redirectStderr: Boolean = true
): String = memScoped {
    println("executing: $command")

    val commandToExecute = if (redirectStderr) "$command 2>&1" else command
    val fp = popen?.invoke(commandToExecute.cstr.ptr, "r".cstr.ptr) ?: error("Failed to run command: $command")

    val stdout = buildString {
        val buffer = ByteArray(4096)
        while (true) {
            val input = fgets(buffer.refTo(0), buffer.size, fp) ?: break
            append(input.toKString())
        }
    }

    val status = pclose?.invoke(fp)
    if (status != 0) {
        error("Command `$command` failed with status $status${if (redirectStderr) ": $stdout" else ""}")
    }

    if (trim) stdout.trim() else stdout
}