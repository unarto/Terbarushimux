package moe.shizuku.manager.filemanager.utils

import android.content.Context
import android.net.Uri

object SafHelper {
    fun getAppNameFromSafUri(context: Context, uri: Uri): String? {
        try {
            val authority = uri.authority ?: return null
            val providerInfo = context.packageManager.resolveContentProvider(authority, 0)
            if (providerInfo != null) {
                val packageName = providerInfo.packageName
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                return context.packageManager.getApplicationLabel(appInfo).toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getCleanPathFromSafUri(uri: Uri): String {
        val decoded = Uri.decode(uri.toString())
        val treeIndex = decoded.indexOf("/tree/")
        if (treeIndex != -1) {
            return decoded.substring(treeIndex + 6)
        }
        return decoded
    }
}
