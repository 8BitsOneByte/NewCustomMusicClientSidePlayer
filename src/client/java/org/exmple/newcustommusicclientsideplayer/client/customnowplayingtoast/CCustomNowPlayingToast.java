package org.exmple.newcustommusicclientsideplayer.client.customnowplayingtoast;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.ColorLerper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;

public final class CCustomNowPlayingToast implements Toast {
    private static final Object TOKEN = new Object();
    private static final Identifier NOW_PLAYING_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/now_playing");
    private static final Identifier MUSIC_NOTES_SPRITE = Identifier.parse("icon/music_notes");
    private static final RenderPipeline GUI_TEXTURED = RenderPipelines.GUI_TEXTURED;
    private static final int MAX_WIDTH = 200;
    private static final int HEIGHT = 30;
    private static final int MUSIC_NOTES_SIZE = 16;
    private static final int TEXT_X = 30;
    private static final int RIGHT_PADDING = 7;
    private static final int TEXT_COLOR = DyeColor.LIGHT_GRAY.getTextColor();
    private static final int VISIBILITY_DURATION_MS = 5000;
    private static final long MUSIC_COLOR_CHANGE_FREQUENCY_MS = 25L;
    private static final String ELLIPSIS = "...";

    private static int musicNoteColorTick;
    private static long lastMusicNoteColorChange;
    private static int musicNoteColor = -1;

    private Component title;
    private long lastChanged;
    private boolean changed = true;
    private boolean forceHide;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

    private CCustomNowPlayingToast(Component title) {
        this.title = title;
    }

    public static void show(Minecraft client, Component title) {
        ToastManager toastManager = client.gui.toastManager();
        CCustomNowPlayingToast toast = toastManager.getToast(CCustomNowPlayingToast.class, TOKEN);
        if (toast == null) {
            toastManager.addToast(new CCustomNowPlayingToast(title));
            return;
        }

        toast.updateTitle(title);
    }

    private void updateTitle(Component title) {
        this.title = title;
        this.changed = true;
        this.forceHide = false;
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return wantedVisibility;
    }

    @Override
    public void update(ToastManager toastManager, long time) {
        if (changed) {
            lastChanged = time;
            changed = false;
        }

        double visibleDuration = VISIBILITY_DURATION_MS * toastManager.getNotificationDisplayTimeMultiplier();
        wantedVisibility = !forceHide && time - lastChanged < visibleDuration
            ? Toast.Visibility.SHOW
            : Toast.Visibility.HIDE;
        tickMusicNotes();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long time) {
        int width = getWidth(font);
        graphics.blitSprite(GUI_TEXTURED, NOW_PLAYING_BACKGROUND_SPRITE, 0, 0, width, HEIGHT);
        graphics.blitSprite(GUI_TEXTURED, MUSIC_NOTES_SPRITE, 7, 7, MUSIC_NOTES_SIZE, MUSIC_NOTES_SIZE, musicNoteColor);
        graphics.text(font, fitToWidth(font, title.getString(), width - TEXT_X - RIGHT_PADDING), TEXT_X, 11, TEXT_COLOR);
    }

    @Override
    public Object getToken() {
        return TOKEN;
    }

    @Override
    public int width() {
        return getWidth(Minecraft.getInstance().font);
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    private static void tickMusicNotes() {
        long now = System.currentTimeMillis();
        if (now <= lastMusicNoteColorChange + MUSIC_COLOR_CHANGE_FREQUENCY_MS) {
            return;
        }

        musicNoteColorTick++;
        lastMusicNoteColorChange = now;
        musicNoteColor = ColorLerper.getLerpedColor(ColorLerper.Type.MUSIC_NOTE, musicNoteColorTick);
    }

    private int getWidth(Font font) {
        return Math.min(MAX_WIDTH, TEXT_X + font.width(title) + RIGHT_PADDING);
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
}
