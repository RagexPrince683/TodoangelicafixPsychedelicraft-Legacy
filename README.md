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
