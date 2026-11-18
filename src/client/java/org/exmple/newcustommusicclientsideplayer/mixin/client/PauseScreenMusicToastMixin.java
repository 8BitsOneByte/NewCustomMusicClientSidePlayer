package org.exmple.newcustommusicclientsideplayer.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.exmple.newcustommusicclientsideplayer.client.customnowplayingtoast.CCustomNowPlayingToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMusicToastMixin extends Screen {
    @Unique
    private final CCustomNowPlayingToast.PauseDisplay newcustommusicclientsideplayer$musicDisplay =
        new CCustomNowPlayingToast.PauseDisplay();

    protected PauseScreenMusicToastMixin(Component title) {
        super(title);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void newcustommusicclientsideplayer$extractCurrentTrack(
        GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callbackInfo
    ) {
        if (((PauseScreen) (Object) this).showsPauseMenu()) {
            this.newcustommusicclientsideplayer$musicDisplay.extract(graphics, this.font, this.width);
        }
    }
}
