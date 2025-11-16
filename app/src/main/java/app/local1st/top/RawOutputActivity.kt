package app.local1st.top

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import app.local1st.top.databinding.ActivityRawOutputBinding

class RawOutputActivity : Activity() {

    private lateinit var binding: ActivityRawOutputBinding
    private var latestRawOutput: String = ""
    private var toolbarBaseTopPadding = 0
    private var scrollBaseBottomPadding = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRawOutputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbarBaseTopPadding = binding.rawOutputToolbar.paddingTop
        scrollBaseBottomPadding = binding.rawOutputScrollView.paddingBottom

        setActionBar(binding.rawOutputToolbar)
        supportStandardActionBar()
        setupWindowInsets()
        applySystemBarsAppearance()
        populateRawOutput()
    }

    private fun supportStandardActionBar() {
        actionBar?.apply {
            title = getString(R.string.dialog_raw_output_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeActionContentDescription(R.string.raw_output_close)
            elevation = 0f
        }
    }

    private fun setupWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.rawOutputToolbar.updatePadding(top = toolbarBaseTopPadding + systemBars.top)
            binding.rawOutputScrollView.updatePadding(bottom = scrollBaseBottomPadding + systemBars.bottom)

            windowInsets
        }
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

    private fun populateRawOutput() {
        latestRawOutput = intent.getStringExtra(EXTRA_RAW_OUTPUT).orEmpty()
        val displayText = latestRawOutput
            .ifBlank { getString(R.string.raw_output_empty_state) }
            .trimEnd()

        binding.rawOutputText.text = displayText
        binding.rawOutputText.setTextIsSelectable(latestRawOutput.isNotBlank())
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.raw_output_menu, menu)
        updateCopyActionState(menu.findItem(R.id.action_copy))
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        updateCopyActionState(menu.findItem(R.id.action_copy))
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_copy -> {
                copyToClipboard(latestRawOutput)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateCopyActionState(menuItem: MenuItem?) {
        if (menuItem == null) return
        val enabled = latestRawOutput.isNotBlank()
        menuItem.isEnabled = enabled
    }

    private fun copyToClipboard(rawOutput: String) {
        if (rawOutput.isBlank()) return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("top_raw_output", rawOutput))
        Toast.makeText(this, R.string.raw_output_copied, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val EXTRA_RAW_OUTPUT = "extra_raw_output"

        fun start(context: Context, rawOutput: String) {
            val intent = Intent(context, RawOutputActivity::class.java).apply {
                putExtra(EXTRA_RAW_OUTPUT, rawOutput)
            }
            context.startActivity(intent)
        }
    }
}
