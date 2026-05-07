package phasico.precise;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phasico.precise.content.precise_staff.PreciseStaffServerHandler;
import phasico.precise.content.precise_staff.PreciseSubLevelObserver;
import phasico.precise.index.PreciseItems;
import phasico.precise.index.PreciseSoundEvents;
import phasico.precise.network.PrecisePacketManager;

public class Precise {

    public static final String MOD_ID   = "precise";
    public static final String MOD_NAME = "Precise Physics Staff";
    public static final Logger LOGGER   = LoggerFactory.getLogger(MOD_ID);

    public static void init(IEventBus modBus) {
        PreciseItems.ITEMS.register(modBus);
        PreciseSoundEvents.SOUNDS.register(modBus);
        PrecisePacketManager.init();

        SableEventPlatform.INSTANCE.onPhysicsTick((physicsSystem, timeStep) ->
                PreciseStaffServerHandler.get(physicsSystem.getLevel()).physicsTick(physicsSystem));

        SableEventPlatform.INSTANCE.onSubLevelContainerReady((level, container) -> {
            if (container instanceof ServerSubLevelContainer serverContainer) {
                serverContainer.addObserver(new PreciseSubLevelObserver((ServerLevel) level));
            }
        });
    }

    public static ResourceLocation path(String path) {
        return ResourceLocation.tryBuild(MOD_ID, path);
    }
}
