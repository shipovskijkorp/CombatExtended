package com.shipovskijkorp.combatextended.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAttackCooldownAccessor {
    @Accessor("attackStrengthTicker")
    int combatExtended$getAttackStrengthTicker();

    @Accessor("attackStrengthTicker")
    void combatExtended$setAttackStrengthTicker(int attackStrengthTicker);
}
