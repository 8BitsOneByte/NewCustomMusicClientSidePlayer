package org.exmple.newcustommusicclientsideplayer.client.audio.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

final class CAudioCatalogIndex {
    final long generation;
    final List<Identifier> resources;
    private final Map<Identifier, Integer> positions;
    private final Map<String, List<Identifier>> byNamespace;
    private final AtomicReferenceArray<CAudioResourceInfo> entries;
    int completed;
    int failed;

    private CAudioCatalogIndex(long generation, List<Identifier> resources,
                               Map<Identifier, Integer> positions,
                               Map<String, List<Identifier>> byNamespace,
                               AtomicReferenceArray<CAudioResourceInfo> entries) {
        this.generation = generation;
        this.resources = resources;
        this.positions = positions;
        this.byNamespace = byNamespace;
        this.entries = entries;
    }

    static CAudioCatalogIndex empty(long generation) {
        return new CAudioCatalogIndex(generation, List.of(), Map.of(), Map.of(),
                new AtomicReferenceArray<>(0));
    }

    static CAudioCatalogIndex build(long generation, Map<Identifier, Resource> found,
                                    CAudioCatalogIndex previous) {
        List<Identifier> ids = new ArrayList<>(found.keySet());
        ids.sort(Comparator.comparing(Identifier::getNamespace).thenComparing(Identifier::getPath));
        Map<Identifier, Integer> positions = new HashMap<>();
        Map<String, List<Identifier>> groups = new LinkedHashMap<>();
        Map<String, String> sources = new HashMap<>();
        AtomicReferenceArray<CAudioResourceInfo> entries = new AtomicReferenceArray<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            Identifier id = ids.get(i);
            String source = found.get(id).sourcePackId();
            CAudioResourceInfo old = previous.find(id);
            if (old != null) {
                id = old.resourceId();
                if (source.equals(old.sourcePackId())) {
                    source = old.sourcePackId();
                }
            }
            String sharedSource = sources.putIfAbsent(source, source);
            if (sharedSource != null) {
                source = sharedSource;
            }
            ids.set(i, id);
            positions.put(id, i);
            groups.computeIfAbsent(id.getNamespace(), ignored -> new ArrayList<>()).add(id);
            entries.set(i, CAudioResourceInfo.pending(id, source));
        }
        groups.replaceAll((namespace, values) -> List.copyOf(values));
        return new CAudioCatalogIndex(generation, List.copyOf(ids), Map.copyOf(positions),
                Collections.unmodifiableMap(groups), entries);
    }

    Set<String> namespaces() {
        return byNamespace.keySet();
    }

    List<Identifier> resources(String namespace) {
        return byNamespace.getOrDefault(namespace, List.of());
    }

    CAudioResourceInfo find(Identifier id) {
        Integer position = positions.get(id);
        return position == null ? null : entries.get(position);
    }

    void complete(Identifier id, CAudioResourceInfo.MetadataStatus status,
                  CAudioResourceInfo.Metadata metadata) {
        int position = positions.get(id);
        CAudioResourceInfo old = entries.get(position);
        if (old.status() != CAudioResourceInfo.MetadataStatus.PENDING) {
            return;
        }
        entries.set(position, old.withResult(status, metadata));
        completed++;
        if (status != CAudioResourceInfo.MetadataStatus.READY) {
            failed++;
        }
    }
}
