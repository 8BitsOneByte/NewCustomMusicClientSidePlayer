package org.exmple.newcustommusicclientsideplayer.client.playback;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.exmple.newcustommusicclientsideplayer.client.customnowplayingtoast.CCustomNowPlayingToast;
import org.exmple.newcustommusicclientsideplayer.client.storage.CPlaybackVolumeSettings;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackNameRepository;
import org.exmple.newcustommusicclientsideplayer.mixin.client.SoundEngineAccessor;
import org.exmple.newcustommusicclientsideplayer.mixin.client.SoundManagerAccessor;

public final class CPlaySoundController {
    public static final float DEFAULT_PITCH = 1.0F;
    public static final float DISPLAY_PITCH_MIN = 0.5F;
    public static final float DISPLAY_PITCH_MAX = 2.0F;

    public enum SessionMode {
        NONE,
        SINGLE_TRACK,
        PLAYLIST
    }

    public enum SkipResult {
        SUCCESS,
        NOT_IN_PLAYLIST_MODE,
        OUT_OF_BOUNDARY,
        TARGET_NOT_PLAYABLE
    }

    public enum PauseResult {
        PAUSED,
        RESUMED,
        NO_PLAYBACK,
        NOT_PAUSED
    }

    public enum VolumeAdjustResult {
        UPDATED,
        ALREADY_MUTED,
        ALREADY_MAX
    }

    private static final COwnedSoundRegistry OWNED_SOUNDS = new COwnedSoundRegistry();
    private static List<Identifier> playlistQueue = List.of();
    private static List<Integer> playlistDisplayIndices = List.of();
    private static int playlistNextIndex;
    private static int playlistCurrentIndex = -1;
    private static int playlistTotalTracks;
    private static boolean playlistLoop;
    private static boolean playlistShuffle;
    private static boolean playlistActive;
    private static String playlistName;
    private static boolean skipNextNowPlayingHeader;
    private static boolean musicVolumeLocked;
    private static double cachedMusicVolume;
    private static SessionMode lastSessionMode = SessionMode.NONE;
    private static int playbackVolumePercent = 100;
    private static boolean customPaused;

    private CPlaySoundController() {
    }

    public static boolean shouldKeepMasterDuringPause() {
        return OWNED_SOUNDS.hasOwnedSounds() || playlistActive;
    }

    public static boolean shouldKeepPlayingDuringLevelChange() {
        return OWNED_SOUNDS.hasOwnedSounds() || playlistActive;
    }

    public static boolean hasActivePlayback() {
        return OWNED_SOUNDS.hasOwnedSounds() || playlistActive;
    }

    public static boolean isCustomPaused() {
        return customPaused;
    }

    public static boolean isPlaylistModeActive() {
        return playlistActive && !playlistQueue.isEmpty();
    }

    public static boolean isPlaylistLoopEnabled() {
        return playlistActive && playlistLoop;
    }

    public static VolumeAdjustResult adjustPlaybackVolume(int stepPercent) {
        int current = playbackVolumePercent;
        int target = Math.max(0, Math.min(100, current + stepPercent));

        if (target == current) {
            return stepPercent < 0 ? VolumeAdjustResult.ALREADY_MUTED : VolumeAdjustResult.ALREADY_MAX;
        }

        playbackVolumePercent = target;
        try {
            CPlaybackVolumeSettings.savePlaybackVolumePercent(playbackVolumePercent);
        } catch (Exception ignored) {
        }

        CManagedSoundInstance managedSoundInstance = OWNED_SOUNDS.current();
        if (managedSoundInstance != null) {
            managedSoundInstance.setBaseVolume(playbackVolumePercent / 100.0F);
            Minecraft.getInstance().getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
        }

        return VolumeAdjustResult.UPDATED;
    }

    public static int getPlaybackVolumePercent() {
        return playbackVolumePercent;
    }

    public static float clampPitchForDisplay(float pitch) {
        return Math.max(DISPLAY_PITCH_MIN, Math.min(DISPLAY_PITCH_MAX, pitch));
    }

    public static String formatPitch(float pitch) {
        float clampedPitch = clampPitchForDisplay(pitch);
        if (clampedPitch == (long) clampedPitch) {
            return Long.toString((long) clampedPitch);
        }

        return Float.toString(clampedPitch);
    }

    public static void loadPlaybackVolumePercent(int playbackVolumePercent) {
        CPlaySoundController.playbackVolumePercent = Math.max(0, Math.min(100, playbackVolumePercent));
    }

    public static SkipResult skipPlaylistBy(int offset) {
        if (!isPlaylistModeActive()) {
            return SkipResult.NOT_IN_PLAYLIST_MODE;
        }

        Minecraft client = Minecraft.getInstance();
        int total = playlistQueue.size();
        int currentIndex = Math.floorMod(playlistNextIndex - 1, total);
        int rawTargetIndex = currentIndex + offset;
        if (!playlistLoop && (rawTargetIndex < 0 || rawTargetIndex >= total)) {
            return SkipResult.OUT_OF_BOUNDARY;
        }

        int targetIndex = Math.floorMod(rawTargetIndex, total);
        Identifier soundId = playlistQueue.get(targetIndex);
        if (!startSound(client, soundId, false, DEFAULT_PITCH)) {
            return SkipResult.TARGET_NOT_PLAYABLE;
        }

        markPlaylistTrackStarted(targetIndex);
        int displayIndex = playlistDisplayIndices.size() > targetIndex ? playlistDisplayIndices.get(targetIndex) : (targetIndex + 1);
        announceNowPlaying(client, soundId, displayIndex, playlistTotalTracks > 0 ? playlistTotalTracks : total);
        return SkipResult.SUCCESS;
    }

    public static SkipResult skipPlaylistRandom() {
        if (!isPlaylistModeActive() || playlistQueue.isEmpty()) {
            return SkipResult.NOT_IN_PLAYLIST_MODE;
        }

        Minecraft client = Minecraft.getInstance();
        int total = playlistQueue.size();
        if (total == 1) {
            return playPlaylistIndex(client, 0) ? SkipResult.SUCCESS : SkipResult.TARGET_NOT_PLAYABLE;
        }

        int currentIndex = playlistCurrentIndex;
        if (currentIndex < 0 || currentIndex >= total) {
            currentIndex = Math.floorMod(playlistNextIndex - 1, total);
        }

        int firstCandidate = ThreadLocalRandom.current().nextInt(total);
        for (int attempts = 0; attempts < total; attempts++) {
            int targetIndex = (firstCandidate + attempts) % total;
            if (targetIndex == currentIndex) {
                continue;
            }

            if (playPlaylistIndex(client, targetIndex)) {
                return SkipResult.SUCCESS;
            }
        }

        return SkipResult.TARGET_NOT_PLAYABLE;
    }

    public static PauseResult togglePause() {
        Minecraft client = Minecraft.getInstance();
        if (!hasActivePlayback()) {
            customPaused = false;
            return PauseResult.NO_PLAYBACK;
        }

        if (customPaused) {
            if (!resumeCurrentSound(client)) {
                customPaused = false;
                return PauseResult.NO_PLAYBACK;
            }

            customPaused = false;
            return PauseResult.RESUMED;
        }

        if (!pauseCurrentSound(client)) {
            customPaused = false;
            return PauseResult.NO_PLAYBACK;
        }

        customPaused = true;
        return PauseResult.PAUSED;
    }

    public static PauseResult resumePaused() {
        Minecraft client = Minecraft.getInstance();
        if (!customPaused || !hasActivePlayback()) {
            customPaused = false;
            return PauseResult.NOT_PAUSED;
        }

        if (!resumeCurrentSound(client)) {
            customPaused = false;
            return PauseResult.NO_PLAYBACK;
        }

        customPaused = false;
        return PauseResult.RESUMED;
    }

    public static int play(FabricClientCommandSource source, Identifier soundId) {
        return play(source, soundId, false);
    }

    public static int play(FabricClientCommandSource source, Identifier soundId, boolean loop) {
        return play(source, soundId, loop, DEFAULT_PITCH);
    }

    public static int play(FabricClientCommandSource source, Identifier soundId, boolean loop, float pitch) {
        Minecraft client = source.getClient();
        if (client.level == null) {
            source.sendError(Component.translatable("message.custommusicclientsideplayer.no_client_world"));
            return 0;
        }

        clearPlaylistSession();
        forceStopTrackedSound(client);
        if (!startSound(client, soundId, loop, pitch)) {
            source.sendError(Component.translatable("message.custommusicclientsideplayer.unknown_sound_id", soundId.toString()));
            return 0;
        }

        lastSessionMode = SessionMode.SINGLE_TRACK;

        return 1;
    }

    public static int playFromUi(Minecraft client, Identifier soundId, boolean loop) {
        return playFromUi(client, soundId, loop, DEFAULT_PITCH);
    }

    public static int playFromUi(Minecraft client, Identifier soundId, boolean loop, float pitch) {
        if (client.level == null) {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatable("message.custommusicclientsideplayer.no_client_world"));
            }

            return 0;
        }

        clearPlaylistSession();
        forceStopTrackedSound(client);
        if (!startSound(client, soundId, loop, pitch)) {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatable("message.custommusicclientsideplayer.unknown_sound_id", soundId.toString()));
            }

            return 0;
        }

        lastSessionMode = SessionMode.SINGLE_TRACK;
        if (client.player != null) {
            client.player.sendSystemMessage(buildSingleTrackMessage(soundId, loop, pitch));
        }

        return 1;
    }
//Legacy method
    public static int playPlaylist(FabricClientCommandSource source, String name, List<Identifier> playlist, boolean loop) {
        return playPlaylist(source, name, playlist, loop, 0);
    }

    public static int playPlaylist(FabricClientCommandSource source, String name, List<Identifier> playlist, boolean loop, int startTrackIndex) {
        return playPlaylist(source, name, playlist, loop, false, startTrackIndex);
    }

    public static int playPlaylist(FabricClientCommandSource source, String name, List<Identifier> playlist, boolean loop, boolean shuffle, int startTrackIndex) {
        Minecraft client = source.getClient();
        if (client.level == null) {
            source.sendError(Component.translatable("message.custommusicclientsideplayer.no_client_world"));
            return 0;
        }

        if (playlist.isEmpty()) {
            source.sendError(Component.translatable("message.custommusicclientsideplayer.playlist_empty"));
            return 0;
        }

        List<Identifier> playablePlaylist = filterPlayableSounds(client, playlist);
        if (playablePlaylist.isEmpty()) {
            source.sendError(Component.translatable("message.custommusicclientsideplayer.no_playable_sound"));
            return 0;
        }

        List<Integer> displayIndices = buildPlayableDisplayIndices(client, playlist);
        int startQueueIndex = findStartQueueIndex(displayIndices, startTrackIndex, loop);
        if (startQueueIndex < 0) {
            source.sendError(Component.translatable("message.custommusicclientsideplayer.no_playable_sound"));
            return 0;
        }

        forceStopTrackedSound(client);

        playlistQueue = playablePlaylist;
        playlistDisplayIndices = displayIndices;
        playlistNextIndex = startQueueIndex;
        playlistCurrentIndex = -1;
        playlistTotalTracks = playlist.size();
        playlistLoop = loop;
        playlistShuffle = loop && shuffle;
        playlistActive = true;
        playlistName = name;
        skipNextNowPlayingHeader = true;

        announcePlaylistStart(client, loop);

        if (!playPlaylistIndex(client, startQueueIndex)) {
            clearPlaylistSession();
            source.sendError(Component.translatable("message.custommusicclientsideplayer.no_playable_sound"));
            unlockMusicVolume(client);
            return 0;
        }

        lastSessionMode = SessionMode.PLAYLIST;

        return 1;
    }

    public static int playPlaylistFromUi(Minecraft client, String name, List<Identifier> playlist, boolean loop) {
        return playPlaylistFromUi(client, name, playlist, loop, 0);
    }

    public static int playPlaylistFromUi(Minecraft client, String name, List<Identifier> playlist, boolean loop, int startTrackIndex) {
        return playPlaylistFromUi(client, name, playlist, loop, false, startTrackIndex);
    }

    public static int playPlaylistFromUi(Minecraft client, String name, List<Identifier> playlist, boolean loop, boolean shuffle, int startTrackIndex) {
        if (client.level == null) {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatable("message.custommusicclientsideplayer.no_client_world"));
            }

            return 0;
        }

        if (playlist.isEmpty()) {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatable("message.custommusicclientsideplayer.playlist_empty"));
            }

            return 0;
        }

        List<Identifier> playablePlaylist = filterPlayableSounds(client, playlist);
        if (playablePlaylist.isEmpty()) {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatable("message.custommusicclientsideplayer.no_playable_sound"));
            }

            return 0;
        }

        List<Integer> displayIndices = buildPlayableDisplayIndices(client, playlist);
        int startQueueIndex = findStartQueueIndex(displayIndices, startTrackIndex, loop);
        if (startQueueIndex < 0) {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatable("message.custommusicclientsideplayer.no_playable_sound"));
            }

            return 0;
        }

        forceStopTrackedSound(client);

        playlistQueue = playablePlaylist;
        playlistDisplayIndices = displayIndices;
        playlistNextIndex = startQueueIndex;
        playlistCurrentIndex = -1;
        playlistTotalTracks = playlist.size();
        playlistLoop = loop;
        playlistShuffle = loop && shuffle;
        playlistActive = true;
        playlistName = name;
        skipNextNowPlayingHeader = true;

        announcePlaylistStart(client, loop);

        if (!playPlaylistIndex(client, startQueueIndex)) {
            clearPlaylistSession();
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatable("message.custommusicclientsideplayer.no_playable_sound"));
            }

            unlockMusicVolume(client);
            return 0;
        }

        lastSessionMode = SessionMode.PLAYLIST;

        return 1;
    }

    public static SessionMode getLastSessionMode() {
        return lastSessionMode;
    }

    public static void tick(Minecraft client) {
        if (client.level == null) {
            return;
        }

        OWNED_SOUNDS.recoverRetiredSounds(client);

        if (customPaused) {
            if (!hasActivePlayback()) {
                customPaused = false;
                return;
            }

            pauseCurrentSound(client);
            return;
        }

        OWNED_SOUNDS.releaseFinishedCurrent(client);

        if (!OWNED_SOUNDS.hasCurrent() && playlistActive && playNextInPlaylist(client)) {
            return;
        }

        if (musicVolumeLocked && !OWNED_SOUNDS.hasCurrent() && !playlistActive) {
            unlockMusicVolume(client);
        }
    }

    public static void stop() {
        Minecraft client = Minecraft.getInstance();

        if (!client.isSameThread()) {
            client.execute(CPlaySoundController::stop);
            return;
        }

        stopInternal(client, true);
    }

    public static void onClientStopping(Minecraft client) {
        stopInternal(client, true);
    }

    private static void stopInternal(Minecraft client, boolean stopSound) {
        clearPlaylistSession();

        if (stopSound) {
            forceStopTrackedSound(client, "SESSION_STOP");
        }

        unlockMusicVolume(client);
    }

    private static void lockMusicVolume(Minecraft client) {
        if (musicVolumeLocked) {
            return;
        }

        cachedMusicVolume = client.options.getSoundSourceVolume(SoundSource.MUSIC);
        client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0);
        musicVolumeLocked = true;
    }

    private static void unlockMusicVolume(Minecraft client) {
        if (!musicVolumeLocked) {
            return;
        }

        client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(cachedMusicVolume);
        client.options.save();
        musicVolumeLocked = false;
        cachedMusicVolume = 1.0;
    }

    private static void clearPlaylistSession() {
        playlistQueue = List.of();
        playlistDisplayIndices = List.of();
        playlistNextIndex = 0;
        playlistCurrentIndex = -1;
        playlistTotalTracks = 0;
        playlistLoop = false;
        playlistShuffle = false;
        playlistActive = false;
        playlistName = null;
        skipNextNowPlayingHeader = false;
    }

    private static boolean playNextInPlaylist(Minecraft client) {
        if (!playlistActive || playlistQueue.isEmpty()) {
            return false;
        }

        if (playlistShuffle) {
            return playRandomNextInPlaylist(client);
        }

        return playSequentialNextInPlaylist(client);
    }

    private static boolean playSequentialNextInPlaylist(Minecraft client) {
        int total = playlistQueue.size();
        int remainingAttempts = total;
        while (remainingAttempts-- > 0) {
            if (playlistNextIndex >= total) {
                if (!playlistLoop) {
                    clearPlaylistSession();
                    return false;
                }

                playlistNextIndex = 0;
            }

            int currentIndex = playlistNextIndex;
            playlistNextIndex++;
            if (playPlaylistIndex(client, currentIndex)) {
                return true;
            }
        }

        clearPlaylistSession();
        return false;
    }

    private static boolean playRandomNextInPlaylist(Minecraft client) {
        int total = playlistQueue.size();
        if (total == 1) {
            if (playPlaylistIndex(client, 0)) {
                return true;
            }

            clearPlaylistSession();
            return false;
        }

        int firstCandidate = ThreadLocalRandom.current().nextInt(total);
        for (int attempts = 0; attempts < total; attempts++) {
            int candidateIndex = (firstCandidate + attempts) % total;
            if (candidateIndex == playlistCurrentIndex) {
                continue;
            }

            if (playPlaylistIndex(client, candidateIndex)) {
                return true;
            }
        }

        clearPlaylistSession();
        return false;
    }

    private static boolean playPlaylistIndex(Minecraft client, int queueIndex) {
        if (queueIndex < 0 || queueIndex >= playlistQueue.size()) {
            return false;
        }

        Identifier soundId = playlistQueue.get(queueIndex);
        if (!startSound(client, soundId, false, DEFAULT_PITCH)) {
            return false;
        }

        markPlaylistTrackStarted(queueIndex);
        int total = playlistQueue.size();
        int displayIndex = playlistDisplayIndices.size() > queueIndex ? playlistDisplayIndices.get(queueIndex) : (queueIndex + 1);
        announceNowPlaying(client, soundId, displayIndex, playlistTotalTracks > 0 ? playlistTotalTracks : total);
        return true;
    }

    private static void markPlaylistTrackStarted(int queueIndex) {
        playlistCurrentIndex = queueIndex;
        playlistNextIndex = queueIndex + 1;
    }

    private static void announceNowPlaying(Minecraft client, Identifier soundId, int index, int total) {
        if (client.player == null) {
            return;
        }

        String songName = getDisplayName(soundId);
        MutableComponent message = Component.empty();
        if (skipNextNowPlayingHeader) {
            skipNextNowPlayingHeader = false;
        } else {
            message.append(Component.translatable("mode.custommusicclientsideplayer.playlist").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)).append(Component.literal("\n"));
        }

        message
            .append(Component.translatable("message.custommusicclientsideplayer.now_playing_header").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
            .append(Component.literal("\n"))
            .append(Component.literal(playlistName == null ? "(unknown)" : playlistName).withStyle(ChatFormatting.AQUA))
            .append(Component.literal("("))
            .append(Component.literal(String.valueOf(index)).withStyle(ChatFormatting.GOLD))
            .append(Component.literal("/"))
            .append(Component.literal(String.valueOf(total)).withStyle(ChatFormatting.GOLD))
            .append(Component.literal("):"))
            .append(Component.literal(songName).withStyle(ChatFormatting.YELLOW));
        client.player.sendSystemMessage(message);
    }

    private static void announcePlaylistStart(Minecraft client, boolean loop) {
        if (client.player == null) {
            return;
        }

        MutableComponent message = Component.empty()
            .append(Component.translatable("mode.custommusicclientsideplayer.playlist").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
            .append(Component.translatable("message.custommusicclientsideplayer.start_playing_playlist"))
            .append(Component.literal(playlistName == null ? "(unknown)" : playlistName).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(", loop="))
            .append(Component.literal(loop ? "true" : "false").withStyle(loop ? ChatFormatting.GREEN : ChatFormatting.RED))
            .append(Component.literal(", shuffle=").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(playlistShuffle ? "true" : "false").withStyle(playlistShuffle ? ChatFormatting.GREEN : ChatFormatting.RED));
        client.player.sendSystemMessage(message);
    }

    public static MutableComponent buildSingleTrackMessage(Identifier soundId, boolean loop) {
        return buildSingleTrackMessage(soundId, loop, DEFAULT_PITCH);
    }

    public static MutableComponent buildSingleTrackMessage(Identifier soundId, boolean loop, float pitch) {
        String namespace = soundId.getNamespace();
        String songName = getDisplayName(soundId);

        return Component.empty()
            .append(Component.translatable("mode.custommusicclientsideplayer.singletrack").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
            .append(Component.literal("\n"))
            .append(Component.translatable("message.custommusicclientsideplayer.start_playing_single"))
            .append(Component.literal(namespace).withStyle(ChatFormatting.GRAY))
            .append(Component.literal(":"))
            .append(Component.literal(songName).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(", loop="))
            .append(Component.literal(loop ? "true" : "false").withStyle(loop ? ChatFormatting.GREEN : ChatFormatting.RED))
            .append(Component.literal(", pitch=").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(formatPitch(pitch)).withStyle(ChatFormatting.GREEN));
    }

    private static String getDisplayName(Identifier id) {
        return CTrackNameRepository.getDisplayName(id);
    }

    private static boolean startSound(Minecraft client, Identifier soundId, boolean loop, float pitch) {
        SoundManager soundManager = client.getSoundManager();
        if (!isTrackPlayable(client, soundId)) {
            return false;
        }

        forceStopTrackedSound(client, "TRACK_REPLACED");

        lockMusicVolume(client);
        CManagedSoundInstance managedSound = new CManagedSoundInstance(
            soundId,
            SoundSource.MASTER,
            playbackVolumePercent / 100.0F,
            pitch,
            SoundInstance.createUnseededRandom(),
            loop,
            0,
            SoundInstance.Attenuation.NONE,
            0.0,
            0.0,
            0.0,
            true
        );
        OWNED_SOUNDS.activate(managedSound);
        soundManager.play(managedSound);
        showCustomNowPlayingToast(client, soundId);
        return true;
    }

    private static void showCustomNowPlayingToast(Minecraft client, Identifier soundId) {
        CCustomNowPlayingToast.show(
            client,
            Component.literal(soundId.getNamespace() + "-" + getDisplayName(soundId))
        );
    }

    private static boolean pauseCurrentSound(Minecraft client) {
        return executeOnCurrentSoundChannel(client, channel -> channel.pause());
    }

    private static boolean resumeCurrentSound(Minecraft client) {
        return executeOnCurrentSoundChannel(client, channel -> channel.unpause());
    }

    private static boolean executeOnCurrentSoundChannel(Minecraft client, java.util.function.Consumer<com.mojang.blaze3d.audio.Channel> action) {
        CManagedSoundInstance currentSound = OWNED_SOUNDS.current();
        if (client == null || currentSound == null) {
            return false;
        }

        SoundEngine soundEngine = ((SoundManagerAccessor) client.getSoundManager()).newcustommusicclientsideplayer$getSoundEngine();
        Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel =
            ((SoundEngineAccessor) soundEngine).newcustommusicclientsideplayer$getInstanceToChannel();
        ChannelAccess.ChannelHandle channelHandle = instanceToChannel.get(currentSound);
        if (channelHandle == null || channelHandle.isStopped()) {
            return false;
        }

        channelHandle.execute(action);
        return true;
    }

    private static List<Identifier> filterPlayableSounds(Minecraft client, List<Identifier> sounds) {
        return sounds.stream().filter(id -> isTrackPlayable(client, id)).toList();
    }

    private static List<Integer> buildPlayableDisplayIndices(Minecraft client, List<Identifier> sounds) {
        java.util.ArrayList<Integer> indices = new java.util.ArrayList<>();
        for (int i = 0; i < sounds.size(); i++) {
            if (isTrackPlayable(client, sounds.get(i))) {
                indices.add(i + 1);
            }
        }

        return indices;
    }

    private static int findStartQueueIndex(List<Integer> displayIndices, int startTrackIndex, boolean loop) {
        int requestedDisplayIndex = startTrackIndex + 1;
        for (int i = 0; i < displayIndices.size(); i++) {
            if (displayIndices.get(i) >= requestedDisplayIndex) {
                return i;
            }
        }

        return loop ? 0 : -1;
    }

    public static boolean isTrackPlayable(Minecraft client, Identifier soundId) {
        if (client == null || soundId == null) {
            return false;
        }

        return client.getSoundManager().getSoundEvent(soundId) != null;
    }

    private static void forceStopTrackedSound(Minecraft client) {
        forceStopTrackedSound(client, "TRACK_REPLACED");
    }

    private static void forceStopTrackedSound(Minecraft client, String reason) {
        if ("SESSION_STOP".equals(reason)) {
            OWNED_SOUNDS.stopAll(client);
        } else {
            OWNED_SOUNDS.retireCurrent(client);
        }
    }

}
