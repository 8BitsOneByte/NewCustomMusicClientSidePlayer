package org.exmple.newcustommusicclientsideplayer.client.bootstrap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.lwjgl.glfw.GLFW;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.exmple.newcustommusicclientsideplayer.client.storage.CPlaybackVolumeSettings;
import org.exmple.newcustommusicclientsideplayer.client.command.CPauseCommand;
import org.exmple.newcustommusicclientsideplayer.client.command.CPlaySoundCommand;
import org.exmple.newcustommusicclientsideplayer.client.command.CPlaylistCommand;
import org.exmple.newcustommusicclientsideplayer.client.command.CResumeCommand;
import org.exmple.newcustommusicclientsideplayer.client.command.CSkipCommand;
import org.exmple.newcustommusicclientsideplayer.client.command.CSkipRandomCommand;
import org.exmple.newcustommusicclientsideplayer.client.command.CStopSoundCommand;
import org.exmple.newcustommusicclientsideplayer.client.gui.CMainConfigScreen;
import org.exmple.newcustommusicclientsideplayer.client.update.CUpdateChecker;

public class NewcustommusicclientsideplayerClient implements ClientModInitializer {
    public static final String MOD_ID = "newcustommusicclientsideplayer";
    private static final int PLAYLIST_ARROW_SKIP_COOLDOWN_TICKS = 5;
    private static long nextAllowedPlaylistArrowSkipTick;
    private static boolean previousTrackKeyHeld;
    private static boolean nextTrackKeyHeld;
    private static final KeyMapping.Category CUSTOM_KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "newcustom_music_client_side_player")
    );
    private static final KeyMapping OPEN_PLAYLIST_CONFIG_KEY = new KeyMapping(
            "key.custommusicclientsideplayer.open_playlist_config",
            GLFW.GLFW_KEY_Z,
            CUSTOM_KEY_CATEGORY
    );
    private static final KeyMapping VOLUME_UP_KEY = new KeyMapping(
            "key.custommusicclientsideplayer.volume_up",
            GLFW.GLFW_KEY_UP,
            CUSTOM_KEY_CATEGORY
    );
    private static final KeyMapping VOLUME_DOWN_KEY = new KeyMapping(
            "key.custommusicclientsideplayer.volume_down",
            GLFW.GLFW_KEY_DOWN,
            CUSTOM_KEY_CATEGORY
    );
    private static final KeyMapping PREVIOUS_TRACK_KEY = new KeyMapping(
            "key.custommusicclientsideplayer.previous_track",
            GLFW.GLFW_KEY_LEFT,
            CUSTOM_KEY_CATEGORY
    );
    private static final KeyMapping NEXT_TRACK_KEY = new KeyMapping(
            "key.custommusicclientsideplayer.next_track",
            GLFW.GLFW_KEY_RIGHT,
            CUSTOM_KEY_CATEGORY
    );
    private static final KeyMapping TOGGLE_PAUSE_KEY = new KeyMapping(
            "key.newcustommusicclientsideplayer.toggle_pause",
            GLFW.GLFW_KEY_Y,
            CUSTOM_KEY_CATEGORY
    );

    @Override
    public void onInitializeClient() {
        CPlaySoundController.loadPlaybackVolumePercent(CPlaybackVolumeSettings.loadPlaybackVolumePercent());

        KeyMappingHelper.registerKeyMapping(OPEN_PLAYLIST_CONFIG_KEY);
        KeyMappingHelper.registerKeyMapping(VOLUME_UP_KEY);
        KeyMappingHelper.registerKeyMapping(VOLUME_DOWN_KEY);
        KeyMappingHelper.registerKeyMapping(PREVIOUS_TRACK_KEY);
        KeyMappingHelper.registerKeyMapping(NEXT_TRACK_KEY);
        KeyMappingHelper.registerKeyMapping(TOGGLE_PAUSE_KEY);

        CPlaySoundCommand.register();
        CPauseCommand.register();
        CResumeCommand.register();
        CStopSoundCommand.register();
        CSkipCommand.register();
        CSkipRandomCommand.register();
        CPlaylistCommand.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CPlaySoundController.tick(client);

            while (OPEN_PLAYLIST_CONFIG_KEY.consumeClick()) {
                client.setScreen(new CMainConfigScreen(client.screen));
            }

            while (VOLUME_UP_KEY.consumeClick()) {
                showVolumeAdjustFeedback(client, CPlaySoundController.adjustPlaybackVolume(5));
            }

            while (VOLUME_DOWN_KEY.consumeClick()) {
                showVolumeAdjustFeedback(client, CPlaySoundController.adjustPlaybackVolume(-5));
            }

            while (TOGGLE_PAUSE_KEY.consumeClick()) {
                handleTogglePauseKey(client);
            }

            boolean previousDown = PREVIOUS_TRACK_KEY.isDown();
            if (previousDown && !previousTrackKeyHeld) {
                handlePlaylistArrowSkip(client, -1);
            }
            previousTrackKeyHeld = previousDown;
            while (PREVIOUS_TRACK_KEY.consumeClick()) {
            }

            boolean nextDown = NEXT_TRACK_KEY.isDown();
            if (nextDown && !nextTrackKeyHeld) {
                handlePlaylistArrowSkip(client, 1);
            }
            nextTrackKeyHeld = nextDown;
            while (NEXT_TRACK_KEY.consumeClick()) {
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CPlaySoundController.stop());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CPlaySoundController.onClientStopping(client);
            try {
                CPlaybackVolumeSettings.savePlaybackVolumePercent(CPlaySoundController.getPlaybackVolumePercent());
            } catch (Exception ignored) {
            }
        });

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath(MOD_ID, "cplaysound_stop_on_reload"),
                new ResourceManagerReloadListener() {
                    @Override
                    public void onResourceManagerReload(ResourceManager resourceManager) {
                        CPlaySoundController.stop();
                    }
                }
        );

        CUpdateChecker.initialize();
    }

    private static void showVolumeAdjustFeedback(Minecraft client, CPlaySoundController.VolumeAdjustResult result) {
        if (client.player == null) {
            return;
        }

        Component message;
        switch (result) {
            case UPDATED -> message = Component.translatable("message.custommusicclientsideplayer.current_volume", CPlaySoundController.getPlaybackVolumePercent())
                    .withStyle(ChatFormatting.GREEN);
            case ALREADY_MUTED -> message = Component.translatable("message.custommusicclientsideplayer.already_muted").withStyle(ChatFormatting.RED);
            case ALREADY_MAX -> message = Component.translatable("message.custommusicclientsideplayer.already_max_volume").withStyle(ChatFormatting.GREEN);
            default -> {
                return;
            }
        }

        client.player.sendOverlayMessage(message);
    }

    private static void handleTogglePauseKey(Minecraft client) {
        CPlaySoundController.PauseResult result = CPlaySoundController.togglePause();
        if (client.player == null) {
            return;
        }

        Component message = switch (result) {
            case PAUSED -> Component.translatable("message.newcustommusicclientsideplayer.pause.paused").withStyle(ChatFormatting.RED);
            case RESUMED -> Component.translatable("message.newcustommusicclientsideplayer.pause.resumed").withStyle(ChatFormatting.GREEN);
            case NO_PLAYBACK, NOT_PAUSED -> Component.translatable("message.newcustommusicclientsideplayer.pause.no_playback").withStyle(ChatFormatting.RED);
        };
        client.player.sendOverlayMessage(message);
        if (result == CPlaySoundController.PauseResult.PAUSED || result == CPlaySoundController.PauseResult.RESUMED) {
            client.player.sendSystemMessage(message);
        }
    }

    private static void handlePlaylistArrowSkip(Minecraft client, int offset) {
        if (!CPlaySoundController.isPlaylistModeActive()) {
            return;
        }

        if (client.level == null) {
            return;
        }

        long gameTick = client.level.getGameTime();
        if (gameTick < nextAllowedPlaylistArrowSkipTick) {
            return;
        }
        nextAllowedPlaylistArrowSkipTick = gameTick + PLAYLIST_ARROW_SKIP_COOLDOWN_TICKS;

        boolean loopEnabled = CPlaySoundController.isPlaylistLoopEnabled();
        CPlaySoundController.SkipResult result = CPlaySoundController.skipPlaylistBy(offset);


        if (loopEnabled || client.player == null) {
            return;
        }

        if (result == CPlaySoundController.SkipResult.OUT_OF_BOUNDARY) {
            Component boundaryMessage = offset < 0
                    ? Component.translatable("message.custommusicclientsideplayer.already_first_track").withStyle(ChatFormatting.RED)
                    : Component.translatable("message.custommusicclientsideplayer.already_last_track").withStyle(ChatFormatting.RED);
            client.player.sendOverlayMessage(boundaryMessage);
        }
    }
}
