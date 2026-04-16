package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BackupService {
    private final CrossroadsPlugin plugin;
    private final File backupsDirectory;

    public BackupService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        this.backupsDirectory = new File(plugin.getDataFolder(), "backups");
        if (!backupsDirectory.exists()) {
            backupsDirectory.mkdirs();
        }
    }

    public File createBackup(String reason) throws IOException {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(new Date());
        File backupFile = new File(backupsDirectory, "crossroads-" + reason + "-" + timestamp + ".zip");

        try (ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(backupFile));
             var stream = Files.walk(plugin.getDataFolder().toPath())) {
            Path root = plugin.getDataFolder().toPath();
            stream
                .filter(Files::isRegularFile)
                .filter(path -> !path.startsWith(backupsDirectory.toPath()))
                .forEach(path -> zipEntry(outputStream, root, path));
        }

        pruneOldBackups(plugin.getConfig().getInt("backups.max-files", 10));
        return backupFile;
    }

    private void zipEntry(ZipOutputStream outputStream, Path root, Path file) {
        try (FileInputStream inputStream = new FileInputStream(file.toFile())) {
            String entryName = root.relativize(file).toString().replace('\\', '/');
            outputStream.putNextEntry(new ZipEntry(entryName));
            inputStream.transferTo(outputStream);
            outputStream.closeEntry();
        } catch (IOException exception) {
            plugin.getLogger().warning("Unable to include " + file + " in backup archive.");
        }
    }

    private void pruneOldBackups(int maxFiles) throws IOException {
        if (maxFiles < 1 || !backupsDirectory.exists()) {
            return;
        }

        List<File> backups;
        try (var stream = Files.list(backupsDirectory.toPath())) {
            backups = stream
                .map(Path::toFile)
                .filter(File::isFile)
                .sorted(Comparator.comparingLong(File::lastModified).reversed())
                .toList();
        }

        for (int index = maxFiles; index < backups.size(); index++) {
            Files.deleteIfExists(backups.get(index).toPath());
        }
    }
}
