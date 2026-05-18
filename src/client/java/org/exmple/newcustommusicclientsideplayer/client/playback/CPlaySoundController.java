package org.exmple.newcustommusicclientsideplayer.client.playback;

import java.util.List;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.exmple.newcustommusicclientsideplayer.client.customnowplayingtoast.CCustomNowPlayingToast;
import org.exmple.newcustommusicclientsideplayer.client.storage.CPlaybackVolumeSettings;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackNameRepository;

public final class CPlaySoundController {
    private static final int STARTUP_GRACE_TICKS = 20;
    public enum SessionMode {
        NONE,
        SINGLE_TRACK,
        PLAYLIST
    }

    public enum SkipResult {
        SUCCESS,
        NOT_IN_PLAYLIST_MODE,
        OUT_OF_BOUNDARY,
        TARGET_NOT_PLAYABLE,
        SWITCH_LOCKED
    }

    public enum VolumeAdjustResult {
        UPDATED,
        ALREADY_MUTED,
        ALREADY_MAX
    }

    private static SoundInstance currentSound;
    private static Identifier currentSoundId;
    private static List<Identifier> playlistQueue = List.of();
    private static List<Integer> playlistDisplayIndices = List.of();
    private static int playlistNextIndex;
    private static int playlistTotalTracks;
    private static boolean playlistLoop;
    private static boolean playlistActive;
    private static String playlistName;
    private static boolean skipNextNowPlayingHeader;
    private static boolean musicVolumeLocked;
    private static double cachedMusicVolume;
    private static SessionMode lastSessionMode = SessionMode.NONE;
    private static int playbackVolumePercent = 100;
    private static int inactiveTickCount;
    private static int startupGraceTicks;
    private static boolean currentSoundObservedActive;
    private static Identifier whiteListedSoundId;

    private CPlaySoundController() {
    }

    public static boolean shouldKeepMasterDuringPause() {
        return currentSound != null || currentSoundId != null || playlistActive;
    }

    public static boolean shouldKeepPlayingDuringLevelChange() {
        return currentSound != null || currentSoundId != null || playlistActive;
    }

    public static boolean hasActivePlayback() {
        return currentSound != null || currentSoundId != null || playlistActive;
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

        if (currentSound instanceof AdjustableSoundInstance adjustableSoundInstance) {
            adjustableSoundInstance.setBaseVolume(playbackVolumePercent / 100.0F);
            Minecraft.getInstance().getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
        }

        return VolumeAdjustResult.UPDATED;
    }

    public static int getPlaybackVolumePercent() {
        return playbackVolumePercent;
    }

    public static void loadPlaybackVolumePercent(int playbackVolumePercent) {
        CPlaySoundController.playbackVolumePercent = Math.max(0, Math.min(100, playbackVolumePercent));
    }

    public static SkipResult skipPlaylistBy(int offset) {
        if (!isPlaylistModeActive()) {
            return SkipResult.NOT_IN_PLAYLIST_MODE;
        }

        if (isSwitchLocked()) {
            return SkipResult.SWITCH_LOCKED;
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
        whiteListedSoundId = soundId;
        if (!startSound(client, soundId, false)) {
            return SkipResult.TARGET_NOT_PLAYABLE;
        }

        playlistNextIndex = targetIndex + 1;
        int displayIndex = playlistDisplayIndices.size() > targetIndex ? playlistDisplayIndices.get(targetIndex) : (targetIndex + 1);
        announceNowPlaying(client, soundId, displayIndex, playlistTotalTracks > 0 ? playlistTotalTracks : total);
        return SkipResult.SUCCESS;
    }

    public static int play(FabricClientCommandSource source, Identifier soundId) {
        return play(source, soundId, false);
    }

    public static int play(FabricClientCommandSource source, Identifier soundId, boolean loop) {
        Minecraft client = source.getClient();
        if (client.level == null) {
            source.sendError(Component.translatable("message.custommusicclientsideplayer.no_client_world"));
            return 0;
        }

        clearPlaylistSession();
        forceStopTrackedSound(client);
        whiteListedSoundId = soundId;
        if (!startSound(client, soundId, loop)) {
            source.sendError(Component.translatable("message.custommusicclientsideplayer.unknown_sound_id", soundId.toString()));
            return 0;
        }

        lastSessionMode = SessionMode.SINGLE_TRACK;

        return 1;
    }

    public static int playFromUi(Minecraft client, Identifier soundId, boolean loop) {
        if (client.level == null) {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatable("message.custommusicclientsideplayer.no_client_world"));
            }

            return 0;
        }

        clearPlaylistSession();
        forceStopTrackedSound(client);
        whiteListedSoundId = soundId;
        if (!startSound(client, soundId, loop)) {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatable("message.custommusicclientsideplayer.unknown_sound_id", soundId.toString()));
            }

            return 0;
        }

        lastSessionMode = SessionMode.SINGLE_TRACK;
        if (client.player != null) {
            client.player.sendSystemMessage(buildSingleTrackMessage(soundId, loop));
        }

        return 1;
    }
//Legacy method
    public static int playPlaylist(FabricClientCommandSource source, String name, List<Identifier> playlist, boolean loop) {
        return playPlaylist(source, name, playlist, loop, 0);
    }

    public static int playPlaylist(FabricClientCommandSource source, String name, List<Identifier> playlist, boolean loop, int startTrackIndex) {
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
        playlistTotalTracks = playlist.size();
        playlistLoop = loop;
        playlistActive = true;
        playlistName = name;
        skipNextNowPlayingHeader = true;

        announcePlaylistStart(client, loop);

        if (!playNextInPlaylist(client)) {
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
        playlistTotalTracks = playlist.size();
        playlistLoop = loop;
        playlistActive = true;
        playlistName = name;
        skipNextNowPlayingHeader = true;

        announcePlaylistStart(client, loop);

        if (!playNextInPlaylist(client)) {
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

        if (currentSound != null) {
            if (client.getSoundManager().isActive(currentSound)) {
                currentSoundObservedActive = true;
                startupGraceTicks = 0;
                inactiveTickCount = 0;
            } else if (!currentSoundObservedActive) {
                if (startupGraceTicks > 0) {
                    startupGraceTicks--;
                } else {
                    currentSound = null;
                    currentSoundId = null;
                    inactiveTickCount = 0;
                }
            } else if (++inactiveTickCount >= 3) {
                currentSound = null;
                currentSoundId = null;
                inactiveTickCount = 0;
            }
        }

        if (currentSound == null && playlistActive && playNextInPlaylist(client)) {
            return;
        }

        if (musicVolumeLocked && currentSound == null && !playlistActive) {
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
            forceStopTrackedSound(client);
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
        playlistTotalTracks = 0;
        playlistLoop = false;
        playlistActive = false;
        playlistName = null;
        skipNextNowPlayingHeader = false;
    }

    private static boolean isSwitchLocked() {
        return currentSound != null && !currentSoundObservedActive && startupGraceTicks > 0;
    }

    private static boolean playNextInPlaylist(Minecraft client) {
        if (!playlistActive || playlistQueue.isEmpty()) {
            return false;
        }

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
            Identifier soundId = playlistQueue.get(playlistNextIndex++);
            whiteListedSoundId = soundId;
            if (startSound(client, soundId, false)) {
                int displayIndex = playlistDisplayIndices.size() > currentIndex ? playlistDisplayIndices.get(currentIndex) : (currentIndex + 1);
                announceNowPlaying(client, soundId, displayIndex, playlistTotalTracks > 0 ? playlistTotalTracks : total);
                return true;
            }
        }

        clearPlaylistSession();
        return false;
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
            .append(Component.literal(loop ? "true" : "false").withStyle(loop ? ChatFormatting.GREEN : ChatFormatting.RED));
        client.player.sendSystemMessage(message);
    }

    public static MutableComponent buildSingleTrackMessage(Identifier soundId, boolean loop) {
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
            .append(Component.literal(loop ? "true" : "false").withStyle(loop ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private static String getDisplayName(Identifier id) {
        return CTrackNameRepository.getDisplayName(id);
    }

    private static boolean startSound(Minecraft client, Identifier soundId, boolean loop) {
        SoundManager soundManager = client.getSoundManager();
        if (whiteListedSoundId != null && !soundId.equals(whiteListedSoundId)) {
            return false;
        }

        if (!isTrackPlayable(client, soundId)) {
            return false;
        }

        forceStopTrackedSound(client);

        lockMusicVolume(client);
        currentSound = new AdjustableSoundInstance(
            soundId,
            SoundSource.MASTER,
            playbackVolumePercent / 100.0F,
            1.0F,
            SoundInstance.createUnseededRandom(),
            loop,
            0,
            SoundInstance.Attenuation.NONE,
            0.0,
            0.0,
            0.0,
            true
        );
        currentSoundId = soundId;
        inactiveTickCount = 0;
        startupGraceTicks = STARTUP_GRACE_TICKS;
        currentSoundObservedActive = false;
        soundManager.play(currentSound);
        showCustomNowPlayingToast(client, soundId);
        return true;
    }

    private static void showCustomNowPlayingToast(Minecraft client, Identifier soundId) {
        CCustomNowPlayingToast.show(
            client,
            Component.literal(soundId.getNamespace() + "-" + getDisplayName(soundId))
        );
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
        SoundManager soundManager = client.getSoundManager();
        if (currentSound != null) {
            soundManager.stop(currentSound);
            currentSound = null;
        }

        if (currentSoundId != null) {
            soundManager.stop(currentSoundId, SoundSource.MASTER);
            currentSoundId = null;
        }

        inactiveTickCount = 0;
        startupGraceTicks = 0;
        currentSoundObservedActive = false;
        whiteListedSoundId = null;
    }

    private static final class AdjustableSoundInstance extends SimpleSoundInstance {
        private AdjustableSoundInstance(
            Identifier identifier,
            SoundSource soundSource,
            float volume,
            float pitch,
            net.minecraft.util.RandomSource randomSource,
            boolean looping,
            int delay,
            SoundInstance.Attenuation attenuation,
            double x,
            double y,
            double z,
            boolean relative
        ) {
            super(identifier, soundSource, volume, pitch, randomSource, looping, delay, attenuation, x, y, z, relative);
        }

        private void setBaseVolume(float volume) {
            this.volume = volume;
        }
    }
}
