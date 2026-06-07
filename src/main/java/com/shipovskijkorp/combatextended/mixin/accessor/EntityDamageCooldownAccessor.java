package com.shipovskijkorp.combatextended.mixin.accessor;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityDamageCooldownAccessor {
    @Accessor("invulnerableTime")
    int combatExtended$getInvulnerableTime();

    @Accessor("invulnerableTime")
    void combatExtended$setInvulnerableTime(int invulnerableTime);
}
