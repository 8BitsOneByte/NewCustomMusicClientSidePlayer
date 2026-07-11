package org.exmple.newcustommusicclientsideplayer.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CTransferToast implements Toast {
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/advancement");
    private static final int WIDTH = 160;
    private static final int HEIGHT = 32;
    private static final int ICON_X = 8;
    private static final int ICON_Y = 8;
    private static final int TEXT_X = 30;
    private static final int TITLE_Y = 7;
    private static final int DESCRIPTION_Y = 18;
    private static final int TEXT_WIDTH = 125;
    private static final int SUCCESS_TITLE_COLOR = -256;
    private static final int FAILURE_TITLE_COLOR = 0xFFFF5555;
    private static final int DESCRIPTION_COLOR = -1;
    private static final int DISPLAY_TIME_MS = 5000;
    private static final String ELLIPSIS = "...";

    private final Kind kind;
    private Component title;
    private Component description;
    private long lastChanged;
    private boolean changed = true;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

    private CTransferToast(Kind kind, Component title, Component description) {
        this.kind = kind;
        this.title = title;
        this.description = description;
    }

    public static void show(Minecraft client, Kind kind, Component title, Component description) {
        ToastManager toastManager = client.gui.toastManager();
        CTransferToast toast = toastManager.getToast(CTransferToast.class, kind);
        if (toast == null) {
            toastManager.addToast(new CTransferToast(kind, title, description));
            return;
        }

        toast.reset(title, description);
    }

    private void reset(Component title, Component description) {
        this.title = title;
        this.description = description;
        this.changed = true;
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return this.wantedVisibility;
    }

    @Override
    public void update(ToastManager toastManager, long time) {
        if (this.changed) {
            this.lastChanged = time;
            this.changed = false;
        }

        double visibleDuration = DISPLAY_TIME_MS * toastManager.getNotificationDisplayTimeMultiplier();
        this.wantedVisibility = time - this.lastChanged < visibleDuration
            ? Toast.Visibility.SHOW
            : Toast.Visibility.HIDE;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long time) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
        graphics.text(font, fitToWidth(font, this.title.getString(), TEXT_WIDTH), TEXT_X, TITLE_Y, this.titleColor(), false);
        graphics.text(font, fitToWidth(font, this.description.getString(), TEXT_WIDTH), TEXT_X, DESCRIPTION_Y, DESCRIPTION_COLOR, false);
        graphics.fakeItem(this.icon(), ICON_X, ICON_Y);
    }

    @Override
    public Object getToken() {
        return this.kind;
    }

    @Override
    public int width() {
        return WIDTH;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    private ItemStack icon() {
        return new ItemStack(this.kind == Kind.FAILURE ? Items.BARRIER : Items.WRITTEN_BOOK);
    }

    private int titleColor() {
        return this.kind == Kind.FAILURE ? FAILURE_TITLE_COLOR : SUCCESS_TITLE_COLOR;
    }

    private static String fitToWidth(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        int ellipsisWidth = font.width(ELLIPSIS);
        if (ellipsisWidth >= maxWidth) {
            return font.plainSubstrByWidth(ELLIPSIS, maxWidth);
        }

        return font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + ELLIPSIS;
    }

    public enum Kind {
        SUCCESS,
        FAILURE
    }
}
