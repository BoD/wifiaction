package org.jraf.wifiaction

import com.beust.jcommander.Parameter

class Arguments {
    @Parameter(
        names = ["-h", "--help"],
        description = "Show this help",
        help = true
    )
    var help: Boolean = false

    @Parameter(
        names = ["-m", "--mac-address"],
        description = "The mac address to monitor, e.g. fc:a6:67:ee:31:c0"
    )
    var macAddress: String? = null

    @Parameter(
        names = ["-d", "--debug"],
        description = "Debug mode, if enabled, debug logs will be output"
    )
    var debugMode: Boolean = false

    @Parameter(
        description = "The command to execute"
    )
    var commandToExecute = mutableListOf<String>()
}
