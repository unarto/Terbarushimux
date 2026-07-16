package moe.shizuku.manager.home

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.shizuku.manager.R
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.databinding.HomeTerminalBinding
import moe.shizuku.manager.filemanager.presentation.FileManagerActivity
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator
import moe.shizuku.manager.model.ServiceStatus

class FileManagerViewHolder(private val binding: HomeTerminalBinding, private val root: View) :
    BaseViewHolder<ServiceStatus>(root), View.OnClickListener {

    companion object {
        val CREATOR = Creator<ServiceStatus> { inflater: LayoutInflater, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
            val inner = HomeTerminalBinding.inflate(inflater, outer.root, true)
            FileManagerViewHolder(inner, outer.root)
        }
    }

    init {
        root.setOnClickListener(this)
        binding.icon.setImageResource(R.drawable.ic_terminal_24) // Reuse terminal icon for now
        binding.text1.text = "File Manager"
        binding.text2.text = "Browse file system with Shizuku privilege"
    }

    override fun onBind() {
        val status = data
        root.isEnabled = status.isRunning
        if (status.isRunning) {
            root.alpha = 1f
        } else {
            root.alpha = 0.5f
        }
    }

    override fun onClick(v: View) {
        val intent = Intent(v.context, FileManagerActivity::class.java)
        v.context.startActivity(intent)
    }
}
