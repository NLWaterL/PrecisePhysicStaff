package phasico.precise.index;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public enum PreciseKeys {

    FINE_MODE("key.precise.fine_mode", GLFW.GLFW_KEY_TAB, "key.categories.precise"),
    ROTATE_MODE("key.precise.rotate_mode", GLFW.GLFW_KEY_R, "key.categories.precise"),
    ;

    private KeyMapping keybind;
    private final String translation;
    private final int    defaultKey;
    private final String category;

    PreciseKeys(String translation, int defaultKey, String category) {
        this.translation = translation;
        this.defaultKey  = defaultKey;
        this.category    = category;
    }

    public static void registerTo(Consumer<KeyMapping> consumer) {
        for (PreciseKeys key : values()) {
            key.keybind = new KeyMapping(key.translation, key.defaultKey, key.category);
            consumer.accept(key.keybind);
        }
    }

    public boolean isDown() {
        return keybind != null && keybind.isDown();
    }

    public boolean consumeClick() {
        return keybind != null && keybind.consumeClick();
    }
}
