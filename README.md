# KillSound 🎵

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-blue.svg)](https://fabricmc.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Author](https://img.shields.io/badge/Author-ignwisep-purple.svg)](https://github.com/ignwisep)

**KillSound** is a lightweight, client-side Fabric mod for Minecraft 1.21 that brings custom audio feedback to your PvP gameplay. Play custom `.ogg` sound effects whenever you eliminate an enemy player or when you die.

---

## ✨ Features

- **100% Client-Side**: Safe to use on any multiplayer server (Vanilla, Fabric, Paper, Spigot, Purpur, Forge/NeoForge) without requiring server installation or OP permissions.
- **Dynamic Runtime Loading**: Drop `.ogg` audio files directly into `.minecraft/killsound/sounds/`—no game restarts or resource pack editing required.
- **Independent Audio Controls**: Customize sound selection, volume, and pitch separately for both **Enemy Kills** and **Own Deaths**.
- **In-Game Configuration GUI**: Seamless integration with [Mod Menu](https://modrinth.com/mod/modmenu) and [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl), complete with instant audio preview test buttons and a one-click button to open your sounds folder.
- **Zero-Crash Audio Engine**: Built on low-level OpenAL and STBVorbis with automatic resource management, mono/stereo decoding, and 2D non-spatial HUD audio playback.

---

## 📥 Installation

1. Install **[Fabric Loader](https://fabricmc.net/)** (version `0.16.0` or newer) for Minecraft `1.21`.
2. Download and place the following mods into your `.minecraft/mods` folder:
   - **[Fabric API](https://modrinth.com/mod/fabric-api)**
   - **[Mod Menu](https://modrinth.com/mod/modmenu)**
   - **[YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl)** (v3)
   - **KillSound** (`killsound-1.0.0.jar`)
3. Launch Minecraft!

---

## 🎮 How to Use

1. In Minecraft, open **Mods** ➔ **KillSound** ➔ **Configure**.
2. Click **Open Sounds Folder** (or manually navigate to `.minecraft/killsound/sounds/`).
3. Place any `.ogg` audio files into the folder.
4. Click **Refresh Sound List** in the configuration screen.
5. Choose your desired sounds from the dropdown menus for **Enemy Kill Sound** and **Own Death Sound**.
6. Adjust volume and pitch sliders, and click **Test** to preview in real-time.

---

## 🛠️ Building from Source

To compile the mod JAR from source code, clone the repository and run Gradle:

```bash
# Clone the repository
git clone https://github.com/ignwisep/killsound.git

# Navigate to directory
cd killsound

# Build the project
./gradlew build
```

The compiled mod JAR will be output to `build/libs/killsound-1.0.0.jar`.

---

## 👥 Credits

- Developed and maintained by **[ignwisep](https://github.com/ignwisep)**.

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.
