package phasico.precise.neoforge;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import phasico.precise.content.precise_staff.PreciseStaffClientHandler;

public class CommandInputScreen extends Screen {

    private final PreciseStaffClientHandler handler;
    private final StringBuilder buffer = new StringBuilder();
    private boolean skipNextChar;

    public CommandInputScreen(PreciseStaffClientHandler handler, boolean skipNextChar) {
        super(Component.empty());
        this.handler = handler;
        this.skipNextChar = skipNextChar;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            handler.executeCommand(buffer.toString());
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && buffer.length() > 0) {
            buffer.deleteCharAt(buffer.length() - 1);
            return true;
        }
        return true; // consume all keys so nothing leaks to game input
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (skipNextChar) { skipNextChar = false; return true; }
        buffer.append(codePoint);
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // render at action bar position (above hotbar/xp bar)
        String text = "> " + buffer + "_";
        int x = width / 2 - font.width(text) / 2;
        int y = height - 60;
        graphics.drawString(font, text, x, y, 0xFFFFFF, true);
    }

    @Override
    protected void init() {}
}
