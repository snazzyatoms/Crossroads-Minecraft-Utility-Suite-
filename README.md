<img width="1536" height="1024" alt="CROSSROADS - a Minecraft adventure unfolds" src="https://github.com/user-attachments/assets/1cf695f3-4365-49a8-b767-47a1029fefe5" />

<div align="center">

[![Minecraft](https://img.shields.io/badge/Minecraft-Utility%20Suite-00C853?style=for-the-badge&logo=minecraft&logoColor=white)](https://minecraft.net)
[![Paper / Spigot](https://img.shields.io/badge/Paper%20%2F%20Spigot-1.16.5%2B-2196F3?style=for-the-badge&logo=paper&logoColor=white)](https://papermc.io)
[![Version](https://img.shields.io/badge/Version-0.2.0-F57C00?style=for-the-badge)](./pom.xml)
[![Storage](https://img.shields.io/badge/Storage-YAML%20%7C%20SQLite%20%7C%20MySQL-4CAF50?style=for-the-badge)](#storage)
[![License](https://img.shields.io/badge/License-MIT-263238?style=for-the-badge&logo=opensourceinitiative&logoColor=white)](./LICENSE)

# Crossroads Minecraft Utility Suite

Crossroads is a server-core utility suite for Paper and Spigot servers. It is designed to cover the daily server layer that most communities end up piecing together from multiple plugins: homes, warps, spawn, kits, staff tooling, moderation, messaging, text pages, storage, integrations, and extension hooks.

The goal is not to clone Essentials. The goal is to be cleaner to operate, easier to extend, safer to integrate, and more opinionated about modern server workflows.

</div>

---

## What Crossroads is aiming to be

Crossroads is built around four design goals:

- one suite instead of a stack of loosely connected utility plugins
- operationally clean startup, reload, storage, and backup behavior
- extension-first architecture through API and SPI artifacts
- practical integration with economy, protection, placeholder, and migration ecosystems

That means the project should feel strong in three places at the same time:

- player quality-of-life features
- staff and moderation workflows
- server-owner configuration and extensibility

---

## What ships in this repository today

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

### Integration surface

- PlaceholderAPI support
- Coffers-first economy resolution with Vault fallback
- AegisGuard-aware extras
- protection compatibility for WorldGuard, GriefPrevention, Lands, Residence, Towny, and PlotSquared
- Essentials migration entry point

---

## Why this is distinct from Essentials-style suites

Crossroads is intended to differentiate on architecture and operability, not just command count.

- profile-aware data instead of purely flat global behavior
- modular extension points instead of keeping everything closed in one monolith
- better startup diagnostics and less noisy default behavior
- cleaner persistence choices for small servers and networked deployments
- stronger integration posture for modern protection and placeholder stacks

---

## Architecture

This repository currently produces three artifact types from one codebase:

- main plugin jar: the runnable Crossroads plugin
- API jar: public-facing integration classes under [`src/main/java/dev/crossroadsmc/crossroads/api`](./src/main/java/dev/crossroadsmc/crossroads/api)
- SPI jar: module-facing contracts under [`src/main/java/dev/crossroadsmc/crossroads/api/module`](./src/main/java/dev/crossroadsmc/crossroads/api/module)

Important entry points:

- [`CrossroadsPlugin.java`](./src/main/java/dev/crossroadsmc/crossroads/CrossroadsPlugin.java)
- [`CrossroadsAPI.java`](./src/main/java/dev/crossroadsmc/crossroads/api/CrossroadsAPI.java)
- [`CrossroadsModule.java`](./src/main/java/dev/crossroadsmc/crossroads/api/module/CrossroadsModule.java)
- [`CrossroadsModuleContext.java`](./src/main/java/dev/crossroadsmc/crossroads/api/module/CrossroadsModuleContext.java)

External module jars are loaded from:

```text
plugins/Crossroads/modules
```

---

## Storage

Crossroads supports three persistence modes:

| Backend | Best fit |
| --- | --- |
| YAML | lightweight servers and quick installs |
| SQLite | single-node production servers |
| MySQL | shared infrastructure and larger networks |

Stored state includes player data, homes, warps, spawn profiles, mail, kit cooldowns, moderation history, jails, and back locations.

Primary persistence implementations live under [`src/main/java/dev/crossroadsmc/crossroads/storage`](./src/main/java/dev/crossroadsmc/crossroads/storage).

---

## Configuration files

Bundled defaults live in [`src/main/resources`](./src/main/resources) and are provisioned into `plugins/Crossroads/` on first startup.

- [`config.yml`](./src/main/resources/config.yml)
- [`kits.yml`](./src/main/resources/kits.yml)
- [`motd.yml`](./src/main/resources/motd.yml)
- [`help.yml`](./src/main/resources/help.yml)
- [`info.yml`](./src/main/resources/info.yml)

Crossroads now treats existing bundled YAML files as normal server state. If `kits.yml` already exists, startup leaves it alone silently instead of logging repeated warnings.

---

## Commands

### Travel

- `/home`, `/sethome`, `/delhome`, `/homes`
- `/warp`, `/setwarp`, `/delwarp`, `/warps`
- `/spawn`, `/setspawn`, `/back`
- `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpacancel`
- `/rtp`

### Social and utility

- `/msg`, `/reply`, `/ignore`, `/mail`
- `/nick`
- `/kit`
- `/motd`, `/help`, `/info`, `/rules`

### Staff and moderation

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

Command registration is defined in [`plugin.yml`](./src/main/resources/plugin.yml) and routed through [`CrossroadsCommandRouter.java`](./src/main/java/dev/crossroadsmc/crossroads/command/CrossroadsCommandRouter.java).

---

## Build

Crossroads targets Java 17 and Spigot/Paper `1.16.5+`.

Typical package flow:

```text
mvn clean package
```

The Maven build is configured in [`pom.xml`](./pom.xml).

Build output is intentionally kept outside the repository so the project root stays clean for GitHub:

- intermediate Maven build files: `D:\Crossroads Build\crossroads`
- release jars and checksums: `D:\Crossroads Release Jars\crossroads-0.2.0`

---

## Roadmap to rival the bigger suites

The current codebase already covers a meaningful utility surface, but the next quality jump should come from depth, not feature spam.

Recommended priorities:

- complete command parity for the features already present in storage and service layers
- add stronger permission granularity and audit visibility for staff actions
- formalize import and migration tooling beyond Essentials basics
- split compatibility concerns cleanly if modern and legacy server lines need separate build targets
- expand the module ecosystem so server owners can extend Crossroads without forking core
- harden startup, reload, and config migration paths so production servers stay quiet and predictable

---

## License

Crossroads is released under the [MIT License](./LICENSE).
