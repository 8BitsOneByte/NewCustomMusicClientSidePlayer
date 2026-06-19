package org.exmple.newcustommusicclientsideplayer.client.gui.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Coordinates dropdown overlays with a host screen.
 *
 * <p>The host calls the input methods before its normal {@code super} input
 * dispatch, calls {@link #extractOverlays} after its normal render-state
 * extraction, calls {@link #resetForRebuild} before rebuilding widgets, and
 * calls {@link #closeAll} when removed. Returning {@code false} from
 * {@link #mouseClicked} after closing an outside popup is intentional: it lets
 * the same click reach the original widget underneath.</p>
 */
public final class CDropdownScreenCoordinator {
    private final List<CDropdownSelector<?>> dropdowns = new ArrayList<>();
    private CDropdownSelector<?> draggingDropdown;

    /**
     * Registers a dropdown and returns it for convenient widget construction.
     */
    public <T> CDropdownSelector<T> register(
        CDropdownSelector<T> dropdown
    ) {
        Objects.requireNonNull(dropdown, "dropdown");
        if (!this.dropdowns.contains(dropdown)) {
            this.dropdowns.add(dropdown);
            dropdown.setOverlayManagedByHost(true);
        }
        return dropdown;
    }

    public void unregister(CDropdownSelector<?> dropdown) {
        if (this.dropdowns.remove(dropdown)) {
            if (this.draggingDropdown == dropdown) {
                this.draggingDropdown = null;
            }
            dropdown.close();
            dropdown.setOverlayManagedByHost(false);
        }
    }

    /**
     * Gives an expanded dropdown first refusal over its collapsed control and
     * popup. An outside click closes all dropdowns and remains unconsumed.
     */
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return this.mouseClicked(event, doubleClick, () -> {
        });
    }

    /**
     * Gives an expanded dropdown first refusal and clears the host's focus
     * whenever a mouse action closes it. This includes selecting a row or
     * pressing the collapsed control while it is open, so mouse users do not
     * retain a keyboard-style focus highlight. Keyboard confirmation bypasses
     * this method and therefore intentionally keeps focus. An outside click
     * remains unconsumed so the host can dispatch it to the widget underneath.
     */
    public boolean mouseClicked(
        MouseButtonEvent event,
        boolean doubleClick,
        Runnable clearHostFocus
    ) {
        Objects.requireNonNull(clearHostFocus, "clearHostFocus");
        boolean hadExpandedDropdown = this.topmostExpandedDropdown() != null;
        CDropdownSelector<?> dropdown = this.expandedDropdownAt(
            event.x(),
            event.y()
        );
        if (dropdown == null) {
            this.closeAll();
            if (hadExpandedDropdown) {
                clearHostFocus.run();
            }
            return false;
        }

        boolean handled = dropdown.mouseClicked(event, doubleClick);
        if (handled && dropdown.isDraggingScrollbar()) {
            this.draggingDropdown = dropdown;
        }
        if (handled && !dropdown.isExpanded()) {
            clearHostFocus.run();
        }
        return handled;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.draggingDropdown == null) {
            return false;
        }

        CDropdownSelector<?> dropdown = this.draggingDropdown;
        this.draggingDropdown = null;
        return dropdown.mouseReleased(event);
    }

    public boolean mouseDragged(
        MouseButtonEvent event,
        double dragX,
        double dragY
    ) {
        return this.draggingDropdown != null
            && this.draggingDropdown.mouseDragged(event, dragX, dragY);
    }

    public boolean mouseScrolled(
        double mouseX,
        double mouseY,
        double horizontalAmount,
        double verticalAmount
    ) {
        for (int index = this.dropdowns.size() - 1; index >= 0; index--) {
            CDropdownSelector<?> dropdown = this.dropdowns.get(index);
            if (dropdown.isExpanded()
                    && dropdown.mouseScrolled(
                        mouseX,
                        mouseY,
                        horizontalAmount,
                        verticalAmount
                    )) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles Escape before {@code Screen} can close itself and forwards other
     * dropdown keys to the topmost expanded selector.
     */
    public boolean keyPressed(KeyEvent event) {
        CDropdownSelector<?> dropdown = this.topmostExpandedDropdown();
        return dropdown != null && dropdown.keyPressed(event);
    }

    /**
     * Extracts expanded popups after all normal screen content.
     */
    public void extractOverlays(
        GuiGraphicsExtractor guiGraphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        for (CDropdownSelector<?> dropdown : this.dropdowns) {
            dropdown.extractExpandedOverlay(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
            );
        }
    }

    /**
     * Returns whether an expanded popup visually covers this mouse position.
     * Hosts can use this to withhold the real pointer coordinates from normal
     * content while extracting its render state.
     */
    public boolean isMouseOverExpandedOverlay(double mouseX, double mouseY) {
        for (int index = this.dropdowns.size() - 1; index >= 0; index--) {
            if (this.dropdowns.get(index).containsExpandedOverlay(
                    mouseX,
                    mouseY
                )) {
                return true;
            }
        }
        return false;
    }

    /**
     * Closes every registered dropdown without removing registrations.
     * Call this when the host screen is removed.
     */
    public void closeAll() {
        this.draggingDropdown = null;
        for (CDropdownSelector<?> dropdown : this.dropdowns) {
            dropdown.close();
        }
    }

    /**
     * Closes and unregisters old widgets. Call this before rebuilding a
     * screen's widget tree, then register the newly-created dropdowns.
     */
    public void resetForRebuild() {
        this.closeAll();
        for (CDropdownSelector<?> dropdown : this.dropdowns) {
            dropdown.setOverlayManagedByHost(false);
        }
        this.dropdowns.clear();
    }

    private CDropdownSelector<?> expandedDropdownAt(
        double mouseX,
        double mouseY
    ) {
        for (int index = this.dropdowns.size() - 1; index >= 0; index--) {
            CDropdownSelector<?> dropdown = this.dropdowns.get(index);
            if (dropdown.containsExpandedInteractionArea(mouseX, mouseY)) {
                return dropdown;
            }
        }
        return null;
    }

    private CDropdownSelector<?> topmostExpandedDropdown() {
        for (int index = this.dropdowns.size() - 1; index >= 0; index--) {
            CDropdownSelector<?> dropdown = this.dropdowns.get(index);
            if (dropdown.isExpanded()) {
                return dropdown;
            }
        }
        return null;
    }
}
