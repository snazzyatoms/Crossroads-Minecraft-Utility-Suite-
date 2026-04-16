<img width="1536" height="1024" alt="CROSSROADS - a Minecraft adventure unfolds" src="https://github.com/user-attachments/assets/1cf695f3-4365-49a8-b767-47a1029fefe5" />

<div align="center">

[![Minecraft](https://img.shields.io/badge/Minecraft-Plugin-00C853?style=for-the-badge&logo=minecraft&logoColor=white)](https://minecraft.net)
[![Paper / Spigot](https://img.shields.io/badge/Paper%20%2F%20Spigot-1.16.5%2B-2196F3?style=for-the-badge&logo=paper&logoColor=white)](https://papermc.io)
[![Version](https://img.shields.io/badge/Version-0.1.0-F57C00?style=for-the-badge)](/Users/kai/Documents/GitHub/Crossroads/pom.xml)
[![Storage](https://img.shields.io/badge/Storage-YAML%20%7C%20SQLite%20%7C%20MySQL-4CAF50?style=for-the-badge)](#storage)
[![License](https://img.shields.io/badge/License-MIT-9C27B0?style=for-the-badge&logo=mit&logoColor=white)](LICENSE)

# Crossroads

**Crossroads is a modern Essentials-style server-core plugin for Paper and Spigot servers, built to cover everyday utility, teleport flow, moderation, persistence, and staff operations in one polished package.**

Homes, warps, spawn profiles, teleport requests, mail, nicknames, kits, moderation tools, staff inspection, text pages, PlaceholderAPI support, Coffers-first economy integration, AegisGuard compatibility, and migration tooling all ship together in `0.1.0`.

</div>

---

## Why Crossroads

- Per-world profiles for homes, warps, spawn points, and kit availability
- Persistent player data across YAML, SQLite, and MySQL
- Teleport requests, `/back`, `/rtp`, and configurable command cooldowns
- Offline mail, nickname support, MOTD, help, and info text files
- GUI warp and kit menus plus clickable Crossroads signs
- Staff tools, moderation logs, temp bans, jails, shadow mute, warning categories, and staff notes
- PlaceholderAPI support for scoreboards, chat, menus, signs, and TAB setups
- AegisGuard `1.2.7` compatibility with plot-aware teleports, RTP avoidance, placeholders, and optional ClaimBlocks-backed costs
- Coffers-first economy support with Vault-compatible fallback providers
- Essentials import for homes, warps, spawn, and nicknames

---

## Feature Set

### Player Utility

- Named homes with scope support like `global:home` or per-world profile homes
- Profile-aware warps and spawn points
- `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpacancel`
- `/back` tracking with persistent storage
- `/rtp` with radius, attempts, and world restrictions
- `/msg`, `/reply`, `/ignore`, and offline `/mail`
- `/nick`, `/motd`, `/help`, `/info`, `/rules`
- Rich kits with cooldowns, permissions, costs, and world-profile restrictions

### Staff & Moderation

- `/fly`, `/vanish`, `/staffmode`, `/socialspy`
- `/invsee` and `/endersee`
- `/freeze`, `/mute`, `/warn`, `/seen`
- `/kick`, `/tempban`, `/unban`
- `/setjail`, `/jail`, `/unjail`
- `/shadowmute` and `/staffnote`
- Persistent moderation history with `/stafflog` and `/history`

### Server Integration

- GUI-driven warp and kit browsing
- Crossroads-powered interaction signs for warps, spawn, RTP, MOTD, help, and info
- PlaceholderAPI placeholders for nicknames, unread mail, storage/economy status, homes, jail state, mute state, and balances
- Extension SPI and module folder support
- Essentials import through `/crossroads import essentials`

---

## Storage

Crossroads supports three persistence modes:

| Backend | Best fit |
| --- | --- |
| YAML | lightweight installs and small servers |
| SQLite | single-server production setups |
| MySQL | shared infrastructure and larger networks |

Stored data includes:

- homes by profile
- command cooldowns
- kit cooldowns
- unread and read mail
- nicknames
- mute, freeze, ban, jail, and shadow mute state
- staff notes and moderation logs
- warps, spawn profiles, and jails
- back locations

Autosave is built in, and Crossroads flushes persistent state during normal shutdown as well.

---

## Economy Support

Crossroads is ready for economy-backed costs and prefers **Coffers** first when its direct service is available.

If Coffers is not present, Crossroads falls back to any compatible **Vault** economy provider. That means you can use costs for teleports, RTP, and kits without locking the server into one economy stack.

If AegisGuard is installed, Crossroads can also switch to `aegis_claim_blocks` mode so Crossroads costs are paid from AegisGuard ClaimBlocks instead of money.

Economy-aware features currently include:

- teleport costs
- RTP costs
- kit costs
- PlaceholderAPI balance output

## AegisGuard Compatibility

Crossroads now detects AegisGuard `1.2.7` at runtime without requiring a hard compile dependency.

When AegisGuard is present, Crossroads can:

- respect protected-plot entry rules for Crossroads teleports
- keep `/rtp` out of protected plots
- expose AegisGuard plot and ClaimBlocks placeholders
- use AegisGuard ClaimBlocks as an optional Crossroads cost source

Relevant config keys:

- `economy.mode: money` or `economy.mode: aegis_claim_blocks`
- `aegisguard.teleport-respect-entry`
- `aegisguard.rtp-avoid-protected-plots`

---

## Customization

Crossroads is built to be themed and trimmed to fit a server instead of forcing every feature on every install.

Server owners can customize:

- semantic message colors and prefix formatting
- feature toggles for major systems
- per-world/profile behavior
- command cooldowns
- teleport request timeouts
- RTP settings
- jail radius
- MOTD, help, and info pages from dedicated text files
- kit permissions, costs, cooldowns, and profile restrictions

Main configurable resources:

- [config.yml](/Users/kai/Documents/GitHub/Crossroads/src/main/resources/config.yml)
- [kits.yml](/Users/kai/Documents/GitHub/Crossroads/src/main/resources/kits.yml)
- [motd.yml](/Users/kai/Documents/GitHub/Crossroads/src/main/resources/motd.yml)
- [help.yml](/Users/kai/Documents/GitHub/Crossroads/src/main/resources/help.yml)
- [info.yml](/Users/kai/Documents/GitHub/Crossroads/src/main/resources/info.yml)

---

## Commands

### Core Travel

- `/home`, `/sethome`, `/delhome`, `/homes`
- `/warp`, `/setwarp`, `/delwarp`, `/warps`
- `/spawn`, `/setspawn`, `/back`
- `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpacancel`
- `/rtp`

### Social & Utility

- `/msg`, `/reply`, `/ignore`, `/mail`
- `/nick`
- `/kit`
- `/motd`, `/help`, `/info`, `/rules`

### Staff & Moderation

- `/fly`, `/vanish`, `/staffmode`, `/socialspy`
- `/invsee`, `/endersee`, `/seen`
- `/freeze`, `/unfreeze`, `/mute`, `/unmute`
- `/kick`, `/tempban`, `/unban`
- `/setjail`, `/jail`, `/unjail`
- `/warn`, `/shadowmute`, `/staffnote`
- `/stafflog`, `/history`

### Administration

- `/crossroads about`
- `/crossroads reload`
- `/crossroads modules`
- `/crossroads backup create`
- `/crossroads import essentials`

---

## Import & Migration

Crossroads includes an Essentials import flow for admins moving to a cleaner server-core stack.

Current import support covers:

- player homes
- server warps
- spawn
- nicknames

Use:

```text
/crossroads import essentials
```

---

## API & Modules

Crossroads exposes:

- [CrossroadsAPI.java](/Users/kai/Documents/GitHub/Crossroads/src/main/java/dev/crossroadsmc/crossroads/api/CrossroadsAPI.java)
- [HomeTeleportEvent.java](/Users/kai/Documents/GitHub/Crossroads/src/main/java/dev/crossroadsmc/crossroads/api/event/HomeTeleportEvent.java)
- [CrossroadsModule.java](/Users/kai/Documents/GitHub/Crossroads/src/main/java/dev/crossroadsmc/crossroads/api/module/CrossroadsModule.java)
- [CrossroadsModuleContext.java](/Users/kai/Documents/GitHub/Crossroads/src/main/java/dev/crossroadsmc/crossroads/api/module/CrossroadsModuleContext.java)

External module jars can be placed in:

```text
plugins/Crossroads/modules
```

---

## Compatibility Notes

- Tested compile target: Java 17
- Server API target: Spigot/Paper `1.16.5+`
- Optional integrations: AegisGuard, Coffers, Vault, PlaceholderAPI

The current packaged release jar is [crossroads-0.1.0.jar](/Users/kai/Documents/GitHub/Crossroads/target/crossroads-0.1.0.jar).
