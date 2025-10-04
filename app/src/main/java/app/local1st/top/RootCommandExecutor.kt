package app.local1st.top

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object RootCommandExecutor {
    
    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
    
    fun executeTopCommand(iterations: Int = 1): String {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            val inputStream = BufferedReader(InputStreamReader(process.inputStream))
            
            // Execute top command with specific parameters
            // -b: batch mode
            // -n: number of iterations
            outputStream.writeBytes("top -b -n $iterations\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            
            val output = StringBuilder()
            var line: String?
            while (inputStream.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            process.waitFor()
            output.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }
    
    fun executeCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            val inputStream = BufferedReader(InputStreamReader(process.inputStream))
            
            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            
            val output = StringBuilder()
            var line: String?
            while (inputStream.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            process.waitFor()
            output.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }
}
