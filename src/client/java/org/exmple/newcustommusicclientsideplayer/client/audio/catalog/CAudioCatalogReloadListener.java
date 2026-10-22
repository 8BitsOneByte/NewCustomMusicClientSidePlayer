package org.exmple.newcustommusicclientsideplayer.client.audio.catalog;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

public final class CAudioCatalogReloadListener implements PreparableReloadListener {
    private final CAudioCatalog catalog;

    public CAudioCatalogReloadListener(CAudioCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public CompletableFuture<Void> reload(SharedState sharedState, Executor preparationExecutor,
                                           PreparationBarrier barrier, Executor applicationExecutor) {
        long generation = catalog.beginReload();
        if (generation < 0) {
            return CompletableFuture.completedFuture(null);
        }
        ResourceManager manager = sharedState.resourceManager();
        try {
            CompletableFuture<Void> reload = CompletableFuture.supplyAsync(() -> {
                        catalog.prepareAndStage(generation, manager);
                        return generation;
                    }, preparationExecutor)
                    .thenCompose(barrier::wait)
                    .thenAcceptAsync(token -> catalog.applyPrepared(token, manager), applicationExecutor);
            reload.whenComplete((ignored, error) -> {
                if (error != null) {
                    catalog.failReload(generation);
                }
            });
            return reload;
        } catch (RuntimeException error) {
            catalog.failReload(generation);
            return CompletableFuture.failedFuture(error);
        }
    }
}
