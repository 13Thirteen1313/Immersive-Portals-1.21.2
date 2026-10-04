package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.render.CrossPortalEntityRenderer;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

/** Restores the shader-selection clipping maintenance preserved in the 1.21.3 fix. */
@Mixin(value = RenderSystem.class, remap = false)
public abstract class MixinRenderSystem_PortalClipping {
    @Inject(
        method = "setShader(Lnet/minecraft/class_5944;)V",
        at = @At("RETURN"), require = 1, allow = 1, remap = false
    )
    private static void ip_onCompiledShaderSelected(CallbackInfo ci) {
        ip_updateShaderClipping();
    }

    // This overload assigns RenderSystem.shader directly; it does not delegate
    // to setShader(CompiledShaderProgram). Vanilla RenderTypes use this path too.
    @Inject(
        method = "setShader(Lnet/minecraft/class_10156;)Lnet/minecraft/class_5944;",
        at = @At("RETURN"), require = 1, allow = 1, remap = false
    )
    private static void ip_onShaderProgramSelected(CallbackInfoReturnable<Object> ci) {
        ip_updateShaderClipping();
    }

    @Unique
    private static void ip_updateShaderClipping() {
        if (!IPGlobal.enableClippingMechanism) {
            return;
        }
        if (CrossPortalEntityRenderer.isRenderingEntityNormally
            || CrossPortalEntityRenderer.isRenderingEntityProjection) {
            FrontClipping.updateClippingEquationUniformForCurrentShader(true);
        }
        else if (RenderStates.isRenderingPortalWeather) {
            FrontClipping.updateClippingEquationUniformForCurrentShader(false);
        }
        else {
            // Terrain uploads its plane just before CompiledShaderProgram.apply
            // using the existing MixinLevelRenderer_Optional hook. Other draws
            // must not inherit the uniform left by a previous portal render.
            FrontClipping.unsetClippingUniform();
        }
    }
}
