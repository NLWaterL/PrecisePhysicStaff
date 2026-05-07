package phasico.precise.network.packets;

import foundry.veil.api.network.handler.ServerPacketContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import phasico.precise.Precise;
import phasico.precise.content.precise_staff.PreciseStaffAction;
import phasico.precise.content.precise_staff.PreciseStaffServerHandler;

import java.util.UUID;

public record PreciseStaffActionPacket(
        PreciseStaffAction action,
        UUID subLevel
) implements CustomPacketPayload {

    public static final Type<PreciseStaffActionPacket> TYPE =
            new Type<>(ResourceLocation.tryBuild(Precise.MOD_ID, "precise_staff_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PreciseStaffActionPacket> CODEC =
            StreamCodec.of((buf, v) -> v.write(buf), PreciseStaffActionPacket::read);

    private static PreciseStaffActionPacket read(RegistryFriendlyByteBuf buf) {
        return new PreciseStaffActionPacket(buf.readEnum(PreciseStaffAction.class), buf.readUUID());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeUUID(subLevel);
    }

    public void handle(ServerPacketContext context) {
        ServerPlayer player = context.player();
        PreciseStaffServerHandler handler = PreciseStaffServerHandler.get((ServerLevel) player.level());
        if (action == PreciseStaffAction.STOP_DRAG) {
            handler.stopDrag(player.getUUID());
        }
        if (action == PreciseStaffAction.LOCK) {
            PhysicsStaffServerHandler.get((ServerLevel) player.level()).toggleLock(subLevel);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
