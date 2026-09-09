package io.github.polymeta.wondertrade.fabric.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.battles.*;
import com.cobblemon.mod.common.battles.ai.StrongBattleAI;
import com.cobblemon.mod.common.battles.ai.strongBattleAI.TrackerPokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import io.github.polymeta.wondertrade.WonderTrade;
import kotlin.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(StrongBattleAI.class)
public abstract class StrongBattleAIMixin {
    @Inject(method = "shouldSwitchOut", at = @At("HEAD"), cancellable = true, remap = false)
    public void lol(BattleSide side, PokemonBattle battle, ActiveBattlePokemon activeBattlePokemon, ShowdownMoveset moveset, CallbackInfoReturnable<Boolean> ctx) {
        WonderTrade.logger.info("do NOT fucking switch");
        ctx.setReturnValue(false);
    }

    @Inject(method = "considerSwitching", at = @At("HEAD"), cancellable = true, remap = false)
    public void fuckoff(ActiveBattlePokemon activeBattlePokemon, TrackerPokemon activeTrackerPokemon, List<TrackerPokemon> opponents, List<? extends Pair<InBattleMove, ? extends MoveTemplate>> availableMoves, List<? extends Pair<TrackerPokemon, ? extends BattlePokemon>> availableSwitches, PokemonBattle battle, BattleSide aiSide, CallbackInfoReturnable<ShowdownActionResponse> ctx) {
        WonderTrade.logger.info("dont even think about it");
        ctx.setReturnValue(null);
    }
}
