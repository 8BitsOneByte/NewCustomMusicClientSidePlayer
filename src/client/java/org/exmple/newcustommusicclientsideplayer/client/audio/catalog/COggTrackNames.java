package org.exmple.newcustommusicclientsideplayer.client.audio.catalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class COggTrackNames {
    private static List<Identifier> indexedResources;
    private static Map<String, Integer> counts = Map.of();

    private COggTrackNames() { }

    public static synchronized String defaultName(Identifier id) {
        List<Identifier> resources = CAudioCatalog.getInstance().resources();
        if (resources != indexedResources) {
            Map<String, Integer> updated = new HashMap<>();
            for (Identifier resource : resources) updated.merge(key(resource), 1, Integer::sum);
            counts = Map.copyOf(updated);
            indexedResources = resources;
        }
        String file = fileName(id);
        return counts.getOrDefault(key(id), 0) > 1 ? id.getPath()
            : file.endsWith(".ogg") ? file.substring(0, file.length() - 4) : file;
    }

    private static String key(Identifier id) {
        return id.getNamespace() + ":" + fileName(id);
    }

    private static String fileName(Identifier id) {
        String path = id.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public static int compare(Identifier left, Identifier right) {
        String[] a = left.getPath().split("/");
        String[] b = right.getPath().split("/");
        for (int i = 1; i <= Math.min(a.length, b.length); i++) {
            int order = a[a.length - i].compareTo(b[b.length - i]);
            if (order != 0) return order;
        }
        int depth = Integer.compare(a.length, b.length);
        return depth != 0 ? depth : left.getNamespace().compareTo(right.getNamespace());
    }
}
