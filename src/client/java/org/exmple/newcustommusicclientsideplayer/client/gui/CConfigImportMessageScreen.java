package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

public final class CConfigImportMessageScreen extends Screen {
    private static final int MESSAGE_MARGIN = 100;
    private static final int MESSAGE_PADDING = 12;
    private static final int LAYOUT_SPACING = 8;
    private static final int DOUBLE_BUTTON_WIDTH = 150;
    private static final int BUTTON_SPACING = 8;

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

    public CConfigImportMessageScreen(
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
        this.layout.arrangeElements();
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
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
        return Math.max(this.font.lineHeight + 8, this.height - MESSAGE_MARGIN);
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
}
