package app.local1st.top

import org.junit.Assert.assertEquals
import org.junit.Test

class TopOutputParserTest {

    @Test
    fun parseTopOutput_usesTaskSummaryInsteadOfMemLine() {
        val sampleTopOutput = """
            Tasks: 672 total,   1 running, 671 sleeping,   0 stopped,   0 zombie
            Mem:    11540M total,    10948M used,      591M free,        5M buffers
            800%cpu  17%user   0%nice  42%sys 736%idle   0%iow   3%irq   3%sirq   0%host
            PID USER         PR  NI VIRT  RES  SHR S[%CPU] %MEM     TIME+ ARGS
            10776 shell        20   0  10G 4.8M 3.7M R 44.4   0.0   0:00.17 top -b -n 1
            9207 u0_a189      20   0  18G 218M 138M S  2.7   1.8   3:32.73 com.google.android.gms.persistent
        """.trimIndent()

        val (systemInfo, processList) = TopOutputParser.parseTopOutput(sampleTopOutput)

        assertEquals(672, systemInfo.totalProcesses)
        assertEquals(1, systemInfo.runningProcesses)
        assertEquals(
            "Mem:    11540M total,    10948M used,      591M free,        5M buffers",
            systemInfo.memoryUsage
        )
        assertEquals(
            "800%cpu  17%user   0%nice  42%sys 736%idle   0%iow   3%irq   3%sirq   0%host",
            systemInfo.cpuUsage
        )
        assertEquals(2, processList.size)
        assertEquals(10776, processList.first().pid)
        assertEquals(44.4f, processList.first().cpuPercent, 0.01f)
    }
}
