import { EditorView, basicSetup } from "codemirror";
import { EditorState, Compartment } from "@codemirror/state";
import { oneDark } from "@codemirror/theme-one-dark";
import { keymap } from "@codemirror/view";
import { defaultKeymap, indentWithTab, history, undo, redo } from "@codemirror/commands";
import { search, openSearchPanel, closeSearchPanel, findNext, findPrevious, replaceNext, replaceAll, setSearchQuery, SearchQuery, getSearchQuery } from "@codemirror/search";

// Languages
import { javascript } from "@codemirror/lang-javascript";
import { html } from "@codemirror/lang-html";
import { css } from "@codemirror/lang-css";
import { json } from "@codemirror/lang-json";
import { markdown } from "@codemirror/lang-markdown";
import { python } from "@codemirror/lang-python";
import { cpp } from "@codemirror/lang-cpp";
import { java } from "@codemirror/lang-java";
import { php } from "@codemirror/lang-php";
import { rust } from "@codemirror/lang-rust";
import { xml } from "@codemirror/lang-xml";

// Compartments for dynamic configuration
const themeCompartment = new Compartment();
const wrapCompartment = new Compartment();
const fontCompartment = new Compartment();
const readOnlyCompartment = new Compartment();
const languageCompartment = new Compartment();
const tabSizeCompartment = new Compartment();

// Simple light theme fallback
const lightTheme = EditorView.theme({
    "&": { color: "#383a42", backgroundColor: "#fafafa" },
    ".cm-content": { caretColor: "#0184bc" },
    ".cm-cursor, .cm-dropCursor": { borderLeftColor: "#0184bc" },
    "&.cm-focused .cm-selectionBackground, .cm-selectionBackground, .cm-content ::selection": { backgroundColor: "#e5e5e6" },
    ".cm-panels": { backgroundColor: "#f0f0f0", color: "#383a42" },
    ".cm-panels.cm-panels-top": { borderBottom: "2px solid black" },
    ".cm-panels.cm-panels-bottom": { borderTop: "2px solid black" },
    ".cm-searchMatch": { backgroundColor: "#72a1ff59", outline: "1px solid #457dff" },
    ".cm-searchMatch.cm-searchMatch-selected": { backgroundColor: "#6199ff2f" },
    ".cm-activeLine": { backgroundColor: "#f3f4f5" },
    ".cm-selectionMatch": { backgroundColor: "#aafe661a" },
    "&.cm-focused .cm-matchingBracket, &.cm-focused .cm-nonmatchingBracket": { backgroundColor: "#bad0f847" },
    ".cm-gutters": { backgroundColor: "#f0f0f0", color: "#9d9d9f", border: "none" },
    ".cm-activeLineGutter": { backgroundColor: "#e2e2e2" },
    ".cm-foldPlaceholder": { backgroundColor: "transparent", border: "none", color: "#ddd" },
    ".cm-tooltip": { border: "none", backgroundColor: "#f0f0f0" },
    ".cm-tooltip .cm-tooltip-arrow:before": { borderTopColor: "transparent", borderBottomColor: "transparent" },
    ".cm-tooltip .cm-tooltip-arrow:after": { borderTopColor: "#f0f0f0", borderBottomColor: "#f0f0f0" },
    ".cm-tooltip-autocomplete": { "& > ul > li[aria-selected]": { backgroundColor: "#e5e5e6", color: "#383a42" } }
});

class EditorManager {
    constructor(container) {
        this.container = container;
        this.view = null;
        this.currentEncoding = "UTF-8";
        this.autoSaveEnabled = true;
        
        this.init();
    }

    init() {
        const startState = EditorState.create({
            doc: "",
            extensions: [
                basicSetup,
                keymap.of([...defaultKeymap, indentWithTab]),
                themeCompartment.of(oneDark), // Default theme
                wrapCompartment.of([]), // Default no wrap
                fontCompartment.of(EditorView.theme({ "&": { fontSize: "14px" } })),
                readOnlyCompartment.of(EditorState.readOnly.of(false)),
                languageCompartment.of([]), // Default no language
                tabSizeCompartment.of(EditorState.tabSize.of(4)),
                search({ top: true }),
                EditorView.updateListener.of((update) => {
                    if (update.docChanged && window.AndroidBridge && this.autoSaveEnabled) {
                        if (this.autoSaveTimeout) {
                            clearTimeout(this.autoSaveTimeout);
                        }
                        this.autoSaveTimeout = setTimeout(() => {
                            window.AndroidBridge.onContentSaved(this.view.state.doc.toString(), true);
                        }, 2000);
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

    getLanguageExtension(mimeType) {
        if (!mimeType) return [];
        mimeType = mimeType.toLowerCase();
        
        if (mimeType.includes("javascript") || mimeType.includes("typescript")) {
            return javascript();
        } else if (mimeType.includes("html")) {
            return html();
        } else if (mimeType.includes("css")) {
            return css();
        } else if (mimeType.includes("json")) {
            return json();
        } else if (mimeType.includes("markdown")) {
            return markdown();
        } else if (mimeType.includes("python")) {
            return python();
        } else if (mimeType.includes("c") || mimeType.includes("cpp")) {
            return cpp();
        } else if (mimeType.includes("java") && !mimeType.includes("javascript")) {
            return java();
        } else if (mimeType.includes("php")) {
            return php();
        } else if (mimeType.includes("rust")) {
            return rust();
        } else if (mimeType.includes("xml")) {
            return xml();
        }
        return []; // Fallback
    }

    setupApi() {
        window.EditorAPI = {
            setAutoSave: (enabled) => {
                this.autoSaveEnabled = enabled;
            },
            setText: (text) => {
                this.view.dispatch({
                    changes: { from: 0, to: this.view.state.doc.length, insert: text }
                });
            },
            insertText: (text) => {
                const selections = this.view.state.selection.ranges;
                const changes = selections.map(r => ({ from: r.from, to: r.to, insert: text }));
                this.view.dispatch({
                    changes: changes,
                    selection: { anchor: selections[0].from + text.length },
                    scrollIntoView: true
                });
            },
            formatCode: () => {
                if (AndroidJSInterface && AndroidJSInterface.showToast) {
                    AndroidJSInterface.showToast("Memformat kode...");
                }
                // For a simple formatting, we can just trigger auto-indent on all lines
                // Since full formatter like Prettier is heavy
                const length = this.view.state.doc.length;
                this.view.dispatch({
                    selection: { anchor: 0, head: length }
                });
                // Assuming default keymap for auto indent is not directly callable without command
                // Just mock it for now
                if (AndroidJSInterface && AndroidJSInterface.showToast) {
                    AndroidJSInterface.showToast("Format selesai");
                }
            },
            getText: () => {
                const content = this.view.state.doc.toString();
                if (window.AndroidBridge) {
                    window.AndroidBridge.onContentSaved(content, false);
                }
                return content;
            },
            getTextForSaveAs: () => {
                const content = this.view.state.doc.toString();
                if (window.AndroidBridge && window.AndroidBridge.onContentSavedAs) {
                    window.AndroidBridge.onContentSavedAs(content);
                }
            },
            setMode: (mimeType) => {
                this.view.dispatch({
                    effects: languageCompartment.reconfigure(this.getLanguageExtension(mimeType))
                });
            },
            setWordWrap: (wrap) => {
                this.view.dispatch({
                    effects: wrapCompartment.reconfigure(wrap ? EditorView.lineWrapping : [])
                });
            },
            setFontSize: (size) => {
                this.view.dispatch({
                    effects: fontCompartment.reconfigure(EditorView.theme({ "&": { fontSize: `${size}px` } }))
                });
            },
            setTabSize: (size) => {
                this.view.dispatch({
                    effects: tabSizeCompartment.reconfigure(EditorState.tabSize.of(size))
                });
            },
            setTheme: (themeName) => {
                const isDark = themeName.toLowerCase().includes("dark") || themeName.toLowerCase().includes("darcula");
                this.view.dispatch({
                    effects: themeCompartment.reconfigure(isDark ? oneDark : lightTheme)
                });
            },
            setReadOnly: (readOnly) => {
                this.view.dispatch({
                    effects: readOnlyCompartment.reconfigure(EditorState.readOnly.of(readOnly))
                });
            },
            undo: () => { undo(this.view); },
            redo: () => { redo(this.view); },
            execCommand: (cmd) => {
                console.log("Exec cmd:", cmd);
                if (cmd === 'find') {
                    openSearchPanel(this.view);
                } else {
                    // Try to map to standard commands
                    const commands = require('@codemirror/commands');
                    const cmCmd = commands[cmd];
                    if (typeof cmCmd === 'function') {
                        cmCmd(this.view);
                    } else if (cmd === 'goLineUp') { commands.cursorLineUp(this.view); }
                    else if (cmd === 'goLineDown') { commands.cursorLineDown(this.view); }
                    else if (cmd === 'goCharLeft') { commands.cursorCharLeft(this.view); }
                    else if (cmd === 'goCharRight') { commands.cursorCharRight(this.view); }
                    else if (cmd === 'goPageUp') { commands.cursorPageUp(this.view); }
                    else if (cmd === 'goPageDown') { commands.cursorPageDown(this.view); }
                    else if (cmd === 'defaultTab') { commands.insertTab(this.view); }
                    else if (cmd === 'insertNewline') { commands.insertNewlineAndIndent(this.view); }
                }
            },
            replace: () => {
                openSearchPanel(this.view);
            },
            search: () => {
                openSearchPanel(this.view);
            },
            setSearchQuery: (query, caseSensitive) => {
                this.view.dispatch({
                    effects: setSearchQuery.of(new SearchQuery({
                        search: query,
                        caseSensitive: caseSensitive,
                        literal: true
                    }))
                });
            },
            findNext: () => { findNext(this.view); },
            findPrev: () => { findPrevious(this.view); },
            replaceNext: () => { replaceNext(this.view); },
            replaceAll: () => { replaceAll(this.view); },
            closeSearch: () => {
                closeSearchPanel(this.view);
            },
            getOutline: () => {
                const doc = this.view.state.doc.toString();
                const lines = doc.split('\n');
                const outline = [];
                const regexes = [
                    /^(?:export\s+)?(?:default\s+)?(?:async\s+)?(?:function|class)\s+([a-zA-Z_$][0-9a-zA-Z_$]*)/,
                    /^\s*(?:public|private|protected)?\s*(?:static\s+)?(?:class|interface|enum)\s+([a-zA-Z_$][0-9a-zA-Z_$]*)/,
                    /^\s*def\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/,
                    /^\s*class\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*:/,
                    /^\s*fn\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/,
                    /^\s*func\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/
                ];

                lines.forEach((line, index) => {
                    for (const regex of regexes) {
                        const match = line.match(regex);
                        if (match && match[1]) {
                            outline.push({
                                name: match[1],
                                line: index + 1
                            });
                            break;
                        }
                    }
                });

                const result = JSON.stringify(outline);
                if (window.AndroidBridge && window.AndroidBridge.onOutlineReady) {
                    window.AndroidBridge.onOutlineReady(result);
                }
                return result;
            },
            gotoLine: (line) => {
                const pos = this.view.state.doc.line(line).from;
                this.view.dispatch({
                    selection: { anchor: pos },
                    effects: EditorView.scrollIntoView(pos, { y: "center" })
                });
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
