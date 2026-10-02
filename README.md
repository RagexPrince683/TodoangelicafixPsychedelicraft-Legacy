# Psychedelicraft

Psychedelicraft adds farming, brewing, drugs, and hallucinations to **Minecraft 1.7.10**. Grow ingredients, dry your harvest, prepare drinks, and see the world change through distorted visuals, altered sound, and drug effects.

This is a continuation of the original mod, with rebuilt screen effects for Angelica compatibility and an updated development setup. It is self-contained: **Psychedelicraft no longer requires IvToolkit**.

## What you can do

- Grow cannabis, tobacco, coca, hops, and coffee; harvest grapes, juniper berries, and peyote.
- Turn harvested ingredients into usable items with wooden or iron Drying Tables.
- Make joints, cigarettes, drinks, and other consumables, or use a pipe or bong.
- Brew, ferment, distill, and mature alcoholic drinks using Wooden Vats, Barrels, and Distilleries.
- Experience hallucinations, color changes, blur, double vision, and other effects. Deserts also shimmer with heat, and water leaves droplets on your view.
- Build a growing and brewing area with Grow Lights, drinkware, and Bottle Racks.

Reality rifts, Rift Jars, and Harmonium are optional content and are **disabled by default**.

## Install and play

1. Use a **Minecraft 1.7.10** instance with **Forge 10.13.4.1614**.
2. Put the compiled Psychedelicraft mod JAR in that instance's `mods` folder. If you have the source code rather than a JAR, follow [the development steps below](#get-started-with-development).
3. For multiplayer, install the same version on the server and on each player's client.
4. Launch Minecraft and check that Psychedelicraft appears in the Mods list.

**Optional:** Not Enough Items (NEI) adds recipe lookups and pages explaining where ingredients come from. The integration targets **NEI 2.7.4-GTNH**. NEI and Angelica are not required to play; other mods in your pack may still need IvToolkit for their own purposes.

Screen effects have been visually checked with **Angelica 2.1.29 without a shaderpack**. That does not guarantee every shaderpack or combination of rendering mods. The full screen effects require OpenGL 3.0 support.

## Your first harvest

A Drying Table is a good first project:

1. Find ingredients while exploring, farming, or trading with villagers. With NEI installed, hover over an item and press **R** for recipes or **U** for uses, unless you have changed those keys. The **Acquisition** pages explain how to obtain supported ingredients.
2. Craft a **Drying Table** with wooden planks and redstone. Its two-row recipe is three planks across the top, then a plank, redstone, and a plank underneath.
3. Fill all **nine input slots**, one item per slot, with the same supported ingredient. For example, nine cannabis buds produce three dried cannabis buds. Ordinary red or brown mushrooms can also be dried into magic mushrooms.
4. Give the table plenty of light and keep it sheltered from rain. Warm, bright conditions improve drying; exposed rain can reset progress.
5. Place a **Grow Light** within one block of the table, including diagonally, for a **30% increase in processing speed**. It also lights up the room, although ordinary Minecraft lighting remains white.
6. Use the dried ingredients in crafting. Dried cannabis buds and paper make a joint; a pipe can consume dried cannabis buds or dried tobacco directly from your inventory when you hold right-click.

For another starting project, harvest Coffea Cherries, smelt them into Coffee Beans, and look up drink preparation. Brewing alcohol is a longer project involving fermentation, distillation, and maturation.

NEI covers crafting, drying, drink preparation, and ingredient acquisition. It does **not** yet provide a complete step-by-step brewing guide. See [the NEI coverage reference](docs/nei-coverage.md) for details.

## Adjust the experience

After the first launch, use **Mods → Psychedelicraft → Config**, or edit `config/psychedelicraft.cfg` while the game is closed. Server owners should change gameplay settings in the server's configuration.

These are the most useful settings to start with:

| Category | Setting | What it changes |
| --- | --- | --- |
| `visual` | `shader2DEnabled` | Toggles screen effects, including drug, heat, and water effects. It does not disable the underlying drug gameplay. |
| `visual` | `motionBlur` | Toggles motion trails. |
| `visual` | `biomeHeatDistortion` | Toggles desert heat shimmer. |
| `visual` | `waterDistortion` | Toggles underwater distortion. |
| `visual` | `waterOverlayEnabled` | Toggles droplets on your view. |
| `visual` | `sunFlareIntensity` | Sets sun glare strength; use `0` to turn glare off. |
| `balancing` | `enableRealityRifts` | Enables reality rifts. Turning it off also removes existing rifts when they next update. |
| `balancing` | `randomTicksUntilRiftSpawn` | Controls random rift frequency when rifts are enabled. Use `0` or `-1` to stop random spawns. |
| `balancing` | `enableRiftJars` / `enableHarmonium` | Enables the corresponding optional survival content. |

The old `shaderEnabled` setting does not control the rebuilt screen effects; use `shader2DEnabled` instead. Depth-of-field effects are currently inactive.

If you report a visual problem, include your mod version, rendering mods, shaderpack if any, and what you were doing when it happened. `visual.debugPostProcessing` can add graphics diagnostics to the log; leave it off during normal play.

## Get started with development

You need **Git** and a **Java 17 or 21 JDK**. A JDK includes the tools needed to build the mod. The project includes a Gradle wrapper, so you do not need to install Gradle separately. The built mod still targets Java 8.

### Get the source and launch a test game

On Windows, open PowerShell and run:

```powershell
git clone https://github.com/RagexPrince683/TodoangelicafixPsychedelicraft-Legacy.git
cd TodoangelicafixPsychedelicraft-Legacy
.\gradlew.bat setupDecompWorkspace
.\gradlew.bat runClient
```

On Linux or macOS, use `./gradlew` instead of `.\gradlew.bat`. The first setup downloads the build tools and Minecraft development files, so it needs an internet connection and may take a while.

You can edit with IntelliJ IDEA or another Java editor. Open the repository folder as a Gradle project and choose your JDK for Gradle. `runClient` launches a separate development game where you can try your changes.

### Build a mod JAR

```powershell
.\gradlew.bat build
```

The output is in `build/libs/`. Use the regular mod JAR for playing, rather than a sources or development JAR. To launch a dedicated development server, use `.\gradlew.bat runServer`.

### Try changes with other mods

Put extra development mod JARs in:

- `devmods/` for mods that can run on both the client and a dedicated server.
- `devmods/client/` for client-only mods.

These JARs stay local and are not bundled into your build. Put each mod in only one location to avoid duplicate loading.

To test with the project's pinned Angelica setup, run:

```powershell
.\gradlew.bat runClient -PangelicaDev
```

This adds Angelica **2.1.29** and its supporting libraries to the development client. Keep duplicate copies out of your development mod folders. It does not add Angelica to the server or make it a requirement for players.

### Where to make your first change

- **Names, descriptions, and textures:** `src/main/resources/assets/psychedelicraft/`.
- **Items and blocks:** `src/main/java/ivorius/psychedelicraft/items/` and `blocks/`.
- **Recipes:** `src/main/java/ivorius/psychedelicraft/crafting/`.
- **Gameplay and drug effects:** `src/main/java/ivorius/psychedelicraft/entities/drugs/`.
- **Active screen effects:** `src/main/java/ivorius/psychedelicraft/client/rendering/post/`.

For a first contribution, choose one small change, build it, and try it in the development game. Include what you changed and what you actually tested in your contribution, and add meaningful changes to [CHANGELOG.md](CHANGELOG.md). Files under `REFERENCEFOLDER/` are reference material, not the mod's active source.

## Credits

Original development by **Ivorius (Lukas Tenbrink)**, with visuals by **TripleHeadedSheep**. This continuation is maintained by **RagexPrince683** and builds on the Psychedelicraft Legacy project.
