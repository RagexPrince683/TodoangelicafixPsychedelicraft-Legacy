# Suppress lens flares while drug shaders are active

## Changed

* The 2D effect wrappers are now prepared once for each rendered view before
  overlays are drawn. The same prepared `shouldApply` result controls both the
  active-drug-shader state and the shader pass, so a drug effect without a
  shader that will actually render does not suppress lens flares.
* Lens-flare updates and drawing are skipped whenever that shared state reports
  an active drug shader. Suppression clears the accumulated visibility state;
  after the final drug shader stops, the next view starts the flare from zero
  rather than displaying stale alpha.
* Shader wrappers are considered drug shaders by default so future drug shader
  wrappers automatically use this path. The existing depth-of-field, heat,
  underwater, and water-overlay wrappers opt out because their activation is
  environmental or configuration-driven. The mixed pause-menu/drug blur wrapper
  reports only its drug-provided blur component.

## Compatibility and validation

* This remains entirely in client rendering code and does not alter drug values,
  durations, gameplay, shader programs, Angelica, or other mods.
* Runtime visual validation is intentionally left to the developer, including
  shader transitions, separate camera views, and Angelica framebuffer paths.

# Fix drug shader corruption and restore lens flares

## Confirmed causes and fixes

* The scene-copy operation used whichever read framebuffer happened to be bound,
  even though the completed world belonged to the incoming draw framebuffer.
  Angelica permits those bindings to differ. Object-dependent contents from the
  stale read target were therefore copied into the reusable scene texture and
  appeared as repeated strips, displaced fragments, frozen rectangles, and
  growing bright bloom regions. Scene capture now temporarily binds the completed
  draw framebuffer for reading, selects its color buffer explicitly, honors the
  viewport origin, and restores the independent incoming bindings and buffer
  selections before composition.
* Every intermediate target is prepared as a complete full-screen overwrite with
  scissor, depth, stencil, blending, depth writes, and color masks made explicit.
  Texture unit zero remains the scene sampler, cache textures retain clamp-to-edge
  wrapping, and each pass still samples one ping-pong attachment while writing
  the other. A failed effect now abandons its incomplete intermediate instead of
  composing it over the valid incoming scene.
* The lens-flare regression was caused by projecting a camera-relative sun vector
  through an unsuitable matrix captured after world rendering and adding the
  interpolated camera position. Minecraft's world model-view expects the sun as
  a camera-relative direction there, so the addition applied camera translation
  a second time; the post-world capture could also already be an overlay/hand
  matrix. The default pass now captures model-view, projection, viewport, and
  render-view identity at the pre-sky world stage. Flare projection uses the
  direction directly, rejects non-positive clip `w` before perspective division,
  checks normalized clip depth, and maps viewport pixels into the physical
  overlay coordinate system.

## Scope and audit

* Cannabis (including `/drug Developer Cannabis set 1000`) and redshrooms enter
  the same corrected scene capture, shader-wrapper, ping-pong, and composition
  path. Their intentional wobble, color, blur, and distortion behavior is not
  disabled or clamped differently by this repair.
* The Angelica audit covered `AngelicaGLStateManagerService.glBindFramebuffer`,
  `GLStateManager` redirects, `FixedFunctionWorldRenderingPipeline.beginLevelRendering`,
  `GlFramebuffer.bindAsReadBuffer`, `GlFramebuffer.bindAsDrawBuffer`,
  `GlFramebuffer.readBuffer`, `CeleritasWorldRenderer.createChunkRenderMatrices`,
  and the composite/final-pass framebuffer ordering. No file under
  `REFERENCEFOLDER/AngelicaSRC` was changed.
* Terrain, translucent geometry, weather, and the hand finish before the existing
  post-world scene capture. Ordinary rain streaks and Minecraft's square sun are
  not treated as corruption, and no synchronization setting is proposed as a
  shader repair.

## Runtime verification

* Visual behavior remains unverified at runtime, including Angelica `2.2.13`
  with its shaderpack off, resized and offset viewports, alternate camera views,
  Cannabis at `1000`, and redshrooms. Code inspection does not establish visual
  success. Shaderpack-on interaction remains outside the reported compatibility
  target and is not claimed visually verified by this change.

# Fix rendering corruption and lens flare flicker

## Fixed

* The 2D ping-pong pipeline now selects texture unit zero explicitly before
  capturing, sampling, and composing a world frame. This prevents a texture unit
  left active by another renderer from making a scene shader sample an unrelated
  texture, including a flare image or a render-target texture.
* Read and draw framebuffer ownership is tracked separately where OpenGL 3.0 is
  available. Psychedelicraft restores both incoming bindings after intermediate
  rendering and composes only into the incoming draw framebuffer, rather than
  replacing Angelica's read binding with a guessed common binding.
* The render guard explicitly snapshots and restores the 2D bindings on texture
  units zero through three, in addition to the shader, framebuffer, viewport,
  matrices, and fixed-function attributes. These are every unit used by the
  affected Psychedelicraft 2D shaders, and restoration goes through Minecraft's
  redirected OpenGL helpers so Angelica's GLSM cache observes the changes.
* Lens flares now project the sun through the live model-view, projection, and
  viewport of the view being composed. Projection occurs before the orthographic
  overlay setup, rejects failed, non-finite, behind-camera, and clipped results,
  and then renders on texture unit zero with the fixed-function program selected.
* Removed same-tick flare suppression. Nested and portal-style views with the same
  tick time are legitimate independent views and must each use their own camera
  matrices and viewport.
* Standard 2D setup no longer clears the depth attachment owned by the current
  world framebuffer. The screen effects do not depth-test, and clearing a shared
  Angelica attachment after world rendering could invalidate later consumers.
* Final ping-pong composition now runs from a `finally` block, ensuring its
  temporary attribute frame and parent framebuffer are restored if an individual
  effect fails.

## Rendering audit

* `/drug Developer Cannabis set 1000` stores `1000` as the desired value, but
  `DrugSimple.update` clamps it to `1.0` before easing the active value. Cannabis'
  shader-facing color strengths are also bounded. Redshrooms use the same common
  post-processing path, while tobacco contributes desaturation to that path.
* The flare squares are drawn by the lens-flare path and use the bundled flare
  PNGs. The flare renderer has no sampler uniform of its own; its corruption risk
  was rendering through an incoming shader or nonzero active texture unit. The
  rectangular world copies and expanding bright regions instead implicate the
  shared scene ping-pong/bloom path, whose sampler uniforms correctly name units
  zero through three but previously did not establish unit zero before binding
  the captured scene.
* The ping-pong attachments remain distinct from their sampled input on every
  pass: it samples the current cache texture and selects the other color
  attachment as its draw buffer. Targets are retained and resized rather than
  allocated in the render loop, and every full-screen pass overwrites its target;
  there is no intentional retained brightness history.
* Lens visibility remains CPU ray tracing and does not read depth from the GPU.
  No stale-depth visibility query, blocking readback, asynchronous query pool, or
  accumulating history exists in this path. Memory growth was not established;
  the reported approximately `42.228 MiB` direct-buffer display is not treated as
  evidence of a leak.

## Angelica compatibility audit

* The included audit snapshot contains Angelica's
  `AngelicaGLStateManagerService` forwarding framebuffer, shader, active-texture,
  and texture operations into `GLStateManager`; `MixinFramebuffer` replaces the
  vanilla depth renderbuffer with a texture; and `FinalPassRenderer` and
  `CompositeRenderer` use separate read/draw framebuffer bindings and explicitly
  manage texture units. Those implementations informed the exact restoration and
  framebuffer split used here.
* The included source snapshot has no build/version metadata, while this project
  declares no Angelica development dependency: optional client mods are supplied
  from the ignored `devmods/client` directory. Therefore the screenshot's
  Angelica `2.2.13` is the only identified runtime version, and source-to-binary
  equality cannot be confirmed from repository metadata.
* No Angelica source, shaderpack setting, other mod class, transformer target, or
  dedicated-server initialization path was changed.

## Runtime verification

* Runtime behavior with Angelica `2.2.13`, shaderpacks both off and on, nested
  views, Cannabis `1000`, redshrooms, tobacco, and framebuffer resizing remains
  for developer validation. No claim is made that the reported frame drops are a
  memory leak.

# Fix LWJGL client attribute mask compilation

## Fixed

* Replaced the unavailable `GL11.GL_CLIENT_ALL_ATTRIB_BITS` reference in the
  client-only render state guard with LWJGL 2.9.1's supported
  `GL11.GL_CLIENT_VERTEX_ARRAY_BIT` mask. The guarded effects use Minecraft's
  `Tessellator`, which changes client vertex-array enable and pointer state, but
  they do not change client pixel-store state.
* The existing paired `glPushClientAttrib` and `glPopClientAttrib` calls remain
  inside the guard, and both rendering callers continue to restore the guard in
  `finally` blocks. Server attribute state, matrices, shader program, framebuffer,
  and active texture restoration are unchanged for Angelica compatibility.

# Fix Psychedelicraft rendering with Angelica

## Fixed

* Psychedelicraft now captures the framebuffer that is actually bound when its
  post-processing begins instead of assuming that Minecraft's main framebuffer
  is still Angelica's active render target. The tobacco desaturation pass and the
  other 2D drug shaders consequently copy from, render into, and return to the
  correct target rather than sampling incomplete or stale output.
* Screen post-processing and lens flares now restore the prior framebuffer,
  shader program, active texture unit, texture/blend/depth/alpha/color state,
  client-array state, viewport, and model-view/projection/texture matrices. The
  explicit object-binding restoration keeps Angelica's tracked GL state in sync;
  the same path falls back to vanilla `OpenGlHelper` when Angelica is absent.
* Lens flares reject missing worlds and cameras, invalid screen sizes, invisible
  effects, behind-camera/invalid projections, and duplicate calls for the same
  rendered frame. Existing `ResourceLocation` and texture-manager allocations
  remain cached, while world-dependent visibility state is discarded on a world
  change or effect destruction.
* Corrected the flare-alpha expression so the central flare selection no longer
  accidentally compares `alpha * index` with `8`, which caused unstable flare
  brightness under drug effects.
* Removed the alcohol effect's Nether portal texture overlay. Camera wobble,
  double vision, motion blur, poisoning, and every unrelated drug shader remain
  unchanged.

## Angelica compatibility audit

* Psychedelicraft has no compile-time dependency on Angelica APIs. Its only direct
  Angelica reference is the existing ASM instruction-owner allowlist for
  `com.gtnewhorizons.angelica.glsm.GLStateManager.glClear(int)`; that class and
  method are present in the locally included Angelica revision.
* The included Angelica source replaces Minecraft framebuffer depth renderbuffers
  with depth textures, tracks framebuffer/program/texture state through GLSM, and
  can keep a pipeline-owned framebuffer bound while world rendering runs. It does
  not expose a stable Forge 1.7.10 API for inserting Psychedelicraft's legacy 2D
  effects. The fix therefore uses the live OpenGL binding plus Minecraft's
  `OpenGlHelper` dispatch, which Angelica redirects through its existing
  `GLStateManagerService` and which remains the original vanilla-compatible path
  without Angelica.
* No Angelica source, global performance option, unrelated mod, mixin target, or
  transformer target was changed. Psychedelicraft's transformer allowlist remains
  limited to its five existing Minecraft classes and does not include MCHELI.

## Resource and lifecycle behavior

* Lens flares continue to use the texture manager's cached textures and allocate
  no textures, framebuffers, or display lists per frame. Their bounded projection
  and visibility values are reset when the world changes.
* Shader programs, ping-pong textures, and depth/framebuffer resources continue
  to be released and recreated by the existing resource-reload lifecycle. No
  rendering class was moved into common or dedicated-server initialization.

# Fix dedicated-server packet handler loading

## Fixed

* Forge 1.7.10 constructs every SimpleImpl handler during common channel
  registration, including handlers whose receiving side is `Side.CLIENT`. The two
  client-bound handlers directly referenced `Minecraft.theWorld` (whose runtime
  type is `WorldClient`), so constructing them while a dedicated server registered
  the channel attempted to load a client-only class.
* Both registered handlers are now common-only adapters. They copy each packet's
  payload out of its Netty buffer and delegate primitive, string, and byte-array
  data through `PSProxy`; `ClientProxy` sends that copied data through the existing
  client-tick queue, while `ServerProxy` safely ignores impossible client-bound
  delivery. Missing worlds, stale worlds, absent entities, unloaded blocks, and
  unsupported partial-update targets continue to be rejected.
* Client GUI construction and client Forge/FML event callbacks were also moved
  behind client-proxy initialization. Common initialization no longer constructs
  event handlers or exposes static handler fields that refer to client-only
  Minecraft, Forge, rendering, shader, particle, or audio classes.

## Compatibility

* The `psychedelicraft` channel, packet discriminators 0 and 1, `Side.CLIENT`
  receivers, wire formats, and multiplayer and integrated-server synchronization
  paths are unchanged.
* Changed files: `CHANGELOG.md`, `Psychedelicraft.java`, `PSProxy.java`,
  `ClientProxy.java`, `ClientEventHandler.java`, `ClientPacketHandler.java`,
  `ServerProxy.java`, `PSEventForgeHandler.java`, `PSEventFMLHandler.java`,
  `PSGuiHandler.java`, `PacketExtendedEntityPropertiesDataHandler.java`, and
  `PacketTileEntityDataHandler.java`.
* The exact five-class Minecraft transformer allowlist and scoped OpenGL hooks
  remain unchanged. Psychedelicraft still returns unapproved classes untouched
  before ASM work and never modifies MC Heli or any other mod.

# Restrict core transformations to approved Minecraft classes

## Transformer safety

* Added an exact `transformedName` allowlist at both bytecode transformation
  entry points. Null bytecode still returns null, while a null or unapproved
  transformed name returns the identical incoming byte-array object before ASM
  parsing, instruction inspection, hierarchy lookup, or frame generation.
* Removed the unrestricted `OpenGLTransfomer` registration. Its OpenGL state and
  clear hooks now run, after each class-specific transformer, only for the same
  five approved Minecraft classes. Both scoped and legacy general-transformer
  paths remain behind the entry-point allowlist.
* Added per-transformer synthetic markers so an approved class which is presented
  to Psychedelicraft again is not given duplicate hooks. Transformations continue
  from the incoming bytes, preserving changes installed earlier by other coremods.
* Psychedelicraft no longer transforms MC Heli or any other mod's classes. It also
  does not transform Forge, FML, LaunchWrapper, LWJGL, Java, or any unlisted
  Minecraft class.

## Remaining exact targets

* `net.minecraft.client.renderer.EntityRenderer` remains necessary for world-pass,
  camera, hand, overlay, fog, lightmap, sky, and render-state boundary hooks.
* `net.minecraft.client.renderer.RenderGlobal` remains necessary for the
  hallucination entity-rendering hook.
* `net.minecraft.client.renderer.OpenGlHelper` remains necessary to mirror vanilla
  blend-function and active-texture changes into Psychedelicraft's render state.
* `net.minecraft.client.renderer.RenderHelper` remains necessary to mirror
  vanilla standard item-lighting changes.
* `net.minecraft.client.audio.SoundManager` remains necessary for drug-related
  sound-volume adjustment.

## Visual limitation

* OpenGL calls made inside other mods and unlisted vanilla classes are no longer
  observed. Psychedelicraft's normal hooks in the five approved classes remain,
  but shader render-state mirroring cannot react to third-party rendering changes;
  unusual third-party render paths may therefore have less accurate psychedelic
  compositing. No effect may depend on intercepting another mod's rendering.

# Diagnose remaining Ragecraft renderWorld transformation report

## Transformer diagnostics and failure reporting

* The core transformer now emits one startup identity diagnostic containing the
  loaded `IvClassTransformerClass` name, its code-source URL, and the artifact's
  available `Implementation-Version`. The JAR manifest is explicitly populated
  from the Gradle project version so a Ragecraft log can identify the selected
  artifact instead of inferring it from an isolated transformation message.
* Required-method bookkeeping now records target matching independently from hook
  installation. An unmatched target reports candidate methods with each actual
  name, descriptor, normalized SRG name, and normalized descriptor; a matched
  target whose instruction hook fails is reported as a different failure.
  `renderWorld`, `renderWorldAdditions`, and `preRenderSky` remain three separately
  tracked required transformations even though they share `func_78471_a(FJ)V`.
* The render-hand depth hook no longer reports success for an unsupported
  instruction pattern or for an existing unpaired pre/post hook. Those cases now
  fail the required transformation, causing the transformer's existing outer
  transaction boundary to return the original renderer bytes rather than retain
  partial changes. Supported vanilla and Angelica depth-clear patterns, operand
  validation, branch targets, and paired pre/post insertion are unchanged.

## Source and runtime findings

* The quoted `Could not transform expected method in class ...` text is emitted
  by the repository's copied
  `ivorius.psychedelicraft.internal.asm.IvClassTransformerClass` at revision
  `3847739`. Revision `f90d180` replaced that non-throwing error with `Required
  transformation failed`, and the current source therefore cannot emit the
  quoted line. The untouched `REFERENCEFOLDER` also contains the upstream
  IvToolkit wording, but it is outside the current source set and was not changed.
* `Obf: false` in that historical message only records whether LaunchWrapper gave
  the manager equal original and transformed class names. It does not establish
  that SRG normalization was wrong. No normalization change was made because the
  inspected path already applies Forge method-name/descriptor remapping followed
  by the development-name fallback.
* No running Ragecraft artifact, surrounding runtime log, Angelica renderer JAR,
  or mod-directory inventory is present in this checkout. The only available JAR
  is the Gradle wrapper. Source-set and build configuration inspection found one
  packaged transformer implementation and no bundled dependency copy; runtime
  staleness, duplicate coremods, the exact loaded revision, and the ultimate
  method-versus-instruction failure therefore remain unconfirmed until the new
  identity line and surrounding failure are captured from the affected instance.

## Compatibility and visual behavior

* Changed files: `CHANGELOG.md`, `build.gradle.kts`,
  `IvClassTransformerClass.java`, `PsychedelicraftClassTransformer.java`, and
  `EntityRendererTransformer.java`.
* No visual behavior is deliberately disabled. If required render-hand pairing
  cannot be installed, Psychedelicraft rejects the complete `EntityRenderer`
  transformation and restores its input bytes; it does not silently claim that
  only the dependent hand-depth behavior was disabled.
* Java 8, Forge 1.7.10, Angelica-compatible supported patterns, gameplay data,
  registrations, NBT, packets, and the build-system version remain unchanged.

# Fix DHPsychedelicraft 1.0.1 Ragecraft startup crash

## Fixed

* Confirmed that the failing `renderWorld` stage was the hand-rendering depth-clear
  match, not method remapping: `func_78471_a(FJ)V` had already been found, while
  Angelica can rewrite the adjacent `GL11.glClear(I)V` owner to its
  `GLStateManager.glClear(I)V` wrapper.
* The hand hook now anchors on the supported `renderHand(float, int)` invocation,
  accepts its valid special or virtual invocation forms, and only selects the
  immediately associated `GL_DEPTH_BUFFER_BIT` clear. Vanilla LWJGL and Angelica
  GLSM owners are supported without matching an unrelated earlier clear.
* All hand-hook insertion points and operand shapes are validated before mutation.
  Existing paired hooks are not duplicated. An unknown renderer now leaves this
  one hook unchanged and logs its class, method, hook, and failed match stage once,
  disabling only the non-default render-pass hand depth effect rather than aborting
  startup or returning partially patched bytecode. Other required hooks remain
  required and visible on failure.
* Corrected `postRenderHand` to post `RenderHandEvent.Post`, restoring the intended
  pre/post pairing and depth-multiplier reset after hand rendering.

## Compatibility notes

* Changed files: `CHANGELOG.md`, `EntityRendererTransformer.java`, and
  `PsycheCoreBusClient.java`.
* The inspected code supports the vanilla/development and production SRG names,
  vanilla `GL11.glClear`, and Angelica's GLSM `glClear` replacement. Runtime
  confirmation with the exact Ragecraft Angelica build is still unresolved because
  no Angelica binary or source is available in this checkout.
* Weather2's annotation warning and HardcoreDarkness's preceding log message have
  no code connection to this confirmed instruction-match failure and remain
  separate. No gameplay, data format, registration, or dedicated-server loading
  path was changed.

# Add Grow Light purple appearance and crafting recipe

## Added

* Added a shaped Ore Dictionary recipe for one Grow Light using glass, purple
  dye, redstone, glowstone, and iron ingots.
* Documented the Grow Light's animated purple appearance, maximum vanilla light,
  and 30% Drying Table processing bonus.

## Changed

* Bound the existing `grow_light` block to the supplied `grow_lamp` animated
  texture and corrected the texture metadata filename for Minecraft resource
  loading. Vanilla Minecraft 1.7.10 still emits white world light without a
  compatible shader feature.

# Add grow light drying accelerator

## Added

* Added the `grow_light` glass-style, full-cube light block to the main
  Psychedelicraft creative tab. It emits the maximum Minecraft light level and
  intentionally has no crafting recipe, tile entity, or custom renderer.
* Added the Grow Light display name to every bundled language file and renamed
  the supplied animated texture assets to match the block registry name.

## Changed

* Drying tables now process valid recipes 30% faster while at least one Grow
  Light is present in the surrounding 3x3x3 cube. Detection is cached for 20
  processing ticks, does not stack, and preserves fractional progress in NBT.

# Restore NEI drying and drink lookup

## Fixed

* Replaced unsupported `Object[]` recipe inputs with copied, validated typed
  drying arrays and drink alternative lists, preventing lookup-time
  `PositionedStack` failures.
* Restored every valid `DryingRegistry` entry as a nine-slot drying recipe,
  preserved Ore Dictionary alternatives, metadata, NBT and output quantities,
  and retained both Drying Table catalysts.
* Added bounded shared descriptor snapshots while keeping mutable NEI recipe and
  ingredient-cycling objects local to each handler instance.
* Matched filled drink results by container and meaningful fluid identity/state
  without requiring an exact quantity.

## Changed

* Replaced the misleading acquisition crafting grid with a plain, localized
  information page and separated generation, harvesting, trade, and loot paths.
* Added an explicit Drying Table layout and process-arrow category rectangle;
  this lookup rectangle does not enable crafting-table inventory transfer.

## Verification

* Source and whitespace checks were run. Dependency resolution could not run
  because the environment proxy rejected the Gradle distribution download.
* Per project instructions no binary compilation or game launch was attempted;
  runtime rendering, navigation, lookup, and catalyst behavior remain unverified.

# Fix NEI cached-recipe handler ownership

## Fixed

- Changed the Acquisition, Drying, and Drink Preparation cached recipe types to
  non-static inner classes so each `TemplateRecipeHandler.CachedRecipe` has the
  required enclosing handler instance.
- Moved each affected lazy recipe index from shared static state to its owning
  handler instance, while retaining immutable bounded indexes and all existing
  recipe, usage, ingredient-alternative, slot, catalyst, and localization data.

## Compatibility and verification

- Forge 1.7.10, Java 8, and the optional NEI dependency declarations are
  unchanged.
- The affected source paths and repository diff were inspected without compiling
  binaries, adding tests, or launching Minecraft.

# Remove normal ItemLightingEvent rendering allocations

## Item lighting path

- The normal injected `RenderHelper` hooks now call a primitive client callback rather than constructing and posting `ItemLightingEvent`. The reported 35,137 objects (1,124,384 bytes) establish allocation pressure, but do not establish retained-memory leakage.
- The normalized vanilla light directions are initialized once. The callback retains the original two directions, 0.4 ambient strength, 0.6 diffuse strength, 0.0 specular strength, and enable/update ordering.
- Logical item-lighting state is retained even when no Psychedelicraft shader is active and is applied when each shader becomes active. Minecraft's own `RenderHelper` calls remain untouched and no Psychedelicraft program is bound just for a notification.
- `ShaderMain` now skips uploads when the enabled flag, either standard light position/strength pair, or ambient strength already matches that program's successfully uploaded value. Fixed uniform names remove string construction for standard light indices. Cache validity is cleared when its program is deleted, replaced, or relinked, including resource reload recreation.

## Event compatibility

- `ItemLightingEvent`, its public constructor, and the subscribed `standardItemLighting` adapter remain available. `postItemLightingEvent(boolean)` is the explicit allocation-producing compatibility dispatch.
- Normal internal hooks no longer automatically notify external `ItemLightingEvent` listeners. Internal operations use either the primitive path or an explicitly requested event dispatch, never both.
- The completed primitive `GLSwitchEvent` path and its explicit compatibility dispatcher are unchanged.

## Verification limits and follow-up work

- Source call paths, shader activation, program deletion/recreation, fixed-argument uniform overloads, and the transformer method names/descriptors were reviewed. Per project instruction, no binary compilation or runtime launch was performed.
- Runtime profiling, gameplay coverage, Angelica integration, memory impact, and frame-rate impact remain unmeasured and require developer feedback.
- Separate confirmed allocation sites remain in the normal rendering hooks for `GLTranslateEvent`, `GLRotateEvent`, `GLScaleEvent`, and other rendering events, and `ShaderMain.activate` still creates its two color arrays. They are intentionally outside this item-lighting change.

# Reduce Psychedelicraft rendering allocation and resource lifetime overhead

## Confirmed causes fixed

- `PsycheCoreBusClient.psycheGLEnable`, `psycheGLDisable`, blend, active-texture, fog, and clear hooks allocated a Forge event for every injected GL call. The injected internal path now invokes primitive callbacks in `PSCoreHandlerClient`; public event types and an explicit compatibility event dispatch remain available, without delivering twice to Psychedelicraft's handler.
- `IvShaderInstance.setUniformIntsOfType`, `setUniformFloatsOfType`, and `setUniformMatrix` created direct NIO buffers for every upload. Uploads now use render-thread-local, fixed-capacity scratch buffers, correctly clear/limit/flip each upload, and common one-to-four argument overloads avoid varargs arrays at hot call sites. Larger legacy array uploads retain their compatible fallback.
- `ScreenEffectWrapper.dealloc` did not destroy its owned effect, leaving `EffectMotionBlur`'s 30 texture objects alive across reloads. Wrapper cleanup now reaches `destruct`, and motion-blur cleanup is repeatable and clears IDs, arrays, dimensions, indices, and timing state.
- Motion blur and ping-pong captures redefined texture storage on every sample. Their already-sized textures now use `glCopyTexSubImage2D`; storage is still recreated when display dimensions change, preserving history size and sampling semantics.
- Shader resource streams were not closed, and compile/link/validation failure paths could retain shader/program objects. Streams now close and failed GL objects are deleted.
- `DrugRenderer.update` performed the same camera-only lens-flare ray traces for every client living entity. Camera visual updates are now scoped to Minecraft's current render-view entity; per-entity drug state remains separate.
- `DrugProperties.changeDrugModifier` removed and recreated an identical modifier every update. It now keeps an equal UUID/amount/operation modifier and correctly uses the requested attribute.
- Disabled 2D shaders no longer enter framebuffer/post-processing setup.
- Shader activation records and restores the caller's current program rather than assuming program zero, avoiding desynchronization with rendering state caches such as Angelica's.

## Compatibility and limits

- Forge 1.7.10, Java 8, LWJGL 2, existing registry/NBT/network data, IvToolkit independence, development-mod support, and reality-rift behavior are unchanged.
- No Angelica binary or source is present in this checkout. Changes therefore use only Minecraft/LWJGL state entry points already used by the mod and do not invent or link Angelica APIs. Runtime interaction with a particular Angelica build remains unverified.
- The supplied histogram establishes allocation churn, not by itself a retained leak. The empty wrapper cleanup is a confirmed Psychedelicraft GPU-resource leak; NEI populations, unrelated stream populations, Angelica-owned state/native buffers, and GPU-driver retention remain untraced suspects and are not attributed to Psychedelicraft.
- Runtime memory and frame-rate results are unverified; no measured improvement is claimed.
# Add devmods support and fix EntityRealityRift configuration and spawning

## Changed

* Added non-publishable local development-mod loading from `devmods/` for client
  and dedicated-server runs, plus isolated `devmods/client/` loading for the
  client run. Empty tracked directories remain valid and local JARs are ignored.
* Added the balancing key `enableRealityRifts`, defaulting to `true`, and applied
  it to random, command, rift-jar, and saved-entity paths without removing or
  renaming entity registration.
* Constrained `randomTicksUntilRiftSpawn` to the values accepted by its random
  bound. Positive values preserve the existing default and spawn chance, while
  `0` and `-1` disable random spawning.
* Moved rift-jar release decisions fully onto the logical server and retained the
  existing per-player, per-tick random spawn conditions.

## Root cause

* The only rift control was a random-spawn interval whose description treated a
  negative value as a global disable even though the value was checked in only
  one of three spawn paths. Commands and rift jars bypassed it, and saved rifts
  never consulted configuration.
* Rift-jar release mutated its stored fraction on both logical sides, even though
  only its entity creation was server guarded. Local mod JARs had no isolated,
  non-publishable runtime path.

## Verification limits

* Source, configuration flow, Gradle wiring, Git ignores, and diffs were reviewed
  without compiling binaries, running automated tests, or launching Minecraft.
* The original cubic random offset and per-player tick frequency have no clear
  design documentation. Developer feedback is required before changing those
  conditions.

# Fix launch verification and runtime dependency discovery errors

## Fixed

* Replaced max-stack-only ASM output with stack-map frame computation for every
  internal transformer pass. The hierarchy resolver reads class metadata without
  defining classes through `LaunchClassLoader`, considers Forge development and
  production names, and computes real superclass/interface relationships instead
  of unconditionally merging references to `java/lang/Object`.
* Reworked the `renderWorld` cancellation branch to locate the exact
  `GL11.glClear(I)V` call and branch around both its integer operand and invocation.
  This leaves an empty operand stack at the new target and allows a valid frame to
  be emitted there.
* Reworked the `renderHand` cancellation branch to identify its receiver and
  float argument by instruction shape rather than fixed instruction offsets.
  Frames, labels, and line-number nodes can therefore no longer move the insertion
  point onto an invalid operand stack. The `setupCameraTransform` early-return
  branch is covered by the same frame computation.
* Made required hook failures reject the entire class transformation with the
  exact class, method descriptor, and hook identifier instead of writing a
  partially transformed class and treating the pass as successful.
* Moved JGit from the mod's `implementation` dependencies to the Gradle buildscript
  classpath. The version helpers retain JGit and the existing `noCommitHash`
  behavior, while Minecraft runtime and published dependency metadata no longer
  receive JGit, its annotation archive, or JavaEWAH transitively.

## Diagnosis

* `COMPUTE_MAXS` only recalculated operand-stack and local-variable limits. It did
  not create the stack-map frame required at the new `IFNE` target in
  `EntityRenderer.renderWorld`, which caused the fatal `VerifyError`.
* The earlier discoveries of `org/eclipse/jgit/annotations/NonNull.class` and the
  multi-release `META-INF/versions/9/module-info.class` came from JGit and its
  transitive build libraries being placed on the game runtime classpath. Those
  scanner messages are separate from the transformed Minecraft bytecode failure
  and do not, by themselves, indicate corrupt archives.

## Verification limits

* Changes were inspected at source and Gradle dependency-model level only. In
  accordance with repository instructions, no binaries were compiled and the
  Minecraft client was not launched. Runtime rendering and launch verification
  remain for the user.

# Fix chunk-watcher player type mismatch

## Fixed

* Replaced the invalid assignment of `WorldServer.playerEntities` to a
  `List<EntityPlayerMP>` with explicit, type-safe player filtering.
* Preserved chunk-watcher recipient filtering while ensuring only
  `EntityPlayerMP` instances are passed to the server player manager.

## Verification

* The compilation error occurred before the Minecraft client could start.
* This source-only change was inspected for type safety and recipient filtering;
  runtime behavior was not exercised, and no binary compilation was attempted.

# Remove IvToolkit dependency

## Changed

* Removed IvToolkit from Gradle dependencies, Forge mod metadata, core-plugin
  loading checks, and publication metadata.
* Added Psychedelicraft-owned implementations for the packet, partial-update,
  multiblock, tile-entity, inventory, math, matrix, ray-tracing, Bézier, chat,
  rendering, shader, framebuffer, and ASM facilities used by the mod.
* Preserved the existing packet discriminators and wire layout while moving
  received world changes onto the client tick thread and validating their
  targets without loading chunks.
* Added shader recreation on resource reload. Existing graphics allocations are
  released before replacements are created, and display-size changes continue
  to recreate size-dependent framebuffer resources.
* Updated installation and build documentation for the self-contained JAR.

## Compatibility

* Psychedelicraft no longer needs IvToolkit to be installed.
* Other installed mods may still have their own IvToolkit requirement.
* No packet format compatibility change is intentional.

## Blockers

None known. Runtime gameplay and graphics behavior were not exercised as part of
this source-only migration.

# Add optional NEI acquisition and processing support

## Added

* Added an optional, discovery-loaded NEI client integration with cached Drying,
  Drink Preparation, and Acquisition categories.
* Added live-registry drying displays for all supported ingredient forms, nine
  occupied input slots, preserved output counts, both machine catalysts, base
  times, and environmental behavior.
* Added non-recipe acquisition pages for the implemented crop, tree, mushroom,
  peyote, villager, and loot paths, including cannabis buds and seeds.
* Added accurate custom drink-filling displays that use copied fluid output data
  instead of `RecipeFillDrink`'s placeholder recipe output.
* Added `docs/nei-coverage.md` with coverage ownership, disabled content, and
  remaining processing-display limitations.

## Compatibility

* NEI is compile-only and is not a required or published runtime dependency.
* The existing NEI development runtime remains unchanged, and common/dedicated
  server initialization does not reference NEI classes.

## Verification

* Source, localization, dependency configuration, and whitespace were inspected.
* In accordance with repository instructions, no binary compilation or runtime
  launch was attempted. Runtime behavior remains unverified.
