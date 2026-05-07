package phasico.precise.neoforge;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import phasico.precise.Precise;
import phasico.precise.index.PreciseItems;

@Mod(Precise.MOD_ID)
public class PreciseNeoForge {

    public PreciseNeoForge(IEventBus modBus) {
        Precise.init(modBus);
        modBus.addListener(PreciseNeoForge::onBuildCreativeTab);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            PreciseClient.init(modBus);
        }
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(PreciseItems.PRECISE_STAFF);
        }
    }
}
