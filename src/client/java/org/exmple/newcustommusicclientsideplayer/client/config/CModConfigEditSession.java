package org.exmple.newcustommusicclientsideplayer.client.config;

import java.util.Objects;

public final class CModConfigEditSession {
    private CModConfig applied;
    private CModConfig draft;

    public CModConfigEditSession(CModConfig applied) {
        this.applied = Objects.requireNonNull(applied, "applied");
        this.draft = applied;
    }

    public CModConfig applied() {
        return this.applied;
    }

    public CModConfig draft() {
        return this.draft;
    }

    public boolean isDirty() {
        return !this.draft.equals(this.applied);
    }

    public void setCheckForUpdates(boolean enabled) {
        this.draft = this.draft.withCheckForUpdates(enabled);
    }

    /**
     * Replaces only the editable draft. Import flows use this to preview imported values without
     * changing the clean applied baseline or saving to disk.
     */
    public void replaceDraft(CModConfig draft) {
        this.draft = Objects.requireNonNull(draft, "draft");
    }

    public void resetDraftToDefaults() {
        this.replaceDraft(CModConfig.defaults());
    }

    /**
     * Promotes the current draft to the clean applied baseline. This method does not persist the
     * configuration or change runtime services; callers must perform those operations successfully
     * before committing the draft so a failed Apply leaves the session dirty.
     */
    public void applyDraft() {
        this.applied = this.draft;
    }

    /**
     * Discards only changes made since the most recent successful Apply. Because Apply replaces the
     * baseline, a later Back or Esc restores that latest applied value rather than the value that was
     * loaded when the session was originally opened.
     */
    public void discardDraft() {
        this.draft = this.applied;
    }
}
