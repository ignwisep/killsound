# Code Style Requirement

The entire project must contain no comments of any kind. Do not add comments to Java, JSON, Gradle, Markdown code blocks, configuration files, or any other project file. Do not add inline comments, block comments, documentation comments, TODO comments, explanatory comments, or commented-out code. Keep the source code clean and self-explanatory through clear naming and structure.

# KillSound — Build Specification

A client-side Fabric mod for Minecraft 1.21.11 that lets players configure separate custom OGG sound effects for:
- killing another player ("Enemy Kill Sound")
- dying themselves ("Own Death Sound")

The mod must not require installation on the server and must not require OP. It should work entirely from the player's client, using client-observable Minecraft events/statistics.

---
# 1. Project Goals
## Required behavior

1. When the local player kills another player:
   - Play the configured Enemy Kill Sound.
   - Do not play it when another player gets a kill.
   - Do not play it for killing mobs.
2. When the local player dies:
   - Play the configured Own Death Sound.
3. Each event has its own:
   - Enabled/disabled toggle.
   - Selected OGG file.
   - Volume.
   - Pitch.
4. Users can add their own `.ogg` files without rebuilding the mod.
5. Settings persist across Minecraft restarts.
6. A Mod Menu configuration screen provides all settings.
7. The configuration UI should provide a sound test button for each sound.
8. Invalid/missing files must fail gracefully rather than crashing Minecraft.
## Non-goals

- No server-side component.
- No OP requirement.
- No modification of server files.
- No automatic uploading/downloading of sounds.
- No dependency on a particular server's plugin.
- No hardcoded requirement that sounds be named `victory.ogg` or `death.ogg`.

---

# 2. Target Environment

| Component | Target |
|---|---|
| Minecraft | 1.21.11 |
| Mod loader | Fabric |
| Environment | Client only |
| Language | Java |
| Java | JDK 21 |
| Build system | Gradle + Fabric Loom |
| Config UI | Mod Menu + YetAnotherConfigLib (YACL) |
| Audio | OGG Vorbis |
| Mod ID | `killsound` |
| Suggested package | `com.example.killsound` |

Use the exact Fabric Loader, Fabric API, Loom, Mod Menu, and YACL versions appropriate for the generated 1.21.11 template. Do not blindly copy versions from an older Minecraft project.

---

# 3. Recommended Dependencies
# Required
## Fabric Loader
Provides the mod-loading environment.
## Fabric API
Use Fabric API for client lifecycle/events and compatibility with the target Minecraft version.
# Configuration
## Mod Menu
Adds the mod's configuration entry to the Mod Menu screen.
## YetAnotherConfigLib (YACL)
Use YACL for:
- Boolean controls
- Sliders
- Dropdowns
- Buttons
- Config persistence/UI generation

YACL should be used instead of manually implementing a large Minecraft `Screen`.

---

# 4. Directory Layout

Recommended source layout:

```text
KillSound/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── gradlew
├── gradlew.bat
├── gradle/
│   └── wrapper/
├── LICENSE
├── README.md
├── BUILD.md
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── killsound/
        │               ├── KillSoundClient.java
        │               ├── config/
        │               │   ├── KillSoundConfig.java
        │               │   └── ConfigManager.java
        │               ├── detection/
        │               │   ├── KillDetector.java
        │               │   └── DeathDetector.java
        │               ├── audio/
        │               │   ├── CustomSoundManager.java
        │               │   └── SoundFile.java
        │               ├── gui/
        │               │   └── KillSoundConfigScreen.java
        │               └── util/
        │                   └── FileUtil.java
        │
        └── resources/
            ├── fabric.mod.json
            └── assets/
                └── killsound/
                    └── icon.png
```

Do not put user-provided OGG files inside `src/main/resources`. Those are runtime files.

---

# 5. Runtime Sound Directory

The mod should create a dedicated directory:

```text
.minecraft/
└── killsound/
    └── sounds/
```

Example:

```text
.minecraft/
└── killsound/
    └── sounds/
        ├── victory.ogg
        ├── death.ogg
        ├── anime_kill.ogg
        └── custom_death.ogg
```

The mod must create the directory automatically if it does not exist.

Do not assume that `.minecraft` is always the current working directory. Use Minecraft's configured game directory from the client instance/environment.

---

# 6. Supported Sound Files

Primary supported format:

```text
.ogg
```

Prefer:
- OGG Vorbis
- Reasonable sample rate such as 44.1 kHz or 48 kHz
- Mono or stereo
- Short sound effects

The mod should reject unsupported extensions instead of trying to load arbitrary files.

Recommended validation:
- File exists
- Regular file, not directory
- Extension is `.ogg`
- File is readable
- File is below a configurable/sensible size limit

Never execute or interpret the file as code.

---

# 7. Configuration Model

Create a persistent configuration object similar to:

```java
public class KillSoundConfig {

    public boolean enemyKillEnabled = true;
    public String enemyKillSound = "victory.ogg";
    public float enemyKillVolume = 1.0f;
    public float enemyKillPitch = 1.0f;

    public boolean ownDeathEnabled = true;
    public String ownDeathSound = "death.ogg";
    public float ownDeathVolume = 1.0f;
    public float ownDeathPitch = 1.0f;
}
```

Recommended ranges:

```text
Volume: 0.0 - 2.0
Pitch:  0.5 - 2.0
```

Use sensible UI step sizes, for example:

```text
Volume: 0.05
Pitch:  0.05
```

The config should be serialized to JSON or another simple persistent format.

Recommended path:

```text
.minecraft/config/killsound.json
```

Keep runtime sound files separate:

```text
.minecraft/killsound/sounds/
```

---

# 8. Default Configuration

On first launch:

```json
{
  "enemyKillEnabled": true,
  "enemyKillSound": "victory.ogg",
  "enemyKillVolume": 1.0,
  "enemyKillPitch": 1.0,
  "ownDeathEnabled": true,
  "ownDeathSound": "death.ogg",
  "ownDeathVolume": 1.0,
  "ownDeathPitch": 1.0
}
```

If the configured sound does not exist, do not crash.

Display an error/warning and either:
- play nothing, or
- use a bundled fallback sound if one is deliberately included.

Prefer playing nothing rather than silently substituting a different user sound.

---

# 9. Mod Initialization

Create:

```text
KillSoundClient.java
```

It should:

1. Initialize the client mod.
2. Locate the Minecraft game directory.
3. Create:
   ```text
   killsound/sounds/
   ```
4. Load the configuration.
5. Initialize the custom sound manager.
6. Register client-side detection logic.
7. Register Mod Menu integration.
8. Register any client tick/event handlers required by the detection implementation.

The mod must be explicitly client-only.

---

# 10. Player Kill Detection

This is the most important part.

The mod must distinguish:

```text
Local player kills another player
```

from:

```text
Another player kills someone
```

and:

```text
Local player kills a mob
```
# Preferred approach

Use a client-observable player kill statistic/state and track its previous value.

Conceptually:

```text
previousPlayerKills
currentPlayerKills
```

Every appropriate client tick:

```text
if currentPlayerKills > previousPlayerKills:
    local player has obtained one or more player kills
```

For each increase:

```text
play configured enemy kill sound
```

Then:

```text
previousPlayerKills = currentPlayerKills
```
# Important state initialization

Do not initialize the previous value incorrectly.

When:
- joining a world
- changing worlds
- respawning
- reconnecting

the detector should synchronize its baseline.

Otherwise the mod could incorrectly play multiple sounds because it sees the player's lifetime statistic as a new increase.

Recommended behavior:

```text
On client/world/player initialization:
    previousPlayerKills = currentPlayerKills
    initialized = true
```

Only detect increases after initialization.
# Example logic

```text
if player == null:
    reset detector state

else:
    current = player kill statistic

    if not initialized:
        previous = current
        initialized = true
        return

    if current > previous:
        difference = current - previous

        repeat difference times:
            trigger enemy kill sound

    previous = current
```

Normally `difference` will be 1.

Do not allow a huge statistic jump to cause hundreds of sounds. Add a reasonable maximum per tick/session as a safety guard.

---

# 11. Own Death Detection

The own-death sound must trigger when the local player dies.

The detector should monitor the local player's death state rather than every player death.

Conceptually:

```text
previouslyAlive
currentlyAlive
```

When:

```text
previouslyAlive == true
currentlyAlive == false
```

trigger the Own Death Sound once.
# Important

Do not repeatedly play the sound every tick while the player remains dead.

Use a latch/state:

```text
deathHandled = true
```

Reset it after the player is alive again or after a new player/world state is initialized.

Example:

```text
if player.isDeadOrDying():
    if !deathHandled:
        play own death sound
        deathHandled = true
else:
    deathHandled = false
```

Use the exact 1.21.11 mapped API method available in the generated project.

---

# 12. Avoiding False Triggers

The implementation must handle:
## Respawn

```text
Death → Respawn
```

must produce exactly one death sound.
## Reconnect

Joining a server must not trigger a fake kill/death sound.
## World change

Changing worlds must reset detection state.
## Spectator

Switching to spectator should not accidentally trigger a death sound unless the local player actually died according to the intended event.
## Statistics

Lifetime statistics must not be treated as fresh kills when the client first loads.
## Multiple kills between ticks

If the statistic increased by more than one, handle the difference carefully.

---

# 13. Custom Sound Loading

Minecraft normally expects registered sound events/resources. Because this project allows users to drop arbitrary `.ogg` files into a runtime directory, implement a runtime sound-loading layer.

Recommended architecture:

```text
CustomSoundManager
        |
        +-- scanSoundsDirectory()
        |
        +-- validateSound()
        |
        +-- create/load sound resource
        |
        +-- play(soundFile, volume, pitch)
```

The implementation must use APIs that are actually available in Minecraft 1.21.11.

Do not assume that an older Minecraft version's internal sound-engine classes will work unchanged.

If direct runtime registration of arbitrary sound events is not viable with the chosen 1.21.11 mappings, use a stable client-side audio mechanism compatible with the target version rather than modifying private internals unnecessarily.

---

# 14. Sound Manager Requirements

Create:

```text
CustomSoundManager.java
```

Responsibilities:

1. Scan:
   ```text
   .minecraft/killsound/sounds/
   ```
2. Find `.ogg` files.
3. Cache valid files.
4. Provide a list of filenames for the config UI.
5. Reload the list when requested.
6. Play selected files.
7. Handle missing/corrupt files gracefully.
8. Avoid memory leaks.
9. Avoid opening a new permanent audio resource for every playback.

Useful methods:

```java
initialize()
refreshSounds()
List<SoundFile> getAvailableSounds()
boolean isAvailable(String filename)
void play(String filename, float volume, float pitch)
void stopAll()
```

---

# 15. Config UI

Mod Menu should expose:

```text
KillSound
```

Clicking it should open the YACL configuration screen.

Suggested UI:

```text
KillSound Settings

────────────────────────────

Enemy Kill Sound

[ Enabled ]

Sound:
[ victory.ogg ▼ ]

Volume:
[──────────────] 100%

Pitch:
[──────────────] 100%

[ Test Enemy Kill Sound ]

────────────────────────────

Own Death Sound

[ Enabled ]

Sound:
[ death.ogg ▼ ]

Volume:
[──────────────] 100%

Pitch:
[──────────────] 100%

[ Test Own Death Sound ]

────────────────────────────

[ Refresh Sound List ]

[ Open Sounds Folder ]

[ Done ]
```

---

# 16. Sound Dropdown

The dropdown should contain every valid `.ogg` file found in:

```text
.minecraft/killsound/sounds/
```

Example:

```text
victory.ogg
death.ogg
anime_kill.ogg
minecraft_hit.ogg
custom1.ogg
```

If no sounds exist:

```text
No .ogg files found
```

The UI should not crash.

---

# 17. Refresh Button

The user may add a new sound while Minecraft is running.

Therefore:

```text
Refresh Sound List
```

should re-scan the directory.

Example:

1. Minecraft is open.
2. User adds:
   ```text
   ultra_kill.ogg
   ```
3. Clicks:
   ```text
   Refresh Sound List
   ```
4. `ultra_kill.ogg` appears in dropdown.

If the currently selected sound was deleted, mark it unavailable and prompt the user to choose another sound.

---

# 18. Open Sounds Folder

Provide a button:

```text
Open Sounds Folder
```

It should open:

```text
.minecraft/killsound/sounds/
```

using the operating system's file manager.

If opening the folder fails, show a user-friendly error.

---

# 19. Test Buttons

The test buttons must use the exact same audio playback path as actual events.

This prevents a situation where:

```text
Test button works
```

but:

```text
Actual kill event fails
```

Recommended:

```java
playEnemyKillSound();
playOwnDeathSound();
```

Both the test buttons and detectors should call these same methods.

---

# 20. Volume and Pitch
## Volume

Default:

```text
1.0
```

Suggested range:

```text
0.0 - 2.0
```
## Pitch

Default:

```text
1.0
```

Suggested range:

```text
0.5 - 2.0
```

Clamp values before playback:

```text
volume = clamp(volume, 0.0, 2.0)
pitch  = clamp(pitch, 0.5, 2.0)
```

---

# 21. Configuration Persistence

Whenever the user changes settings and closes the config screen:

```text
save config
```

Write to:

```text
.minecraft/config/killsound.json
```

At startup:

```text
load config
```

If the config is malformed:

1. Log the problem.
2. Back up or ignore the invalid configuration as appropriate.
3. Restore defaults.
4. Continue launching.

Never crash the game because a simple configuration file is malformed.

---

# 22. Logging

Use a dedicated logger:

```text
KillSound
```

Useful messages:

```text
KillSound initialized
Sound directory: ...
Loaded X sound files
Enemy kill sound: victory.ogg
Own death sound: death.ogg
Playing enemy kill sound
Playing own death sound
```

Warnings:

```text
Configured sound does not exist
Invalid OGG file
Unable to open sound directory
Failed to play sound
```

Do not spam the log every tick.

---

# 23. Fabric Mod Metadata

`fabric.mod.json` should declare:

```text
id: killsound
environment: client
```

The client entrypoint should point to:

```text
com.example.killsound.KillSoundClient
```

Dependencies should include compatible versions of:

```text
fabricloader
minecraft
fabric-api
modmenu
yacl
```

Use the exact versions generated/resolved for Minecraft 1.21.11.

---

# 24. Mod Menu Integration

Implement Mod Menu's config-screen factory.

Conceptually:

```java
public Screen getModConfigScreenFactory(Screen parent) {
    return YaclConfigScreen.create(parent);
}
```

Use the exact Mod Menu API interface/method names for the selected Mod Menu version.

Do not hardcode old Mod Menu APIs from earlier Minecraft versions.

---

# 25. User Experience

On first launch:

```text
KillSound has been installed.

Sounds folder:
.minecraft/killsound/sounds/

Drop .ogg files there and select them in Mod Menu.
```

Do not show a message every launch.

If the folder is empty, the config screen should explain:

```text
No sounds found.

Add .ogg files to:
.minecraft/killsound/sounds/

Then click Refresh Sound List.
```

---

# 26. Default Sounds

The mod may optionally ship with:

```text
victory.ogg
death.ogg
```

However, these should only be bundled if the project has appropriate rights/licenses for the audio.

The mod should never require these exact filenames.

If they are not bundled:

```text
No default sound dependency.
```

Users can provide their own.

---

# 27. Security / Robustness

Do not:
- execute files
- download arbitrary files
- run external programs from sound files
- accept network URLs as sounds
- automatically download sounds
- scan the entire computer

Only read from:

```text
.minecraft/killsound/sounds/
```

and only process supported `.ogg` files.

---

# 28. Server Compatibility

The mod is client-only.

It should work when joining:

```text
Vanilla server
Fabric server
Paper server
Spigot server
Forge/NeoForge server
```

provided the client can connect normally.

The server does not need the mod.

No server plugin is required.

No OP is required.

The server should not need to know which sound the client selected.

---

# 29. Important Multiplayer Limitation

Client-only detection must be based on information the client actually receives.

The goal is:

```text
You kill Player B
→ your client observes its own player-kill state increasing
→ enemy kill sound
```

and:

```text
Player A kills Player B
→ your own kill state does not increase
→ no enemy kill sound
```

If a particular server plugin modifies normal death/statistic behavior, the mod should fail gracefully rather than attempting unsafe packet manipulation.

---

# 30. Recommended Class Responsibilities
# `KillSoundClient`

Main initializer.

Responsibilities:
- initialize config
- initialize sound manager
- initialize detectors
- register client events
# `KillSoundConfig`

Data model.

Responsibilities:
- hold settings
- provide defaults
- validate values
# `ConfigManager`

Responsibilities:
- load JSON
- save JSON
- recover from invalid config
# `KillDetector`

Responsibilities:
- track local player kill state
- detect increases
- call enemy sound callback
# `DeathDetector`

Responsibilities:
- track local player death state
- trigger once per death
- reset after respawn
# `CustomSoundManager`

Responsibilities:
- scan sound directory
- validate files
- expose available sounds
- play sounds
# `SoundFile`

Represents one available OGG file.

Possible fields:

```java
String filename;
Path path;
```
# `KillSoundConfigScreen`

Builds the YACL UI.

---

# 31. Event Flow
# Enemy kill

```text
Minecraft Client Tick
        ↓
KillDetector
        ↓
Read local player's player-kill state
        ↓
Compare with previous state
        ↓
Increase detected?
        ↓
YES
        ↓
Check enemyKillEnabled
        ↓
Check selected file
        ↓
CustomSoundManager.play(...)
```
# Own death

```text
Minecraft Client Tick/Event
        ↓
DeathDetector
        ↓
Is local player dead?
        ↓
Was death already handled?
        ↓
NO
        ↓
Check ownDeathEnabled
        ↓
CustomSoundManager.play(...)
        ↓
Mark death handled
```

---

# 32. Testing Plan
# Test 1 — Own death

Create a singleplayer world.

Enable:

```text
Own Death Sound = ON
```

Select a test OGG.

Use:

```mcfunction
/kill @s
```

Expected:

```text
Exactly one own-death sound.
```

---
# Test 2 — Enemy kill

Use two players.

Player A = local player.

Player B = another player.

A kills B.

Expected:

```text
Enemy Kill Sound: YES
Own Death Sound: NO
```

---
# Test 3 — Other player gets a kill

Player B kills Player C.

Expected for Player A:

```text
Enemy Kill Sound: NO
Own Death Sound: NO
```

---
# Test 4 — Local player dies

Player B kills Player A.

Expected:

```text
Enemy Kill Sound: NO
Own Death Sound: YES
```

---
# Test 5 — Mob kill

Local player kills a zombie.

Expected:

```text
Enemy Kill Sound: NO
```

---
# Test 6 — Respawn

Die and respawn.

Expected:

```text
One sound per death.
```

Not:

```text
Multiple sounds while dead.
```

---
# Test 7 — Reconnect

Disconnect and reconnect.

Expected:

```text
No fake kill sound.
No fake death sound.
```

---
# Test 8 — Custom sound selection

Add:

```text
test1.ogg
test2.ogg
```

Click:

```text
Refresh Sound List
```

Expected both appear.

Select `test1.ogg`.

Click Test.

Expected `test1.ogg` plays.

---
# Test 9 — Missing sound

Delete the configured sound.

Expected:

```text
No crash.
Clear warning.
No playback.
```

---
# Test 10 — Invalid file

Put:

```text
fake.ogg
```

that is not actually an OGG file.

Expected:

```text
Invalid file is rejected gracefully.
```

---

# 33. Build Commands

From the project root:

Windows:

```powershell
.\gradlew.bat build
```

Linux/macOS:

```bash
./gradlew build
```

Expected:

```text
BUILD SUCCESSFUL
```

Output:

```text
build/libs/
```

Use the normal remapped JAR intended for distribution, not a development-only artifact.

---

# 34. Development Testing

Run the client from Gradle/IDEA using the generated client run configuration.

Recommended cycle:

```text
Edit code
    ↓
Run Minecraft client
    ↓
Test
    ↓
Check logs
    ↓
Fix
    ↓
Run again
```

For a clean build:

```powershell
.\gradlew.bat clean build
```

---

# 35. Distribution

Final user installation should be:

```text
.minecraft/mods/
├── fabric-api.jar
├── modmenu.jar
├── yacl.jar
└── killsound.jar
```

The user then launches Fabric 1.21.11.

After first launch:

```text
.minecraft/killsound/sounds/
```

is created.

Users put their OGG files there.

---

# 36. README Instructions for End Users

The final README should explain:

```text
1. Install Fabric for Minecraft 1.21.11.
2. Install Fabric API.
3. Install Mod Menu.
4. Install YACL.
5. Put KillSound.jar into .minecraft/mods.
6. Launch Minecraft.
7. Open Mod Menu.
8. Open KillSound settings.
9. Put .ogg files into .minecraft/killsound/sounds/.
10. Click Refresh Sound List.
11. Select Enemy Kill Sound.
12. Select Own Death Sound.
13. Configure volume/pitch.
14. Click Done.
```

---

# 37. Future Features

Keep the architecture extensible so future versions can add:

- Kill streak sounds
- Headshot sounds
- Bed break sound
- Bow kill sound
- Different sounds by weapon
- Different sounds by player
- Kill sound cooldown
- Random sound selection
- Sound packs
- Per-server configurations
- Sound preview waveform
- Keybind to test selected sound
- Resource-pack based sounds

Do not implement these unless explicitly requested.

---

# 38. Final Acceptance Criteria

The project is considered complete only when all of the following are true:

- [ ] Builds successfully for Minecraft 1.21.11.
- [ ] Runs as a client-only Fabric mod.
- [ ] Does not require a server-side mod.
- [ ] Does not require OP.
- [ ] Mod appears in Mod Menu.
- [ ] YACL settings screen opens.
- [ ] User can enable/disable Enemy Kill Sound.
- [ ] User can enable/disable Own Death Sound.
- [ ] User can select arbitrary `.ogg` files.
- [ ] User can change volume.
- [ ] User can change pitch.
- [ ] User can test each sound.
- [ ] User can refresh sound list.
- [ ] User can open the sounds folder.
- [ ] User settings persist.
- [ ] Own player kill triggers only Enemy Kill Sound.
- [ ] Other players' kills do not trigger Enemy Kill Sound.
- [ ] Mob kills do not trigger Enemy Kill Sound.
- [ ] Local player death triggers only Own Death Sound.
- [ ] Death sound triggers once per death.
- [ ] Reconnect/world changes do not cause false triggers.
- [ ] Missing sound files do not crash Minecraft.
- [ ] Invalid sound files do not crash Minecraft.
- [ ] No server modifications are required.
- [ ] No hardcoded `victory.ogg` or `death.ogg` dependency exists.
