package org.exmple.newcustommusicclientsideplayer.client.gui.component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Stores the selection and viewport state of a dropdown without depending on
 * Minecraft rendering or input classes.
 */
public final class CDropdownSelectionModel<T> {
    private List<T> options = List.of();
    private int selectedIndex = -1;
    private int highlightedIndex = -1;
    private int firstVisibleIndex;
    private boolean expanded;

    public CDropdownSelectionModel(List<? extends T> options) {
        this.replaceOptions(options);
    }

    public CDropdownSelectionModel(List<? extends T> options, T initialValue) {
        this.replaceOptions(options);
        this.selectValue(initialValue);
    }

    public List<T> options() {
        return this.options;
    }

    public int size() {
        return this.options.size();
    }

    public boolean isEmpty() {
        return this.options.isEmpty();
    }

    public int selectedIndex() {
        return this.selectedIndex;
    }

    public Optional<T> selectedValue() {
        return this.valueAt(this.selectedIndex);
    }

    public int highlightedIndex() {
        return this.highlightedIndex;
    }

    public Optional<T> highlightedValue() {
        return this.valueAt(this.highlightedIndex);
    }

    public int firstVisibleIndex() {
        return this.firstVisibleIndex;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    /**
     * Replaces all options while preserving the selected and highlighted values
     * when equal values are still present.
     *
     * @return whether the selected value changed
     */
    public boolean replaceOptions(List<? extends T> replacement) {
        Objects.requireNonNull(replacement, "replacement");
        T previousSelection = this.selectedValue().orElse(null);
        T previousHighlight = this.highlightedValue().orElse(null);

        this.options = List.copyOf(replacement);
        int replacementSelection = this.indexOfValue(previousSelection);
        this.selectedIndex = replacementSelection >= 0
            ? replacementSelection
            : (this.options.isEmpty() ? -1 : 0);

        int replacementHighlight = this.indexOfValue(previousHighlight);
        this.highlightedIndex = replacementHighlight >= 0
            ? replacementHighlight
            : this.selectedIndex;
        this.firstVisibleIndex = this.clampFirstVisibleIndex(this.firstVisibleIndex, 1);

        if (this.options.isEmpty()) {
            this.expanded = false;
        }

        return !Objects.equals(previousSelection, this.selectedValue().orElse(null));
    }

    /**
     * Selects an existing value without changing the expanded state.
     *
     * @return whether the selected value changed
     */
    public boolean selectValue(T value) {
        return this.selectIndex(this.indexOfValue(value));
    }

    /**
     * Selects an existing index without changing the expanded state.
     *
     * @return whether the selected value changed
     */
    public boolean selectIndex(int index) {
        if (!this.isValidIndex(index)) {
            return false;
        }

        boolean changed = index != this.selectedIndex;
        this.selectedIndex = index;
        this.highlightedIndex = index;
        return changed;
    }

    public boolean open(int visibleItemCount) {
        requirePositiveVisibleItemCount(visibleItemCount);
        if (this.options.size() <= 1) {
            return false;
        }

        this.expanded = true;
        this.highlightedIndex = this.selectedIndex;
        this.ensureHighlightedVisible(visibleItemCount);
        return true;
    }

    public boolean close() {
        if (!this.expanded) {
            return false;
        }

        this.expanded = false;
        this.highlightedIndex = this.selectedIndex;
        return true;
    }

    public boolean toggle(int visibleItemCount) {
        return this.expanded ? this.close() : this.open(visibleItemCount);
    }

    public boolean highlightIndex(int index, int visibleItemCount) {
        requirePositiveVisibleItemCount(visibleItemCount);
        if (!this.expanded || !this.isValidIndex(index)) {
            return false;
        }

        boolean changed = index != this.highlightedIndex;
        this.highlightedIndex = index;
        this.ensureHighlightedVisible(visibleItemCount);
        return changed;
    }

    /**
     * Moves the highlighted item without wrapping at either end.
     */
    public boolean moveHighlight(int offset, int visibleItemCount) {
        requirePositiveVisibleItemCount(visibleItemCount);
        if (!this.expanded || this.options.isEmpty() || offset == 0) {
            return false;
        }

        int current = this.isValidIndex(this.highlightedIndex)
            ? this.highlightedIndex
            : Math.max(0, this.selectedIndex);
        int target = Math.max(0, Math.min(this.options.size() - 1, current + offset));
        return this.highlightIndex(target, visibleItemCount);
    }

    /**
     * Confirms the highlighted item and closes the dropdown. Selecting the
     * already-selected item still returns that value so callers can treat the
     * click as a completed selection.
     */
    public Optional<T> confirmHighlighted() {
        if (!this.expanded || !this.isValidIndex(this.highlightedIndex)) {
            return Optional.empty();
        }

        this.selectedIndex = this.highlightedIndex;
        T selected = this.options.get(this.selectedIndex);
        this.close();
        return Optional.of(selected);
    }

    public boolean scrollBy(int offset, int visibleItemCount) {
        requirePositiveVisibleItemCount(visibleItemCount);
        if (!this.expanded || offset == 0) {
            return false;
        }

        return this.setFirstVisibleIndex(this.firstVisibleIndex + offset, visibleItemCount);
    }

    public boolean setFirstVisibleIndex(int index, int visibleItemCount) {
        requirePositiveVisibleItemCount(visibleItemCount);
        int clamped = this.clampFirstVisibleIndex(index, visibleItemCount);
        boolean changed = clamped != this.firstVisibleIndex;
        this.firstVisibleIndex = clamped;
        return changed;
    }

    public int visibleEndIndexExclusive(int visibleItemCount) {
        requirePositiveVisibleItemCount(visibleItemCount);
        return Math.min(this.options.size(), this.firstVisibleIndex + visibleItemCount);
    }

    public void ensureHighlightedVisible(int visibleItemCount) {
        requirePositiveVisibleItemCount(visibleItemCount);
        if (!this.isValidIndex(this.highlightedIndex)) {
            this.firstVisibleIndex = this.clampFirstVisibleIndex(this.firstVisibleIndex, visibleItemCount);
            return;
        }

        if (this.highlightedIndex < this.firstVisibleIndex) {
            this.firstVisibleIndex = this.highlightedIndex;
        } else if (this.highlightedIndex >= this.firstVisibleIndex + visibleItemCount) {
            this.firstVisibleIndex = this.highlightedIndex - visibleItemCount + 1;
        }

        this.firstVisibleIndex = this.clampFirstVisibleIndex(this.firstVisibleIndex, visibleItemCount);
    }

    private Optional<T> valueAt(int index) {
        return this.isValidIndex(index)
            ? Optional.of(this.options.get(index))
            : Optional.empty();
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < this.options.size();
    }

    private int indexOfValue(T value) {
        return value == null ? -1 : this.options.indexOf(value);
    }

    private int clampFirstVisibleIndex(int index, int visibleItemCount) {
        int maximum = Math.max(0, this.options.size() - visibleItemCount);
        return Math.max(0, Math.min(index, maximum));
    }

    private static void requirePositiveVisibleItemCount(int visibleItemCount) {
        if (visibleItemCount <= 0) {
            throw new IllegalArgumentException("visibleItemCount must be positive");
        }
    }
}
