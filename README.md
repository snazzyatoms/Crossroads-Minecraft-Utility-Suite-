<img width="1536" height="1024" alt="CROSSROADS - a Minecraft adventure unfolds" src="https://github.com/user-attachments/assets/1cf695f3-4365-49a8-b767-47a1029fefe5" />



<div align="center">

[![Minecraft](https://img.shields.io/badge/Minecraft-Plugin-00C853?style=for-the-badge&logo=minecraft&logoColor=white)](https://minecraft.net)
[![Legacy+Modern](https://img.shields.io/badge/Legacy%20%26%20Modern-1.16.5%2B-2196F3?style=for-the-badge&logo=paper&logoColor=white)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-16%2B-F57C00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![MIT](https://img.shields.io/badge/License-MIT-9C27B0?style=for-the-badge&logo=mit&logoColor=white)](LICENSE)
[![API](https://img.shields.io/badge/API-SPI%20Ready-4CAF50?style=for-the-badge&logo=codeigniter&logoColor=white)](/api)

<br>

> **The ultimate Minecraft utility suite — homes, warps, chat, moderation & backups in one trusted plugin.**

</div>

---

## 🌟 Why Crossroads?

**Crossroads replaces Essentials** with a modern, modular utility platform built for **both legacy and modern servers**.

Unlike bloated old plugins, Crossroads delivers:

- **🏠 Player-friendly** — intuitive homes, warps, spawn  
- **🛡️ Admin-trusted** — moderation, backups, safety first
- **⚙️ Server-owner focused** — per-world configs, feature toggles
- **🔌 Developer-ready** — public API + SPI integrations
- **💾 Data-safe** — automatic backups, portable storage

---

## 🚀 Core Features

| Feature | Players | Admins | Legacy | Modern |
|---------|---------|--------|--------|--------|
| 🏠 Homes & Warps | ✅ | ✅ | ✅ | ✅ |
| 🧭 Spawn & Teleport | ✅ | ✅ | ✅ | ✅ |
| 💬 Messaging & Reply | ✅ | ✅ | ✅ | ✅ |
| 🛡️ Moderation Tools | - | ✅ | ✅ | ✅ |
| 📦 Kits & Rewards | ✅ | ✅ | ✅ | ✅ |
| 💾 Storage Backups | - | ✅ | ✅ | ✅ |
| 🎛️ Full Config | - | ✅ | ✅ | ✅ |

---

## ⌨️ Essential Commands

| Category | Player Commands | Admin Commands |
|----------|----------------|---------------|
| **🏠 Homes** | `/home [name]`, `/sethome [name]`, `/delhome` | ✅ |
| **🧭 Warps** | `/warp [name]`, `/setwarp [name]`, `/delwarp` | ✅ |
| **🌍 Spawn** | `/spawn`, `/setspawn`, `/back` | ✅ |
| **💬 Chat** | `/msg <player>`, `/reply`, `/ignore <player>` | ✅ |
| **🛡️ Staff** | `/fly`, `/vanish`, `/staffmode` | ✅ |
| **🎁 Kits** | `/kit`, `/rules` | ✅ |
| **⚙️ Admin** | `/crossroads` | ✅ |

**Clean permissions:**
```

crossroads.home     crossroads.warp     crossroads.spawn
crossroads.back     crossroads.msg      crossroads.kit
crossroads.staff    crossroads.admin

```

---

## 🛠️ Supported Platforms

| Version | Java | Status |
|---------|------|--------|
| **1.16.5+** | 16+ | 🟢 Legacy Ready |
| **1.20+** | 21 | 🟢 Modern Ready |
| **Paper/Spigot** | ✅ | Primary Target |

---

## 💾 Smart Storage

| Type | Best For |
|------|----------|
| 📁 **YAML** | Small servers |
| 🗄️ **SQLite** | Medium servers |
| ☁️ **MySQL** | Large networks |

**Built-in backup system:**
- Auto-backups on restart
- Manual export/restore
- Data migration tools
- Integrity checks

---

## 🔌 Developer API

Crossroads exposes a **clean public API** for seamless integration:

```java
@EventHandler
public void onHomeTeleport(HomeTeleportEvent event) { 
    // Custom home logic
}

PlayerData data = CrossroadsAPI.getPlayerData(player);
```

**SPI for extensions** — add custom modules without forking!

---

## 🎯 What Makes Us Different

| Feature | EssentialsX | Crossroads |
| :-- | :-- | :-- |
| **Code Quality** | Legacy | **Modern** |
| **Version Support** | Modern only | **Legacy + Modern** |
| **Storage Safety** | Basic | **Full backups** |
| **Developer API** | Limited | **Full SPI** |
| **Config Power** | Flat | **Per-world/groups** |
| **Performance** | Average | **Optimized** |


---

## 📦 Quick Install

```bash
1. Download crossroads-[version].jar
2. Drop in /plugins/ folder
3. Restart server
4. Edit config.yml
5. Enjoy! 🎉
```


---

## 🗺️ Development Roadmap

```
🔴 Alpha     → Core commands (homes/warps/spawn)
🟡 Beta      → Storage, backups, API
🟢 Stable    → Legacy + Modern releases
🔵 v1.0      → Full feature suite + modules
```


---

<div align="center">

## 🤝 Join the Journey

[![Discord](https://img.shields.io/discord/123456789?logo=discord&logoColor=white&style=for-the-badge)](https://discord.gg/example)
[![Issues](https://img.shields.io/github/issues-raw/username/Crossroads?logo=github&style=for-the-badge)](https://github.com/username/Crossroads/issues)
[![Stars](https://img.shields.io/github/stars/username/Crossroads?style=social&logo=github)](https://github.com/username/Crossroads)

**Made with ❤️ for Minecraft server owners**

</div>

---

*© 2026 Crossroads. MIT License. Built for the community, by the community.*
