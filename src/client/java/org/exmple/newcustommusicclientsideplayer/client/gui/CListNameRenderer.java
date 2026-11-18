package org.exmple.newcustommusicclientsideplayer.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.network.chat.Component;

final class CListNameRenderer {
    private CListNameRenderer() {
    }

    static boolean extract(
        GuiGraphicsExtractor graphics, Font font, Component name, int color,
        int x, int y, int rowRight, AbstractSelectionList<?> list, int scrollbarX,
        int mouseX, int mouseY
    ) {
        int left = Math.max(0, Math.max(x, list.getX()));
        int right = Math.min(graphics.guiWidth(), Math.min(rowRight, list.getRight()));
        if (list.maxScrollAmount() > 0 && list.scrollbarWidth() > 0
            && scrollbarX < right && scrollbarX + list.scrollbarWidth() > left) {
            right = Math.min(right, scrollbarX);
        }
        int top = Math.max(0, list.getY());
        int bottom = Math.min(graphics.guiHeight(), list.getBottom());
        if (right <= left || bottom <= top) {
            return false;
        }
        graphics.enableScissor(left, top, right, bottom);
        try {
            graphics.text(font, name, x, y, color, false);
        } finally {
            graphics.disableScissor();
        }
        int textRight = x + font.width(name);
        boolean clipped = x < left || textRight > right;
        if (clipped && mouseX >= left && mouseX < Math.min(right, textRight)
            && mouseY >= Math.max(y, top) && mouseY < Math.min(y + font.lineHeight, bottom)
            && graphics.containsPointInScissor(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(name.copy().withStyle(style -> style.withColor(color & 0xFFFFFF)),
                mouseX, mouseY);
            return true;
        }
        return false;
    }
}
