package phasico.precise.index;

import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import phasico.precise.Precise;
import phasico.precise.content.precise_staff.PreciseStaffItem;

public class PreciseItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Precise.MOD_ID);

    public static final DeferredItem<PreciseStaffItem> PRECISE_STAFF =
            ITEMS.registerItem("precise_physics_staff", PreciseStaffItem::new,
                    new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC).stacksTo(1));
}
