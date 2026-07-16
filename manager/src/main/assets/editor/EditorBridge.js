// EditorBridge.js - Menjembatani WebView CodeMirror dengan Kotlin JSInterface

let editor = CodeMirror.fromTextArea(document.getElementById("editor"), {
    lineNumbers: true,
    theme: "darcula",
    mode: "javascript", // Default mode
    matchBrackets: true,
    autoCloseBrackets: true,
    indentUnit: 4,
    lineWrapping: true
});

// Listener untuk mengirim update ke Kotlin jika ada perubahan (Auto-Save)
let autoSaveTimeout = null;
editor.on("change", function(cm, change) {
    clearTimeout(autoSaveTimeout);
    autoSaveTimeout = setTimeout(function() {
        if (window.AndroidBridge && window.AndroidBridge.onAutoSaveContent) {
            window.AndroidBridge.onAutoSaveContent(cm.getValue());
        }
    }, 3000); // 3 detik debounce auto-save
});

// Auto-save saat editor kehilangan fokus (on-blur)
editor.on("blur", function() {
    if (window.AndroidBridge && window.AndroidBridge.onAutoSaveContent) {
        window.AndroidBridge.onAutoSaveContent(editor.getValue());
    }
});

// Fungsi yang akan dipanggil oleh Kotlin
window.EditorAPI = {
    // Memuat teks baru ke editor (dipanggil saat buka file)
    setText: function(text) {
        editor.setValue(text);
        editor.clearHistory(); // Prevent undoing back to empty
    },

    // Mengambil seluruh teks (dipanggil saat save)
    getText: function() {
        if (window.AndroidBridge && window.AndroidBridge.onSaveContent) {
            window.AndroidBridge.onSaveContent(editor.getValue());
        }
        return editor.getValue();
    },

    // Mengatur mode bahasa (syntax highlighting)
    setMode: function(mimeType) {
        editor.setOption("mode", mimeType);
    },
    
    // Mengatur tema (Syntax Theme Customizer)
    setTheme: function(themeName) {
        editor.setOption("theme", themeName);
        if (themeName === 'eclipse') {
            document.body.style.backgroundColor = '#ffffff';
        } else {
            document.body.style.backgroundColor = '#2b2b2b';
        }
    },

    // Fungsi tambahan seperti undo/redo/dll bisa ditambahkan di sini
    undo: function() { editor.undo(); },
    redo: function() { editor.redo(); },
    insertText: function(text) { editor.replaceSelection(text); },
    execCommand: function(cmd) { editor.execCommand(cmd); },
    setWordWrap: function(wrap) { editor.setOption("lineWrapping", wrap); },
    setFontSize: function(size) { 
        document.querySelector('.CodeMirror').style.fontSize = size + 'px'; 
        editor.refresh();
    },
    setReadOnly: function(readOnly) {
        editor.setOption("readOnly", readOnly);
    },
    find: function() { editor.execCommand('find'); },
    replace: function() { editor.execCommand('replace'); },
    deleteLine: function() { editor.execCommand('deleteLine'); },
    selectAll: function() { editor.execCommand('selectAll'); },
    goDocStart: function() { editor.execCommand('goDocStart'); },
    goDocEnd: function() { editor.execCommand('goDocEnd'); },
    goLineUp: function() { editor.execCommand('goLineUp'); },
    goLineDown: function() { editor.execCommand('goLineDown'); },
    goCharLeft: function() { editor.execCommand('goCharLeft'); },
    goCharRight: function() { editor.execCommand('goCharRight'); },
    undo: function() { editor.execCommand('undo'); },
    redo: function() { editor.execCommand('redo'); },
    copyAll: function() {
        if (window.AndroidBridge && window.AndroidBridge.onCopyAll) {
            window.AndroidBridge.onCopyAll(editor.getValue());
        }
    }
};

// Memberitahu Kotlin bahwa editor sudah siap
if (window.AndroidBridge && window.AndroidBridge.onEditorReady) {
    window.AndroidBridge.onEditorReady();
}
