#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>
#include <dirent.h>

void chmod_recursive(const char* path) {
    DIR *dir = opendir(path);
    if (!dir) return;

    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL) {
        if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0)
            continue;

        char full_path[4096];
        snprintf(full_path, sizeof(full_path), "%s/%s", path, entry->d_name);
        
        struct stat st;
        if (lstat(full_path, &st) == -1) continue;

        if (S_ISDIR(st.st_mode)) {
            chmod(full_path, 0755);
            chmod_recursive(full_path);
        } else if (S_ISREG(st.st_mode)) {
            chmod(full_path, 0700);
        }
    }
    closedir(dir);
}

void chown_recursive(const char* path, uid_t uid, gid_t gid) {
    DIR *dir = opendir(path);
    if (!dir) return;

    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL) {
        if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0)
            continue;

        char full_path[4096];
        snprintf(full_path, sizeof(full_path), "%s/%s", path, entry->d_name);
        
        struct stat st;
        if (lstat(full_path, &st) == -1) continue;

        lchown(full_path, uid, gid);

        if (S_ISDIR(st.st_mode)) {
            chown_recursive(full_path, uid, gid);
        }
    }
    closedir(dir);
}

int main(int argc, char** argv) {
    if (argc < 4) {
        fprintf(stderr, "Usage: %s <prefix_dir> <uid> <gid>\n", argv[0]);
        return 1;
    }

    const char* prefix_dir = argv[1];
    uid_t uid = (uid_t)atoi(argv[2]);
    gid_t gid = (gid_t)atoi(argv[3]);

    printf("Starting native bootstrap configuration for %s\n", prefix_dir);

    // 1. Process SYMLINKS.txt
    char symlinks_file[4096];
    snprintf(symlinks_file, sizeof(symlinks_file), "%s/SYMLINKS.txt", prefix_dir);
    FILE* infile = fopen(symlinks_file, "r");
    if (infile) {
        char line[4096];
        while (fgets(line, sizeof(line), infile)) {
            // Remove newline
            size_t len = strlen(line);
            if (len > 0 && line[len-1] == '\n') {
                line[len-1] = '\0';
            }

            // Find "←" (UTF-8 E2 86 90)
            char* delim = strstr(line, "←");
            if (!delim) continue;

            *delim = '\0';
            const char* target = line;
            const char* link_rel = delim + 3;

            char link_name[4096];
            snprintf(link_name, sizeof(link_name), "%s/%s", prefix_dir, link_rel);

            symlink(target, link_name);
            printf("Symlink: %s -> %s\n", link_name, target);
        }
        fclose(infile);
        unlink(symlinks_file);
    } else {
        fprintf(stderr, "SYMLINKS.txt not found, proceeding anyway.\n");
    }

    // 2. Fix permissions for bin and libexec
    printf("Fixing executable permissions...\n");
    char path[4096];
    snprintf(path, sizeof(path), "%s/bin", prefix_dir);
    chmod_recursive(path);
    snprintf(path, sizeof(path), "%s/libexec", prefix_dir);
    chmod_recursive(path);
    snprintf(path, sizeof(path), "%s/lib/apt/apt-helper", prefix_dir);
    chmod_recursive(path);
    snprintf(path, sizeof(path), "%s/lib/apt/methods", prefix_dir);
    chmod_recursive(path);

    // 3. Fix ownership recursively
    printf("Changing ownership to %d:%d\n", uid, gid);
    lchown(prefix_dir, uid, gid);
    chown_recursive(prefix_dir, uid, gid);

    printf("Native bootstrap configuration completed successfully.\n");
    return 0;
}
