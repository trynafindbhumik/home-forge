<div align="center">

# 🏠 HomeForge

**A modern, feature-rich sethome plugin for Paper 1.21.11+ and Folia**

[![Build](https://github.com/trynafindbhumik/HomeForge/actions/workflows/build.yml/badge.svg)](https://github.com/trynafindbhumik/HomeForge/actions/workflows/build.yml)
[![Paper](https://img.shields.io/badge/Paper-1.21.11+-blue)](https://papermc.io)
[![Folia](https://img.shields.io/badge/Folia-Supported-brightgreen)](https://papermc.io/software/folia)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-MIT-green)](#license)

*Set homes. Teleport instantly. Manage everything from a clean GUI.*
*Now fully compatible with Folia's regionized multithreading.*

</div>

---

## ✨ Features

- **Chest GUI** — browse all homes in one place, click any home to manage it
- **Per-home settings** — teleport, update location, set primary, change icon, delete
- **Icon picker** — choose from 80+ materials as a home's visual symbol
- **Primary home** — mark one home as default for `/home` with no arguments
- **Stackable permissions** — add `homeforge.limit.N` permissions together, or use highest
- **Admin extra homes** — grant bonus slots per player on top of their permission limit
- **Teleport delay** — optional countdown with movement cancellation
- **Cooldown** — configurable time between teleports
- **Sound & particles** — visual feedback on arrival
- **BungeeCord support** — share homes across servers using MySQL
- **EssentialsX importer** — migrate all existing Essentials homes with one command
- **SQLite (default)** — zero-config, single-file, WAL mode
- **MySQL (optional)** — HikariCP connection pool for multi-server networks
- **Tab completion** — all commands suggest home names and player names
- **100% async** — all database I/O off the main thread, zero TPS impact
- **Fully configurable messages** — every chat message is customizable
- **✅ Folia compatible** — all schedulers migrated to `EntityScheduler`, `RegionScheduler`, `AsyncScheduler`, and `GlobalRegionScheduler`

---

## 🧩 Compatibility

| Requirement | Version |
|---|---|
| Server | Paper 1.21.11+ **or** Folia (any recent build) |
| Java | 21 or higher |
| Dependencies | None — SQLite & HikariCP downloaded automatically |

> Works with Java and Bedrock (Geyser) players. The GUI uses single-click so Bedrock players on mobile/console have full access to all features.

> **Folia note:** HomeForge is explicitly marked `folia-supported: true` and has been fully rewritten to use region-aware schedulers. It is safe to drop into any Folia server without modification.

---

## 📥 Installation

1. 👉 [Download Latest](https://github.com/trynafindbhumik/HomeForge/releases/latest)
2. Drop it into your server's `plugins/` folder
3. Start the server — Paper/Folia downloads SQLite and HikariCP automatically
4. Edit `plugins/HomeForge/config.yml` to your liking
5. Run `/hfreload` to apply changes without restarting

---

## 🗄️ Database

### SQLite (Default — single server)
Works out of the box, no configuration needed. Data is stored in `plugins/HomeForge/homes.db`.

### MySQL (Multi-server / BungeeCord)
```yaml
database:
  type: MYSQL
  mysql:
    host: localhost
    port: 3306
    database: homeforge
    username: root
    password: 'yourpassword'
```

Also set `settings.server_name` to each server's name as configured in BungeeCord's `config.yml`.

---

## 📋 Commands

| Command | Description | Permission |
|---|---|---|
| `/sethome [name]` | Set or update a home at your location | `homeforge.use` |
| `/home [name]` | Teleport to a home (primary if no name given) | `homeforge.use` |
| `/removehome <name>` | Delete a home (alias: `/delhome`) | `homeforge.use` |
| `/homes` | Open the homes GUI | `homeforge.use` |
| `/homes <player>` | View another player's homes | `homeforge.admin.viewother` |
| `/homes add <player> <n>` | Grant extra home slots | `homeforge.admin.extrahomes` |
| `/homes remove <player> <n>` | Remove extra home slots | `homeforge.admin.extrahomes` |
| `/homes set <player> <n>` | Set extra home slots to exactly N | `homeforge.admin.extrahomes` |
| `/homes info <player>` | Show home count, limit, and bonus slots | `homeforge.admin.extrahomes` |
| `/hfreload` | Reload config without restart (alias: `/homeforgereload`) | `homeforge.admin.reload` |
| `/importhomes essentials` | Import all homes from EssentialsX | `homeforge.admin.import` |

---

## 🔑 Permissions

```
homeforge.use                     # Basic home commands (default: true)

homeforge.limit.1                 # Allow 1 home
homeforge.limit.2                 # Allow 2 homes
homeforge.limit.3                 # Allow 3 homes
homeforge.limit.5                 # Allow 5 homes
homeforge.limit.10                # Allow 10 homes
homeforge.limit.20                # Allow 20 homes
homeforge.limit.50                # Allow 50 homes
homeforge.limit.*                 # Allow max homes defined in config (default 54)

homeforge.cooldown.bypass         # Skip teleport cooldown

homeforge.admin                   # All admin permissions (default: op)
  ├─ homeforge.admin.viewother    # View other players' homes
  ├─ homeforge.admin.editother    # Edit other players' homes
  ├─ homeforge.admin.extrahomes   # Manage bonus home slots
  ├─ homeforge.admin.reload       # Use /hfreload
  └─ homeforge.admin.import       # Use /importhomes
```

### Home limits explained

```yaml
# In config.yml:
settings:
  count_home_permission_limit_together: false
```

- `false` (default) — highest matching `homeforge.limit.N` permission is used
  - Player has `limit.3` and `limit.5` → allowed **5** homes
- `true` — all matching permissions are added together
  - Player has `limit.3` and `limit.5` → allowed **8** homes

Admin-granted bonus slots (`/homes add`) stack on top of either calculation.

### LuckPerms example

```bash
# Give VIP rank 5 homes
lp group vip permission set homeforge.limit.5 true

# Give a specific player 3 bonus slots on top of their rank
/homes add Steve 3
```

---

## ⚙️ Configuration

```yaml
settings:
  use_permissions: true           # false = unlimited homes for everyone
  default_home_limit: 3           # limit for players without homeforge.limit.N
  max_home_limit: 54              # limit for homeforge.limit.*
  count_home_permission_limit_together: false

  command_cooldown_home: 0        # seconds between /home uses (0 = off)
  teleport_delay: 0               # countdown before teleport (0 = instant)
  teleport_delay_check_movement: true

  sound_on_teleport: true
  particle_on_teleport: true

  server_name: ''                 # BungeeCord server name (blank = single server)
```

All chat messages are configurable under `messages:` in `config.yml`. Supports `&` color codes and `%placeholder%` variables.

---

## 🔀 Folia Threading Model

HomeForge uses the correct scheduler for every operation:

| Scheduler | Used for |
|---|---|
| `AsyncScheduler` | All database I/O (SQLite / MySQL) |
| `GlobalRegionScheduler` | Chat messages, future completion callbacks |
| `EntityScheduler` | Inventory opens, teleport delay timers, post-teleport effects, join delay |
| `RegionScheduler` | Location-based block operations |

The teleport delay countdown runs on the `EntityScheduler` so it correctly follows the player if they cross a region boundary during the countdown. Post-teleport sound and particle effects are dispatched back onto the player's entity region after `teleportAsync` completes. All `CompletableFuture` callbacks that open inventories are re-dispatched onto the player's entity region before calling `openInventory`.

> `folia-supported: true` is declared in `plugin.yml`. The plugin also works identically on regular Paper — the new scheduler APIs are available in Paper 1.19.4+ and behave as single-threaded equivalents.

---

## 🗃️ Database Schema

```sql
CREATE TABLE Homes (
    id         BIGINT      NOT NULL,
    owner      VARCHAR(40) NOT NULL,   -- Player UUID
    serverName TEXT,                   -- BungeeCord server (null = local)
    world      TEXT        NOT NULL,
    loc_x      DECIMAL(65,5) NOT NULL,
    loc_y      DECIMAL(65,5) NOT NULL,
    loc_z      DECIMAL(65,5) NOT NULL,
    loc_yaw    DECIMAL(65,5) NOT NULL,
    loc_pitch  DECIMAL(65,5) NOT NULL,
    name       TEXT        NOT NULL,
    symbol     VARCHAR(128) NOT NULL,  -- Material enum name
    last_used  BIGINT      NOT NULL,   -- Epoch milliseconds
    PRIMARY KEY (id)
);

CREATE TABLE Players (
    uuid         VARCHAR(40) NOT NULL,
    primary_home BIGINT      NULL,     -- FK → Homes.id
    extra_homes  BIGINT      NULL,     -- Admin-granted bonus slots
    PRIMARY KEY (uuid)
);
```

---

## 🔨 Building from Source

### Requirements
- Java 21+
- Maven 3.8+

```bash
git clone https://github.com/trynafindbhumik/HomeForge.git
cd HomeForge
mvn clean package
# Output: target/HomeForge-1.1.0.jar
```

---

## 📁 Project Structure

```
HomeForge/
├── pom.xml
└── src/main/
    ├── resources/
    │   ├── plugin.yml
    │   └── config.yml
    └── java/io/github/homeforge/
        ├── HomeForge.java              ← Main plugin class
        ├── commands/
        │   ├── SetHomeCommand.java
        │   ├── HomeCommand.java
        │   ├── RemoveHomeCommand.java
        │   ├── HomesCommand.java
        │   ├── ReloadCommand.java
        │   └── ImportHomesCommand.java
        ├── config/
        │   └── ConfigManager.java
        ├── database/
        │   ├── Database.java           ← Interface
        │   ├── SQLiteDatabase.java
        │   └── MySQLDatabase.java
        ├── gui/
        │   ├── HomesGUI.java           ← Home list chest GUI
        │   ├── HomeSettingsGUI.java    ← Per-home management
        │   └── SymbolPickerGUI.java    ← Material icon picker
        ├── listeners/
        │   ├── GUIListener.java
        │   ├── PlayerListener.java
        │   └── BungeeCordListener.java
        ├── managers/
        │   ├── HomeManager.java        ← Async DB + cache layer
        │   └── TeleportManager.java    ← Delay, cooldown, cross-server
        ├── models/
        │   ├── Home.java
        │   └── PlayerData.java
        └── utils/
            ├── MessageUtil.java
            └── SchedulerUtil.java      ← Folia/Paper scheduler abstraction (NEW)
```

---

## 📄 License

```
MIT License

Copyright (c) 2026 Bhumik Jain

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<div align="center">

Made with ☕ by [Bhumik Jain](https://github.com/trynafindbhumik)

</div>