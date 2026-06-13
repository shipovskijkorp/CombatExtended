package com.shipovskijkorp.combatextended.network;

import com.shipovskijkorp.combatextended.CombatExtended;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record DamagePreviewRequestPayload(int requestId, ItemStack stack) implements CustomPayload {
    public static final Id<DamagePreviewRequestPayload> ID = new Id<>(CombatExtended.id("damage_preview_request"));
    public static final PacketCodec<RegistryByteBuf, DamagePreviewRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER,
            DamagePreviewRequestPayload::requestId,
            ItemStack.PACKET_CODEC,
            DamagePreviewRequestPayload::stack,
            DamagePreviewRequestPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
