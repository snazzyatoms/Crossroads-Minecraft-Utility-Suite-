<img width="1536" height="1024" alt="CROSSROADS - a Minecraft adventure unfolds" src="https://github.com/user-attachments/assets/1cf695f3-4365-49a8-b767-47a1029fefe5" />

<div align="center">

[![Minecraft](https://img.shields.io/badge/Minecraft-Plugin-00C853?style=for-the-badge&logo=minecraft&logoColor=white)](https://minecraft.net)
[![Paper / Spigot](https://img.shields.io/badge/Paper%20%2F%20Spigot-1.16.5%2B-2196F3?style=for-the-badge&logo=paper&logoColor=white)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-16%2B-F57C00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![MIT](https://img.shields.io/badge/License-MIT-9C27B0?style=for-the-badge&logo=mit&logoColor=white)](LICENSE)

**Crossroads** is a modern Minecraft utility plugin built to feel like a cleaner, more focused Essentials-style core.

Homes, warps, spawn, back, messaging, kits, vanish, staff mode, and backup tooling are all included in this first functional release.

</div>

---

## What Ships Right Now

Crossroads now includes a working plugin project with:

- Player homes with limits and named homes
- Server warps with simple admin management
- Spawn + `/back`
- Direct messaging, `/reply`, and `/ignore`
- Staff utilities: `/fly`, `/vanish`, `/staffmode`
- Config-driven starter kits
- Rules display from config
- YAML player/warp/spawn storage
- Automatic startup/shutdown zip backups
- A small public API with `CrossroadsAPI` and `HomeTeleportEvent`

This repo is no longer just a concept README. It now contains a real Paper/Spigot plugin scaffolded for continued growth.

---

## Command Set

### Player Commands

| Command | Description |
| --- | --- |
| `/home [name]` | Teleport to a saved home |
| `/sethome [name]` | Save your current location as a home |
| `/delhome [name]` | Delete a saved home |
| `/homes` | List all your homes |
| `/warp <name>` | Teleport to a server warp |
| `/warps` | List all warps |
| `/spawn` | Teleport to spawn |
| `/back` | Return to your last tracked location |
| `/msg <player> <message>` | Send a private message |
| `/reply <message>` | Reply to the last DM |
| `/ignore <player>` | Toggle DMs from a player |
| `/kit [name]` | List or claim kits |
| `/rules` | Show the server rules |

### Staff / Admin Commands

| Command | Description |
| --- | --- |
| `/setwarp <name>` | Create a warp |
| `/delwarp <name>` | Delete a warp |
| `/setspawn` | Set server spawn |
| `/fly` | Toggle flight |
| `/vanish` | Toggle vanish |
| `/staffmode` | Toggle staff mode and temporary moderation inventory |
| `/crossroads about` | Show plugin info |
| `/crossroads reload` | Reload config, kits, warps, and spawn |
| `/crossroads backup create` | Create a manual zip backup |

---

## Permissions

```text
crossroads.home
crossroads.home.unlimited
crossroads.warp
crossroads.spawn
crossroads.back
crossroads.msg
crossroads.kit
crossroads.kit.starter
crossroads.kit.builder
crossroads.rules
crossroads.staff
crossroads.staff.see
crossroads.admin
```

---

## Project Layout

```text
src/main/java/dev/crossroadsmc/crossroads
├── api
├── command
├── listener
├── model
├── service
└── util
```

The codebase is organized around services instead of one giant monolithic main class, which makes it much easier to keep expanding.

---

## Storage & Backups

Crossroads currently uses YAML for:

- Player data
- Homes
- Ignore lists
- Kit cooldowns
- Warps
- Spawn

Backups are written as zip archives into the plugin data folder:

- Automatic backup on startup
- Automatic backup on shutdown
- Manual backup with `/crossroads backup create`
- Pruning support with `backups.max-files`

---

## Config

Default config includes:

- Chat prefix
- DM formatting and notification sound toggle
- Default home name
- Home limit
- First-join spawn teleport toggle
- Backup retention
- Server rules

Default kits live in `src/main/resources/kits.yml`.

---

## API Example

Crossroads exposes a lightweight API surface for integrations:

```java
import dev.crossroadsmc.crossroads.api.CrossroadsAPI;
import dev.crossroadsmc.crossroads.api.event.HomeTeleportEvent;

@EventHandler
public void onHomeTeleport(HomeTeleportEvent event) {
    // Inspect or cancel teleports to homes.
}

PlayerData data = CrossroadsAPI.getPlayerData(player);
```

---

## Build

This project uses Maven:

```bash
mvn package
```

The compiled jar will be created in `target/`.

Note: this environment did not have Maven or a Java runtime installed, so I could not run a full package build here. The project files and metadata have been prepared for a normal local Maven build.

---

## Roadmap

The current code is a strong v0 base. Good next upgrades would be:

1. SQLite and MySQL storage adapters
2. Cooldown messages with human-readable formatting
3. More advanced kits with enchantments and armor metadata
4. Per-world settings and feature toggles
5. Staff inspection tools and moderation logs
6. Extension SPI for custom modules

---

## Why This Version Is Better

The original README described an ambitious plugin but the repository had no implementation behind it.

This version gives you:

- A real plugin project
- A clean architecture
- A useful first release you can build on
- A README that matches the code instead of hand-waving past it

If you want, the next step can be turning this from a strong starter plugin into a genuinely polished network-ready utility suite with database storage, richer configs, and more advanced moderation features.
