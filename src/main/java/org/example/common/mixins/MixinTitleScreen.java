package org.example.common.mixins;

import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    @Inject(method = "init()V", at = @At("HEAD"))
    public void onInit(CallbackInfo ci) {
        System.out.println("Hello from MixinTitleScreen! " + this.getClass().getName());
    }

}
