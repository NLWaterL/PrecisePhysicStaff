package phasico.precise.network;

import foundry.veil.api.network.VeilPacketManager;
import phasico.precise.Precise;
import phasico.precise.network.packets.PreciseStaffActionPacket;
import phasico.precise.network.packets.PreciseStaffDragPacket;

public class PrecisePacketManager {

    public static final VeilPacketManager INSTANCE = VeilPacketManager.create(Precise.MOD_ID, "0.1");

    public static void init() {
        INSTANCE.registerServerbound(PreciseStaffDragPacket.TYPE,   PreciseStaffDragPacket.CODEC,   PreciseStaffDragPacket::handle);
        INSTANCE.registerServerbound(PreciseStaffActionPacket.TYPE, PreciseStaffActionPacket.CODEC, PreciseStaffActionPacket::handle);
    }
}
