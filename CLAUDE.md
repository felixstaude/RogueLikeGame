# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Flux Core** - A 2D roguelike game built with Java 21 and Swing/Java2D. The player IS an unstable flux core defending against corrupted energy entities.

### Core Theme
- **Player**: Unstable flux core with dynamic instability (0-100%)
- **Enemies**: Corrupted energy cores (purple corruption)
- **Visuals**: Energy-based with glitch effects, pulsing grids, RGB shifts
- **Mechanics**: Risk/reward instability system with 5 tiers

The game features wave-based combat, weapon combining mechanics, a shop system with stat modifications, progressive difficulty scaling, and the unique Flux Core Instability system.

## Build and Development Commands

### Build and Run
```bash
# Compile the project
mvn compile

# Run the game
mvn exec:exec

# Clean build artifacts
mvn clean
```

### Main Entry Point
- Main class: `de.felixstaude.roguelike.app.App`
- The game launches in fullscreen mode by default (toggle with F11)

## Architecture Overview

### Core Game Loop (`core/`)
- **Engine**: Top-level coordinator managing game states (RUNNING, SHOP, GAME_OVER). Updates at 60 UPS.
- **GameLoop**: Fixed timestep loop handling update/render cycle
- **GameCanvas**: Custom Canvas for BufferStrategy rendering
- **EngineArena**: Viewport/camera system managing world-to-screen transformations
- **GameState**: Enum for RUNNING, SHOP, GAME_OVER

### Stat System (`stats/`)
The game uses a **compositional stat system** with multiple layers:

1. **Base stats** (`EffectiveStats.Base`): Raw player values (maxHp, fireRate, bulletDamage, etc.)
2. **Modifier stats** (`Stats`): Flat/percentage modifiers from items and weapons (Map<Stat, Integer>)
3. **Effective stats** (`EffectiveStats`): Final computed values combining base + modifiers via `StatRules`
4. **StatComposer**: Aggregates base + passive items + weapon hotbar → EffectiveStats

Key insight: Items and weapons provide `Mod` objects (stat + amount), which are aggregated into `Stats`, then mapped to `EffectiveStats` by applying multiplicative/additive rules defined in `StatRules`.

#### Flux Core Instability System
**NEW**: The core mechanic of the game (`stats/InstabilityEffects.java`)

5 Instability Tiers (0-100%):
- **Tier 1 (0-25%)**: Stable - no effects
- **Tier 2 (26-50%)**: Flux Leak - 10% chance random bullet
- **Tier 3 (51-75%)**: Core Fracture - 20% bullet split, -15% max HP
- **Tier 4 (76-99%)**: Critical Mass - +50% damage, 2 HP/s drain
- **Tier 5 (100%)**: Meltdown - 100 dmg/s aura, 5 HP/s drain

New Stats:
- `FLUX_INSTABILITY_PCT`: Additive instability (0-100%, clamped)
- `FLUX_STABILITY_FLAT`: Reduces instability (negative values)
- `CORE_OVERCHARGE_PCT`: Damage multiplier that scales with instability

Calculation: `effectiveInstability = base + instabilityPct - stabilityFlat` (clamped 0-100)

### Weapons System (`weapons/`)
**Flux Core Themed**: All weapons are energy-based

Weapon Types (6 total):
- `PULSE_CORE` (was PISTOL): Balanced precision
- `FLUX_STREAM` (was SMG): High fire rate beam
- `WAVE_BURST` (was SHOTGUN): Multishot with knockback
- `PHASE_LANCE` (was CROSSBOW): High pierce + damage
- `SINGULARITY_CANNON` (was RAILGUN): Long range + pull
- `ENTROPY_FIELD` (was ARC_WAND): Homing + magic

- **WeaponDef**: Blueprint for a weapon type with tier-based mods
- **WeaponInstance**: Instantiated weapon at a specific tier (COMMON → UNCOMMON → RARE → EPIC)
- **WeaponHotbar**: 4-slot inventory with **auto-combine** logic
  - Adding a weapon matching existing type+tier combines them into next tier
  - Supports chain-combining (e.g., adding COMMON may trigger multiple combines if duplicates exist)
  - Returns `Result` record indicating success/failure and whether it was added or combined

**All higher-tier weapons add 2-14% FLUX_INSTABILITY_PCT** - power comes at a cost!

### Shop System (`shop/Shop.java`)
Complex single-file implementation (~1400 lines) with:
- **Offer generation**: Randomized passive items and weapons based on luck/harvesting stats
- **Lock persistence**: Offers can be locked to carry over to next shop rotation (one wave only)
- **Stat preview**: Live delta calculation when hovering/focusing offers (shows how stats change if purchased)
- **Reroll mechanics**: Increasing cost per reroll, locked offers persist through rerolls
- **Layout engine**: Responsive UI with single-row cards, stats panel, hotbar
- **Purchase flow**: Handles passive item uniqueness, weapon combining, insufficient gold/space
- **PanelStat enum**: Defines all displayable stats with custom formatting (int, percent, float, combined)

The shop computes two `StatsSnapshot` objects:
- **Base**: Current player stats (passives + hotbar)
- **Preview**: Hypothetical stats if hovered/focused offer were purchased
- Delta rendering shows stat changes in green (+) or red (-)

### Wave System (`world/`)
- **WaveManager**: Tracks wave number, timer, difficulty scaling
- **EnemySpawner**: Spawns enemies based on wave difficulty
- **WaveDifficulty**: Difficulty curve scaling enemy count/stats
- **Arena**: World bounds definition

### Combat System (`combat/`)
- **DamageSystem**: Handles bullet-enemy and enemy-player collision/damage
- Supports lifesteal, crit chance, pierce, homing projectiles

### Entities (`entity/`)
**Flux Core Visuals**: All entities have energy-themed rendering

- **Player**: Glowing flux core with:
  - Dynamic color based on instability (Cyan→Magenta→Orange→Red)
  - 3 rotating energy rings (different speeds)
  - Pulsating glow (speed increases with instability)
  - Glitch effects at >75% instability (RGB shift, position jitter)
  - Field: `instability` (0-100%) set by Engine from EffectiveStats

- **Enemy**: Corrupted purple cores with:
  - RGB chromatic aberration (30% chance)
  - Position glitch jitter (±1.5px)
  - 6 pulsating energy cracks
  - Flickering outline
  - HP bar when damaged

- **Bullet**: Energy orbs with:
  - Color matches player instability
  - 5-point motion trail with fade
  - Glow halo effect
  - Inner bright spot
  - Homing indicator ring

- **Particle**: Visual effects (muzzle flashes, explosions)

### Items (`items/`)
**Flux Core Themed**: 16 items across 5 rarity tiers

Rarity Tiers:
- **LEGENDARY** (60-80g, NEW): Flux Singularity, Perfect Stabilizer
- **EPIC** (35-45g): Overcharge Core, Fractal Splitter, Resonance Amplifier
- **RARE** (20-28g): Entropy Siphon, Phase Shifter, Flux Capacitor
- **UNCOMMON** (12-16g): Cooling Matrix, Energy Cell, Flux Conduit
- **COMMON** (8-10g): Stable Core Fragment, Energy Lens, etc.

- **Item**: Passive item with mods and rarity
- **Mod**: Stat + amount pair
- **ItemRarity**: COMMON, UNCOMMON, RARE, EPIC, LEGENDARY (affects UI color/pricing)
- **PassiveItemCatalog**: Hardcoded list of passive items

**All high-power items add FLUX_INSTABILITY_PCT** - risk/reward balance is core to gameplay

### UI (`ui/`)
- **HUD**: Renders top banner, health/xp bars, debug overlay, game over screen, crosshair

### Utilities (`util/`)
- **Draw**: Rendering helpers (panels, buttons, badges, icons, shadows)
- **Colors**: Flux Core color palette with helpers
  - Core colors: `FLUX_STABLE`, `FLUX_UNSTABLE`, `FLUX_CRITICAL`, `CORRUPTION`
  - Methods: `fluxCoreColor(instability)`, `bulletColor()`, `blend()`, `withAlpha()`
- **Fonts**: Font management (regular, bold, italic)
- **ImageCache**: Image loading/caching
- **Layout**: Layout calculation helpers
- **Mathx**: Math utilities
- **Time**: Time formatting

### Arena (`core/EngineArena.java`)
**Flux Core Environment**: Animated energy grid background
- Pulsing grid lines (100px spacing, sine-based brightness)
- 15 orbiting energy sparks (circular pattern)
- Animated pulsing border
- Dark BACKDROP for contrast
- `update(dt)` must be called each frame for animations

## Important Design Patterns

### Stat Modification Flow
```
Player base stats → Stats (passive items) + WeaponHotbar.toStats() → StatComposer.compute() → EffectiveStats
```

When making changes to stats:
1. Modify `Stats` object (add/remove mods)
2. Call `StatComposer.compute()` to get new `EffectiveStats`
3. `EffectiveStats` values are read-only snapshots used for gameplay

### Shop Preview Calculation
The shop creates hypothetical `StatsSnapshot` objects by:
1. Cloning current `passiveStats` and `hotbar`
2. Simulating the purchase (apply item mods or simulate weapon combine)
3. Computing new `EffectiveStats` with `StatComposer`
4. Comparing base vs preview to show deltas

**Critical**: The shop's `simulateAddOrCombine()` replicates `WeaponHotbar.tryAddOrCombine()` logic exactly. If hotbar logic changes, update both.

### Game State Transitions
```
RUNNING → (wave timer ends) → SHOP → (start next wave) → RUNNING
RUNNING → (player dies) → GAME_OVER → (restart) → RUNNING
```

Shop state:
- Clears bullets/enemies/particles on entry
- Persists locked offers to `carryLockedNextShop` list
- Reloads locked offers on next shop phase via `prepareForWave()`

## Common Development Tasks

### Adding a New Stat
1. Add enum value to `stats/Stat.java`
2. Define mapping in `StatRules.java` (additive vs multiplicative)
3. Add field to `EffectiveStats` if needed
4. Add `PanelStat` entry in `Shop.java` for UI display
5. Add `pretty()` and `shortName()` mappings in `Shop.java`
6. Update `isPercent()` in `Shop.java` if it's a percentage stat

### Adding a New Weapon
1. Create `WeaponDef` in `WeaponCatalog.all()`
2. Define tier-based mods via lambda
3. Specify `WeaponType` and `rarityHint`
4. Add weapon icon to `src/main/resources/icons/weapons/<type>.png`

### Adding a New Passive Item
1. Add `Item` to `PassiveItemCatalog.all()`
2. Provide mods, rarity, price, unique flag
3. Add icon to `src/main/resources/icons/items/<id>.png`
4. For shop-affecting items (like `lucky_charm`), add handling in `Shop.buy()` switch

### Debugging
- **F3**: Toggle debug overlay (FPS, UPS, entity counts, player stats)
- **K**: Damage player (for testing)
- **L**: Heal player (for testing)
- **F11**: Toggle fullscreen

## Code Organization Notes

- **Immutability**: `Mod`, `Item`, `WeaponDef` are effectively immutable; `Stats` is mutable
- **Records**: Used for `Mod`, `OfferSlot`, `StatLine`, `ModLine`, `WeaponHotbar.Result`, `Shop.PurchaseResult`
- **German UI**: Some UI strings are in German (e.g., "Gekauft", "Nicht genug Gold")
- **Single-file complexity**: `Shop.java` is intentionally monolithic (~1400 lines) with nested enums/records for cohesion
- **Resource loading**: Images loaded from classpath via `ImageCache.get(path)` with fallback to placeholder

## Testing Strategy

No formal test suite exists. Manual testing workflow:
1. Run game via `mvn exec:exec`
2. Test combat mechanics with K/L debug keys
3. Complete waves to test shop flow
4. Verify stat calculations via F3 debug overlay
5. Test weapon combining by buying duplicate weapons
6. Test lock persistence across shop phases
