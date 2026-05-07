package phasico.precise.neoforge.events;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;
import phasico.precise.Precise;
import phasico.precise.index.PreciseKeys;
import phasico.precise.neoforge.CommandInputScreen;
import phasico.precise.neoforge.PreciseClient;

@EventBusSubscriber(modid = Precise.MOD_ID, value = Dist.CLIENT)
public class PreciseNeoForgeClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        PreciseClient.STAFF_HANDLER.tick();
        PreciseClient.tickIdleSound(mc);
        while (PreciseKeys.ROTATE_MODE.consumeClick()) {
            PreciseClient.STAFF_HANDLER.toggleMode();
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        int key = event.getKey();
        if (key != GLFW.GLFW_KEY_ENTER && key != GLFW.GLFW_KEY_KP_ENTER && key != GLFW.GLFW_KEY_SLASH) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return;
        if (!PreciseClient.STAFF_HANDLER.isDragging()) return;
        mc.setScreen(new CommandInputScreen(PreciseClient.STAFF_HANDLER, key == GLFW.GLFW_KEY_SLASH));
    }

    // Prevent vanilla item-use from firing every tick while we are dragging.
    // Minecraft.handleKeybinds() fires InteractionKeyMappingTriggered continuously
    // while the use-item key is held, independently of MouseHandler.onPress.
    // Without this, vanilla right-click interactions hit the sublevel every tick.
    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isUseItem() && PreciseClient.STAFF_HANDLER.isDragging()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }
}
