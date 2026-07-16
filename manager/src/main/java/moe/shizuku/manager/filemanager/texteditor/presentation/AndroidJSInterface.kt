package moe.shizuku.manager.filemanager.texteditor.presentation

import android.webkit.JavascriptInterface

class AndroidJSInterface(
    private val onContentSaved: (String, Boolean) -> Unit,
    private val onEditorReadyCallback: () -> Unit,
    private val onCopyAllCallback: (String) -> Unit,
    private val onOutlineReadyCallback: ((String) -> Unit)? = null,
    private val onContentSavedAsCallback: ((String) -> Unit)? = null
) {
    @JavascriptInterface
    fun onContentSaved(content: String, isAutoSave: Boolean) {
        onContentSaved.invoke(content, isAutoSave)
    }

    @JavascriptInterface
    fun onSaveContent(content: String) {
        onContentSaved(content, false)
    }

    @JavascriptInterface
    fun onAutoSaveContent(content: String) {
        onContentSaved(content, true)
    }

    @JavascriptInterface
    fun onEditorReady() {
        onEditorReadyCallback()
    }

    @JavascriptInterface
    fun onEditorReadyCallback() {
        onEditorReadyCallback.invoke()
    }

    @JavascriptInterface
    fun onCopyAll(content: String) {
        onCopyAllCallback(content)
    }
    
    @JavascriptInterface
    fun onContentSavedAs(content: String) {
        onContentSavedAsCallback?.invoke(content)
    }

    @JavascriptInterface
    fun onOutlineReady(outlineJson: String) {
        onOutlineReadyCallback?.invoke(outlineJson)
    }
}
