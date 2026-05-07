package phasico.precise.content.precise_staff;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class PreciseStaffItem extends Item {

    public static final float RANGE = 128.0f;

    public PreciseStaffItem(Properties properties) {
        super(properties);
    }

    public static boolean isHolding(Player player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof PreciseStaffItem
                || player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof PreciseStaffItem;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(tipLine("precise.tooltip.key.rmb",            ChatFormatting.YELLOW, "precise.tooltip.desc.rmb"));
            tooltip.add(tipLine("precise.tooltip.key.lmb",            ChatFormatting.YELLOW, "precise.tooltip.desc.lmb"));

            tooltip.add(tipLine("",                                    ChatFormatting.BLACK, ""));
            tooltip.add(tipLine("precise.tooltip.selected",            ChatFormatting.AQUA, ""));
            tooltip.add(tipLine("precise.tooltip.key.r",               ChatFormatting.GOLD, "precise.tooltip.desc.r"));
            tooltip.add(tipLine("precise.tooltip.key.enter_slash",     ChatFormatting.GOLD, "precise.tooltip.desc.enter_slash"));
            tooltip.add(tipLine("precise.tooltip.key.tab",             ChatFormatting.GOLD, "precise.tooltip.desc.tab"));

            tooltip.add(tipLine("",                                    ChatFormatting.BLACK, ""));
            tooltip.add(tipLine("precise.tooltip.mode.rotate",         ChatFormatting.AQUA, ""));
            tooltip.add(tipLine("precise.tooltip.key.ws",              ChatFormatting.GOLD, "precise.tooltip.desc.ws.rotate"));
            tooltip.add(tipLine("precise.tooltip.key.ad",              ChatFormatting.GOLD, "precise.tooltip.desc.ad.rotate"));
            tooltip.add(tipLine("precise.tooltip.key.shift",           ChatFormatting.GOLD, "precise.tooltip.desc.shift"));
            tooltip.add(tipLine("precise.tooltip.key.ctrl",            ChatFormatting.GOLD, "precise.tooltip.desc.ctrl"));

            tooltip.add(tipLine("",                                    ChatFormatting.BLACK, ""));
            tooltip.add(tipLine("precise.tooltip.mode.move",           ChatFormatting.AQUA, ""));
            tooltip.add(tipLine("precise.tooltip.key.ws",              ChatFormatting.GOLD, "precise.tooltip.desc.ws.move"));
            tooltip.add(tipLine("precise.tooltip.key.ad",              ChatFormatting.GOLD, "precise.tooltip.desc.ad.move"));
            tooltip.add(tipLine("precise.tooltip.key.up_down",         ChatFormatting.GOLD, "precise.tooltip.desc.up_down.move"));

            tooltip.add(tipLine("",                                    ChatFormatting.BLACK, ""));
            tooltip.add(tipLine("precise.tooltip.hint",                ChatFormatting.AQUA, ""));
        } else {
            tooltip.add(Component.translatable("precise.tooltip.hold_shift",
                    Component.literal("[SHIFT]").withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static Component tipLine(String keyLang, ChatFormatting keyColor, String descLang) {
        return Component.empty()
                .append(Component.translatable(keyLang).withStyle(keyColor))
                .append(Component.literal(" "))
                .append(Component.translatable(descLang).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }
}
