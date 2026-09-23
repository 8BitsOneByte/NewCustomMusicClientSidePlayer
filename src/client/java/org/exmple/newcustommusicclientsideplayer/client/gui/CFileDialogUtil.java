package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLProperties;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;

public final class CFileDialogUtil {
    private static final AtomicLong NEXT_REQUEST_ID = new AtomicLong(1L);
    private static final Map<Long, DialogRequest> ACTIVE_DIALOGS = new ConcurrentHashMap<>();
    private static final SDL_DialogFileCallback FILE_DIALOG_CALLBACK = SDL_DialogFileCallback.create(
        CFileDialogUtil::onDialogCompleted
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
        showDialog(dialog, title, origin, filterLabel, filters, future);
        return future;
    }

    private static void showDialog(
        DialogType dialog,
        String title,
        @Nullable Path origin,
        @Nullable String filterLabel,
        String[] filters,
        CompletableFuture<Optional<Path>> future
    ) {
        long requestId = NEXT_REQUEST_ID.getAndIncrement();
        DialogRequest request = null;
        try {
            request = DialogRequest.create(future, filterLabel, filters);
            int properties = request.properties();
            setProperty(SDLProperties.SDL_SetPointerProperty(
                properties,
                SDLDialog.SDL_PROP_FILE_DIALOG_WINDOW_POINTER,
                Minecraft.getInstance().getWindow().handle()
            ));
            setProperty(SDLProperties.SDL_SetStringProperty(
                properties,
                SDLDialog.SDL_PROP_FILE_DIALOG_TITLE_STRING,
                title
            ));

            if (origin != null) {
                setProperty(SDLProperties.SDL_SetStringProperty(
                    properties,
                    SDLDialog.SDL_PROP_FILE_DIALOG_LOCATION_STRING,
                    origin.toAbsolutePath().toString()
                ));
            }
            if (request.nativeFilters() != null) {
                setProperty(SDLProperties.SDL_SetPointerProperty(
                    properties,
                    SDLDialog.SDL_PROP_FILE_DIALOG_FILTERS_POINTER,
                    request.nativeFilters().address()
                ));
                setProperty(SDLProperties.SDL_SetNumberProperty(
                    properties,
                    SDLDialog.SDL_PROP_FILE_DIALOG_NFILTERS_NUMBER,
                    request.nativeFilters().capacity()
                ));
            }
            setProperty(SDLProperties.SDL_SetBooleanProperty(
                properties,
                SDLDialog.SDL_PROP_FILE_DIALOG_MANY_BOOLEAN,
                false
            ));

            ACTIVE_DIALOGS.put(requestId, request);
            SDLDialog.SDL_ShowFileDialogWithProperties(
                dialog == DialogType.SAVE
                    ? SDLDialog.SDL_FILEDIALOG_SAVEFILE
                    : SDLDialog.SDL_FILEDIALOG_OPENFILE,
                FILE_DIALOG_CALLBACK,
                requestId,
                properties
            );
        } catch (Throwable throwable) {
            ACTIVE_DIALOGS.remove(requestId);
            if (request != null) {
                request.close();
            }
            future.completeExceptionally(throwable);
        }
    }

    private static void setProperty(boolean success) {
        if (!success) {
            String error = SDLError.SDL_GetError();
            throw new IllegalStateException(error == null || error.isBlank() ? "SDL property setup failed" : error);
        }
    }

    private static void onDialogCompleted(long requestId, long fileList, int selectedFilter) {
        DialogRequest request = ACTIVE_DIALOGS.remove(requestId);
        if (request == null) {
            return;
        }

        try {
            if (fileList == MemoryUtil.NULL) {
                String error = SDLError.SDL_GetError();
                IllegalStateException exception = new IllegalStateException(
                    error == null || error.isBlank() ? "SDL file dialog failed" : error
                );
                request.future().completeExceptionally(exception);
                return;
            }

            long selectedPath = MemoryUtil.memGetAddress(fileList);
            if (selectedPath == MemoryUtil.NULL) {
                request.future().complete(Optional.empty());
                return;
            }

            request.future().complete(Optional.of(Paths.get(MemoryUtil.memUTF8(selectedPath))));
        } catch (Throwable throwable) {
            request.future().completeExceptionally(throwable);
        } finally {
            request.close();
        }
    }

    private static String toSdlFilterPattern(String[] filters) {
        StringBuilder pattern = new StringBuilder();
        for (String filter : filters) {
            String extension = filter.startsWith("*.") ? filter.substring(2) : filter;
            if (extension.isBlank()) {
                continue;
            }
            if (!pattern.isEmpty()) {
                pattern.append(';');
            }
            pattern.append(extension);
        }
        return pattern.isEmpty() ? "*" : pattern.toString();
    }

    public static void shutdown() {
        // SDL owns any active native dialog until its callback returns. The process shutdown
        // will release the persistent callback and any still-active request allocations.
    }

    public enum DialogType {
        SAVE,
        OPEN
    }

    private record DialogRequest(
        CompletableFuture<Optional<Path>> future,
        int properties,
        SDL_DialogFileFilter.Buffer nativeFilters,
        ByteBuffer nativeFilterName,
        ByteBuffer nativeFilterPattern
    ) implements AutoCloseable {
        private static DialogRequest create(
            CompletableFuture<Optional<Path>> future,
            @Nullable String filterLabel,
            String[] filters
        ) {
            int properties = SDLProperties.SDL_CreateProperties();
            if (properties == 0) {
                String error = SDLError.SDL_GetError();
                throw new IllegalStateException(
                    error == null || error.isBlank() ? "Unable to create SDL properties" : error
                );
            }

            if (filters.length == 0) {
                return new DialogRequest(future, properties, null, null, null);
            }

            ByteBuffer nativeName = null;
            ByteBuffer nativePattern = null;
            SDL_DialogFileFilter.Buffer nativeFilters = null;
            try {
                nativeName = MemoryUtil.memUTF8(filterLabel == null ? "Files" : filterLabel);
                nativePattern = MemoryUtil.memUTF8(toSdlFilterPattern(filters));
                nativeFilters = SDL_DialogFileFilter.calloc(1);
                nativeFilters.get(0).name(nativeName).pattern(nativePattern);
                return new DialogRequest(future, properties, nativeFilters, nativeName, nativePattern);
            } catch (Throwable throwable) {
                if (nativeFilters != null) {
                    nativeFilters.free();
                }
                MemoryUtil.memFree(nativeName);
                MemoryUtil.memFree(nativePattern);
                SDLProperties.SDL_DestroyProperties(properties);
                throw throwable;
            }
        }

        @Override
        public void close() {
            SDLProperties.SDL_DestroyProperties(this.properties);
            if (this.nativeFilters != null) {
                this.nativeFilters.free();
            }
            MemoryUtil.memFree(this.nativeFilterName);
            MemoryUtil.memFree(this.nativeFilterPattern);
        }
    }
}
