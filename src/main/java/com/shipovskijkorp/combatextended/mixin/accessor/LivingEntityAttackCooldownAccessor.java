package com.shipovskijkorp.combatextended.mixin.accessor;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAttackCooldownAccessor {
    @Accessor("ticksSinceLastAttack")
    int combatExtended$getTicksSinceLastAttack();

    @Accessor("ticksSinceLastAttack")
    void combatExtended$setTicksSinceLastAttack(int ticksSinceLastAttack);
}
