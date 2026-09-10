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
