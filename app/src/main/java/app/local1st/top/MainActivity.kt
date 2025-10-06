package app.local1st.top

import android.app.Activity
import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import app.local1st.top.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel

class MainActivity : Activity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var processAdapter: ProcessAdapter
    private var autoRefreshJob: Job? = null
    private var isAutoRefreshing = false
    private val uiScope = MainScope()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        applySystemBarsAppearance()
        setupWindowInsets()
        setupRecyclerView()
        setupButtons()
        checkRootAccess()
    }

    private fun applySystemBarsAppearance() {
        val isNightMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val barColor = ContextCompat.getColor(this, R.color.background)
        window.statusBarColor = barColor
        window.navigationBarColor = barColor

        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = !isNightMode
            isAppearanceLightNavigationBars = !isNightMode
        }
    }
    
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top)
            binding.processRecyclerView.updatePadding(bottom = systemBars.bottom)
            windowInsets
        }
    }
    
    private fun setupRecyclerView() {
        processAdapter = ProcessAdapter(packageManager)
        binding.processRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = processAdapter
            clipToPadding = false
        }
    }
    
    private fun setupButtons() {
        binding.refreshButton.setOnClickListener {
            refreshProcessList()
        }
        
        binding.autoRefreshButton.setOnClickListener {
            toggleAutoRefresh()
        }
    }
    
    private fun checkRootAccess() {
        uiScope.launch {
            val hasRoot = withContext(Dispatchers.IO) {
                RootCommandExecutor.isRootAvailable()
            }
            
            if (hasRoot) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.toast_root_granted),
                    Toast.LENGTH_SHORT
                ).show()
                refreshProcessList()
            } else {
                showRootErrorDialog()
            }
        }
    }
    
    private fun refreshProcessList() {
        uiScope.launch {
            try {
                // Execute top command in background
                val output = withContext(Dispatchers.IO) {
                    RootCommandExecutor.executeTopCommand(1)
                }
                
                // Parse output
                val (systemInfo, processList) = withContext(Dispatchers.Default) {
                    TopOutputParser.parseTopOutput(output, packageManager)
                }
                
                // Update UI
                updateSystemInfo(systemInfo)
                processAdapter.submitList(processList)
                
            } catch (e: Exception) {
                val errorMessage = e.message ?: getString(R.string.unknown_error)
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.toast_error, errorMessage),
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
    }
    
    private fun updateSystemInfo(systemInfo: SystemInfo) {
        binding.cpuInfoText.text = if (systemInfo.cpuUsage.isNotEmpty()) {
            systemInfo.cpuUsage
        } else {
            getString(R.string.cpu_info_unavailable)
        }
        
        binding.memInfoText.text = if (systemInfo.memoryUsage.isNotEmpty()) {
            systemInfo.memoryUsage
        } else {
            getString(R.string.memory_info_unavailable)
        }
        
        binding.processCountText.text = getString(
            R.string.process_count_format,
            systemInfo.totalProcesses,
            systemInfo.runningProcesses
        )
    }
    
    private fun toggleAutoRefresh() {
        if (isAutoRefreshing) {
            stopAutoRefresh()
        } else {
            startAutoRefresh()
        }
    }
    
    private fun startAutoRefresh() {
        isAutoRefreshing = true
        binding.autoRefreshButton.text = getString(R.string.auto_refresh_stop)
        
        autoRefreshJob = uiScope.launch {
            while (isActive) {
                refreshProcessList()
                delay(3000) // Refresh every 3 seconds
            }
        }
    }
    
    private fun stopAutoRefresh() {
        isAutoRefreshing = false
        binding.autoRefreshButton.text = getString(R.string.auto_refresh)
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }
    
    private fun showRootErrorDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_root_title)
            .setMessage(R.string.dialog_root_message)
            .setPositiveButton(R.string.dialog_retry) { _, _ ->
                checkRootAccess()
            }
            .setNegativeButton(R.string.dialog_exit) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAutoRefresh()
        uiScope.cancel()
    }
}
