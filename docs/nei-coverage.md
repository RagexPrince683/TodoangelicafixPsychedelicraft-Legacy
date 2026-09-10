# NEI coverage

The integration is optional and client-only. NEI discovers
`NEIPsychedelicraftConfig`; common and server initialization never reference an
NEI class. Its API is `compileOnly`, while the existing development runtime is
non-publishable. No NEI code or dependency is bundled into the published mod.

Handlers build bounded indexes on first use after registration. Caches own copied
item/fluid data and retain no world, player, GUI, container, or tile entity.
Restart the client after another mod changes a supported registry at runtime.

## Coverage

| Group | Display | Handler | Implementation source |
| --- | --- | --- | --- |
| Cannabis seeds, leaves, buds | Generation precedence, seed loop, growth and exact mature yields, possible dealer source | Acquisition | `BlockCannabisPlant`, `PSWorldGen`, `VillagerTradeHandlerDrugDealer` |
| Dried cannabis leaves/buds | Nine inputs to three output; both tables, base times and environment | Drying | `DryingRegistry`, `TileEntityDryingTable`, `PSConfig` |
| Tobacco | Seed loop, growth/yield, generation, Farmer; drying and chest source | Acquisition, Drying | `BlockTobaccoPlant`, `PSWorldGen`, `VillagerTradeHandlerFarmer` |
| Coca | Seed loop, growth/yield, generation, dealer; drying | Acquisition, Drying | `BlockCocaPlant`, `PSWorldGen`, `VillagerTradeHandlerDrugDealer` |
| Hops | Seed loop, cone yield, generation and Farmer | Acquisition | `BlockHopPlant`, `PSWorldGen`, `VillagerTradeHandlerFarmer` |
| Coffee | Cherry seed loop, ripe yields, Farmer; beans use furnace display | Acquisition, furnace | `BlockCoffea`, `PSCrafting` |
| Grapes | Mature break/shears yields; lattice uses crafting display | Acquisition, crafting | `BlockWineGrapeLattice` |
| Juniper | Ordered generation rules, berry leaves/harvest, sapling loop | Acquisition | `BlockPsycheLeaves`, `PSWorldGen` |
| Peyote | Configured biome generation, block drop and possible dealer | Acquisition | `BlockPeyote`, `PSWorldGen` |
| Magic mushrooms | Vanilla starting mushrooms and nine-to-three drying | Acquisition, Drying | `DryingRegistry` |
| Prepared non-alcoholic drinks/injectables | Exact registered ingredients, amount/NBT, each compatible holder; ignores placeholder recipe output | Drink Preparation | `RecipeFillDrink`, `PSCrafting` |
| Smoking items, cocaine, muffin, machines, drinkware | Ordinary recipes | NEI crafting | `PSCrafting` |
| Mash-tub wort variants | Filled Mash Tub crafting is distinct from processing after placement | NEI crafting | `PSCrafting`, `ItemMashTub` |
| Alcohol states | Filled item subtypes retain fermentation/distillation/maturation/vinegar NBT | item display | `FluidAlcohol` |
| Pour/container conversion | Crafting is retained; unsafe automatic transfer is omitted | crafting where matched | `RecipePourDrink`, `RecipeConvertFluidContainer` |
| Slurry | Actual distillation remainder; dedicated transition page remains unavailable | item/fluid display | `FluidAlcohol.distillStep` |

Drying pages read the live registry, including additions from other mods using an
`Item`, `Block`, `ItemStack`, or Ore Dictionary key. Ore equivalents cycle as one
ingredient across nine slots instead of generating every possible combination.

## Disabled or unavailable

* Harmonium has no survival source while `enableHarmonium` is false; its enabled
  recipes use the normal crafting handler.
* Rift jars have no survival source while `enableRiftJars` is false. Reality rifts
  remain disabled by default.
* The internal `glitched` effect block remains hidden.
* Optional Ore Dictionary mash ingredients (corn, rice, honey, pineapple and
  banana) depend on the supplying mod for acquisition.

## Limitations

Added categories are reachable with NEI's configured recipe/usage controls
(default `R`/`U`), and both drying tables are catalysts. Dedicated transition
pages for open fermentation/acetification, closed maturation, distillation
requirements/byproducts, and pouring are not yet implemented. These unbounded
NBT state machines require finite examples and adjacent-state query derivation to
avoid misleading recipes. Runtime behavior is unverified: this change was
source-reviewed only, with no binary compilation or game launch.
