package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.IPCGlobal;

/** Repairs the framebuffer-clear hook for the released Minecraft 1.21.2 port. */
@Mixin(targets = "net.minecraft.class_761", remap = false)
public abstract class MixinLevelRenderer_FramegraphClear {
    // 1.21.2 moved this clear from renderLevel into a static framegraph callback.
    // glClear ignores the stencil test, so allowing it during a nested world render
    // erases the outer world's color AND depth outside the portal. The existing
    // renderer replaces it with a stencil-tested fog-color triangle when needed.
    // Use the exact intermediary callback descriptor; this source is compiled
    // directly for the released JAR and must not use the old renderLevel refmap.
    @Redirect(
        method = "method_62218(Lorg/joml/Vector4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(I)V",
            remap = false
        ),
        remap = false,
        require = 1,
        allow = 1
    )
    private static void ip_preserveOuterWorldDuringPortalClear(int mask) {
        if (!IPCGlobal.renderer.replaceFrameBufferClearing()) {
            RenderSystem.clear(mask);
        }
    }
}
