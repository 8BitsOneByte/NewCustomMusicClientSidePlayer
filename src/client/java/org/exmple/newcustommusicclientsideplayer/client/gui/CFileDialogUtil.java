package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

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
        String result;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filterBuffer = stack.mallocPointer(filters.length);
            for (String filter : filters) {
                filterBuffer.put(stack.UTF8(filter));
            }
            filterBuffer.flip();

            String originPath = origin == null ? null : origin.toAbsolutePath().toString();
            result = switch (dialog) {
                case SAVE -> TinyFileDialogs.tinyfd_saveFileDialog(title, originPath, filterBuffer, filterLabel);
                case OPEN -> TinyFileDialogs.tinyfd_openFileDialog(title, originPath, filterBuffer, filterLabel, false);
            };
        }

        return Optional.ofNullable(result).map(Paths::get);
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
