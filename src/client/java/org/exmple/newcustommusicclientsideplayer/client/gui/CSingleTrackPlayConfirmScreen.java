package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class CSingleTrackPlayConfirmScreen extends Screen {
    private static final int BUTTON_WIDTH = 98;

    private final Screen lastScreen;
    private final Component message;
    private final Consumer<Boolean> onConfirm;
    private final LinearLayout layout = LinearLayout.vertical().spacing(8);

    public CSingleTrackPlayConfirmScreen(Screen lastScreen, Identifier soundId, Consumer<Boolean> onConfirm) {
        super(Component.empty());
        this.lastScreen = lastScreen;
        this.message = Component.translatable(
            "screen.custommusicclientsideplayer.play_confirm.singletrack.message",
            Component.literal(CSingleTrackSelectScreen.getDisplayName(soundId)).withStyle(ChatFormatting.YELLOW)
        );
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        this.layout.defaultCellSetting().alignHorizontallyCenter();
        this.layout.addChild(new StringWidget(this.title, this.font));
        this.layout.addChild(new MultiLineTextWidget(this.message, this.font).setMaxWidth(this.width - 50).setMaxRows(15).setCentered(true));

        LinearLayout buttonRow = this.layout.addChild(LinearLayout.horizontal().spacing(4));
        buttonRow.defaultCellSetting().paddingTop(16);
        buttonRow.addChild(Button.builder(Component.translatable("screen.custommusicclientsideplayer.play_confirm.loop"), button -> this.onConfirm.accept(true)).width(BUTTON_WIDTH).build());
        buttonRow.addChild(Button.builder(Component.translatable("screen.custommusicclientsideplayer.play_confirm.dont_loop"), button -> this.onConfirm.accept(false)).width(BUTTON_WIDTH).build());
        buttonRow.addChild(Button.builder(Component.translatable("screen.custommusicclientsideplayer.play_confirm.cancel"), button -> this.onClose()).width(BUTTON_WIDTH).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}


