package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

public final class CImportMessageScreen extends Screen {
    private static final int MESSAGE_MARGIN = 100;
    private static final int MESSAGE_PADDING = 12;
    private static final int LAYOUT_SPACING = 8;
    private static final int DOUBLE_BUTTON_WIDTH = 150;
    private static final int BUTTON_SPACING = 8;
    private static final int MAX_VISIBLE_LIST_ROWS = 12;
    private static final int LIST_ROW_HEIGHT = 12;

    private final Screen parent;
    private final Component body;
    private final @Nullable Component listTitle;
    private final List<Component> listItems;
    private final Component primaryButton;
    private final Runnable primaryAction;
    private final @Nullable Component secondaryButton;
    private final @Nullable Runnable secondaryAction;
    private final FrameLayout layout = new FrameLayout();
    private FittingMultiLineTextWidget messageWidget;
    private MessageLineList messageLineList;
    private List<FormattedCharSequence> wrappedMessageLines = List.of();

    public CImportMessageScreen(
        Screen parent,
        Component title,
        Component body,
        @Nullable Component listTitle,
        List<Component> listItems,
        Component primaryButton,
        Runnable primaryAction,
        @Nullable Component secondaryButton,
        @Nullable Runnable secondaryAction
    ) {
        super(Objects.requireNonNull(title, "title"));
        this.parent = Objects.requireNonNull(parent, "parent");
        this.body = Objects.requireNonNull(body, "body");
        this.listTitle = listTitle;
        this.listItems = List.copyOf(Objects.requireNonNull(listItems, "listItems"));
        this.primaryButton = Objects.requireNonNull(primaryButton, "primaryButton");
        this.primaryAction = Objects.requireNonNull(primaryAction, "primaryAction");
        this.secondaryButton = secondaryButton;
        this.secondaryAction = secondaryAction;
    }

    @Override
    protected void init() {
        this.layout.removeChildren();

        LinearLayout content = this.layout.addChild(LinearLayout.vertical().spacing(LAYOUT_SPACING));
        content.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(new StringWidget(this.title, this.font));

        this.wrappedMessageLines = this.wrappedMessageLines();
        if (this.shouldScrollMessage()) {
            this.messageLineList = content.addChild(new MessageLineList(
                this.minecraft,
                this.font,
                this.messageWidth(),
                this.messageHeight(),
                this.wrappedMessageLines
            ));
        } else {
            this.messageWidget = content.addChild(
                new FittingMultiLineTextWidget(
                    0,
                    0,
                    this.messageWidth(),
                    this.messageHeight(),
                    this.message(),
                    this.font
                ),
                settings -> settings.padding(MESSAGE_PADDING)
            );
        }

        LinearLayout buttonRow = content.addChild(LinearLayout.horizontal().spacing(BUTTON_SPACING));
        if (this.secondaryButton == null || this.secondaryAction == null) {
            buttonRow.addChild(Button.builder(this.primaryButton, button -> this.primaryAction.run())
                .build());
        } else {
            buttonRow.addChild(Button.builder(this.primaryButton, button -> this.primaryAction.run())
                .width(DOUBLE_BUTTON_WIDTH)
                .build());
            buttonRow.addChild(Button.builder(this.secondaryButton, button -> this.secondaryAction.run())
                .width(DOUBLE_BUTTON_WIDTH)
                .build());
        }

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        if (this.messageWidget != null) {
            this.messageWidget.setWidth(this.messageWidth());
            this.messageWidget.setHeight(this.messageHeight());
            this.messageWidget.minimizeHeight();
        }
        if (this.messageLineList != null) {
            this.wrappedMessageLines = this.wrappedMessageLines();
            this.messageLineList.updateLines(this.wrappedMessageLines);
            this.messageLineList.updateSize(this.messageWidth(), this.messageHeight());
        }
        this.layout.arrangeElements();
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
        if (this.messageLineList != null) {
            this.messageLineList.updateSize(this.messageWidth(), this.messageHeight());
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    private int messageWidth() {
        return Math.max(DOUBLE_BUTTON_WIDTH, this.width - MESSAGE_MARGIN);
    }

    private int messageHeight() {
        int visibleRows = this.shouldScrollMessage()
            ? MAX_VISIBLE_LIST_ROWS
            : Math.max(1, this.wrappedMessageLines.size());
        return visibleRows * LIST_ROW_HEIGHT;
    }

    private boolean shouldScrollMessage() {
        return this.wrappedMessageLines.size() > MAX_VISIBLE_LIST_ROWS;
    }

    private Component message() {
        MutableComponent message = Component.empty().append(this.body);
        if (this.listTitle != null || !this.listItems.isEmpty()) {
            message.append("\n");
        }
        if (this.listTitle != null) {
            message.append("\n").append(this.listTitle);
        }
        for (Component item : this.listItems) {
            message.append("\n").append(Component.literal("- ")).append(item);
        }

        return message;
    }

    private List<FormattedCharSequence> wrappedMessageLines() {
        java.util.ArrayList<FormattedCharSequence> lines = new java.util.ArrayList<>();
        for (Component line : this.logicalMessageLines()) {
            List<FormattedCharSequence> wrapped = this.font.split(line, this.messageTextWidth());
            if (wrapped.isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
            } else {
                lines.addAll(wrapped);
            }
        }

        return lines;
    }

    private List<Component> logicalMessageLines() {
        java.util.ArrayList<Component> lines = new java.util.ArrayList<>();
        String[] bodyLines = this.body.getString().split("\n", -1);
        for (String bodyLine : bodyLines) {
            lines.add(Component.literal(bodyLine).withStyle(this.body.getStyle()));
        }
        if (this.listTitle != null || !this.listItems.isEmpty()) {
            lines.add(Component.empty());
        }
        if (this.listTitle != null) {
            lines.add(this.listTitle);
        }
        for (Component item : this.listItems) {
            lines.add(Component.empty().append(Component.literal("- ")).append(item));
        }
        return lines;
    }

    private int messageTextWidth() {
        return this.messageWidth() - MESSAGE_PADDING * 2;
    }

    private static final class MessageLineList extends ObjectSelectionList<MessageLineList.Entry> {
        private final Font font;
        private int rowWidth;

        private MessageLineList(Minecraft minecraft, Font font, int width, int height, List<FormattedCharSequence> lines) {
            super(minecraft, width, height, 0, LIST_ROW_HEIGHT);
            this.font = font;
            this.rowWidth = width;
            this.centerListVertically = false;

            this.updateLines(lines);
        }

        private void updateSize(int width, int height) {
            this.rowWidth = width;
            this.updateSizeAndPosition(width, height, this.getX(), this.getY());
        }

        private void updateLines(List<FormattedCharSequence> lines) {
            this.clearEntries();
            for (FormattedCharSequence line : lines) {
                this.addEntry(new Entry(this, line));
            }
            this.setScrollAmount(0.0);
        }

        @Override
        public int getRowWidth() {
            return this.rowWidth - 16;
        }

        @Override
        protected int scrollBarX() {
            return this.getRowRight() + 6;
        }

        private static final class Entry extends ObjectSelectionList.Entry<Entry> {
            private final MessageLineList list;
            private final FormattedCharSequence message;

            private Entry(MessageLineList list, FormattedCharSequence message) {
                this.list = list;
                this.message = message;
            }

            @Override
            public void extractContent(
                GuiGraphicsExtractor guiGraphics,
                int mouseX,
                int mouseY,
                boolean hovered,
                float delta
            ) {
                guiGraphics.text(
                    this.list.font,
                    this.message,
                    this.getContentX(),
                    this.getContentY() + 1,
                    0xFFFFFFFF,
                    false
                );
            }

            @Override
            public Component getNarration() {
                return Component.empty();
            }
        }
    }
}
