package mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Перехватывает рендер ванильного TitleScreen и подменяет его кастомным
 * фоном/вотермаркой MainMenu. cancel() на HEAD — рендерим полностью своё,
 * не трогая renderYawOffset/прочую игровую логику (это просто экран меню).
 *
 * gui.MainMenu появится отдельным файлом следующим сообщением — здесь
 * сигнатура render(DrawContext, int, int, float) согласована заранее,
 * чтобы не было рассинхрона при сборке.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        gui.MainMenu.render(context, mouseX, mouseY, delta);
        ci.cancel();
    }
}
