package qouteall.imm_ptl.core.mixin.client.render;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

/** Keep destination terrain behind the portal plane, including cutout foliage. */
@Mixin(targets = "net.minecraft.class_761", remap = false)
public abstract class MixinLevelRenderer_PortalTerrainClipping {
    // renderSectionLayer itself is stable across framegraph scheduling changes.
    // The old injections around calls from renderLevel no longer match in 1.21.2.
    // Object + @Coerce avoids compiling an intermediary RenderType dependency;
    // the exact selector below still pins the complete runtime signature.
    @Inject(
        method = "method_3251(Lnet/minecraft/class_1921;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
        at = @At("HEAD"), require = 1, allow = 1, remap = false
    )
    private void ip_beginTerrainClipping(
        @Coerce Object renderType, double x, double y, double z,
        Matrix4f modelView, Matrix4f projection, CallbackInfo ci
    ) {
        if (PortalRendering.isRendering()) {
            FrontClipping.setupInnerClipping(
                PortalRendering.getActiveClippingPlane(), modelView, -FrontClipping.ADJUSTMENT
            );
            if (PortalRendering.isRenderingOddNumberOfMirrors()) {
                MyRenderHelper.applyMirrorFaceCulling();
            }
            if (IPGlobal.enableDepthClampForPortalRendering) {
                CHelper.enableDepthClamp();
            }
        }
    }

    // Covers both the normal return and the missing-shader early return.
    @Inject(
        method = "method_3251(Lnet/minecraft/class_1921;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
        at = @At("RETURN"), require = 1, remap = false
    )
    private void ip_endTerrainClipping(CallbackInfo ci) {
        if (PortalRendering.isRendering()) {
            FrontClipping.disableClipping();
            MyRenderHelper.recoverFaceCulling();
            if (IPGlobal.enableDepthClampForPortalRendering) {
                CHelper.disableDepthClamp();
            }
        }
    }
}
