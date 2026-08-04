<img width="1536" height="1024" alt="CROSSROADS - a Minecraft adventure unfolds" src="https://github.com/user-attachments/assets/1cf695f3-4365-49a8-b767-47a1029fefe5" />

<div align="center">

[![Minecraft](https://img.shields.io/badge/Minecraft-Utility%20Suite-00C853?style=for-the-badge&logo=minecraft&logoColor=white)](https://minecraft.net)
[![Paper / Spigot](https://img.shields.io/badge/Paper%20%2F%20Spigot-1.16.5%2B-2196F3?style=for-the-badge&logo=paper&logoColor=white)](https://papermc.io)
[![Version](https://img.shields.io/badge/Version-1.0.0-F57C00?style=for-the-badge)](./pom.xml)
[![Storage](https://img.shields.io/badge/Storage-YAML%20%7C%20SQLite%20%7C%20MySQL-4CAF50?style=for-the-badge)](#storage)
[![License](https://img.shields.io/badge/License-MIT-263238?style=for-the-badge&logo=opensourceinitiative&logoColor=white)](./LICENSE)

# Crossroads Minecraft Utility Suite

Crossroads is a self-reliant server-core suite for Paper and Spigot. It covers daily utilities, native permissions, native economy, multi-language packs, staff tooling, moderation, storage, and soft integrations — so servers do not need LuckPerms, Essentials, or Vault to run a complete core stack.

Crossroads keeps its own identity. Soft partners (AegisGuard, Coffers, Vault, Essentials import, PlaceholderAPI) are optional compatibility layers, never hard requirements.

</div>

---

## What Crossroads is aiming to be

- one suite instead of a stack of loosely connected utility plugins
- self-reliant permissions and economy with optional bridges
- multi-language replies that can sync with AegisGuard language packs
- operationally clean startup, reload, storage, and backup behavior
- extension-first architecture through API and SPI artifacts

---

## What ships in 1.0.0

### Localization

- YAML language packs under `plugins/Crossroads/lang/<style>/`
- Bundled styles: `modern_english`, `french_fr`, `spanish_mx`, `spanish_ar`, `portuguese_br`, `italian_it`, `german_de`, `polish_pl`
- Per-player `/language`, server default, and AegisGuard sync (`localization.sync_aegisguard`)
- Command, staff, moderation, menu, economy, and permission replies are keyed translations

### Native permissions

- Players + groups, inheritance, prefixes/suffixes, temporary nodes, world/server contexts
- Bukkit `PermissionAttachment` injection so `hasPermission` works without LuckPerms
- `/crperms` (`/cperm`) for user/group management, checks, and LuckPerms group import
- Optional Vault Permissions bridge (`permissions.vault-bridge`, default off)

### Native economy

- Built-in balances with `/balance`, `/pay`, `/baltop`, and `/eco`
- Prefers Coffers when present; native Crossroads balances otherwise
- Vault economy bridge only when `economy.vault-bridge` is enabled
- Existing AegisGuard ClaimBlocks economy mode still supported

### Core player utility

- named homes with world-profile support
- warps, spawn management, `/back`, and teleport request flow
- random teleport with configurable limits and restrictions
- kits with permissions, cooldowns, costs, icons, and profile restrictions
- private messaging, reply, ignore, and offline mail
- nicknames plus text-page powered `/motd`, `/help`, `/info`, and `/rules`

### Staff and moderation

- `/fly`, `/vanish`, `/staffmode`, and `/socialspy`
- `/invsee`, `/endersee`, and `/seen`
- `/freeze`, `/mute`, `/warn`, `/kick`, `/tempban`, `/unban`
- jail tools, shadow mute, staff notes, and moderation history

### Soft compatibility

- PlaceholderAPI
- AegisGuard language sync, plots, and ClaimBlocks
- Coffers-preferred economy
- optional Vault bridges for foreign plugins
- protection compatibility for WorldGuard, GriefPrevention, Lands, Residence, Towny, and PlotSquared
- Essentials migration entry point

---

## Localization

Configure in `config.yml`:

```yaml
localization:
  folder: lang
  default_language: modern_english
  fallback_language: modern_english
  extract_defaults: true
  sync_aegisguard: true
  allow_player_language: true
```

Resolution order per player:

1. Crossroads personal language (`/language <style>`)
2. AegisGuard player/server style when sync is enabled
3. Crossroads default language
4. Fallback language

Add custom packs by copying `lang/modern_english/` to a new folder id and listing it under `localization.available_languages`.

---

## Permissions

```yaml
permissions:
  enabled: true
  default-group: default
  vault-bridge: false
  contexts:
    world: true
    server-name: global
```

Useful commands:

- `/crperms user <player> info|permission|parent|group ...`
- `/crperms group <name> create|delete|info|permission|parent|meta ...`
- `/crperms list`
- `/crperms check <player> <node>`
- `/crperms import luckperms`

---

## Economy

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

Resolution order for `mode: money`:

1. Coffers (when present and preferred)
2. Native Crossroads balances
3. Vault only if `vault-bridge: true`

---

## Architecture

This repository produces three artifact types:

- main plugin jar
- API jar under [`src/main/java/dev/crossroadsmc/crossroads/api`](./src/main/java/dev/crossroadsmc/crossroads/api)
- SPI jar under [`src/main/java/dev/crossroadsmc/crossroads/api/module`](./src/main/java/dev/crossroadsmc/crossroads/api/module)

`CrossroadsAPI` exposes language, permission, and economy services for modules.

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

Stored state includes player data, homes, warps, spawn profiles, mail, kit cooldowns, moderation history, jails, back locations, balances, language prefs, and permission memberships. Group definitions persist through the storage document API.

---

## Configuration files

Bundled defaults live in [`src/main/resources`](./src/main/resources):

- [`config.yml`](./src/main/resources/config.yml)
- [`kits.yml`](./src/main/resources/kits.yml)
- [`motd.yml`](./src/main/resources/motd.yml)
- [`help.yml`](./src/main/resources/help.yml)
- [`info.yml`](./src/main/resources/info.yml)
- [`lang/<style>/*.yml`](./src/main/resources/lang)

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
- `/language`
- `/balance`, `/pay`, `/baltop`, `/eco`
- `/kit`, `/motd`, `/help`, `/info`, `/rules`

### Staff, moderation, and permissions

- `/fly`, `/vanish`, `/staffmode`, `/socialspy`
- `/invsee`, `/endersee`, `/seen`
- moderation suite (`/freeze`, `/mute`, `/kick`, `/tempban`, jails, notes, history)
- `/crperms` (`/cperm`)

### Administration

- `/crossroads about|reload|modules|backup create|import essentials|language|perms`

---

## Build

Crossroads targets Java 17 and Spigot/Paper `1.16.5+`.

```text
mvn clean package
```

Build output is kept outside the repository:

- intermediate Maven build files: `../Crossroads Build/crossroads`
- release jars and checksums: `../Crossroads Release Jars/crossroads-1.0.0`

---

## Roadmap

- permission tracks / richer meta GUI
- deeper LuckPerms import (users + track maps)
- tighter Coffers dual-currency alignment
- more complete non-English pack polish beyond core strings
- expand the module ecosystem

---

## License

Crossroads is released under the [MIT License](./LICENSE).
