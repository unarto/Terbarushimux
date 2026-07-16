import { EditorView, basicSetup } from "codemirror";
import { EditorState } from "@codemirror/state";
import { oneDark } from "@codemirror/theme-one-dark";
import { keymap } from "@codemirror/view";
import { defaultKeymap, indentWithTab, history, undo, redo } from "@codemirror/commands";
import { search } from "@codemirror/search";

class EditorManager {
    constructor(container) {
        this.container = container;
        this.view = null;
        this.currentEncoding = "UTF-8";
        
        this.init();
    }

    init() {
        const startState = EditorState.create({
            doc: "",
            extensions: [
                basicSetup,
                keymap.of([...defaultKeymap, indentWithTab]),
                oneDark, // Default theme for now
                search({ top: true }),
                EditorView.updateListener.of((update) => {
                    if (update.docChanged && window.AndroidBridge) {
                        // In a real app we might debounce autosave
                    }
                })
            ]
        });

        this.view = new EditorView({
            state: startState,
            parent: this.container
        });

        this.setupApi();
    }

    setupApi() {
        window.EditorAPI = {
            setText: (text) => {
                this.view.dispatch({
                    changes: { from: 0, to: this.view.state.doc.length, insert: text }
                });
            },
            getText: () => {
                const content = this.view.state.doc.toString();
                if (window.AndroidBridge) {
                    window.AndroidBridge.onContentSaved(content, false);
                }
                return content;
            },
            setMode: (mimeType) => {
                console.log("Set mode:", mimeType);
            },
            setWordWrap: (wrap) => {
                console.log("Set word wrap:", wrap);
            },
            setFontSize: (size) => {
                this.container.style.fontSize = `${size}px`;
            },
            setTheme: (themeName) => {
                console.log("Set theme:", themeName);
            },
            setReadOnly: (readOnly) => {
                console.log("Set read only:", readOnly);
            },
            undo: () => { undo(this.view); },
            redo: () => { redo(this.view); },
            execCommand: (cmd) => {
                console.log("Exec cmd:", cmd);
            },
            replace: () => {
                console.log("Find & replace triggered");
            }
        };

        if (window.AndroidBridge) {
            window.AndroidBridge.onEditorReadyCallback();
        }
    }
}

document.addEventListener("DOMContentLoaded", () => {
    const container = document.getElementById("editor-container");
    window.editorManager = new EditorManager(container);
});
