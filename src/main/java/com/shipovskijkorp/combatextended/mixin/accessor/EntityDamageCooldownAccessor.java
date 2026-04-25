package com.shipovskijkorp.combatextended.mixin.accessor;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityDamageCooldownAccessor {
    @Accessor("timeUntilRegen")
    int combatExtended$getTimeUntilRegen();

    @Accessor("timeUntilRegen")
    void combatExtended$setTimeUntilRegen(int timeUntilRegen);
}
