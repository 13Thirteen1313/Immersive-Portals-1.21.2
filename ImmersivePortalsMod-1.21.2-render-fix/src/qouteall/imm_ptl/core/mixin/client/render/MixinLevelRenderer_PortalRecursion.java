package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.class_3695;
import net.minecraft.class_4184;
import net.minecraft.class_4604;
import net.minecraft.class_9779;
import net.minecraft.class_9925;
import net.minecraft.class_9958;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.imm_ptl.core.render.MyGameRenderer;

/** Render portals in every world view, before its translucent batches are drawn. */
@Mixin(targets = "net.minecraft.class_761", remap = false)
public abstract class MixinLevelRenderer_PortalRecursion {
    // 1.21.2 moved this draw into the main framegraph callback, and renamed
    // Sheets.translucentCullBlockSheet to translucentItemSheet (method_29382).
    // The old optional injection into renderLevel consequently matches nothing.
    @Inject(
        method = "method_62214(Lnet/minecraft/class_9958;Lnet/minecraft/class_9779;Lnet/minecraft/class_4184;Lnet/minecraft/class_3695;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/class_9925;Lnet/minecraft/class_9925;Lnet/minecraft/class_9925;Lnet/minecraft/class_9925;ZLnet/minecraft/class_4604;Lnet/minecraft/class_9925;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/class_4722;method_29382()Lnet/minecraft/class_1921;"),
        require = 1, allow = 1, remap = false
    )
    private void ip_renderPortalsInMainPass(
        class_9958 fog, class_9779 deltaTracker, class_4184 camera,
        class_3695 profiler, Matrix4f modelView, Matrix4f projection,
        class_9925<?> itemTarget, class_9925<?> mainTarget,
        class_9925<?> particleTarget, class_9925<?> outlineTarget,
        boolean renderOutline, class_4604 frustum, class_9925<?> translucentTarget,
        CallbackInfo ci
    ) {
        try {
            IPCGlobal.renderer.onBeforeTranslucentRendering(modelView);
        }
        finally {
            // The child view changes global shader state. Restore the exact fog
            // captured for this parent pass, rather than leaving destination fog.
            MyGameRenderer.updateFogColor();
            RenderSystem.setShaderFog(fog);
            MyGameRenderer.resetDiffuseLighting();
            FrontClipping.disableClipping();
        }
    }
}
