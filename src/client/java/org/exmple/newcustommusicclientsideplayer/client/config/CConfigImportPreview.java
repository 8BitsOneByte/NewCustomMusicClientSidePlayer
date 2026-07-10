package org.exmple.newcustommusicclientsideplayer.client.config;

import java.util.List;
import java.util.Objects;

public final class CConfigImportPreview {
    private final CModConfig compatibleConfig;
    private final boolean hasCompatibleChanges;
    private final List<String> skippedFields;

    CConfigImportPreview(CModConfig compatibleConfig, boolean hasCompatibleChanges, List<String> skippedFields) {
        this.compatibleConfig = Objects.requireNonNull(compatibleConfig, "compatibleConfig");
        this.hasCompatibleChanges = hasCompatibleChanges;
        this.skippedFields = List.copyOf(skippedFields);
    }

    public CModConfig compatibleConfig() {
        return this.compatibleConfig;
    }

    public boolean hasCompatibleChanges() {
        return this.hasCompatibleChanges;
    }

    public boolean hasSkippedFields() {
        return !this.skippedFields.isEmpty();
    }

    public List<String> skippedFields() {
        return this.skippedFields;
    }
}
