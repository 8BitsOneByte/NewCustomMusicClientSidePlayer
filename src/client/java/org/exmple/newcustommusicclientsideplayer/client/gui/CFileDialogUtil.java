package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.awt.FileDialog;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.jspecify.annotations.Nullable;

public final class CFileDialogUtil {
    private static final ExecutorService FILE_DIALOG_EXECUTOR = Executors.newSingleThreadExecutor(
        new FileDialogThreadFactory()
    );

    private CFileDialogUtil() {
    }

    public static CompletableFuture<Optional<Path>> fileSelectDialog(
        DialogType dialog,
        String title,
        @Nullable Path origin,
        @Nullable String filterLabel,
        String... filters
    ) {
        CompletableFuture<Optional<Path>> future = new CompletableFuture<>();

        FILE_DIALOG_EXECUTOR.submit(() -> {
            try {
                future.complete(selectFile(dialog, title, origin, filterLabel, filters));
            } catch (RuntimeException exception) {
                future.completeExceptionally(exception);
            }
        });

        return future;
    }

    private static Optional<Path> selectFile(
        DialogType dialog,
        String title,
        @Nullable Path origin,
        @Nullable String filterLabel,
        String... filters
    ) {
        FileDialog fileDialog = new FileDialog(
            (java.awt.Frame) null,
            title,
            dialog == DialogType.SAVE ? FileDialog.SAVE : FileDialog.LOAD
        );
        if (origin != null) {
            Path absoluteOrigin = origin.toAbsolutePath();
            Path parent = absoluteOrigin.getParent();
            if (parent != null) {
                fileDialog.setDirectory(parent.toString());
            }
            Path fileName = absoluteOrigin.getFileName();
            if (fileName != null) {
                fileDialog.setFile(fileName.toString());
            }
        }
        fileDialog.setFilenameFilter((directory, name) -> matchesFilter(name, filters));
        fileDialog.setVisible(true);

        String fileName = fileDialog.getFile();
        String directory = fileDialog.getDirectory();
        return fileName == null || directory == null
            ? Optional.empty()
            : Optional.of(Paths.get(directory, fileName));
    }

    private static boolean matchesFilter(String fileName, String[] filters) {
        if (filters.length == 0) {
            return true;
        }

        for (String filter : filters) {
            if (filter.startsWith("*.") && fileName.toLowerCase(Locale.ROOT).endsWith(filter.substring(1).toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }

    public static void shutdown() {
        FILE_DIALOG_EXECUTOR.shutdownNow();
    }

    public enum DialogType {
        SAVE,
        OPEN
    }

    private static final class FileDialogThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "newcustommusicclientsideplayer-file-dialog");
            thread.setDaemon(true);
            return thread;
        }
    }
}
