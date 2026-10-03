# Drug renderer architecture

## Render order and ownership

`PsychePostProcessor` is the authoritative screen-effect backend. Core hooks
bracket the actual `updateCameraAndRender -> renderWorld` call and track nested
world views. Processing runs once for the main player view immediately after
Forge's `dispatchRenderLast`, before vanilla's first-person depth clear and hand
rendering. With Angelica 2.1.29, Iris finalizes its world pipeline before that
Forge call. Shaderpack hand pixels already included by Iris remain in the scene.

The processor captures Minecraft's named main color attachment into one of two
display-sized RGBA8 targets. Each owns its own complete, color-only framebuffer.
Active effects alternate those targets; with no active effects, an identity copy
follows the same capture/composition lifecycle. The final color is blitted back
to Minecraft's main framebuffer exactly once. All blits use only
`GL_COLOR_BUFFER_BIT`; the processor neither copies nor clears world depth.

Shader draws use an explicit clip-space quad and standalone programs, without
changing fixed-function projection or model-view matrices. `PostPassState`
restores the raster, depth-write, program, vertex and texture state touched by a
pass. `PostState` restores incoming read/draw framebuffer bindings, their buffer
selectors, viewport and copy/allocation state. Ordinary GL entry points are
redirected through Angelica's GLSM when installed.

Vanilla subsequently clears `GL_DEPTH_BUFFER_BIT` on the restored world/hand
destination and renders the hand with normal depth testing and writes. A null
`PSRenderStates.currentRenderPass` means no legacy auxiliary pass is active and
must allow that clear and vanilla block overlays. Only an explicitly active
legacy non-default pass retains the old clear/overlay suppression semantics.
The processor provides no extra hand clear, depth offset or always-on-top hand
rendering.

## Effects and resource lifecycle

`PostContext` samples the view entity's existing simulated drug values. The
current order is drug color/deformation, blur, radial blur, double vision,
colored bloom, bloom, noise, digital, heat, underwater distortion, wet lens and
motion blur. Environmental and drug screen passes read color only. Radial blur
has no active simulation producer; depth-of-field configuration is retained
without activating an unverified depth source.

Motion blur owns a separate completed-frame history target, committed after the
chain and main composition finish. Disable/re-enable, resize, resource reload,
world/dimension/view changes, pause and long render gaps invalidate history.
Glare uses captured current-camera matrices and renders on the main destination
after composition/history, independently of the screen-effect toggle. HUD artwork
follows the world/hand stage.

Targets are recreated on display/world/dimension changes and released on unload
or resource reload. Programs have a separate lifecycle so target resizing does
not recompile them. Nested/offscreen and changed-entity views do not enter the
main pipeline. Retained external screen/lifecycle entry points terminate at
this backend rather than executing the retired wrapper chain.

## Angelica development checks

`gradlew.bat runClient` runs the control client;
`gradlew.bat runClient -PangelicaDev` adds the pinned Angelica 2.1.29 and GTNHLib
development runtime. Angelica is optional, is not bundled and is not added to
the dedicated server. `REFERENCEFOLDER/` is audit-only; version-specific behavior
must also be checked against the pinned runtime.

The `debugPostProcessing` graphics setting enables sampled stage/error logs and
an identity pixel comparison after target recreation when no effect applies.
It defaults to disabled. Check close-wall empty/block/model hands, normal
distance, sky, transparent blocks, water and camera angles with effects both
enabled and disabled in each client configuration.
