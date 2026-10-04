package qouteall.imm_ptl.core.mixin.client.render;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.renderer.RendererUsingStencil;

/** The once-per-frame fallback guard applies only to the outermost world view. */
@Mixin(value = RendererUsingStencil.class, remap = false)
public abstract class MixinStencilRenderer_PortalRecursion {
    @Shadow(remap = false)
    protected abstract void doPortalRendering(Matrix4f modelView);

    @Inject(method = "onBeforeTranslucentRendering(Lorg/joml/Matrix4f;)V",
        at = @At("HEAD"), cancellable = true, require = 1, allow = 1, remap = false)
    private void ip_renderNestedView(Matrix4f modelView, CallbackInfo ci) {
        if (PortalRendering.isRendering()) {
            // Every nested view needs its own pass, including siblings at the
            // same depth. Do not reset the frame guard or clear parent stencil.
            // renderPortalContent still enforces the configured recursion limit.
            doPortalRendering(modelView);
            ci.cancel();
        }
    }

    @Inject(method = "onBeforeHandRendering(Lorg/joml/Matrix4f;)V",
        at = @At("HEAD"), cancellable = true, require = 1, allow = 1, remap = false)
    private void ip_skipNestedLateFallback(Matrix4f modelView, CallbackInfo ci) {
        if (PortalRendering.isRendering()) {
            // The framegraph hook already rendered this nested view. Re-entering
            // the late fallback can render twice or erase its parent's stencil.
            ci.cancel();
        }
    }
}
