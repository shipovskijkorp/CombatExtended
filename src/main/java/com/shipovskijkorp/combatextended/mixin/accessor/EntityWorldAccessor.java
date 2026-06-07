package com.shipovskijkorp.combatextended.mixin.accessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityWorldAccessor {
    @Accessor("level")
    Level combatExtended$getLevel();
}
