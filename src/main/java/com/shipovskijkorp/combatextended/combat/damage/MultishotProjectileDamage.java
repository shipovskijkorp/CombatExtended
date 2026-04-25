package com.shipovskijkorp.combatextended.combat.damage;

import com.shipovskijkorp.combatextended.mixin.accessor.PersistentProjectileEntityWeaponAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public final class MultishotProjectileDamage {
    private static final Identifier MULTISHOT_ID = Identifier.ofVanilla("multishot");

    private MultishotProjectileDamage() {
    }

    public static boolean shouldBypassDamageCooldown(DamageSource source) {
        Entity sourceEntity = source.getSource();
        if (!(sourceEntity instanceof PersistentProjectileEntity projectile)) {
            return false;
        }

        ItemStack weaponStack = ((PersistentProjectileEntityWeaponAccessor) projectile).combatExtended$getWeaponStack();
        return weaponStack != null
                && !weaponStack.isEmpty()
                && weaponStack.getItem() instanceof CrossbowItem
                && hasMultishot(weaponStack);
    }

    private static boolean hasMultishot(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = stack.getOrDefault(
                DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
        );

        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(MULTISHOT_ID) && entry.getIntValue() > 0) {
                return true;
            }
        }

        return false;
    }
}
