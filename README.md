<img width="1536" height="1024" alt="CROSSROADS - a Minecraft adventure unfolds" src="https://github.com/user-attachments/assets/1cf695f3-4365-49a8-b767-47a1029fefe5" />

<div align="center">

[![Minecraft](https://img.shields.io/badge/Minecraft-Utility%20Suite-00C853?style=for-the-badge&logo=minecraft&logoColor=white)](https://minecraft.net)
[![Paper / Spigot](https://img.shields.io/badge/Paper%20%2F%20Spigot-1.16.5%2B-2196F3?style=for-the-badge&logo=paper&logoColor=white)](https://papermc.io)
[![Version](https://img.shields.io/badge/Version-1.0.1-F57C00?style=for-the-badge)](./pom.xml)
[![Languages](https://img.shields.io/badge/Languages-8%20Packs-7B1FA2?style=for-the-badge)](#localization)
[![Permissions](https://img.shields.io/badge/Permissions-Native-00897B?style=for-the-badge)](#native-permissions)
[![Economy](https://img.shields.io/badge/Economy-Native%20%2B%20Coffers-5D4037?style=for-the-badge)](#native-economy)
[![Storage](https://img.shields.io/badge/Storage-YAML%20%7C%20SQLite%20%7C%20MySQL-4CAF50?style=for-the-badge)](#storage)
[![License](https://img.shields.io/badge/License-MIT-263238?style=for-the-badge&logo=opensourceinitiative&logoColor=white)](./LICENSE)

# Crossroads Minecraft Utility Suite

**Version 1.0.1** — a self-reliant server-core suite for Paper and Spigot.

Homes, warps, teleports, kits, messaging, staff tools, moderation, **native permissions**, **native economy**, and **multi-language packs** in one plugin. Servers can run Crossroads as their core stack without LuckPerms, Essentials, or Vault.

</div>

---

## Why Crossroads 1.0

Most servers stitch together Essentials-style utilities, a permissions plugin, an economy plugin, and a pile of message configs. Crossroads is built to be the one suite you choose instead.

| Need | Crossroads approach |
| --- | --- |
| Daily utilities | Built-in homes, warps, spawn, TPA, RTP, kits, mail, nicknames, text pages |
| Permissions | Native groups, inheritance, temp nodes, contexts — no LuckPerms required |
| Economy | Native balances, Coffers preferred when present, Vault optional only |
| Languages | Full language packs; syncs with AegisGuard French/German/Polish/etc. |
| Identity | Own design — not a clone of LuckPerms or Essentials |

Soft partners stay optional: AegisGuard, Coffers, Vault bridges, Essentials import, PlaceholderAPI, and major protection plugins. None are hard requirements.

---

## What's new in 1.0.1

- Enable safely without Vault installed (Vault remains an optional soft bridge only)

## What's new in 1.0.0

- **Localization engine** — keyed YAML packs for commands, staff replies, moderation, menus, economy, and permissions
- **AegisGuard language sync** — if a player’s AegisGuard pack is French, German, Polish, and so on, Crossroads can follow automatically
- **Native permissions MVP** — `/crperms` with players, groups, inheritance, prefixes/suffixes, temporary permissions, and world/server contexts
- **Native economy** — `/balance`, `/pay`, `/baltop`, `/eco` with Coffers-first resolution
- **API surface** — `CrossroadsAPI` exposes language, permission, and economy services for modules
- **Universal posture** — works alone; plays nicely when AegisGuard, Coffers, Vault, or Essentials data are present

---

## Feature overview

### Localization

- Packs live in `plugins/Crossroads/lang/<style>/`
- Bundled styles: `modern_english`, `french_fr`, `spanish_mx`, `spanish_ar`, `portuguese_br`, `italian_it`, `german_de`, `polish_pl`
- Per-player `/language`, server default, and AegisGuard sync
- Missing keys fall back safely so packs never blank out gameplay

Configure in [`config.yml`](./src/main/resources/config.yml):

```yaml
localization:
  folder: lang
  default_language: modern_english
  fallback_language: modern_english
  extract_defaults: true
  sync_aegisguard: true
  allow_player_language: true
```

**Resolution order**

1. Crossroads personal language (`/language <style>`)
2. AegisGuard player/server style when sync is on
3. Crossroads `default_language`
4. `fallback_language`

Custom packs: copy `lang/modern_english/`, rename the folder, and add the id to `localization.available_languages`.

### Native permissions

- Players and named groups
- Inheritance, prefixes/suffixes, temporary nodes
- World and server contexts
- Bukkit `PermissionAttachment` injection so `hasPermission(...)` works for Crossroads and other plugins
- Optional Vault Permissions bridge for foreign plugins (`permissions.vault-bridge`, default `false`)
- LuckPerms group import helper for migration off LuckPerms

```yaml
permissions:
  enabled: true
  default-group: default
  vault-bridge: false
  contexts:
    world: true
    server-name: global
```

```text
/crperms user <player> info|permission|parent|group ...
/crperms group <name> create|delete|info|permission|parent|meta ...
/crperms list
/crperms check <player> <node>
/crperms import luckperms
```

### Native economy

- Built-in balances stored with Crossroads player data
- Prefers **Coffers** when installed
- Vault economy only if `economy.vault-bridge: true`
- AegisGuard ClaimBlocks mode still available via `economy.mode: aegis_claim_blocks`

```yaml
economy:
  mode: money
  prefer-coffers: true
  vault-bridge: false
  native:
    enabled: true
    currency-name-singular: coin
    currency-name-plural: coins
    starting-balance: 0
    decimals: 2
```

```text
/balance [player]
/pay <player> <amount>
/baltop [limit]
/eco give|take|set <player> <amount>
```

### Player utilities

- Named homes with world-profile support
- Warps, spawn, `/back`, TPA flow, RTP
- Kits with permissions, cooldowns, costs, icons, profile restrictions
- Private messaging, reply, ignore, offline mail
- Nicknames plus `/motd`, `/help`, `/info`, `/rules`
- Signs and GUI menus for warps/kits

### Staff and moderation

- `/fly`, `/vanish`, `/staffmode`, `/socialspy`
- `/invsee`, `/endersee`, `/seen`
- Freeze, mute, warn, kick, tempban, unban
- Jails, shadow mute, staff notes, moderation history

### Soft compatibility

| Integration | Role |
| --- | --- |
| AegisGuard | Language sync, plot/teleport awareness, ClaimBlocks economy mode |
| Coffers | Preferred economy provider when present |
| Vault | Optional permission/economy bridges for other plugins only |
| PlaceholderAPI | Crossroads placeholders |
| WorldGuard, GriefPrevention, Lands, Residence, Towny, PlotSquared | Protection-aware teleports / RTP |
| Essentials | Import path for homes/warps/nicks (`/crossroads import essentials`) |

---

## Quick start

1. Build or download `crossroads-1.0.1.jar`
2. Drop it into `plugins/`
3. Start the server once to generate config, language packs, and defaults
4. Set `localization.default_language` (and keep `sync_aegisguard: true` if you run AegisGuard)
5. Use `/crperms` for groups and `/eco` / `/balance` for money
6. `/crossroads reload` after config or language edits

No LuckPerms, Essentials, or Vault required for a working install.

---

## Commands

### Travel

- `/home`, `/sethome`, `/delhome`, `/homes`
- `/warp`, `/setwarp`, `/delwarp`, `/warps`
- `/spawn`, `/setspawn`, `/back`
- `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpacancel`
- `/rtp`

### Social, language, and economy

- `/msg`, `/reply`, `/ignore`, `/mail`, `/nick`
- `/language [style|list|sync]`
- `/balance` (`/bal`), `/pay`, `/baltop`, `/eco`
- `/kit`, `/motd`, `/help`, `/info`, `/rules`

### Staff, moderation, and permissions

- `/fly`, `/vanish`, `/staffmode`, `/socialspy`
- `/invsee`, `/endersee`, `/seen`
- `/freeze`, `/unfreeze`, `/mute`, `/unmute`
- `/kick`, `/tempban`, `/unban`
- `/setjail`, `/jail`, `/unjail`
- `/warn`, `/shadowmute`, `/staffnote`
- `/stafflog`, `/history`
- `/crperms` (`/cperm`)

### Administration

- `/crossroads about`
- `/crossroads reload`
- `/crossroads modules`
- `/crossroads backup create`
- `/crossroads import essentials`
- `/crossroads language ...`
- `/crossroads perms ...`

Command registration lives in [`plugin.yml`](./src/main/resources/plugin.yml) and is routed through [`CrossroadsCommandRouter.java`](./src/main/java/dev/crossroadsmc/crossroads/command/CrossroadsCommandRouter.java).

---

## Architecture

Crossroads produces three artifacts from one codebase:

| Artifact | Purpose |
| --- | --- |
| Main jar | Runnable Paper/Spigot plugin |
| API jar | Public integration types under [`api/`](./src/main/java/dev/crossroadsmc/crossroads/api) |
| SPI jar | Module contracts under [`api/module/`](./src/main/java/dev/crossroadsmc/crossroads/api/module) |

Important entry points:

- [`CrossroadsPlugin.java`](./src/main/java/dev/crossroadsmc/crossroads/CrossroadsPlugin.java)
- [`CrossroadsAPI.java`](./src/main/java/dev/crossroadsmc/crossroads/api/CrossroadsAPI.java) — language, permissions, economy, modules
- [`LanguageService.java`](./src/main/java/dev/crossroadsmc/crossroads/service/LanguageService.java)
- [`PermissionService.java`](./src/main/java/dev/crossroadsmc/crossroads/service/PermissionService.java)
- [`EconomyService.java`](./src/main/java/dev/crossroadsmc/crossroads/service/EconomyService.java)

External module jars load from:

```text
plugins/Crossroads/modules
```

---

## Storage

| Backend | Best fit |
| --- | --- |
| YAML | lightweight servers and quick installs |
| SQLite | single-node production servers |
| MySQL | shared infrastructure and larger networks |

Persisted state includes player data, homes, warps, spawn profiles, mail, kit cooldowns, moderation history, jails, back locations, balances, language preferences, permission memberships, and group definitions.

Implementations live under [`src/main/java/dev/crossroadsmc/crossroads/storage`](./src/main/java/dev/crossroadsmc/crossroads/storage).

---

## Configuration files

Bundled defaults in [`src/main/resources`](./src/main/resources) are provisioned into `plugins/Crossroads/` on first startup:

- [`config.yml`](./src/main/resources/config.yml) — features, localization, permissions, economy, storage, moderation
- [`kits.yml`](./src/main/resources/kits.yml)
- [`motd.yml`](./src/main/resources/motd.yml), [`help.yml`](./src/main/resources/help.yml), [`info.yml`](./src/main/resources/info.yml)
- [`lang/<style>/*.yml`](./src/main/resources/lang) — translation packs

Server-authored content such as welcome lines, rules, and MOTD/help/info page bodies stays editable in those YAML files so each community can keep its own voice.

---

## Build

Crossroads targets **Java 17** and Spigot/Paper **1.16.5+**.

```text
mvn clean package
```

Build output is kept outside the repository root:

- intermediate Maven files: `../Crossroads Build/crossroads`
- release jars and checksums: `../Crossroads Release Jars/crossroads-1.0.1`

---

## Roadmap

- Permission tracks and richer staff meta GUI
- Deeper LuckPerms import (users + tracks)
- Tighter Coffers dual-currency alignment
- Fuller polish across non-English language packs
- Broader module ecosystem

---

## License

Crossroads is released under the [MIT License](./LICENSE).
