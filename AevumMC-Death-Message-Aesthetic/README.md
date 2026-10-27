# 🏛️ AevumMC Death Message Aesthetic — Imperium Edition

A lightweight, no-dependency Paper plugin that gives vanilla player-death chat a restrained Roman-inspired identity, built around AevumMC's antique gold, ivory, stone-grey and crimson palette.

## Design

- **Clean, not noisy:** one compact line per death, without giant banners or repeated prefixes.
- **Cause-aware styles:** distinct lines for PvP melee, player projectiles, mob attacks, mob projectiles, falls, lava, fire, drowning, void, explosions, freezing, lightning, starvation, suffocation, contact damage, magic, wither, cramming, falling blocks, sonic booms, dryout, world border and command kills.
- **Styled death screen:** optionally carries the matching formatted message onto the screen shown to the player who died.
- **Randomised variants:** multiple lines for common causes help keep chat from feeling repetitive.
- **Safe names:** player and entity names are inserted as plain text, not interpreted as MiniMessage formatting.
- **Admin tools:** preview styles and reload configuration in-game.

## Requirements

- Paper 26.2; compiled against Paper API 26.2.build.121-stable.
- Java 25.
- No Vault, EssentialsX, PlaceholderAPI or other plugin dependencies.

## Build

Run `mvn --batch-mode clean package` from this folder.

Output: `target/AevumMC-Death-Message-Aesthetic.jar`.

The GitHub Actions workflow builds the project and uploads the resulting JAR as a workflow artifact on relevant pushes.

## Install

1. Back up your server and plugins folder.
2. Put the built JAR in the server's `plugins` folder.
3. Restart Paper fully (avoid hot-reloading plugins).
4. Edit `plugins/AevumMCDeathMessageAesthetic/config.yml`.
5. Run `/aevumdeath reload` after changing settings or templates.

## Commands

| Command | Purpose |
| --- | --- |
| `/aevumdeath` | Show help |
| `/aevumdeath preview` | Preview the default PvP death line |
| `/aevumdeath preview fall` | Preview a fall message |
| `/aevumdeath preview player-projectile-kill` | Preview a projectile kill |
| `/aevumdeath causes` | List configurable message keys |
| `/aevumdeath reload` | Reload config |

Aliases: `/deathstyle`, `/aevumdeathmessage`. Permission: `aevumdeath.admin` (operators by default).

## Template options

Templates use MiniMessage formatting and two safe placeholders:

- `<victim>` — player who died.
- `<killer>` — responsible player, mob, or named entity when available.

Each `messages.<key>` can be a list of variants. Set `settings.random-variants: false` to always use the first variant.

Settings:

- `settings.enabled` — enable or disable custom death messages.
- `settings.style-death-screen` — also style the dying player's death screen.
- `settings.random-variants` — randomise selection among message variants.

The plugin only changes death-message presentation. It does not modify damage, items, XP, keep-inventory, respawning, sounds or drops.