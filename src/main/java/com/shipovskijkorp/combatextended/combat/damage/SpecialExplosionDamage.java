package com.shipovskijkorp.combatextended.combat.damage;

import com.shipovskijkorp.combatextended.combat.config.CombatBalance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.decoration.EndCrystalEntity;

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
        return source.isOf(DamageTypes.BAD_RESPAWN_POINT) || isEndCrystalExplosion(source);
    }

    private static boolean isEndCrystalExplosion(DamageSource source) {
        if (!source.isOf(DamageTypes.EXPLOSION) && !source.isOf(DamageTypes.PLAYER_EXPLOSION)) {
            return false;
        }

        return isEndCrystal(source.getSource()) || isEndCrystal(source.getAttacker());
    }

    private static boolean isEndCrystal(Entity entity) {
        return entity instanceof EndCrystalEntity;
    }
}
