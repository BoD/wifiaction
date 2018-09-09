package org.jraf.wifiaction

import com.beust.jcommander.JCommander
import java.io.BufferedReader
import java.io.InputStreamReader

private const val MIN_DETECTION_DURATION_MS = 5000
private const val DETECT_KEYWORD = "DHCP"
private val COMMAND_TSHARK = arrayOf("tshark", "-l", "-f", "ether host %1\$s and (port 67)")

fun executeAction(commandToExecute: List<String>, debugMode: Boolean) {
    try {
        Runtime.getRuntime().exec(commandToExecute.toTypedArray())
    } catch (t: Throwable) {
        if (debugMode) {
            println("Could not execute command")
            t.printStackTrace()
        }
    }
}

@Throws(Throwable::class)
fun main(av: Array<String>) {
    val arguments = Arguments()
    val jCommander = JCommander.newBuilder()
        .addObject(arguments)
        .build()
    jCommander.parse(*av)

    if (arguments.help) {
        jCommander.usage()
        return
    }

    if (arguments.macAddress == null) {
        System.err.println("A mac address must be provided with -m")
        System.exit(-1)
        return
    }

    if (arguments.commandToExecute.isEmpty()) {
        System.err.println("A command to execute must be provided")
        System.exit(-1)
        return
    }

    val tsharkCommand = COMMAND_TSHARK.map { it.format(arguments.macAddress) }
    if (arguments.debugMode) println("Executing $tsharkCommand")

    val process = try {
        ProcessBuilder().command(tsharkCommand).redirectErrorStream(true).start()
    } catch (t: Throwable) {
        if (arguments.debugMode) {
            t.printStackTrace()
            println("Could not execute tshark: give up")
        }
        System.exit(-1)
        return
    }

    val br = BufferedReader(InputStreamReader(process.inputStream))
    object : Thread() {
        override fun run() {
            try {
                var lastTime = 0L
                var line: String?
                while (true) {
                    line = br.readLine()
                    if (arguments.debugMode) println("tshark> $line")
                    if (line == null) {
                        if (arguments.debugMode) println("tshark exited: give up")
                        System.exit(-1)
                        return
                    }
                    if (line.contains(DETECT_KEYWORD)) {
                        val now = System.currentTimeMillis()
                        if (now - lastTime < MIN_DETECTION_DURATION_MS) {
                            if (arguments.debugMode) println("Mac address detected - too soon")
                        } else {
                            if (arguments.debugMode) println("Mac address detected: execute ${arguments.commandToExecute}")
                            executeAction(arguments.commandToExecute, arguments.debugMode)
                        }
                        lastTime = now
                    }
                }
            } catch (t: Throwable) {
                if (arguments.debugMode) {
                    t.printStackTrace()
                    println("Some kind of exception occurred: give up")
                }
                System.exit(-1)
                return
            }
        }
    }.start()
    if (arguments.debugMode) println("Waiting for mac address to be seen")
    process.waitFor()
}
