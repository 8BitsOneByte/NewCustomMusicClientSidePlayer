package org.exmple.newcustommusicclientsideplayer.client.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackNameRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class CPlaySoundCommand {
    private record SuggestionEntry(String suggestion, String fullMatchKey, String localMatchKey) {
    }

    private CPlaySoundCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            var playSoundCommand = dispatcher.register(
                    ClientCommands.literal("cplaysound")
                            .then(
                                    ClientCommands.argument("sound", StringArgumentType.string())
                                            .suggests(CPlaySoundCommand::suggestSoundTokens)
                                            .executes(commandContext -> executePlay(commandContext.getSource(), StringArgumentType.getString(commandContext, "sound"), false))
                                            .then(
                                                    ClientCommands.argument("loop", BoolArgumentType.bool())
                                                            .executes(
                                                                    commandContext -> executePlay(
                                                                            commandContext.getSource(),
                                                                            StringArgumentType.getString(commandContext, "sound"),
                                                                            BoolArgumentType.getBool(commandContext, "loop")
                                                                    )
                                                            )
                                            )
                            )
            );

            dispatcher.register(ClientCommands.literal("playtrack").redirect(playSoundCommand));
        });
    }

    private static int executePlay(FabricClientCommandSource source, String rawSoundToken, boolean loop) {
        Identifier soundId = resolveSoundId(rawSoundToken);

        if (soundId == null) {
            source.sendError(Component.translatable("message.custommusicclientsideplayer.unknown_sound_id", rawSoundToken));
            return 0;
        }

        int result = CPlaySoundController.play(source, soundId, loop);
        if (result > 0) {
            source.getPlayer().sendSystemMessage(CPlaySoundController.buildSingleTrackMessage(soundId, loop));
        }

        return result;
    }

    private static Identifier resolveSoundId(String rawSoundToken) {
        String token = stripBalancedQuotes(rawSoundToken.trim());
        if (token.isEmpty()) {
            return null;
        }

        Identifier parsedId = Identifier.tryParse(token);
        if (parsedId != null) {
            return parsedId;
        }

        int colon = token.indexOf(':');
        if (colon <= 0 || colon >= token.length() - 1) {
            return null;
        }

        String namespace = token.substring(0, colon);
        String localToken = token.substring(colon + 1);
        Minecraft client = Minecraft.getInstance();
        List<Identifier> namespaceSounds = client.getSoundManager()
                .getAvailableSounds()
                .stream()
                .filter(id -> id.getNamespace().equals(namespace))
                .toList();

        LinkedHashSet<Identifier> matches = new LinkedHashSet<>();
        for (Identifier id : namespaceSounds) {
            if (id.getPath().equals(localToken)) {
                matches.add(id);
            }
        }

        for (Identifier id : namespaceSounds) {
            if (CTrackNameRepository.getDisplayName(id).equals(localToken)) {
                matches.add(id);
            }
        }

        for (Identifier id : namespaceSounds) {
            if (CTrackNameRepository.getDefaultDisplayName(id).equals(localToken)) {
                matches.add(id);
            }
        }

        return matches.size() == 1 ? matches.iterator().next() : null;
    }

    private static String stripBalancedQuotes(String token) {
        if (token.length() >= 2) {
            char first = token.charAt(0);
            char last = token.charAt(token.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return token.substring(1, token.length() - 1);
            }
        }

        return token;
    }

    private static CompletableFuture<Suggestions> suggestSoundTokens(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();

        String prefix = remaining.toLowerCase(Locale.ROOT);
        String normalizedPrefix = normalizeSuggestionPrefix(remaining).toLowerCase(Locale.ROOT);
        String localPrefix = extractLocalPrefix(normalizedPrefix);

        List<SuggestionEntry> suggestions = new ArrayList<>();
        for (Identifier id : context.getSource().getClient().getSoundManager().getAvailableSounds()) {
            String rawId = id.toString();
            String customName = CTrackNameRepository.getCustomName(id);
            if (customName == null || customName.isBlank()) {
                suggestions.add(new SuggestionEntry(forceDoubleQuoted(rawId), rawId, id.getPath()));
            } else {
                String displayName = CTrackNameRepository.getDisplayName(id);
                String aliasToken = id.getNamespace() + ":" + displayName;
                suggestions.add(new SuggestionEntry(forceDoubleQuoted(aliasToken), aliasToken, displayName));
            }
        }

        for (SuggestionEntry entry : suggestions) {
            String suggestionLower = entry.suggestion().toLowerCase(Locale.ROOT);
            String fullMatchLower = entry.fullMatchKey().toLowerCase(Locale.ROOT);
            String localMatchLower = entry.localMatchKey().toLowerCase(Locale.ROOT);
            if (suggestionLower.startsWith(prefix)
                    || fullMatchLower.startsWith(normalizedPrefix)
                    || localMatchLower.startsWith(localPrefix)) {
                builder.suggest(entry.suggestion());
            }
        }

        return builder.buildFuture();
    }


    private static String extractLocalPrefix(String normalizedPrefix) {
        int colon = normalizedPrefix.indexOf(':');
        if (colon >= 0 && colon < normalizedPrefix.length() - 1) {
            return normalizedPrefix.substring(colon + 1);
        }

        return normalizedPrefix;
    }

    private static String normalizeSuggestionPrefix(String prefix) {
        if (prefix.startsWith("\"") || prefix.startsWith("'")) {
            return prefix.substring(1);
        }

        return prefix;
    }

    private static String forceDoubleQuoted(String token) {
        String escaped = token.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }
}
