package com.shipovskijkorp.combatextended.combat.damage;

import com.shipovskijkorp.combatextended.combat.config.CombatBalance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

public final class SpecialExplosionDamage {
    private SpecialExplosionDamage() {
    }

    public static float reducePlayerDamageIfNeeded(DamageSource source, float amount) {
        if (!shouldReducePlayerDamage(source)) {
            return amount;
        }

        return amount * CombatBalance.SPECIAL_EXPLOSION_PLAYER_DAMAGE_MULTIPLIER;
    }

    public static boolean shouldReducePlayerDamage(DamageSource source) {
        return source.is(DamageTypes.BAD_RESPAWN_POINT) || isEndCrystalExplosion(source);
    }

    private static boolean isEndCrystalExplosion(DamageSource source) {
        if (!source.is(DamageTypes.EXPLOSION) && !source.is(DamageTypes.PLAYER_EXPLOSION)) {
            return false;
        }

        return isEndCrystal(source.getDirectEntity()) || isEndCrystal(source.getEntity());
    }

    private static boolean isEndCrystal(Entity entity) {
        return entity instanceof EndCrystal;
    }
}
