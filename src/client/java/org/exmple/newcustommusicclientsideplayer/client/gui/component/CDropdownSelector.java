package org.exmple.newcustommusicclientsideplayer.client.gui.component;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.exmple.newcustommusicclientsideplayer.client.gui.component.CDropdownGeometry.ExpansionDirection;
import org.exmple.newcustommusicclientsideplayer.client.gui.component.CDropdownGeometry.Input;
import org.exmple.newcustommusicclientsideplayer.client.gui.component.CDropdownGeometry.Layout;
import org.exmple.newcustommusicclientsideplayer.client.gui.component.CDropdownGeometry.Rect;
import org.exmple.newcustommusicclientsideplayer.client.gui.component.CDropdownGeometry.VisibleRow;

/**
 * Reusable dropdown control with collapsed and expanded visual states, mouse
 * and keyboard input, scrolling, and narration.
 */
public final class CDropdownSelector<T> extends AbstractButton {
    private static final String EXPANDED_NARRATION_KEY =
        "narration.newcustommusicclientsideplayer.dropdown.expanded";
    private static final String COLLAPSED_NARRATION_KEY =
        "narration.newcustommusicclientsideplayer.dropdown.collapsed";

    private static final int TEXT_PADDING = 4;
    private static final int ARROW_WIDTH = 5;
    private static final int ARROW_HEIGHT = 3;
    private static final int ACTIVE_ARROW_COLOR = 0xFFFFFFFF;
    private static final int INACTIVE_ARROW_COLOR = 0xFFA0A0A0;
    private static final WidgetSprites BUTTON_SPRITES = new WidgetSprites(
        Identifier.withDefaultNamespace("widget/button"),
        Identifier.withDefaultNamespace("widget/button_disabled"),
        Identifier.withDefaultNamespace("widget/button_highlighted")
    );

    private static final int POPUP_OUTER_BORDER_COLOR = 0xFF000000;
    private static final int POPUP_INNER_BORDER_COLOR = 0xFF707070;
    private static final int POPUP_DIVIDER_COLOR = 0xFF606060;
    private static final int ROW_SEPARATOR_COLOR = 0xFF111111;
    private static final int ODD_ROW_COLOR = 0xFF202020;
    private static final int EVEN_ROW_COLOR = 0xFF292929;
    private static final int HOVERED_ROW_COLOR = 0xFF4A4A4A;
    private static final int SELECTED_TEXT_COLOR = 0xFF55FFFF;
    private static final Identifier SCROLLER_SPRITE =
        Identifier.withDefaultNamespace("widget/scroller");
    private static final Identifier SCROLLER_BACKGROUND_SPRITE =
        Identifier.withDefaultNamespace("widget/scroller_background");

    private final CDropdownSelectionModel<T> model;
    private final Function<? super T, Component> valueFormatter;
    private final Consumer<? super T> onValueChanged;
    private final ExpansionDirection expansionDirection;
    private final int preferredWidth;
    private final int rowHeight;
    private final int maxVisibleItems;
    private boolean draggingScrollbar;
    private double scrollbarDragPosition;
    private double wheelScrollRemainder;
    private boolean overlayManagedByHost;
    private boolean keyboardHighlightVisible;
    private boolean collapsedFocusOnAccessory;

    public CDropdownSelector(
        int x,
        int y,
        int preferredWidth,
        int height,
        List<? extends T> options,
        T initialValue,
        Function<? super T, Component> valueFormatter,
        ExpansionDirection expansionDirection,
        int rowHeight,
        int maxVisibleItems
    ) {
        this(
            x,
            y,
            preferredWidth,
            height,
            options,
            initialValue,
            valueFormatter,
            ignoredValue -> {
            },
            expansionDirection,
            rowHeight,
            maxVisibleItems
        );
    }

    public CDropdownSelector(
        int x,
        int y,
        int preferredWidth,
        int height,
        List<? extends T> options,
        T initialValue,
        Function<? super T, Component> valueFormatter,
        Consumer<? super T> onValueChanged,
        ExpansionDirection expansionDirection,
        int rowHeight,
        int maxVisibleItems
    ) {
        super(x, y, preferredWidth, height, CommonComponents.EMPTY);
        if (preferredWidth <= 0 || height <= 0) {
            throw new IllegalArgumentException("control dimensions must be positive");
        }
        if (rowHeight <= 0) {
            throw new IllegalArgumentException("rowHeight must be positive");
        }
        if (rowHeight != height) {
            throw new IllegalArgumentException(
                "rowHeight must match the collapsed control height"
            );
        }
        if (maxVisibleItems <= 0) {
            throw new IllegalArgumentException("maxVisibleItems must be positive");
        }

        this.preferredWidth = preferredWidth;
        this.rowHeight = rowHeight;
        this.maxVisibleItems = maxVisibleItems;
        this.valueFormatter = Objects.requireNonNull(valueFormatter, "valueFormatter");
        this.onValueChanged = Objects.requireNonNull(
            onValueChanged,
            "onValueChanged"
        );
        this.expansionDirection = Objects.requireNonNull(
            expansionDirection,
            "expansionDirection"
        );
        this.model = new CDropdownSelectionModel<>(options, initialValue);
        this.refreshCollapsedState();
    }

    public int preferredWidth() {
        return this.preferredWidth;
    }

    /**
     * Uses the preferred width when possible and shrinks to the available width
     * when the host layout is narrower.
     */
    public void fitToAvailableWidth(int availableWidth) {
        if (availableWidth <= 0) {
            throw new IllegalArgumentException("availableWidth must be positive");
        }

        this.setWidth(Math.min(this.preferredWidth, availableWidth));
    }

    public void restorePreferredWidth() {
        this.setWidth(this.preferredWidth);
    }

    public Optional<T> selectedValue() {
        return this.model.selectedValue();
    }

    public boolean isExpanded() {
        return this.model.isExpanded();
    }

    public boolean open(int screenWidth, int screenHeight) {
        Layout layout = this.calculateExpandedLayout(screenWidth, screenHeight);
        boolean opened = layout.canOpen()
            && this.model.open(layout.visibleItemCount());
        if (opened) {
            this.draggingScrollbar = false;
            this.wheelScrollRemainder = 0.0;
            this.keyboardHighlightVisible = false;
        }
        return opened;
    }

    public boolean close() {
        this.draggingScrollbar = false;
        this.wheelScrollRemainder = 0.0;
        this.keyboardHighlightVisible = false;
        return this.model.close();
    }

    public boolean selectValue(T value) {
        boolean changed = this.model.selectValue(value);
        this.refreshCollapsedState();
        return changed;
    }

    public boolean replaceOptions(List<? extends T> options) {
        boolean selectionChanged = this.model.replaceOptions(options);
        this.refreshCollapsedState();
        return selectionChanged;
    }

    public Layout calculateExpandedLayout(int screenWidth, int screenHeight) {
        return CDropdownGeometry.calculate(new Input(
            this.getX(),
            this.getY(),
            this.getWidth(),
            this.getHeight(),
            screenWidth,
            screenHeight,
            this.expansionDirection,
            this.rowHeight,
            this.maxVisibleItems,
            this.model.size(),
            this.model.firstVisibleIndex()
        ));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.visible
                || !this.active
                || !this.isValidClickButton(event.buttonInfo())
                || !this.isMouseOver(event.x(), event.y())) {
            return false;
        }

        if (this.startScrollbarDrag(event)) {
            return true;
        }

        boolean collapsedControl = this.containsCollapsedControl(
            event.x(),
            event.y()
        );
        if (collapsedControl) {
            this.collapsedFocusOnAccessory = this.containsCollapsedAccessory(
                event.x(),
                event.y()
            );
        }

        boolean actionable = collapsedControl
            || this.findClickedRow(event.x(), event.y()).isPresent();
        if (actionable) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onClick(event, doubleClick);
        }

        // Consume clicks on the popup border and reserved accessory area so
        // they cannot pass through to widgets behind the dropdown.
        return true;
    }

    @Override
    public boolean mouseScrolled(
        double mouseX,
        double mouseY,
        double horizontalAmount,
        double verticalAmount
    ) {
        if (!this.visible
                || !this.active
                || !this.model.isExpanded()
                || verticalAmount == 0.0) {
            return false;
        }

        Layout layout = this.currentExpandedLayout();
        if (!layout.canOpen()
                || !layout.popupBounds().contains(mouseX, mouseY)) {
            return false;
        }

        this.wheelScrollRemainder -= verticalAmount;
        int rowOffset = (int) this.wheelScrollRemainder;
        if (rowOffset != 0) {
            this.wheelScrollRemainder -= rowOffset;
            if (!this.model.scrollBy(rowOffset, layout.visibleItemCount())) {
                this.wheelScrollRemainder = 0.0;
            }
        }

        // Keep the wheel event from reaching a list underneath the popup,
        // including when this dropdown is already at either scroll boundary.
        return true;
    }

    @Override
    public boolean mouseDragged(
        MouseButtonEvent event,
        double dragX,
        double dragY
    ) {
        if (!this.draggingScrollbar) {
            return super.mouseDragged(event, dragX, dragY);
        }

        Layout layout = this.currentExpandedLayout();
        Optional<CDropdownGeometry.Scrollbar> scrollbar = layout.scrollbar();
        if (scrollbar.isEmpty()) {
            this.draggingScrollbar = false;
            return false;
        }

        Rect track = scrollbar.get().trackBounds();
        Rect thumb = scrollbar.get().thumbBounds();
        int maximumFirstVisibleIndex = Math.max(
            0,
            this.model.size() - layout.visibleItemCount()
        );
        int availableTravel = track.height() - thumb.height();
        if (event.y() < track.y()) {
            this.scrollbarDragPosition = 0.0;
        } else if (event.y() > track.bottom()) {
            this.scrollbarDragPosition = maximumFirstVisibleIndex;
        } else if (availableTravel > 0) {
            double indicesPerPixel =
                maximumFirstVisibleIndex / (double) availableTravel;
            this.scrollbarDragPosition += dragY * indicesPerPixel;
            this.scrollbarDragPosition = Math.max(
                0.0,
                Math.min(this.scrollbarDragPosition, maximumFirstVisibleIndex)
            );
        }

        this.model.setFirstVisibleIndex(
            (int) Math.round(this.scrollbarDragPosition),
            layout.visibleItemCount()
        );
        return true;
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        this.draggingScrollbar = false;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (this.containsCollapsedControl(event.x(), event.y())) {
            this.toggleFromMouse();
            return;
        }

        Optional<VisibleRow> clickedRow = this.findClickedRow(
            event.x(),
            event.y()
        );
        if (clickedRow.isEmpty()) {
            return;
        }

        Layout layout = this.currentExpandedLayout();
        int previousSelectedIndex = this.model.selectedIndex();
        this.model.highlightIndex(
            clickedRow.get().optionIndex(),
            layout.visibleItemCount()
        );
        this.confirmHighlightedSelection(previousSelectedIndex);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.active) {
            return false;
        }

        if (!this.model.isExpanded()) {
            return super.keyPressed(event);
        }
        if (event.isEscape()) {
            this.close();
            return true;
        }
        if (event.isUp() || event.isDown()) {
            Layout layout = this.currentExpandedLayout();
            if (layout.canOpen()) {
                this.model.moveHighlight(
                    event.isUp() ? -1 : 1,
                    layout.visibleItemCount()
                );
                this.keyboardHighlightVisible = true;
            }
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible
            && this.active
            && this.containsDropdownArea(mouseX, mouseY);
    }

    @Override
    protected void extractContents(
        GuiGraphicsExtractor guiGraphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        this.extractCollapsedSprites(guiGraphics, mouseX, mouseY);
        this.extractCollapsedValue(guiGraphics);
        this.extractArrow(guiGraphics);

        if (!this.overlayManagedByHost) {
            this.extractExpandedOverlay(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
            );
        }
    }

    /**
     * Extracts only the expanded popup on a new stratum. A host coordinator
     * calls this after the screen's normal renderables have been extracted.
     */
    public void extractExpandedOverlay(
        GuiGraphicsExtractor guiGraphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        if (!this.visible || !this.model.isExpanded()) {
            return;
        }

        Layout layout = this.calculateExpandedLayout(
            guiGraphics.guiWidth(),
            guiGraphics.guiHeight()
        );
        if (!layout.canOpen()) {
            return;
        }

        guiGraphics.nextStratum();
        guiGraphics.requestCursor(CursorTypes.ARROW);
        this.extractExpandedList(guiGraphics, mouseX, mouseY, layout);
    }

    private void extractCollapsedValue(GuiGraphicsExtractor guiGraphics) {
        int textLeft = this.getX() + TEXT_PADDING;
        int textRight = Math.max(
            textLeft,
            this.getRight()
                - CDropdownGeometry.DEFAULT_METRICS.accessoryAreaWidth()
                - TEXT_PADDING
        );
        if (textRight <= textLeft || this.getMessage().equals(CommonComponents.EMPTY)) {
            return;
        }

        ActiveTextCollector textCollector = guiGraphics.textRendererForWidget(
            this,
            GuiGraphicsExtractor.HoveredTextEffects.NONE
        );
        textCollector.acceptScrollingWithDefaultCenter(
            this.getMessage().copy().withStyle(
                style -> style.withColor(SELECTED_TEXT_COLOR)
            ),
            textLeft,
            textRight,
            this.getY(),
            this.getBottom()
        );
    }

    /**
     * Draws the value and arrow regions as adjacent vanilla buttons while the
     * selector remains one logical widget for focus, narration, and input.
     */
    private void extractCollapsedSprites(
        GuiGraphicsExtractor guiGraphics,
        int mouseX,
        int mouseY
    ) {
        int accessoryX = this.collapsedAccessoryX();
        boolean mouseOverControl = this.containsCollapsedControl(
            mouseX,
            mouseY
        );
        boolean focusHighlight = this.isFocused() && !mouseOverControl;
        boolean valueHighlighted = mouseOverControl && mouseX < accessoryX
            || focusHighlight && !this.collapsedFocusOnAccessory;
        boolean accessoryHighlighted =
            mouseOverControl && mouseX >= accessoryX
                || focusHighlight && this.collapsedFocusOnAccessory;

        int valueWidth = accessoryX - this.getX();
        if (valueWidth > 0) {
            this.extractCollapsedSprite(
                guiGraphics,
                this.getX(),
                valueWidth,
                valueHighlighted
            );
        }

        int accessoryWidth = this.getRight() - accessoryX;
        if (accessoryWidth > 0) {
            this.extractCollapsedSprite(
                guiGraphics,
                accessoryX,
                accessoryWidth,
                accessoryHighlighted
            );
        }
    }

    private void extractCollapsedSprite(
        GuiGraphicsExtractor guiGraphics,
        int x,
        int width,
        boolean highlighted
    ) {
        guiGraphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            BUTTON_SPRITES.get(this.active, highlighted),
            x,
            this.getY(),
            width,
            this.getHeight(),
            ARGB.white(this.alpha)
        );
    }

    /**
     * Points toward the configured expansion direction while collapsed and
     * reverses while expanded to communicate the action that will close it.
     */
    private void extractArrow(GuiGraphicsExtractor guiGraphics) {
        int arrowCenterX = this.getRight()
            - CDropdownGeometry.DEFAULT_METRICS.accessoryAreaWidth() / 2;
        int arrowTop = this.getY() + (this.getHeight() - ARROW_HEIGHT) / 2;
        int color = this.active ? ACTIVE_ARROW_COLOR : INACTIVE_ARROW_COLOR;
        boolean pointsDown =
            (this.expansionDirection == ExpansionDirection.DOWN)
                != this.model.isExpanded();

        if (!pointsDown) {
            for (int row = 0; row < ARROW_HEIGHT; row++) {
                int rowWidth = 1 + row * 2;
                int rowX = arrowCenterX - rowWidth / 2;
                guiGraphics.fill(
                    rowX,
                    arrowTop + row,
                    rowX + rowWidth,
                    arrowTop + row + 1,
                    color
                );
            }
            return;
        }

        for (int row = 0; row < ARROW_HEIGHT; row++) {
            int rowWidth = ARROW_WIDTH - row * 2;
            int rowX = arrowCenterX - rowWidth / 2;
            guiGraphics.fill(
                rowX,
                arrowTop + row,
                rowX + rowWidth,
                arrowTop + row + 1,
                color
            );
        }
    }

    private void extractExpandedList(
        GuiGraphicsExtractor guiGraphics,
        int mouseX,
        int mouseY,
        Layout layout
    ) {
        Rect popup = layout.popupBounds();
        guiGraphics.fill(
            popup.x(),
            popup.y(),
            popup.right(),
            popup.bottom(),
            POPUP_OUTER_BORDER_COLOR
        );
        if (popup.width() > 2 && popup.height() > 2) {
            guiGraphics.outline(
                popup.x() + 1,
                popup.y() + 1,
                popup.width() - 2,
                popup.height() - 2,
                POPUP_INNER_BORDER_COLOR
            );
        }

        Font font = Minecraft.getInstance().font;
        for (VisibleRow row : layout.visibleRows()) {
            this.extractExpandedRow(guiGraphics, font, mouseX, mouseY, row);
        }

        Rect divider = layout.dividerBounds();
        guiGraphics.fill(
            divider.x(),
            divider.y(),
            divider.right(),
            divider.bottom(),
            POPUP_DIVIDER_COLOR
        );

        layout.scrollbar().ifPresent(scrollbar -> {
            Rect track = scrollbar.trackBounds();
            guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                SCROLLER_BACKGROUND_SPRITE,
                track.x(),
                track.y(),
                track.width(),
                track.height()
            );

            Rect thumb = scrollbar.thumbBounds();
            guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                SCROLLER_SPRITE,
                thumb.x(),
                thumb.y(),
                thumb.width(),
                thumb.height()
            );
            if (track.contains(mouseX, mouseY)) {
                guiGraphics.requestCursor(
                    this.draggingScrollbar
                        ? CursorTypes.RESIZE_NS
                        : CursorTypes.POINTING_HAND
                );
            }
        });
    }

    private void extractExpandedRow(
        GuiGraphicsExtractor guiGraphics,
        Font font,
        int mouseX,
        int mouseY,
        VisibleRow row
    ) {
        Rect rowBounds = row.rowBounds();
        Rect textBounds = row.textBounds();
        boolean selected = row.optionIndex() == this.model.selectedIndex();
        boolean hovered = textBounds.contains(mouseX, mouseY);
        boolean highlighted = row.optionIndex() == this.model.highlightedIndex();
        boolean emphasized = hovered
            || this.keyboardHighlightVisible && highlighted;
        int backgroundColor = emphasized
            ? HOVERED_ROW_COLOR
            : row.optionIndex() % 2 == 0
                ? EVEN_ROW_COLOR
                : ODD_ROW_COLOR;

        guiGraphics.fill(
            textBounds.x(),
            textBounds.y(),
            textBounds.right(),
            textBounds.bottom(),
            backgroundColor
        );
        guiGraphics.fill(
            textBounds.x(),
            textBounds.bottom() - 1,
            textBounds.right(),
            textBounds.bottom(),
            ROW_SEPARATOR_COLOR
        );

        int textLeft = textBounds.x() + TEXT_PADDING;
        int textWidth = Math.max(0, textBounds.right() - TEXT_PADDING - textLeft);
        if (textWidth == 0) {
            return;
        }

        Component value = this.formatValue(
            this.model.options().get(row.optionIndex())
        );
        if (selected) {
            value = value.copy().withStyle(style -> style.withColor(SELECTED_TEXT_COLOR));
        }

        int textY = rowBounds.y() + (rowBounds.height() - font.lineHeight) / 2;
        if (font.width(value) > textWidth) {
            guiGraphics.text(
                font,
                ComponentRenderUtils.clipText(value, font, textWidth),
                textLeft,
                textY,
                0xFFFFFFFF,
                false
            );
        } else {
            guiGraphics.text(font, value, textLeft, textY, 0xFFFFFFFF, false);
        }
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (!this.active || !input.isSelection()) {
            return;
        }

        this.collapsedFocusOnAccessory = false;
        if (this.model.isExpanded()) {
            this.confirmHighlightedSelection(this.model.selectedIndex());
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        this.open(
            minecraft.getWindow().getGuiScaledWidth(),
            minecraft.getWindow().getGuiScaledHeight()
        );
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
        output.add(
            NarratedElementType.HINT,
            Component.translatable(
                this.model.isExpanded()
                    ? EXPANDED_NARRATION_KEY
                    : COLLAPSED_NARRATION_KEY
            )
        );

        int narratedIndex = this.model.isExpanded()
            ? this.model.highlightedIndex()
            : this.model.selectedIndex();
        if (narratedIndex >= 0 && this.model.size() > 1) {
            output.add(
                NarratedElementType.POSITION,
                Component.translatable(
                    "narrator.position.list",
                    narratedIndex + 1,
                    this.model.size()
                )
            );
        }
        if (this.model.isExpanded()) {
            output.add(
                NarratedElementType.USAGE,
                Component.translatable("narration.selection.usage")
            );
        }
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        Component narratedValue = (
            this.model.isExpanded()
                ? this.model.highlightedValue()
                : this.model.selectedValue()
        ).map(this::formatValue).orElse(CommonComponents.EMPTY);
        return wrapDefaultNarrationMessage(narratedValue);
    }

    private void refreshCollapsedState() {
        if (this.model.size() <= 1) {
            this.model.close();
            this.draggingScrollbar = false;
            this.wheelScrollRemainder = 0.0;
            this.keyboardHighlightVisible = false;
        }

        this.setMessage(
            this.model.selectedValue()
                .map(this::formatValue)
                .orElse(CommonComponents.EMPTY)
        );
        this.active = this.model.size() > 1;
    }

    private void toggleFromMouse() {
        if (this.model.isExpanded()) {
            this.close();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        this.open(
            minecraft.getWindow().getGuiScaledWidth(),
            minecraft.getWindow().getGuiScaledHeight()
        );
    }

    private void confirmHighlightedSelection(int previousSelectedIndex) {
        Optional<T> confirmedValue = this.model.confirmHighlighted();
        this.draggingScrollbar = false;
        this.wheelScrollRemainder = 0.0;
        this.keyboardHighlightVisible = false;
        this.refreshCollapsedState();
        if (confirmedValue.isPresent()
                && previousSelectedIndex != this.model.selectedIndex()) {
            this.onValueChanged.accept(confirmedValue.get());
        }
    }

    private Layout currentExpandedLayout() {
        Minecraft minecraft = Minecraft.getInstance();
        return this.calculateExpandedLayout(
            minecraft.getWindow().getGuiScaledWidth(),
            minecraft.getWindow().getGuiScaledHeight()
        );
    }

    private Optional<VisibleRow> findClickedRow(double mouseX, double mouseY) {
        if (!this.model.isExpanded()) {
            return Optional.empty();
        }

        Layout layout = this.currentExpandedLayout();
        if (!layout.canOpen()) {
            return Optional.empty();
        }

        return layout.visibleRows()
            .stream()
            .filter(row -> row.textBounds().contains(mouseX, mouseY))
            .findFirst();
    }

    private boolean startScrollbarDrag(MouseButtonEvent event) {
        if (!this.model.isExpanded()) {
            return false;
        }

        Layout layout = this.currentExpandedLayout();
        Optional<CDropdownGeometry.Scrollbar> scrollbar = layout.scrollbar();
        if (scrollbar.isEmpty()
                || !scrollbar.get().trackBounds().contains(event.x(), event.y())) {
            return false;
        }

        this.draggingScrollbar = true;
        this.scrollbarDragPosition = this.model.firstVisibleIndex();
        return true;
    }

    private boolean containsDropdownArea(double mouseX, double mouseY) {
        if (this.containsCollapsedControl(mouseX, mouseY)) {
            return true;
        }
        if (!this.model.isExpanded()) {
            return false;
        }

        Layout layout = this.currentExpandedLayout();
        return layout.canOpen()
            && layout.popupBounds().contains(mouseX, mouseY);
    }

    void setOverlayManagedByHost(boolean overlayManagedByHost) {
        this.overlayManagedByHost = overlayManagedByHost;
    }

    boolean isDraggingScrollbar() {
        return this.draggingScrollbar;
    }

    boolean containsExpandedInteractionArea(double mouseX, double mouseY) {
        return this.model.isExpanded()
            && this.containsDropdownArea(mouseX, mouseY);
    }

    boolean containsExpandedOverlay(double mouseX, double mouseY) {
        if (!this.model.isExpanded()) {
            return false;
        }

        Layout layout = this.currentExpandedLayout();
        return layout.canOpen()
            && layout.popupBounds().contains(mouseX, mouseY);
    }

    private boolean containsCollapsedControl(double mouseX, double mouseY) {
        return mouseX >= this.getX()
            && mouseX < this.getRight()
            && mouseY >= this.getY()
            && mouseY < this.getBottom();
    }

    private boolean containsCollapsedAccessory(double mouseX, double mouseY) {
        return this.containsCollapsedControl(mouseX, mouseY)
            && mouseX >= this.collapsedAccessoryX();
    }

    private int collapsedAccessoryX() {
        return Math.max(
            this.getX(),
            this.getRight()
                - CDropdownGeometry.DEFAULT_METRICS.accessoryAreaWidth()
        );
    }

    private Component formatValue(T value) {
        return Objects.requireNonNull(
            this.valueFormatter.apply(value),
            "formatted value"
        );
    }
}
