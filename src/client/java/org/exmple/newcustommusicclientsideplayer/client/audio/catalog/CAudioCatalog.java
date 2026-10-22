package org.exmple.newcustommusicclientsideplayer.client.audio.catalog;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public final class CAudioCatalog implements AutoCloseable {
    private final Object lock = new Object();
    private final Object preparationLock = new Object();
    private final CAudioCatalogScanner scanner = new CAudioCatalogScanner(this);
    private volatile CAudioCatalogIndex current = CAudioCatalogIndex.empty(0);
    private CAudioCatalogIndex prepared;
    private long requestedGeneration;
    private ScanState state = ScanState.EMPTY;

    public static CAudioCatalog getInstance() {
        return Holder.INSTANCE;
    }

    public Set<String> namespaces() {
        return current.namespaces();
    }

    public List<Identifier> resources(String namespace) {
        return current.resources(namespace);
    }

    public List<Identifier> resources() {
        return current.resources;
    }

    public Optional<CAudioResourceInfo> find(Identifier resourceId) {
        return Optional.ofNullable(current.find(resourceId));
    }

    public ScanStatus scanStatus() {
        synchronized (lock) {
            return new ScanStatus(current.generation, requestedGeneration, state,
                    current.resources.size(), current.completed, current.failed);
        }
    }

    long beginReload() {
        long generation;
        synchronized (lock) {
            if (state == ScanState.CLOSED) {
                return -1;
            }
            generation = ++requestedGeneration;
            prepared = null;
            state = ScanState.RELOADING;
        }
        scanner.cancelBefore(generation);
        return generation;
    }

    CAudioCatalogIndex prepare(long generation, ResourceManager manager) {
        synchronized (preparationLock) {
            synchronized (lock) {
                if (generation != requestedGeneration || state != ScanState.RELOADING) {
                    return CAudioCatalogIndex.empty(generation);
                }
            }
            var found = manager.listResources("sounds", id -> id.getPath().endsWith(".ogg"));
            synchronized (lock) {
                if (generation != requestedGeneration || state != ScanState.RELOADING) {
                    return CAudioCatalogIndex.empty(generation);
                }
            }
            return CAudioCatalogIndex.build(generation, found, current);
        }
    }

    void apply(CAudioCatalogIndex index, ResourceManager manager) {
        synchronized (lock) {
            if (index.generation != requestedGeneration || state != ScanState.RELOADING) {
                return;
            }
            current = index;
            state = index.resources.isEmpty() ? ScanState.COMPLETE : ScanState.SCANNING;
            if (!index.resources.isEmpty()) {
                scanner.submit(index, manager);
            }
        }
    }

    void prepareAndStage(long generation, ResourceManager manager) {
        CAudioCatalogIndex index = prepare(generation, manager);
        synchronized (lock) {
            if (generation == requestedGeneration && state == ScanState.RELOADING) {
                prepared = index;
            }
        }
    }

    void applyPrepared(long generation, ResourceManager manager) {
        synchronized (lock) {
            if (prepared == null || prepared.generation != generation) {
                return;
            }
            CAudioCatalogIndex index = prepared;
            prepared = null;
            apply(index, manager);
        }
    }

    void failReload(long generation) {
        boolean cancelled = false;
        synchronized (lock) {
            if (requestedGeneration == generation && state != ScanState.CLOSED) {
                prepared = null;
                state = ScanState.RELOAD_FAILED;
                cancelled = true;
            }
        }
        if (cancelled) {
            scanner.cancelBefore(generation + 1);
        }
    }

    void complete(CAudioCatalogIndex index, Identifier id,
                  CAudioResourceInfo.MetadataStatus result, CAudioResourceInfo.Metadata metadata) {
        synchronized (lock) {
            if (index != current || index.generation != requestedGeneration || state != ScanState.SCANNING) {
                return;
            }
            index.complete(id, result, metadata);
            if (index.completed == index.resources.size()) {
                state = ScanState.COMPLETE;
            }
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            state = ScanState.CLOSED;
            prepared = null;
            current = CAudioCatalogIndex.empty(++requestedGeneration);
        }
        scanner.close();
    }

    public enum ScanState {
        EMPTY, RELOADING, SCANNING, COMPLETE, RELOAD_FAILED, CLOSED
    }

    public record ScanStatus(long generation, long requestedGeneration, ScanState state,
                             int total, int completed, int failed) {
    }

    private static final class Holder {
        private static final CAudioCatalog INSTANCE = new CAudioCatalog();
    }
}
