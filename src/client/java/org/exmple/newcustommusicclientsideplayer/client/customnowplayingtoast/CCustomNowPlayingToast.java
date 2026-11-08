package org.exmple.newcustommusicclientsideplayer.client.customnowplayingtoast;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
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
import net.minecraft.client.gui.screens.PauseScreen;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;
import org.exmple.newcustommusicclientsideplayer.client.config.CNowPlayingToastMode;
import org.exmple.newcustommusicclientsideplayer.client.playback.CManagedSoundInstance;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;

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
    private CManagedSoundInstance sound;
    private long lastChanged;
    private boolean changed = true;
    private boolean forceHide;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

    private CCustomNowPlayingToast(Component title, CManagedSoundInstance sound) {
        this.title = title;
        this.sound = sound;
    }

    public static void show(Minecraft client, Component title, CManagedSoundInstance sound) {
        ToastManager toastManager = client.gui.toastManager();
        CCustomNowPlayingToast toast = toastManager.getToast(CCustomNowPlayingToast.class, TOKEN);
        if (toast == null) {
            toastManager.addToast(new CCustomNowPlayingToast(title, sound));
            return;
        }

        toast.updateTitle(title, sound);
    }

    public static void dismiss(Minecraft client) {
        CCustomNowPlayingToast toast = client.gui.toastManager().getToast(CCustomNowPlayingToast.class, TOKEN);
        if (toast != null) {
            toast.forceHide = true;
            toast.wantedVisibility = Toast.Visibility.HIDE;
        }
    }

    private void updateTitle(Component title, CManagedSoundInstance sound) {
        this.title = title;
        this.sound = sound;
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

        if (!canShow()) {
            forceHide = true;
        }
        double visibleDuration = VISIBILITY_DURATION_MS * toastManager.getNotificationDisplayTimeMultiplier();
        wantedVisibility = !forceHide && time - lastChanged < visibleDuration
            ? Toast.Visibility.SHOW
            : Toast.Visibility.HIDE;
        tickMusicNotes();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long time) {
        if (!canShow() || forceHide) {
            return;
        }
        if (NewcustommusicclientsideplayerClient.getModConfig().nowPlayingToastMode() == CNowPlayingToastMode.ON_PLUS
            && Minecraft.getInstance().gui.screen() instanceof PauseScreen pause && pause.showsPauseMenu()) {
            return;
        }
        extractCard(graphics, font, title, 0, 0);
    }

    private boolean canShow() {
        return NewcustommusicclientsideplayerClient.getModConfig().nowPlayingToastMode() != CNowPlayingToastMode.OFF
            && sound != null && sound == CPlaySoundController.currentSoundForMusicHints();
    }

    private static void extractCard(GuiGraphicsExtractor graphics, Font font, Component title, int x, int y) {
        tickMusicNotes();
        int width = getWidth(font, title);
        graphics.blitSprite(GUI_TEXTURED, NOW_PLAYING_BACKGROUND_SPRITE, x, y, width, HEIGHT);
        graphics.blitSprite(GUI_TEXTURED, MUSIC_NOTES_SPRITE, x + 7, y + 7,
            MUSIC_NOTES_SIZE, MUSIC_NOTES_SIZE, musicNoteColor);
        graphics.text(font, fitToWidth(font, title.getString(), width - TEXT_X - RIGHT_PADDING),
            x + TEXT_X, y + 11, TEXT_COLOR);
    }

    public static final class PauseDisplay {
        public void extract(GuiGraphicsExtractor graphics, Font font, int screenWidth) {
            CManagedSoundInstance current = CPlaySoundController.currentSoundForMusicHints();
            if (NewcustommusicclientsideplayerClient.getModConfig().nowPlayingToastMode() != CNowPlayingToastMode.ON_PLUS
                || current == null) {
                return;
            }
            Component title = CPlaySoundController.currentMusicHintTitle(current);
            extractCard(graphics, font, title, screenWidth - getWidth(font, title), 0);
        }
    }

    @Override
    public Object getToken() {
        return TOKEN;
    }

    @Override
    public int width() {
        return getWidth(Minecraft.getInstance().font, title);
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

    private static int getWidth(Font font, Component title) {
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
