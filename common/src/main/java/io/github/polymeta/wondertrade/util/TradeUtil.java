package io.github.polymeta.wondertrade.util;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.permission.CobblemonPermission;
import com.cobblemon.mod.common.api.permission.PermissionLevel;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.evolution.Evolution;
import com.cobblemon.mod.common.api.pokemon.requirement.Requirement;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.requirements.LevelRequirement;
import io.github.polymeta.wondertrade.WonderTrade;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.joml.Math;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

public class TradeUtil {
    private static final Random rng = new Random();
    private static final ConcurrentSkipListSet<UUID> playersOnCooldown = new ConcurrentSkipListSet<>();

    public static boolean isPlayerOnCooldown(UUID playerId, boolean canBypass) {
        return playersOnCooldown.contains(playerId) && !canBypass && WonderTrade.config.cooldownEnabled;
    }

    public static void doWonderTrade(ServerPlayer player, Pokemon slot) {
        var canBypass = Cobblemon.INSTANCE.getPermissionValidator()
                .hasPermission(player, new CobblemonPermission("wondertrade.command.trade.bypass",
                        PermissionLevel.CHEAT_COMMANDS_AND_COMMAND_BLOCKS));
        if (isPlayerOnCooldown(player.getUUID(), canBypass)) {
            player.sendSystemMessage(WonderTrade.config.messages.cooldownFeedback(player.registryAccess()));
            return;
        }
        if (isPokemonForbidden(slot) && !canBypass) {
            player.sendSystemMessage(WonderTrade.config.messages.pokemonNotAllowed(player.registryAccess()));
            return;
        }
        var playerParty = Cobblemon.INSTANCE.getStorage().getParty(player);
        Pokemon wonderPoke = null;
        int levelReq = 1;
        for (int i = 0; i < 20; i++) {
            levelReq = 1;
            int pointer = rng.nextInt(WonderTrade.pool.pokemon.size());
            var poke = WonderTrade.pool.pokemon.get(pointer);
            if (poke.getPreEvolution() != null) {
                var evolutions = poke.getPreEvolution().getForm().getEvolutions();
                for (Evolution evo : evolutions) {
                    for (Requirement req : evo.getRequirements()) {
                        if (req instanceof LevelRequirement) {
                            levelReq = ((LevelRequirement) req).getMinLevel();
                            break;
                        }
                    }
                }
            } else {
                if (poke.isLegendary()) {
                    levelReq = 50;
                } else if (poke.isMythical()) {
                    levelReq = 40;
                }
            }
            for (Pokemon pokemon : playerParty) {
                if (pokemon.getLevel() >= levelReq) {
                    poke = null;
                    break;
                }
            }
            if (i < 9 && poke != null) {
                //player.sendSystemMessage(Component.literal("User is a loser and can't get this pokemon: " + poke.getSpecies().getName()));
                continue;
            }
            wonderPoke = WonderTrade.pool.pokemon.remove(pointer);
            break;
        }
        assert wonderPoke != null;
        if (wonderPoke.getLevel() == 1) {
            int meanLevel = 0;
            for (Pokemon pokemon : playerParty) {
                meanLevel += pokemon.getLevel();
            }
            meanLevel = meanLevel / playerParty.occupied();
            if (levelReq > 1) {
                int bluh = meanLevel - levelReq;
                if (bluh > 0)
                   wonderPoke.setLevel(Math.min(levelReq + rng.nextInt(meanLevel - levelReq), 100));
                else
                    wonderPoke.setLevel(levelReq);
            } else {
                wonderPoke.setLevel(1 + rng.nextInt(meanLevel));
            }
        }

        var tookPoke = playerParty.remove(slot);
        var pokeAdded = playerParty.add(wonderPoke);
        WonderTrade.pool.pokemon.add(slot);
        /*if(WonderTrade.config.adjustNewPokemonToLevelRange) {
            var level = slot.getLevel();
            if(level > WonderTrade.config.poolMaxLevel) {
                slot.setLevel(WonderTrade.config.poolMaxLevel);
            }
            else if (level < WonderTrade.config.poolMinLevel) {
                slot.setLevel(WonderTrade.config.poolMinLevel);
            }
        }*/
        //WonderTrade.savePool();
        if (WonderTrade.config.cooldownEnabled && !canBypass) {
            playersOnCooldown.add(player.getUUID());
            WonderTrade.scheduler.schedule(() -> {
                        playersOnCooldown.remove(player.getUUID());
                    },
                    WonderTrade.config.cooldown, TimeUnit.SECONDS);
        }
        player.sendSystemMessage(WonderTrade.config.messages.successFeedback(player.registryAccess()));
        var server = player.getServer();
        var broadcastMessage = WonderTrade.config.messages.broadcastPokemon(slot, player.registryAccess());
        if (server != null && !broadcastMessage.equals(Component.empty())) {
            server.getPlayerList().broadcastSystemMessage(broadcastMessage, false);
        }
    }

    public static boolean isPokemonForbidden(Pokemon pokemon) {
        for (String property : WonderTrade.config.blacklist) {
            if (PokemonProperties.Companion.parse(property).matches(pokemon)) {
                return true;
            }
        }
        return false;
    }
}
