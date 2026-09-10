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
