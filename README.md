<img width="1536" height="1024" alt="CROSSROADS - a Minecraft adventure unfolds" src="https://github.com/user-attachments/assets/1cf695f3-4365-49a8-b767-47a1029fefe5" />

<div align="center">

[![Minecraft](https://img.shields.io/badge/Minecraft-Plugin-00C853?style=for-the-badge&logo=minecraft&logoColor=white)](https://minecraft.net)
[![Paper / Spigot](https://img.shields.io/badge/Paper%20%2F%20Spigot-1.16.5%2B-2196F3?style=for-the-badge&logo=paper&logoColor=white)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-17%2B-F57C00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![Storage](https://img.shields.io/badge/Storage-YAML%20%7C%20SQLite%20%7C%20MySQL-4CAF50?style=for-the-badge)](#storage)
[![License](https://img.shields.io/badge/License-MIT-9C27B0?style=for-the-badge&logo=mit&logoColor=white)](LICENSE)

# Crossroads

**Crossroads is a full server-core plugin for Minecraft communities that want the convenience of an Essentials-style suite with a cleaner architecture, richer moderation, and modern storage options.**

Homes, warps, spawn, back, messaging, richer kits, moderation tools, welcome profiles, backups, staff inspection, moderation logs, and extension modules all ship in the same release-ready plugin.

</div>

---

## What Crossroads Delivers

### Everyday Utility

- Named homes with configurable limits
- Server warps and spawn management
- `/back` tracking for teleports and death recovery
- Private messaging with `/reply` and `/ignore`
- Configurable rules output for players

### Staff & Moderation

- `/fly`, `/vanish`, and `/staffmode`
- `/socialspy` for private-message oversight
- `/invsee` and `/endersee` for live inspection
- `/freeze`, `/mute`, `/warn`, `/seen`
- Persistent moderation logs with `/stafflog`

### Release-Ready Backend

- YAML, SQLite, or MySQL storage backends
- Automatic startup and shutdown backups
- Structured service-based codebase
- Public API events and module SPI hooks
- World-aware welcome messaging profiles
- Feature toggles for major systems in `config.yml`

---

## Feature Overview

| Area | Included |
| --- | --- |
| Player Utility | Homes, warps, spawn, back, messaging, kits, rules |
| Staff Tools | Vanish, fly, staff mode, social spy |
| Moderation | Freeze, mute, warn, seen, moderation history |
| Inspection | Inventory view, ender chest view |
| Storage | YAML, SQLite, MySQL |
| Safety | Automatic backup archives, persistent moderation logs |
| Extensibility | Public API, `HomeTeleportEvent`, module SPI |
| Welcome Experience | Global and per-world welcome messages |

---

## Commands

### Player Commands

| Command | Description |
| --- | --- |
| `/home [name]` | Teleport to a saved home |
| `/sethome [name]` | Save your current location as a home |
| `/delhome [name]` | Delete one of your homes |
| `/homes` | List your homes |
| `/warp <name>` | Teleport to a server warp |
| `/warps` | List public warps |
| `/spawn` | Teleport to spawn |
| `/back` | Return to your last tracked location |
| `/msg <player> <message>` | Send a private message |
| `/reply <message>` | Reply to the last DM |
| `/ignore <player>` | Toggle direct messages from a player |
| `/kit [name]` | List or claim available kits |
| `/rules` | Show server rules |

### Staff & Admin Commands

| Command | Description |
| --- | --- |
| `/setwarp <name>` | Create a warp |
| `/delwarp <name>` | Delete a warp |
| `/setspawn` | Set the server spawn |
| `/fly` | Toggle flight |
| `/vanish` | Toggle vanish |
| `/staffmode` | Enter a moderation-ready staff state |
| `/socialspy` | Mirror private messages to staff |
| `/invsee <player>` | Inspect a player's inventory |
| `/endersee <player>` | Inspect a player's ender chest |
| `/freeze <player> [reason]` | Freeze a player in place |
| `/unfreeze <player>` | Remove a freeze |
| `/mute <player> <minutes> [reason]` | Mute a player |
| `/unmute <player>` | Remove a mute |
| `/warn <player> <reason>` | Record a warning |
| `/stafflog <player> [limit]` | View moderation history |
| `/seen <player>` | Check last recorded activity |
| `/crossroads about` | Show plugin version, storage backend, and modules |
| `/crossroads reload` | Reload config, spawn, warps, and kits |
| `/crossroads modules` | List loaded Crossroads modules |
| `/crossroads backup create` | Create a manual backup archive |

---

## Storage

Crossroads supports three persistence modes:

| Backend | Best For |
| --- | --- |
| YAML | Small servers and quick installs |
| SQLite | Single-server production setups |
| MySQL | Shared infrastructure and larger networks |

Stored data includes:

- Player homes
- Ignore lists
- Kit cooldowns
- Spawn and warps
- Mute and freeze state
- Last seen tracking
- Moderation logs

Switch the backend in `config.yml`:

```yml
storage:
  type: YAML # YAML, SQLITE, MYSQL
```

---

## Rich Kits

Crossroads kits support more than plain material lists.

Each kit can include:

- Custom display names
- Lore
- Enchantments
- Custom model data
- Unbreakable items
- Item flags
- Console commands executed on claim
- Cooldowns and per-kit permissions

The default release ships with `starter`, `builder`, and `adventurer` kits in [`src/main/resources/kits.yml`](/Users/kai/Documents/GitHub/Crossroads/src/main/resources/kits.yml:1).

---

## Welcome Profiles

Crossroads includes configurable join messaging with support for global and per-world profiles.

Examples:

- First-join welcome lines
- Returning-player messages
- Nether-specific welcome text
- World-name placeholder replacement

This lets the server feel more intentional without needing a separate welcome plugin.

---

## Moderation Logging

Every key moderation action can be recorded into persistent storage:

- Mutes
- Unmutes
- Freezes
- Unfreezes
- Warnings

Staff can review a player's history directly in-game with `/stafflog <player>`.

---

## Module SPI

Crossroads includes a lightweight extension SPI for custom modules.

Drop module jars into:

```text
plugins/Crossroads/modules
```

Modules can implement the `CrossroadsModule` service interface and receive a `CrossroadsModuleContext` with access to:

- The plugin instance
- Player data service
- Warp and spawn services
- Kit service
- Moderation service
- Plugin logger

This makes it possible to extend Crossroads without forking the core plugin.

---

## API Example

```java
import dev.crossroadsmc.crossroads.api.CrossroadsAPI;
import dev.crossroadsmc.crossroads.api.event.HomeTeleportEvent;
import dev.crossroadsmc.crossroads.model.PlayerData;

@EventHandler
public void onHomeTeleport(HomeTeleportEvent event) {
    // Inspect, reroute, or cancel home teleports.
}

PlayerData data = CrossroadsAPI.getPlayerData(player);
```

---

## Installation

1. Place the Crossroads jar into your server's `plugins` folder.
2. Start the server once to generate the default config and kit files.
3. Choose your storage backend in `config.yml`.
4. Adjust feature toggles, kits, rules, welcome messages, and permissions to match your server.
5. Restart or run `/crossroads reload`.

---

## Feature Toggles

Server owners can disable major plugin systems individually in `config.yml`, including:

- Homes
- Warps
- Spawn
- Back
- Messaging
- Kits
- Rules
- Welcome messages
- Staff tools
- Inspection commands
- Moderation tools
- Backups
- Modules

---

## Default Permissions

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
crossroads.kit.adventurer
crossroads.rules
crossroads.staff
crossroads.staff.see
crossroads.inspect
crossroads.moderation
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
├── storage
└── util
```

Crossroads is structured as a service-driven plugin, which keeps the command layer thin and makes future maintenance much easier for real servers.
