# Drug renderer architecture

## Render order and ownership

For every `RenderWorldEvent.Post`, `DrugEffectState` copies the current view
entity's already-simulated effect values. This snapshot is used for pass
selection, uniforms, and flare suppression; it is discarded after composition.
There is no tick/frame global guard, so nested views receive independent work.

The completed incoming draw framebuffer and its draw color selection are the
scene source. The current viewport origin and dimensions define the captured
rectangle. Psychedelicraft owns two equally sized RGBA textures and one FBO.
Passes alternate source and destination attachments, overwrite the complete
destination with scissor, depth, stencil and blending disabled, and clamp scene
samples to valid texel centers. The incoming framebuffer, attachments, and depth
are never cleared. Composition returns the last valid texture to the original
draw framebuffer only after all passes return successfully.

The order is: legacy world deformation replacement; heat; underwater; wet-screen;
simple color effects; motion blur; Power blur; depth-of-field (non-depth fallback
when no private depth is available); radial blur; bloom; colored bloom; double
vision; Power noise; and Zero digital. Overlay artwork stays after the chain.
Lens flares are outside the chain and are suppressed before flare projection or
drawing whenever the same snapshot selects a drug shader.

## Removed assumptions

The old renderer assumed it could own the active world shader, rerender the world
for depth/shadows, use fixed-function built-ins across every third-party vertex
layout, and alter clip coordinates using a mixture of object, eye, and clip
spaces. It also assumed program zero represented the desired fixed-function state.
Those assumptions conflict with Angelica's program and framebuffer ownership and
have been removed. Fullscreen shaders use the explicit quad position and texture
coordinates supplied by the existing `shaderBasic.vert` GLSL 1.20 path.

## Angelica audit and limitations

The implementation was checked against the local source for
`AngelicaGLStateManagerService`, `GLStateManager`, `GlFramebuffer`,
`FixedFunctionWorldRenderingPipeline`, `CompositeRenderer`, and
`FinalPassRenderer`. Their cached program operations, separate framebuffer
bindings, and final-pass sequencing informed explicit state capture/restoration.
Angelica itself is neither modified nor bundled.

The old vertex deformations are represented in screen space because injecting a
foreign program into Angelica-owned terrain submission is not a compatible world
hook. Consequently their exact per-block parallax can differ, but they remain
animated spatial distortions rather than being removed or replaced by a tint.
Depth-of-field and Zero use their established no-depth variants because the
replacement does not take ownership of, clear, or synchronously read Minecraft's
or Angelica's depth attachment. Runtime visual validation remains the developer's
responsibility.
