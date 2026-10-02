<h1 align=center>Psychedelicraft</h1>
============
<p>A continuation of Psychedelicraft for 1.7.10</p>
<p>It's a fork of original repository which uses GTNH Gradle for build, it allows you to build it even with newer version of JDK & Gradle</p>
<p>OpenGL diagnostics are opt-in so normal play does not flood the console.</p>

## Post-processing backend rewrite

Phase 1 has received visual acceptance and a zero-change full RGBA identity
comparison on the pinned client. Phase 2 color-only heat has received visual acceptance with clean shader-pass
GL diagnostics. Phase 3 water distortion and fading wet-lens droplets have also
received visual acceptance with clean pass diagnostics. Phase 4 restores drug
screen effects on the new backend and has received visual acceptance. Phase 5
adds dedicated motion history and has received visual acceptance with clean
motion/history diagnostics. Phase 6 independent lens-flare composition has also
received visual acceptance, including glare with heat and motion blur, with clean
draw/restoration diagnostics. All six stages use the new authoritative backend.
These results apply to the pinned Angelica client without a shaderpack; they do
not certify every shaderpack or third-party view implementation. JourneyMap was
excluded from further checks at the user's direction.
Gameplay, drug simulation, camera effects and existing HUD overlays are unchanged.
The legacy screen wrappers and `IvOpenGLTexturePingPong` are retained temporarily
as inactive source; initialization and world events no longer invoke their chain.
Legacy screen-render/lifecycle entry points delegate to the new processor rather
than retaining an alternate scene capture or effect chain. The old per-entity
lens renderer is no longer instantiated. The legacy `shaderEnabled` key controls
retired geometry-shader source only; active screen effects use `shader2DEnabled`.

`PsychePostProcessor.render(partialTicks)` runs immediately after
`ForgeHooksClient.dispatchRenderLast`, inside the main `renderWorld` call. The
exact Angelica [`2.1.29`](https://github.com/GTNewHorizons/Angelica/tree/2.1.29)
checkout, commit `0e793806183fd74b6bac80c5ac71446946614406`, confirms that its
`MixinEntityRenderer` finalizes the world before that hook and `FinalPassRenderer`
outputs to Minecraft's main color attachment and rebinds the main framebuffer.
Without shaderpacks this precedes vanilla hand rendering. With shaderpacks,
Angelica has already rendered its opaque/translucent hand into the finished
image; those hand pixels intentionally remain part of the source. The HUD is
rendered later in both cases.

The processor accepts only an outer main-player view bracketed around the actual
`updateCameraAndRender -> renderWorld` invocation. Nested world calls, changed
view entities and auxiliary views are excluded. Processing is limited to one
completed-world stage per call (the second eye for anaglyph). The source is always
Minecraft's named main framebuffer, with its attachment identity, completeness
and display-sized texture checked explicitly; incoming bindings do not identify
an image or an intermediate target.

`PsychePostTarget` owns one FBO and one exact display-sized RGBA8 color texture.
There are two color surfaces, `sceneA` and `sceneB`, with no depth attachments.
Phase 1 blits the completed main image into A, performs an identity A-to-B blit,
then blits B back to the main framebuffer. Every copy covers the entire image at
origin zero with nearest filtering. No shader, matrix stack, Tessellator, legacy
screen-copy fallback or shared Angelica attachment participates. The new backend
requires OpenGL 3.0 or OpenGL 2.1 with `ARB_framebuffer_object`, independently of Angelica's
presence. Unsupported hardware reports this limitation without activating the
legacy chain.

Resize, world/dimension change and resource reload destroy owned targets; the
next accepted view recreates and validates both surfaces. World unload releases
them on the client thread. Every pass fully overwrites its destination. Motion
blur alone owns an additional full-resolution history image, allocated only when
needed; it is never a third scene ping-pong surface. Initial activation copies
the current processed scene, and only a completely rendered and composited frame
updates history. Disable/re-enable, resize, world/dimension/reload/view changes,
pausing and render gaps over 250 ms invalidate it. The next temporal pass starts
from current color. `visual.motionBlur` retains its existing toggle and simulation
strengths; frame-time-scaled persistence makes trail decay independent of render
rate. History is written before any later lens overlay, so glare cannot accumulate
in temporal state. Other effects read current scene color only.

The identity backend only changes framebuffer bindings/selectors, viewport,
scissor enable, framebuffer sRGB, texture-unit-zero binding/active unit, and pixel
unpack binding for allocation. It restores those through ordinary GL entry points
redirected by Angelica's `GLSMRedirector`; it uses no cache bypass, raw backend
calls or native attribute-stack inference. Identity copying leaves programs, blend/depth/color masks, matrices and client
arrays untouched. The shader-draw scope explicitly saves/restores its program,
owned VAO/array-buffer binding, color/depth masks, enable flags, polygon mode,
clip/sample coverage, texture bindings and sampler objects on units zero/one.
It never loads matrices or uses Tessellator/fixed-function shader inputs. Main-FBO selectors are restored before
caller bindings. The synthetic legacy skybox is no longer drawn or used to clear
world depth. `bypassPingPongBuffer`, `disableDepthBuffer` and `renderFakeSkybox` are
retained configuration keys but do not control the new color-only backend.

Use `./gradlew runClient -PangelicaDev` for the exact pinned client environment:
Angelica 2.1.29, GTNHLib 0.11.4, and transitive UniMixins. They remain client-only
development dependencies and are not published or added to `runServer`. Avoid
installing duplicate copies in development-mod directories. The reference source
under `REFERENCEFOLDER/AngelicaSRC` is audit-only and is not the authoritative
version-specific checkout.

`visual.shader2DEnabled` enables/disables the new backend. Diagnostic option
`visual.debugPostProcessing` defaults to false. When enabled, it reports entry,
capture/source/owned FBOs, completeness, dimensions, identity/composition order
and stage-specific GL errors. Successful progress is sampled to keep logs small;
errors are always reported. After each target recreation it also compares every
RGBA pixel of the original and returned main image in GPU readback memory and
reports the number changed. This creates no image/test artifacts and is an
identity-only diagnostic, not a claim of visual acceptance.

Heat uses a new GLSL shader with two animated layers from the existing noise
asset, the original biome strength (up to 0.01 UV), and texel-center UV bounds.
It samples only current scene color and noise, with no depth requirement. Its
owned clip-space quad and explicit inputs/outputs require OpenGL 3.0; older
hardware with only the framebuffer extension retains identity copying and
reports the shader capability limitation. `visual.biomeHeatDistortion` controls
heat independently of the overall post-processing enable setting. Water retains
the existing 0.025 underwater strength at a slower animation rate, independently
controlled by `visual.waterDistortion`. Droplet refraction follows the existing
wet-screen timer and `visual.waterOverlayEnabled`, with two sliding layers of the
existing droplet asset. Both sample only current scene color.
All shader effects share `FullscreenPostEffect` and alternate only A/B; the
last written target is composited once. Core-profile polygon-mode restoration
uses its single joint mode, avoiding a nonexistent second result slot.

Drug parameters are sampled once into `PostContext` from the existing simulation.
The ordered chain is waves/fractal/pulse/contrast/color rotation/saturation,
separable blur, radial blur, double vision, colored bloom, bloom, noisy vertical
distortion, digital pixelation/palette/glyphs, then heat, water and wet droplets.
Blur and bloom expose individual axis/repetition passes to the processor, which
owns every A/B swap. Pause-menu blur retains its existing setting and tick fade.
Noise uses a fixed sample budget and a time-derived seed; digital uses the existing
glyph atlas and pixel-scale settings with color-only brightness. All scene samples
are clamped to texel centers. Radial blur has a new pass, but remains inactive
because the existing simulation never requested it. Depth-of-field settings are
retained but depth-dependent DoF is inactive in this color-only replacement.
Only the final motion-blur pass reads previous frames. No pass changes drug simulation.

Lens glare uses an independent GLSL sprite overlay on the explicitly named main
framebuffer after final composition and history commit. It has no scene/history
texture input or capture API. It reuses the flare/blindness assets, sizes, positions,
rain/occlusion smoothing and `visual.sunFlareIntensity`; glare remains available
when screen effects are disabled. Current-view camera matrices project the sun
as a direction at infinity, with facing rejection but no finite far-plane depth
test. Views without a sky do not emit glare. The overlay preserves destination
alpha and restores blend factors/equations in addition to the shared draw state.

Phase 1 must be tested in a normal world and the previous black-screen desert,
with rapid rotation, resize/fullscreen, F1 and leave/rejoin. A normal visible
world, no stale regions/new GL errors, and zero changed identity pixels are the
first gate. Heat is phase 2, water phase 3, drug effects phase 4, dedicated motion
history phase 5 and current-camera lens-flare composition phase 6. Each phase
requires an in-game visual test before implementation proceeds to the next.
The phase gates have passed. The listed scenarios remain a regression checklist
for other installations, shaderpacks and third-party render views.

Psychedelicraft is self-contained and does **not** require IvToolkit. Other mods in
your installation may still require IvToolkit independently.

## Internal replacements

The Psychedelicraft JAR contains the narrowly scoped implementations it uses for:

* SimpleImpl tile-entity and player-property synchronization (packet IDs `0` and
  `1`, with the legacy field order and payload encoding preserved). Incoming
  updates are validated and applied from the client tick thread.
* Multiblock, inventory, NBT, range, chat, Bézier, and ray-tracing helpers.
* The new client post backend under `client.rendering.post`, plus internal
  matrices, particles, rendering primitives and retained inactive legacy shader
  helpers. Owned graphics resources are released on replacement/unload and
  recreated lazily after resource reload or display changes.
* The focused ASM transformer and development-name remapping support required by
  Psychedelicraft's render, camera, OpenGL, and sound hooks.

These implementations live under `ivorius.psychedelicraft.internal`; no
`ivorius.ivtoolkit` production classes are bundled. The adapted portions retain
their original Apache-2.0 notices. There is no intentional packet compatibility
change.

## Core transformer scope

Psychedelicraft only transforms these exact Minecraft classes:

* `net.minecraft.client.renderer.EntityRenderer` for world-pass, camera, hand,
  overlay, fog, lightmap, sky, and render-state boundary hooks.
* `net.minecraft.client.renderer.RenderGlobal` for hallucination entity rendering.
* `net.minecraft.client.renderer.OpenGlHelper` for blend and active-texture state.
* `net.minecraft.client.renderer.RenderHelper` for standard item-lighting state.
* `net.minecraft.client.audio.SoundManager` for drug-related sound volume.

It does not transform MC Heli or any other mod. OpenGL calls made inside other
mods or unlisted vanilla classes are deliberately not intercepted. Consequently,
third-party nested views must enter the normal world scope to be identified as
auxiliary views. The new screen passes establish their own state and do not use
the legacy mirror to infer framebuffer, viewport, source texture or shader state.

## Quick guide:

Requires: [Gradle](https://gradle.org) and a Java 17 or 21 JDK to run the modern
build tooling. Produced classes remain Java 8 compatible.

* `./gradlew setupDecompWorkspace`
* `./gradlew build`

## Local development mods

Place development-only mod JARs that work on both physical sides directly in
`devmods/`. They are added through the non-publishable runtime configuration and
are loaded by both `runClient` and `runServer`. Place client-only JARs in
`devmods/client/`; only `runClient` receives that directory. Never place a
client-only mod directly in `devmods/`, because doing so also exposes it to the
dedicated-server run.

Both directories may be empty. Local JARs are ignored by Git, are not copied into
the Psychedelicraft artifact, and are not included in published dependency
metadata. Use one directory per JAR: putting the same mod in both locations can
still cause Forge to discover duplicate mods during a client run.

## Reality rift configuration

Reality rifts use the following keys in the `balancing` category:

* `enableRealityRifts` defaults to `true`. When `false`, random, command, and rift
  jar spawn paths are blocked. Saved rifts remain registered and loadable, but
  remove themselves on their next logical-server update.
* `randomTicksUntilRiftSpawn` retains its default of `216000` ticks and accepts
  values from `-1` through `2147483647`. Positive values are passed to the random
  player-tick check as its bound; `0` and `-1` disable only random spawning.

When enabled, the existing random behavior is preserved: at the end of a logical
server player tick, the configured chance is evaluated and a successful check
places a rift at independently randomized offsets of less than 50 blocks on each
axis. The historical code does not document whether the resulting cubic offset
or the per-player tick frequency was intentional, so this change does not invent
distance, dimension, terrain, or population rules. Developer feedback is still
needed if those spawn conditions should be narrower.

## Grow Light

The Grow Light is a craftable, animated purple full-cube block that emits vanilla
light level 15. A Grow Light in the surrounding 3x3x3 area makes a Drying Table
process 30% faster. Minecraft 1.7.10 still renders the emitted world light as
white unless a compatible shader feature supplies colored lighting; the block
itself requires no colored-light mod, custom renderer, or tile entity.

## Optional Not Enough Items integration

When Not Enough Items 2.7.4-GTNH is installed on the client, Psychedelicraft adds
Drying, Drink Preparation, and Acquisition pages. NEI is not required on clients
or dedicated servers. See [the coverage reference](docs/nei-coverage.md) for the
source-backed coverage matrix and current limitations.
