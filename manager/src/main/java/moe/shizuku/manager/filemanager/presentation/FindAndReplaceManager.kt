package moe.shizuku.manager.filemanager.presentation

import android.content.Context
import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FindAndReplaceManagerDialog(
    context: Context,
    viewModel: FileManagerViewModel,
    targetDirectory: String,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf("contains") }
    var targetRename by remember { mutableStateOf(false) }
    var targetContent by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Temukan & Ganti (Recursive)") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cari (Query)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = { replaceQuery = it },
                    label = { Text("Ganti Dengan") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Mode Pencarian", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = searchMode == "contains", onClick = { searchMode = "contains" })
                    Text("Berisi")
                    Spacer(Modifier.width(8.dp))
                    RadioButton(selected = searchMode == "prefix", onClick = { searchMode = "prefix" })
                    Text("Awalan")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = searchMode == "suffix", onClick = { searchMode = "suffix" })
                    Text("Akhiran")
                    Spacer(Modifier.width(8.dp))
                    RadioButton(selected = searchMode == "regex", onClick = { searchMode = "regex" })
                    Text("Regex")
                }

                Text("Target Operasi", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = targetRename, onCheckedChange = { targetRename = it })
                    Text("Ganti Nama File/Folder")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = targetContent, onCheckedChange = { targetContent = it })
                    Text("Ganti Isi Teks di File")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (searchQuery.isNotEmpty() && (targetRename || targetContent)) {
                        val pythonScript = """
import os
import re
import sys

target_dir = sys.argv[1]
search = sys.argv[2]
replace = sys.argv[3]
mode = sys.argv[4]
do_rename = sys.argv[5] == 'True'
do_content = sys.argv[6] == 'True'

def match_and_replace(text):
    try:
        if mode == 'regex':
            return re.sub(search, replace, text)
        elif mode == 'contains':
            return text.replace(search, replace)
        elif mode == 'prefix':
            if text.startswith(search):
                return replace + text[len(search):]
        elif mode == 'suffix':
            if text.endswith(search):
                return text[:-len(search)] + replace
    except Exception as e:
        print("Error matching:", e)
    return text

print(f"Starting Find and Replace in {target_dir}...")

for root, dirs, files in os.walk(target_dir, topdown=False):
    if do_content:
        for name in files:
            filepath = os.path.join(root, name)
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                new_content = match_and_replace(content)
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Modifikasi teks: {filepath}")
            except UnicodeDecodeError:
                pass # Skip binary
            except Exception as e:
                print(f"Gagal memproses file {filepath}: {e}")

    if do_rename:
        for name in files + dirs:
            new_name = match_and_replace(name)
            if new_name != name:
                old_path = os.path.join(root, name)
                new_path = os.path.join(root, new_name)
                try:
                    os.rename(old_path, new_path)
                    print(f"Ganti nama: {old_path} -> {new_path}")
                except Exception as e:
                    print(f"Gagal ganti nama {old_path}: {e}")
print("Selesai.")
""".trimIndent()
                        
                        val scriptBase64 = Base64.encodeToString(pythonScript.toByteArray(), Base64.NO_WRAP)
                        val searchQueryB64 = Base64.encodeToString(searchQuery.toByteArray(), Base64.NO_WRAP)
                        val replaceQueryB64 = Base64.encodeToString(replaceQuery.toByteArray(), Base64.NO_WRAP)
                        val targetDirB64 = Base64.encodeToString(targetDirectory.toByteArray(), Base64.NO_WRAP)

                        val cmd = """
                            export PREFIX=/data/data/com.termux/files/usr
                            if ! command -v python3 &> /dev/null; then
                                echo "Python3 belum terinstal. Menginstal python..."
                                pkg install -y python
                            fi
                            TMP_SCRIPT="${'$'}PREFIX/tmp/fnr_script_${'$'}${'$'}_${System.currentTimeMillis()}.py"
                            echo "$scriptBase64" | base64 -d > "${'$'}TMP_SCRIPT"
                            python3 "${'$'}TMP_SCRIPT" "${'$'}(echo "$targetDirB64" | base64 -d)" "${'$'}(echo "$searchQueryB64" | base64 -d)" "${'$'}(echo "$replaceQueryB64" | base64 -d)" "$searchMode" "$targetRename" "$targetContent"
                            rm -f "${'$'}TMP_SCRIPT"
                        """.trimIndent()
                        
                        viewModel.executeTermuxScript(context, cmd)
                        onDismiss()
                    }
                },
                enabled = searchQuery.isNotEmpty() && (targetRename || targetContent)
            ) {
                Text("Jalankan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
