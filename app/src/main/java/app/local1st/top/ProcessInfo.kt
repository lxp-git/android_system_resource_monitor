package app.local1st.top

data class ProcessInfo(
    val pid: Int,
    val user: String,
    val priority: Int,
    val nice: Int,
    val virt: String,
    val res: String,
    val shr: String,
    val state: String,
    val cpuPercent: Float,
    val memPercent: Float,
    val time: String,
    val command: String,
    val packageName: String = ""
) {
    companion object {
        fun fromTopLine(line: String): ProcessInfo? {
            try {
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size < 12) return null
                
                return ProcessInfo(
                    pid = parts[0].toIntOrNull() ?: return null,
                    user = parts[1],
                    priority = parts[2].toIntOrNull() ?: 0,
                    nice = parts[3].toIntOrNull() ?: 0,
                    virt = parts[4],
                    res = parts[5],
                    shr = parts[6],
                    state = parts[7],
                    cpuPercent = parts[8].replace("%", "").toFloatOrNull() ?: 0f,
                    memPercent = parts[9].replace("%", "").toFloatOrNull() ?: 0f,
                    time = parts[10],
                    command = parts.drop(11).joinToString(" ")
                )
            } catch (e: Exception) {
                return null
            }
        }
    }
}

data class SystemInfo(
    val cpuUsage: String = "",
    val memoryUsage: String = "",
    val totalProcesses: Int = 0,
    val runningProcesses: Int = 0
)
