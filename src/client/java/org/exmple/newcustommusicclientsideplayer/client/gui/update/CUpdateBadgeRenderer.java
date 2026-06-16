package org.exmple.newcustommusicclientsideplayer.client.gui.update;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;
import org.exmple.newcustommusicclientsideplayer.client.update.CUpdateStatus;

public final class CUpdateBadgeRenderer {
    private static final Identifier UPDATE_AVAILABLE_SPRITE = Identifier.fromNamespaceAndPath(
        NewcustommusicclientsideplayerClient.MOD_ID,
        "icon/update_available"
    );
    private static final int ICON_SIZE = 8;
    private static final int RIGHT_PADDING = 7;

    private CUpdateBadgeRenderer() {
    }

    public static void render(GuiGraphicsExtractor guiGraphics, Button button, CUpdateStatus status) {
        if (button == null || status == null || !status.updateAvailable()) {
            return;
        }

        int x = button.getX() + button.getWidth() - RIGHT_PADDING - ICON_SIZE;
        int y = button.getY() + (button.getHeight() - ICON_SIZE) / 2;
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, UPDATE_AVAILABLE_SPRITE, x, y, ICON_SIZE, ICON_SIZE);
    }
}
