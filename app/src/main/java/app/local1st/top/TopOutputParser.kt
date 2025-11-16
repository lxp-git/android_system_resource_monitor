package app.local1st.top

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process

object TopOutputParser {
    private val tasksLineRegex = Regex("""^Tasks:\s*(\\d+)\s+total,\s*(\\d+)\s+running""", RegexOption.IGNORE_CASE)

    fun parseTopOutput(output: String, packageManager: PackageManager? = null): Pair<SystemInfo, List<ProcessInfo>> {
        val lines = output.split("\n")
        var systemInfo = SystemInfo()
        val processList = mutableListOf<ProcessInfo>()
        
        var inProcessList = false
        var headerFound = false
        
        for (line in lines) {
            when {
                // Parse CPU info
                line.contains("CPU:") || line.contains("%cpu") -> {
                    systemInfo = systemInfo.copy(cpuUsage = line.trim())
                }
                // Parse Memory info
                line.contains("Mem:") || line.contains("KiB Mem") -> {
                    systemInfo = systemInfo.copy(memoryUsage = line.trim())
                }
                // Parse Tasks/Processes info
                line.trim().startsWith("Tasks:", ignoreCase = true) -> {
                    val match = tasksLineRegex.find(line)
                    if (match != null) {
                        systemInfo = systemInfo.copy(
                            totalProcesses = match.groupValues[1].toIntOrNull() ?: 0,
                            runningProcesses = match.groupValues[2].toIntOrNull() ?: 0
                        )
                    } else {
                        val parts = line.split(",")
                        parts.firstOrNull()?.let {
                            val total = it.filter { c -> c.isDigit() }
                            systemInfo = systemInfo.copy(totalProcesses = total.toIntOrNull() ?: 0)
                        }
                        parts.find { it.contains("running", ignoreCase = true) }?.let {
                            val running = it.filter { c -> c.isDigit() }
                            systemInfo = systemInfo.copy(runningProcesses = running.toIntOrNull() ?: 0)
                        }
                    }
                }
                // Detect process list header
                line.trim().matches(Regex("\\s*PID\\s+USER.*")) -> {
                    headerFound = true
                    inProcessList = true
                }
                // Parse process lines
                inProcessList && headerFound && line.trim().isNotEmpty() -> {
                    ProcessInfo.fromTopLine(line)?.let { processInfo ->
                        val enrichedProcess = enrichProcessInfo(processInfo, packageManager)
                        processList.add(enrichedProcess)
                    }
                }
            }
        }
        
        // Sort by CPU usage descending
        val sortedList = processList.sortedByDescending { it.cpuPercent }
        
        return Pair(systemInfo, sortedList)
    }
    
    private fun enrichProcessInfo(processInfo: ProcessInfo, packageManager: PackageManager?): ProcessInfo {
        if (packageManager == null) return processInfo
        // Try to extract package name from command
        val candidates = mutableListOf<String>()

        val packageNameFromCommand = extractPackageName(processInfo.command)
        if (packageNameFromCommand.isNotEmpty()) {
            candidates += packageNameFromCommand
        }

        val processName = getProcessNameFromProc(processInfo.pid)
        if (!processName.isNullOrEmpty()) {
            candidates += processName
            if (":" in processName) {
                candidates += processName.substringBefore(":")
            }
        }

        val userResolvedPackage = resolvePackageNameFromUser(processInfo.user, processInfo.pid, packageManager)
        if (userResolvedPackage.isNotEmpty()) {
            candidates += userResolvedPackage
        }

        val finalPackage = candidates.firstOrNull { candidate ->
            candidate.isNotEmpty() && hasApplicationInfo(candidate, packageManager)
        } ?: candidates.firstOrNull { it.isNotEmpty() } ?: ""

        return processInfo.copy(packageName = finalPackage)
    }
    
    private fun extractPackageName(command: String): String {
        // Common patterns for package names in Android
        // Example: app.local1st.top, com.android.systemui
        val packagePattern = Regex("([a-z][a-z0-9_]*(\\.[a-z0-9_]+)+)")
        val match = packagePattern.find(command)
        return match?.value ?: ""
    }

    private fun getProcessNameFromProc(pid: Int): String? {
        return try {
            val cmdlinePath = "/proc/$pid/cmdline"
            val cmdlineFile = java.io.File(cmdlinePath)
            if (!cmdlineFile.exists()) return null
            val raw = cmdlineFile.inputStream().use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            }
            raw.replace('\u0000', ' ').trim()
        } catch (_: Exception) {
            null
        }
    }

    private fun resolvePackageNameFromUser(
        user: String,
        pid: Int,
        packageManager: PackageManager
    ): String {
        val uid = getUidForUser(user)
            ?: getUidFromProc(pid)
            ?: return ""
        val packages = runCatching { packageManager.getPackagesForUid(uid) }.getOrNull()
        if (packages.isNullOrEmpty()) return ""
        // Prefer the package whose name appears in the command list order, otherwise fallback to first.
        return packages.first()
    }

    private fun getUidForUser(user: String): Int? {
        return runCatching {
            val uid = Process.getUidForName(user)
            if (uid > 0) uid else null
        }.getOrNull()
    }

    private fun getUidFromProc(pid: Int): Int? {
        return try {
            val statusPath = "/proc/$pid/status"
            val statusContent = java.io.File(statusPath)
            if (!statusContent.exists()) return null
            statusContent.useLines { lines ->
                lines.firstOrNull { it.startsWith("Uid:") }
            }?.split(Regex("\\s+"))?.getOrNull(1)?.toIntOrNull()
        } catch (_: Exception) {
            null
        }
    }
    
    fun getAppInfo(packageName: String, packageManager: PackageManager): ApplicationInfo? {
        if (packageName.isEmpty()) return null

        val candidates = buildList {
            add(packageName)
            if (":" in packageName) {
                add(packageName.substringBefore(":"))
            }
        }

        for (candidate in candidates) {
            val info = runCatching {
                packageManager.getApplicationInfo(candidate, 0)
            }.getOrNull()
            if (info != null) {
                return info
            }
        }
        return null
    }

    private fun hasApplicationInfo(packageName: String, packageManager: PackageManager): Boolean {
        if (packageName.isEmpty()) return false
        return runCatching { packageManager.getApplicationInfo(packageName, 0) != null }.getOrDefault(false)
    }
}
