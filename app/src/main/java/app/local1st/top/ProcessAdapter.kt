package app.local1st.top

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.local1st.top.databinding.ItemProcessBinding

class ProcessAdapter(
    private val packageManager: PackageManager
) : ListAdapter<ProcessInfo, ProcessAdapter.ProcessViewHolder>(ProcessDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProcessViewHolder {
        val binding = ItemProcessBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProcessViewHolder(binding, packageManager)
    }
    
    override fun onBindViewHolder(holder: ProcessViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ProcessViewHolder(
        private val binding: ItemProcessBinding,
        private val packageManager: PackageManager
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(processInfo: ProcessInfo) {
            binding.apply {
                val context = root.context
                // Try to get app icon and name
                val appInfo = TopOutputParser.getAppInfo(processInfo.packageName, packageManager)
                
                if (appInfo != null) {
                    // Show app icon and label
                    appIcon.setImageDrawable(packageManager.getApplicationIcon(appInfo))
                    appName.text = packageManager.getApplicationLabel(appInfo).toString()
                    packageNameText.text = processInfo.packageName
                } else {
                    // Show default icon and command
                    appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                    appName.text = processInfo.command
                    packageNameText.text = context.getString(R.string.pid_format, processInfo.pid)
                }
                
                // Display CPU and Memory usage
                cpuUsage.text = "${processInfo.cpuPercent}%"
                memUsage.text = "${processInfo.memPercent}%"
                
                // Display additional info
                processState.text = context.getString(R.string.process_state_format, processInfo.state)
                processUser.text = context.getString(R.string.process_user_format, processInfo.user)
                
                // Color code CPU usage
                val cpuColor = when {
                    processInfo.cpuPercent > 50f -> android.graphics.Color.RED
                    processInfo.cpuPercent > 20f -> android.graphics.Color.rgb(255, 165, 0) // Orange
                    else -> android.graphics.Color.GREEN
                }
                cpuUsage.setTextColor(cpuColor)
            }
        }
    }
    
    private class ProcessDiffCallback : DiffUtil.ItemCallback<ProcessInfo>() {
        override fun areItemsTheSame(oldItem: ProcessInfo, newItem: ProcessInfo): Boolean {
            return oldItem.pid == newItem.pid
        }
        
        override fun areContentsTheSame(oldItem: ProcessInfo, newItem: ProcessInfo): Boolean {
            return oldItem == newItem
        }
    }
}
