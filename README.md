<h1 align=center>Psychedelicraft</h1>
============
<p>A continuation of Psychedelicraft for 1.7.10</p>
<p>It's a fork of original repository which uses GTNH Gradle for build, it allows you to build it even with newer version of JDK & Gradle</p>
<p>This fork disables most GL ERROR checks so it doesn't infinitely spam console with Angelica or OptiFine. Some effects surprisingly work even with other shader mods</p>

Psychedelicraft is self-contained and does **not** require IvToolkit. Other mods in
your installation may still require IvToolkit independently.

## Internal replacements

The Psychedelicraft JAR contains the narrowly scoped implementations it uses for:

* SimpleImpl tile-entity and player-property synchronization (packet IDs `0` and
  `1`, with the legacy field order and payload encoding preserved). Incoming
  updates are validated and applied from the client tick thread.
* Multiblock, inventory, NBT, range, chat, Bézier, and ray-tracing helpers.
* Shader programs, framebuffer/depth resources, ping-pong textures, OpenGL state
  helpers, matrices, particles, and rendering primitives. Graphics resources are
  released before replacement, recreated after a resource reload, and resized
  when the display changes.
* The focused ASM transformer and development-name remapping support required by
  Psychedelicraft's render, camera, OpenGL, and sound hooks.

These implementations live under `ivorius.psychedelicraft.internal`; no
`ivorius.ivtoolkit` production classes are bundled. The adapted portions retain
their original Apache-2.0 notices. There is no intentional packet compatibility
change.

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

## Optional Not Enough Items integration

When Not Enough Items 2.7.4-GTNH is installed on the client, Psychedelicraft adds
Drying, Drink Preparation, and Acquisition pages. NEI is not required on clients
or dedicated servers. See [the coverage reference](docs/nei-coverage.md) for the
source-backed coverage matrix and current limitations.
