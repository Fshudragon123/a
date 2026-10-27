# AevumMC Death Message Aesthetic

A small, dependency-free Paper plugin that replaces default player death chat with elegant, Roman-inspired AevumMC messages.

## Features
- Antique-gold, ivory and muted-grey MiniMessage styling.
- Custom messages for PvP, projectiles, falls, lava, fire, drowning, void, explosions, freezing and lightning.
- Configurable templates in `plugins/AevumMCDeathMessageAesthetic/config.yml`.
- No extra plugin dependencies; uses Paper's bundled Adventure/MiniMessage API.
- Keeps vanilla death mechanics, drops, keep-inventory rules and respawning unchanged.

## Build
Requires Java 25 and Maven.

```bash
mvn clean package
```

The output JAR is `target/AevumMC-Death-Message-Aesthetic.jar`.

## Install
1. Build the JAR.
2. Put it in the server's `plugins` folder.
3. Restart the Paper server.
4. Edit the generated config to customise messages.

## Placeholders
- `<victim>` — the dead player's username.
- `<killer>` — the attacking player's username (PvP messages only).

Messages use MiniMessage tags, for example `<#C98B24>` for antique gold. This plugin only changes the chat death message; it does not add titles, boss bars, sounds or particles.
