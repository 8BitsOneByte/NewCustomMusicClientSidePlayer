package org.exmple.newcustommusicclientsideplayer.client.audio.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

final class CAudioCatalogScanner implements AutoCloseable {
    private final Object lock = new Object();
    private final CAudioCatalog catalog;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "CustomMusic-AudioCatalog");
        thread.setDaemon(true);
        return thread;
    });
    private Job pending;
    private Job active;
    private boolean draining;
    private boolean closed;

    CAudioCatalogScanner(CAudioCatalog catalog) {
        this.catalog = catalog;
    }

    void submit(CAudioCatalogIndex index, ResourceManager manager) {
        synchronized (lock) {
            if (closed) {
                return;
            }
            pending = new Job(index, manager);
            if (!draining) {
                draining = true;
                executor.execute(this::drain);
            }
        }
    }

    void cancelBefore(long generation) {
        Job cancelled = null;
        synchronized (lock) {
            if (pending != null && pending.index.generation < generation) {
                pending = null;
            }
            if (active != null && active.index.generation < generation) {
                cancelled = active;
                cancelled.cancelled = true;
            }
        }
        if (cancelled != null) {
            cancelled.closeStream();
        }
    }

    private void drain() {
        COggMetadataReader reader = new COggMetadataReader();
        while (true) {
            Job job;
            synchronized (lock) {
                job = pending;
                pending = null;
                if (job == null || closed) {
                    active = null;
                    draining = false;
                    return;
                }
                active = job;
            }
            try {
                scan(job, reader);
            } finally {
                job.closeStream();
                synchronized (lock) {
                    active = null;
                }
            }
        }
    }

    private void scan(Job job, COggMetadataReader reader) {
        for (Identifier id : job.index.resources) {
            if (job.cancelled) {
                return;
            }
            CAudioResourceInfo.MetadataStatus status;
            CAudioResourceInfo.Metadata metadata = null;
            try {
                Resource resource = job.manager.getResource(id).orElseThrow(() -> new IOException("Missing resource"));
                if (!resource.sourcePackId().equals(job.index.find(id).sourcePackId())) {
                    throw new IOException("Changed source");
                }
                InputStream stream = resource.open();
                job.stream.set(stream);
                if (job.cancelled) {
                    return;
                }
                metadata = reader.read(stream, () -> job.cancelled);
                status = CAudioResourceInfo.MetadataStatus.READY;
            } catch (CancellationException ignored) {
                return;
            } catch (COggMetadataReader.ReadException exception) {
                status = exception.status;
            } catch (IOException | RuntimeException exception) {
                status = CAudioResourceInfo.MetadataStatus.IO_ERROR;
            } finally {
                job.closeStream();
            }
            if (job.cancelled) {
                return;
            }
            catalog.complete(job.index, id, status, metadata);
        }
    }

    @Override
    public void close() {
        Job cancelled;
        synchronized (lock) {
            closed = true;
            pending = null;
            cancelled = active;
            if (cancelled != null) {
                cancelled.cancelled = true;
            }
        }
        if (cancelled != null) {
            cancelled.closeStream();
        }
        executor.shutdownNow();
    }

    private static final class Job {
        final CAudioCatalogIndex index;
        final ResourceManager manager;
        final AtomicReference<InputStream> stream = new AtomicReference<>();
        volatile boolean cancelled;

        Job(CAudioCatalogIndex index, ResourceManager manager) {
            this.index = index;
            this.manager = manager;
        }

        void closeStream() {
            InputStream owned = stream.getAndSet(null);
            if (owned != null) {
                try {
                    owned.close();
                } catch (IOException | RuntimeException ignored) {
                }
            }
        }
    }
}
