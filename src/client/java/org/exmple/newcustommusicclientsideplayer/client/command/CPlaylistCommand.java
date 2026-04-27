package org.exmple.newcustommusicclientsideplayer.client.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.exmple.newcustommusicclientsideplayer.client.storage.CPlaylistRepository;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.exmple.newcustommusicclientsideplayer.client.gui.CPlaylistTestScreen;


public final class CPlaylistCommand {
    private static final int MAX_PLAYLIST_NAME_LENGTH = 50;
    private static final Pattern UNQUOTED_PLAYLIST_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");

    private CPlaylistCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            var playlistCommand = dispatcher.register(
                    ClientCommands.literal("cplaylist")
                            .then(
                                    ClientCommands.literal("create")
                                            .then(
                                                    ClientCommands.argument("playlistName", StringArgumentType.string())
                                                            .executes(
                                                                    commandContext -> executeCreate(
                                                                            commandContext.getSource(),
                                                                            StringArgumentType.getString(commandContext, "playlistName")
                                                                    )
                                                            )
                                            )
                            )
                            .then(
                                    ClientCommands.literal("delete")
                                            .then(
                                                    ClientCommands.argument("playlistName", StringArgumentType.string())
                                                            .suggests(CPlaylistCommand::suggestPlaylistNames)
                                                            .executes(
                                                                    commandContext -> executeDelete(
                                                                            commandContext.getSource(),
                                                                            StringArgumentType.getString(commandContext, "playlistName")
                                                                    )
                                                            )
                                            )
                            )
                            .then(
                                    ClientCommands.literal("play")
                                            .then(
                                                    ClientCommands.argument("playlistName", StringArgumentType.string())
                                                            .suggests(CPlaylistCommand::suggestPlaylistNames)
                                                            .executes(
                                                                    commandContext -> executePlay(
                                                                            commandContext.getSource(),
                                                                            StringArgumentType.getString(commandContext, "playlistName"),
                                                                            false,
                                                                            1
                                                                    )
                                                            )
                                                            .then(
                                                                    ClientCommands.argument("startIndex", IntegerArgumentType.integer(1))
                                                                            .executes(
                                                                                    commandContext -> executePlay(
                                                                                            commandContext.getSource(),
                                                                                            StringArgumentType.getString(commandContext, "playlistName"),
                                                                                            false,
                                                                                            IntegerArgumentType.getInteger(commandContext, "startIndex")
                                                                                    )
                                                                            )
                                                            )
                                                            .then(
                                                                    ClientCommands.argument("loop", BoolArgumentType.bool())
                                                                            .executes(
                                                                                    commandContext -> executePlay(
                                                                                            commandContext.getSource(),
                                                                                            StringArgumentType.getString(commandContext, "playlistName"),
                                                                                            BoolArgumentType.getBool(commandContext, "loop"),
                                                                                            1
                                                                                    )
                                                                            )
                                                                            .then(
                                                                                    ClientCommands.argument("startIndex", IntegerArgumentType.integer(1))
                                                                                            .executes(
                                                                                                    commandContext -> executePlay(
                                                                                                            commandContext.getSource(),
                                                                                                            StringArgumentType.getString(commandContext, "playlistName"),
                                                                                                            BoolArgumentType.getBool(commandContext, "loop"),
                                                                                                            IntegerArgumentType.getInteger(commandContext, "startIndex")
                                                                                                    )
                                                                                            )
                                                                            )
                                                            )
                                            )
                            )
                            .then(
                                    ClientCommands.literal("modify")
                                            .then(
                                                    ClientCommands.argument("playlistName", StringArgumentType.string())
                                                            .suggests(CPlaylistCommand::suggestPlaylistNames)
                                                            .executes(
                                                                    commandContext -> executeModify(
                                                                            commandContext.getSource(),
                                                                            StringArgumentType.getString(commandContext, "playlistName")
                                                                    )
                                                            )
                                            )
                            )
            );

            dispatcher.register(ClientCommands.literal("playtracks").redirect(playlistCommand));
        });
    }

    private static int executeCreate(FabricClientCommandSource source, String playlistName) {
        String normalizedName = normalizePlaylistName(playlistName);
        if (normalizedName == null) {
            source.sendError(Component.translatable("command.custommusicclientsideplayer.playlist.name_empty"));
            return 0;
        }

        if (normalizedName.length() > MAX_PLAYLIST_NAME_LENGTH) {
            source.sendError(Component.translatable("screen.custommusicclientsideplayer.error.playlist_name_too_long"));
            return 0;
        }

        try {
            boolean created = CPlaylistRepository.createPlaylist(normalizedName);
            if (created) {
                source.sendFeedback(Component.translatable("command.custommusicclientsideplayer.playlist.created", playlistNameComponent(normalizedName)));
                return 1;
            }

            source.sendError(Component.translatable("command.custommusicclientsideplayer.playlist.already_exists", playlistNameComponent(normalizedName)));
            return 0;
        } catch (IOException exception) {
            source.sendError(Component.translatable("screen.custommusicclientsideplayer.error.create_playlist_failed"));
            return 0;
        }
    }

    private static int executeDelete(FabricClientCommandSource source, String playlistName) {
        String normalizedName = normalizePlaylistName(playlistName);
        if (normalizedName == null) {
            source.sendError(Component.translatable("command.custommusicclientsideplayer.playlist.name_empty"));
            return 0;
        }

        try {
            boolean deleted = CPlaylistRepository.deletePlaylist(normalizedName);
            if (deleted) {
                source.sendFeedback(Component.translatable("command.custommusicclientsideplayer.playlist.deleted", playlistNameComponent(normalizedName)));
                return 1;
            }

            source.sendError(Component.translatable("command.custommusicclientsideplayer.playlist.not_found", playlistNameComponent(normalizedName)));
            return 0;
        } catch (IOException exception) {
            source.sendError(Component.translatable("screen.custommusicclientsideplayer.error.delete_playlist_failed"));
            return 0;
        }
    }

    private static int executePlay(FabricClientCommandSource source, String playlistName, boolean loop, int startIndex) {
        String normalizedName = normalizePlaylistName(playlistName);
        if (normalizedName == null) {
            source.sendError(Component.translatable("command.custommusicclientsideplayer.playlist.name_empty"));
            return 0;
        }

        List<Identifier> playlist;
        try {
            if (!CPlaylistRepository.exists(normalizedName)) {
                source.sendError(Component.translatable("command.custommusicclientsideplayer.playlist.not_found", playlistNameComponent(normalizedName)));
                return 0;
            }

            playlist = CPlaylistRepository.getPlaylist(normalizedName);
        } catch (IOException exception) {
            source.sendError(Component.translatable("screen.custommusicclientsideplayer.error.read_playlist_failed"));
            return 0;
        }

        if (startIndex > playlist.size()) {
            source.sendError(Component.translatable("command.custommusicclientsideplayer.playlist.start_index_out_of_range", startIndex, playlist.size()));
            return 0;
        }

        return CPlaySoundController.playPlaylist(source, normalizedName, playlist, loop, startIndex - 1);
    }

    private static int executeModify(FabricClientCommandSource source, String playlistName) {
        String normalizedName = normalizePlaylistName(playlistName);
        if (normalizedName == null) {
            source.sendError(Component.translatable("command.custommusicclientsideplayer.playlist.name_empty"));
            return 0;
        }

        List<Identifier> initialPlaylist;
        try {
            if (!CPlaylistRepository.exists(normalizedName)) {
                source.sendError(Component.translatable("command.custommusicclientsideplayer.playlist.not_found", playlistNameComponent(normalizedName)));
                return 0;
            }

            initialPlaylist = CPlaylistRepository.getPlaylist(normalizedName);
        } catch (IOException exception) {
            source.sendError(Component.translatable("screen.custommusicclientsideplayer.error.open_playlist_failed"));
            return 0;
        }

        Minecraft client = source.getClient();
        client.execute(
                () -> client.setScreen(
                        new CPlaylistTestScreen(
                                client.screen,
                                normalizedName,
                                initialPlaylist,
                                updatedPlaylist -> CPlaylistRepository.savePlaylist(normalizedName, updatedPlaylist)
                        )
                )
        );
        return 1;
    }

    private static String normalizePlaylistName(String playlistName) {
        String normalized = playlistName.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static CompletableFuture<Suggestions> suggestPlaylistNames(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        String prefix = remaining.toLowerCase(Locale.ROOT);
        String normalizedPrefix = normalizeSuggestionPrefix(remaining).toLowerCase(Locale.ROOT);

        try {
            for (String name : CPlaylistRepository.listPlaylistNames()) {
                String suggestion = toSuggestedPlaylistToken(name);
                String suggestionLower = suggestion.toLowerCase(Locale.ROOT);
                String rawLower = name.toLowerCase(Locale.ROOT);

                if (suggestionLower.startsWith(prefix) || rawLower.startsWith(normalizedPrefix)) {
                    builder.suggest(suggestion);
                }
            }
        } catch (IOException ignored) {

        }

        return builder.buildFuture();
    }

    private static String normalizeSuggestionPrefix(String prefix) {
        if (prefix.startsWith("\"") || prefix.startsWith("'")) {
            return prefix.substring(1);
        }

        return prefix;
    }

    private static String toSuggestedPlaylistToken(String playlistName) {
        if (UNQUOTED_PLAYLIST_NAME_PATTERN.matcher(playlistName).matches()) {
            return playlistName;
        }

        return StringArgumentType.escapeIfRequired(playlistName);
    }

    private static Component playlistNameComponent(String playlistName) {
        return Component.literal(playlistName).withStyle(ChatFormatting.AQUA);
    }
}
