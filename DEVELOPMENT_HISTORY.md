# Development History

This document is an append-oriented record of player-visible features, compatibility decisions, data migrations, and notable behavioral changes in New Custom Music Client-Side Player.

It records project development alongside release notes and Git history:

- Release notes provide a concise overview for players.
- This document preserves the complete product-level history behind those highlights.
- Git commits remain the source for implementation details and individual code changes.

## Recording policy

- Version entries are ordered from oldest to newest. New entries are appended to the end of the document.
- Before a version is published, its draft entry may be expanded or corrected as implementation and testing continue. After publication, the entry follows the historical-preservation rules below.
- Published entries preserve the development record; substantive corrections carry a date.
- If a historical statement is found to be inaccurate, a dated correction should be appended to the relevant entry.
- Spelling, grammar, formatting, and broken links may be corrected directly when their meaning is unchanged.
- A version entry is complete only when it records every relevant player-visible behavior, data format, migration rule and priority, validation boundary, error or warning branch, conflict-resolution rule, failure protection, and important interaction detail introduced or changed by that version.
- Exact limits, ordering, fallback behavior, and persistence guarantees should be stated whenever players, resource-pack authors, translators, or maintainers could depend on them later.
- Entries describe changes, implementation mechanisms, completed tests, and release status from the developer's perspective.
- Before release, the entry should be audited against the complete version diff so concise release notes and current-use documentation can safely omit these details without making them untraceable.
- Class names, private method names, variable renames, formatting-only edits, and pure refactors are excluded unless they affect compatibility, maintenance expectations, or observable behavior. Git history remains the source for those implementation details.

This record begins with version 1.4.4-beta2. Earlier versions may be documented later through a clearly identified retrospective based on the repository's Git history.

## 1.4.4-beta2

**Status:** Beta

### Highlights

- Unified playback-volume storage with the main mod configuration while preserving the established keyboard-shortcut behavior.
- Added import and export support for custom single-track names.
- Added protection against accidentally discarding unapplied configuration changes.
- Added a direct way to restore a track's original sound-event ID as its displayed name.
- Aligned playlist-name validation and change indicators across older and newer interfaces.

### Configuration and playback volume

- Playback volume is now stored in the main `config.json` instead of the separate `playback_volume.json` file.
- The configuration screen provides a Minecraft-style playback-volume slider from 0% to 100% and permits one-percent precision.
- The slider edits only the configuration draft. Applying, importing, or resetting configuration values keeps the slider synchronized with that draft, and a successful Apply updates the active volume immediately.
- Playback-volume keyboard shortcuts continue to change the value in five-percentage-point steps. When a non-multiple such as 98% is increased, it first reaches the 100% boundary; only a subsequent attempt reports that the maximum has already been reached.
- Playback-volume shortcuts are applied only while no screen is open, preventing configuration drafts and Minecraft's keyboard navigation from interfering with one another.
- Shortcut changes are saved to the unified configuration immediately. If that save fails, the in-memory value remains active and the client retries the pending configuration save during shutdown.
- Changing or applying the volume immediately updates the base volume of the currently playing mod-owned sound.
- Existing playback-volume data is migrated silently on startup. If `config.json` has no playback-volume field, the legacy value is used; a valid volume already owned by `config.json` takes priority.
- When migration succeeds, `playback_volume.json` is renamed to `playback_volume.json.migrated`. An existing migrated file is preserved. Retirement of the legacy source follows successful storage of the volume in the unified configuration.
- If saving a new unified configuration fails, the legacy file is kept, its value remains usable in memory, and migration can be attempted again on a later launch. An unreadable or structurally invalid main configuration also keeps the legacy source available.
- Migrated and loaded numeric volume values are constrained to the supported 0-100 range. A non-integer or otherwise unreadable stored volume falls back to the default of 100%.
- Missing fields in an otherwise valid configuration are restored from defaults and written back automatically, so installations created by older versions acquire newly introduced settings.
- This missing-field repair covers update checks, the now-playing toast, playback feedback mode, and playback volume while preserving other members of the existing JSON object during the repair write.
- Configuration import and export now include playback volume. Import accepts an integer from 0 to 100; invalid values are skipped through the recoverable configuration-warning flow, preserving the draft value.
- Configuration-import warnings use labels from the active Minecraft language for known settings.
- An unknown skipped field uses its literal transfer-file field name as the warning label.

### Unapplied configuration changes

- Leaving a modified configuration screen through Back or Esc now opens an unapplied-changes warning.
- The warning lists the settings that would be discarded in the same order as the configuration screen. Each entry shows the applied value, a right arrow, and the draft value with distinct colors for easier comparison.
- The warning shows only fields whose draft values actually differ from the applied configuration.
- Setting labels are aqua, applied values are gray, arrow punctuation is dark gray, and draft values are yellow. Boolean values reuse Minecraft's localized On/Off text, feedback modes reuse their existing translated labels, and volume values include a percent sign.
- **Keep Editing** returns to the exact draft screen and is the initially focused action. **Discard Changes** immediately confirms the discard choice, retains the applied configuration, and exits.
- Clicking **Discard Changes** takes effect immediately.
- Esc-based discard is guarded for three seconds. During the countdown, additional Esc presses are ignored; after it finishes, pressing Esc discards the changes and exits.
- The countdown is centered directly below the title and enclosed in gray parentheses. Remaining seconds are yellow, `Esc` is aqua and bold, and the ready-state instruction is yellow.

### Shared warning-screen presentation

- Warning content now uses a dynamically sized bordered panel for up to twelve wrapped lines.
- Longer warning content switches to a scrollable list while keeping the same overall screen structure.
- Short one- or two-line warnings display their complete text with padding adjusted to the content height.
- The shared warning-screen foundation now supports an optional centered dynamic subtitle and a screen-defined initial button focus.
- The sizing correction applies to every screen using this shared presentation, including playlist import, configuration import, track-name import, and the new unapplied-change warning.

### Custom single-track names

- Added import and export icon buttons to the single-track selection screen.
- The transfer buttons are disabled in fullscreen because the operating-system file dialog is unavailable there. A dedicated track-name-transfer tooltip explains how to disable fullscreen.
- Both dialogs filter for `.txt` files. Closing either dialog with an empty selection preserves the current data. Asynchronous results are discarded after the player leaves the single-track screen.
- The default export filename combines the mod name and mod version with `-track-names.txt`, replacing characters that are unsafe in filenames.
- Export creates a UTF-8, human-readable structured text file containing format version 1, the mod ID, mod version, Minecraft version, export time, and the custom track-name map.
- Export includes custom display-name overrides. An empty custom-name map produces a valid transfer file with an empty map.
- Successful export reports the normalized destination path in a success toast. A file-write failure reports the selected path in a failure toast, while failure to open the save dialog uses a separate failure message.
- Import previews and validates the complete file before changing local data. A missing, non-file, empty, inaccessible, or otherwise unreadable source opens the read-error screen.
- A non-object root, malformed or trailing JSON, duplicate top-level `modId` or `trackNames` members, a non-text `modId`, a non-object `trackNames` value, a missing required member, or a different mod ID opens the invalid-file screen.
- Version and export metadata are informational in format 1. Import identifies compatibility from the mod ID and `trackNames` object and ignores other top-level fields, supporting later metadata additions.
- Recoverable warnings are ordered as invalid sound-event IDs, duplicate IDs, non-text values, empty names, names longer than 50 characters, redundant default-name entries, same-namespace name conflicts, and replacements of existing custom names. A category is displayed only when it actually occurs.
- Imported text names are trimmed before empty, length, default-name, and conflict checks. Repeated IDs cause every occurrence of that ID to be skipped.
- Invalid, duplicate, non-text, empty, overlong, redundant-default, and conflicting entries are skipped while compatible entries remain eligible for import. An empty compatible set ends the operation on a dedicated screen and preserves local data.
- Same-namespace display-name conflicts use case-insensitive comparison. Existing names take priority; when imported candidates collide, the earlier entry keeps its candidate name and later entries are rejected. Conflicts are recalculated until the final set is stable.
- The warning preview distinguishes namespaces, paths, existing names, and imported names with the same color-oriented presentation used by related music-management screens.
- A warning preview requires confirmation before applying compatible entries. A clean preview applies compatible entries directly.
- Mappings for sound events absent from the currently loaded resource packs remain eligible for import, supporting optional or temporarily disabled packs.
- Confirming an import writes the compatible mappings atomically, refreshes the namespace currently being viewed, and clears the previous row selection so controls use the refreshed list state.
- The success toast counts mappings whose stored names actually changed. Importing identical mappings produces a zero-change result and preserves the existing repository file.
- The track-name repository now reads and writes explicitly as UTF-8. Rename and import operations prepare a separate updated map, write it through a temporary-file replacement, and update the in-memory map only after that write succeeds.
- A read or parse error while loading the existing repository stops rename, restore, export, and import operations and preserves the source file.
- Import-save failure opens a dedicated error screen and leaves the existing stored and in-memory names unchanged. Restore failure sends a translated system message, and export failures use the toast paths described above.

### Single-track selection interface

- Reorganized the bottom actions into a consistent two-by-two, 150-pixel button layout: **Play Selected Track**, **Rename Track**, **Restore Default Name**, and **Back**.
- Kept Play as the upper-left action to preserve the most familiar primary-action position.
- Added **Restore Default Name**, which removes the selected track's custom mapping and displays its original sound-event ID again.
- Restore is available for a selected track with a custom name. Its explanatory tooltip appears while the button is available.
- Play and Rename require any selected track; Restore additionally requires that selection to have a custom name. Refreshing entries after rename, restore, or import clears stale selection state and recalculates all three button states.
- Fixed the search field at 20 pixels high and increased the footer to accommodate the second action row. The transfer buttons sit beside the search field while balancing space keeps the search control visually centered.
- The track list now fills all remaining content height.

### Playlist naming and transfer consistency

- Playlist-name conflict checks in graphical create and rename screens now use exact, case-sensitive comparison, matching command and storage semantics.
- Playlist import uses the same exact, case-sensitive comparison. Names that differ by letter case can coexist across graphical and command interfaces.
- A case-only rename is allowed when the target spelling is available. Exact duplicate names and renaming to the current name are rejected.
- Exact import collisions use automatic numeric-suffix renaming. Case-distinct names retain their imported spelling.
- Replaced the outdated graphical naming hint that implied playlist names were restricted to letters, numbers, and underscores, and removed the unused invalid-character translation. Graphical names are trimmed, must contain 1-50 characters, and may contain spaces, Unicode characters, punctuation, and underscores.
- Command arguments continue to use exact, case-sensitive playlist names and suggestions continue to insert the stored spelling, so the graphical and command paths now agree on case-only names.
- Standardized player-visible before-and-after changes on the Unicode right arrow (`→`) instead of mixing it with the older ASCII `->` representation.

### Localization and release metadata

- Added and reviewed English and Simplified Chinese text for playback volume, track-name transfer, restore-default-name behavior, unapplied-change warnings, countdown states, and detailed configuration differences.
- Added dedicated translation keys for new shared behaviors, including the track-name fullscreen requirement.
- Updated the project version to `1.4.4-beta2` for community testing before the next stable release.

## 1.4.4 post-beta2 stabilization

**Status:** Released as stable `1.4.4` on August 15, 2026

This entry records maintenance completed during the `1.4.4-beta2` stabilization period and included in stable `1.4.4`.

### Repository default-branch migration

- Migrated the main GitHub repository from the legacy `master` branch name to `main`, retaining the complete project history and archiving the former placeholder history.
- Before the migration, preserved the former placeholder `main` at commit `8528521` as `archive/legacy-main-before-migration` and created the annotated `pre-main-migration` tag at `bf3b7a0` to preserve the complete project state immediately before the branch-name change.
- Temporarily selected the complete `master` history as the default, removed the placeholder `main`, and then used GitHub's branch-renaming operation to rename the complete branch to `main`. The repository default branch now points to the complete project history.
- Renamed the local development branch to `main`, updated its upstream to `origin/main`, and updated `origin/HEAD` so new clones and ordinary Git operations consistently select the new default branch.
- Updated the source-code URLs stored in Fabric mod metadata and the in-game configuration screen from `/tree/master` to `/tree/main`.
- Updated the main-repository links on the Wiki home page from `/blob/master` to `/blob/main`. The Wiki remains a separate Git repository whose own branch is intentionally still named `master`.
- Updated the external Modrinth project-level source URL from the branch-specific `/tree/master` path to the repository root. The root URL follows the current default `main` branch automatically and avoids retaining another stale branch-specific link if the default branch name changes again.
- Immediately afterward, made the repository-owned source links equally resilient by replacing the branch-specific `/tree/main` URLs in Fabric mod metadata and the in-game configuration screen with the repository root.
- Changed the Wiki's direct development-history and license links from `/blob/main/...` to `/blob/HEAD/...`, preserving their exact file destinations while allowing GitHub to resolve them through whichever branch is currently the repository default.

### Stale Copilot branch cleanup

- Reviewed pull-request associations and removed the stale `copilot/fix-ghost-instances` and `copilot/evaluate-potential-bugs` remote branches.
- The former placeholder commit referenced by `copilot/fix-ghost-instances` is retained in the migration archive.
- The project snapshot from `copilot/evaluate-potential-bugs` is represented by commit `c4301aa`; cleanup also retired its redundant additional `LICENSE` file.

### Playlist playback availability feedback

- Kept the playlist-management screen's **Play Selected Playlist** action available for any selected playlist, including an empty or currently unplayable one. Players can therefore still open the play-instance screen to inspect the saved order, see unavailable tracks marked in the preview, choose a starting index, and review the loop and shuffle controls.
- The play-instance screen now disables **Start Playing** for settings with an empty playable set and displays the reason in a button tooltip, making playback availability visible on the screen.
- Preserved the existing empty-playlist and invalid-start-index validation, messages, and tooltip behavior. Availability validation runs only after those structural input checks have succeeded.
- When the complete playlist contains no currently playable sound, **Start Playing** is disabled in both loop modes and its unstyled tooltip reuses the existing localized `no_playable_sound` message.
- When loop mode is disabled and the playlist has playable tracks only before the selected starting index, **Start Playing** is disabled with a dedicated tooltip explaining that no playable track remains between the selected starting position and the end of the playlist.
- Complete playlist unavailability is checked before the non-loop starting-position condition. An empty playable set receives the general no-playable-playlist message; a playable set entirely before the starting index receives the starting-position message.
- When loop mode is enabled, any playable track anywhere in the playlist is sufficient because playback may wrap around to an earlier position. When loop mode is disabled, at least one playable track must exist at or after the selected starting index.
- Partially playable playlists remain supported. Unavailable entries continue to be marked in the preview and skipped by the playback queue, while a valid playable candidate keeps **Start Playing** available.
- Availability is recalculated when the screen initializes, the starting-index text changes, or the loop or shuffle state changes. It is also recalculated immediately before the start action invokes the playback controller, preventing a stale enabled button from starting playback after resource availability changes while the screen remains open.
- The playback controller retains its independent empty, availability, queue-position, and final-start checks as defense for unexpected callers or state changes. Under ordinary GUI interaction the screen validation prevents these failure paths, while the controller remains able to reject an invalid request safely if the UI is bypassed.
- Command-based playlist playback retains its chat error feedback. All command paths that report the no-playable-sound condition now apply explicit red styling, while the corresponding disabled-button tooltips remain the normal white tooltip color.
- Added reviewed English and Simplified Chinese text for the non-loop starting-position condition.

## 1.5.1 Minecraft 26.3 migration

**Status:** `1.5.1-beta` ready for beta publication; stable promotion pending a one-week observation period

### Migration boundary and version-line policy

- Migrated the final stable `1.4.4` feature set from Minecraft 26.2 to Minecraft 26.3 for version `1.5.1-beta`.
- Beginning with this migration, each `.1` release moves the previous line to the next supported Minecraft version. Later versions carry feature development.
- Before migration work began, the final Minecraft 26.2 source state was retained on the local `26.2` branch at commit `c23ddb0`. Future migrations should preserve the preceding version's final source on a correspondingly named branch before the active development branch is advanced. This allows a security or maintenance fix to be prepared directly against the affected older line without rolling back the whole repository.
- Beta verification included packaged startup testing on the final Minecraft 26.3 release, a minimal-dependency installation, and an end-to-end transfer rehearsal from the previous version.

### Toolchain and dependency baseline

- Updated the Minecraft target from 26.2 to 26.3.
- Updated Fabric Loader from 0.19.3 to 0.19.5 and Fabric API from `0.152.1+26.2` to `0.161.0+26.3`.
- Updated Fabric Loom from 1.15.5 to 1.17.21 and the Gradle wrapper from 9.2.1 to 9.6 so the development toolchain matches the supported Minecraft 26.3 setup.
- Java 25 is the required language and runtime level.
- Set beta metadata to `1.5.1-beta` and the stable target to `1.5.1`. Exported metadata and default transfer filenames identify the active project version.

### Source-level compatibility changes

- Replaced direct GLFW key constants with the equivalent Minecraft `InputConstants` key values after the window/input API migration.
- Updated the `RenderPipeline` import to its Minecraft 26.3 package.
- Converted external-link strings to `URI` values at the Minecraft confirmation-screen boundary required by 26.3.
- Replaced the removed window-level fullscreen query with Minecraft 26.3's overall fullscreen option state. File-transfer controls retain their established safety rule: configuration, playlist, and custom track-name import/export are unavailable in every fullscreen mode and display the existing instruction to disable fullscreen first.
- Minecraft 26.3 distinguishes borderless and exclusive fullscreen. File-transfer controls now check the general fullscreen option, covering both modes.
- The **Exclusive Fullscreen** preference selects the presentation mode for fullscreen. File transfer remains available while the general **Fullscreen** option is off and is disabled when that option is on.

### Native file-dialog migration

- Migrated the asynchronous file-dialog utility shared by configuration, playlist, and custom track-name transfer from Tiny File Dialogs to Minecraft 26.3's SDL3 runtime.
- An initial AWT `FileDialog` implementation produced read/open-error screens during runtime testing with the SDL-based client window. Replaced it with SDL3 file dialogs.
- The implementation uses the SDL3 binding supplied by the Minecraft/LWJGL runtime.
- The shared utility returns `CompletableFuture<Optional<Path>>`, representing a selected path, dialog cancellation, or exceptional failure for configuration, playlist, and track-name transfer.
- SDL file dialogs are asynchronous. Each request therefore receives a unique numeric identifier and is kept in a concurrent active-request map until SDL invokes the persistent callback. This prevents request state and native filter memory from being collected or freed while the operating-system dialog may still use them.
- The callback treats a null file-list pointer as a dialog failure, a list whose first entry is null as cancellation, and the first UTF-8 path as the selected file. Dialogs use single-file selection.
- SDL filter patterns use bare extensions rather than the older `*.extension` spelling. The utility translates existing caller filters such as `*.txt` to SDL's `txt` representation while preserving the caller's localized filter label.
- The SDL properties-based dialog entry point carries the Minecraft window handle, localized title, default path or filename, filter pointer and count, and single-selection setting. It restores the descriptive file-dialog titles.
- Property objects, native filter structures, and the UTF-8 filter strings stay attached to the request and are released only after the callback completes. If setup fails before the dialog is handed to SDL, the partially allocated state is released immediately and the existing exceptional-completion path is used.
- Native setup and callback failures use the existing screen and toast error handling.
- Active dialog properties, filters, and callback state stay allocated until SDL completes the asynchronous request. Callback completion owns normal cleanup; process termination handles requests still active at exit. This lifetime rule protects SDL's access to native request memory.

### Development-runtime findings

- An initial development launch failed during Fabric Loom client-asset resolution because an expected cache object was absent.
- Development launches intermittently terminated after SDL window creation, usually during initial resource loading, with Windows exit code `0xC0000005`. Windows Error Reporting recorded fault locations in `jvm.dll` and unknown addresses, plus a heap-corruption event in `ntdll.dll`. The events occurred with both JBR 25.0.2 and Oracle Java 25.0.2.
- Two Oracle Java launches with new game directories completed resource loading, OpenAL initialization, sound-engine startup, and atlas creation. One used the base mod installation and the other enabled six resource packs. Both reached the main menu.
- Set the local IntelliJ development run configurations to the installed Java 25 runtime.
- A later launch logged `SSLHandshakeException` and `EOFException` during Minecraft's service discovery request to `https://discovery.minecraftservices.com/minecraft/client`. Resource loading, OpenAL initialization, sound-engine startup, and atlas creation completed afterward.

### Verification record

- `compileClientJava` passed on the Minecraft 26.3 dependency set.
- Development testing reached an interactive world on Minecraft 26.3 with resources, sound, rendering, and the migrated screens available. Startup tests succeeded with new game directories, the six-resource-pack set, and the established `run` directory.
- Packaged `1.5.1-beta` testing on the final Minecraft 26.3 release completed three consecutive successful startups. A minimal installation containing Fabric API and this mod also started and ran successfully.
- Runtime testing confirmed the Windows file dialog's default filename, localized file-type filter, and localized title. The properties-based implementation restored the title after the first convenience-overload implementation displayed the generic Windows dialog title.
- Source comparison against the `26.2` branch identified a fullscreen-semantic regression, corrected before beta publication. Runtime tests confirmed that actual fullscreen disables file transfer and that the standalone exclusive-fullscreen preference preserves file-transfer availability. All nine button-state and action-entry guards check the general fullscreen state.
- A migration rehearsal exercised configuration, playlist, and custom track-name transfers from the previous stable version to the 26.3 beta.
- These tests met the beta publication criteria. Scheduled stable `1.5.1` promotion after a one-week observation period covering startup stability, transfer reliability, data integrity, and playback behavior.

## 1.5.2-snapshot1

**Status:** Development snapshot, October 2, 2026; compilation and packaged screen-closure pause testing completed; publication pending

### Incremental snapshot development

- Changed the development version from `1.5.2-beta1` to `1.5.2-snapshot1`. The `1.5.2` line uses incremental snapshots to present completed changes throughout development.
- The first snapshot implements manual-playback pause protection, selective sound cleanup during world changes, and configurable playback continuation after disconnecting. In-game testing has completed for screen-closure pause protection and persistent elytra wind cleanup.
- Mod metadata and transfer metadata derive their version from the Gradle project version.

### Manual pause during screen closure

- Fixed unintended music resumption when closing a chest or returning from another screen to gameplay. The global sound-resume operation previously resumed manually paused music channels.
- The previous tick-end check paused music after the global resume operation had already been queued, leaving a brief resume window.
- Added per-instance filtering to the global resume path. It skips manually paused mod-owned channels and cancelled handles in the captured handle set, and resumes eligible vanilla channels.
- The shared screen-closure resume path now handles pause protection for containers, inventory, chat, and configuration screens.
- Pause and resume operations publish the instance's playback intent before queuing a native operation. The sound executor reads that intent when executing the task. Replaced the per-tick native pause retry with protection at global resume and asynchronous preparation. The paused-session tick branch checks for an ended session and returns before completion handling or automatic playlist advancement. Explicit manual pause queues the native pause operation.

### Pause while audio is loading

- Added logical manual-pause state to each music instance, covering loading, prepared, playing, and paused channel states. Pause requests take effect once the playback handle exists.
- Static-buffer and streamed music callbacks associate preparation with the original instance and check pause and cancellation on the sound executor. Audio prepared during manual pause is attached to its channel and waits for explicit continuation.
- Continuation handles three channel states: an already-started paused channel resumes from its current position; a prepared initial channel receives its first play operation; a loading channel starts after preparation completes and playback intent permits it. Repeated resume requests continue the current playback position.
- Cancelled instances enter a terminal state. Late static-buffer results are discarded. Streamed results arriving after cancellation or handle release are closed on the sound executor. Attached streams are cleaned up by channel release; shared static buffers retain their shared lifecycle.

### Session state initialization and cleanup

- Starting a replacement track or a new single-track or playlist session initializes playback with manual pause cleared.
- Stopping a session immediately clears the controller's manual-pause state, preparing it for a new session within the same tick.

### Selective sound cleanup during world changes

- Replaced the world-change shortcut that skipped global sound cleanup while a music session existed. Entering a replacement client world now retains the controller's current music instance and its active, uncancelled channel handle, then cleans up the remaining sounds. When that pair is unavailable, the original full-stop operation runs.
- The retention check uses the exact instance and handle identity. Playing, manually paused, and loading music retain their existing channel, playback position, and pause intent across the transition.
- Publish cancellation for discarded mapped handles before removing their engine indexes. Remove their instance-to-channel mappings, sound-source entries, delayed playback entries, and deletion deadlines. Clear the tickable sound list, queued tickable sounds, and cached source gains. The retained music instance uses the simple-sound lifecycle.
- Sweep the actual channel-handle set on the sound executor, retaining the selected handle and releasing every other handle, including handles absent from the engine indexes. Remove a handle from the set before releasing it so queued cancellation cleanup shares the same release ownership.
- Route static-buffer and stream preparation through the sound executor for all sounds. The execution-time check discards cancelled static-buffer results and closes late streams before attachment. Channel release cleans up attached streams; shared static buffers continue using the engine's shared buffer lifecycle.
- Apply the same cleanup to elytra wind, biome and underwater ambience, minecart loops, entity-bound sounds, and other sounds from the departing world. Clearing the delayed and tickable queues also removes their scheduled restarts.
- Preserve vanilla jukebox behavior across dimensions: world departure stops the local disc sound, while the server continues the song timer and note particles; returning follows vanilla playback-event handling to keep world audio faithful to the base game.

### Playback continuation after disconnecting

- Added **Keep Playing in Game Menu**, defaulting to OFF. The setting controls session retention when leaving a singleplayer world or ending a server connection to return to a selection screen. ON retains the current music instance, its playback position and manual-pause intent, and the playlist queue and indexes. OFF stops owned sounds, clears the session, and restores the saved vanilla music volume.
- Apply the disconnect decision synchronously at client teardown, before pending client tasks are discarded. Replaced the unconditional Fabric disconnect-event stop with handling at the world-exit boundary, covering pause-menu exits, low-level connection closure, kicks, and connection loss.
- Reconfiguration keeps the existing session while the connection moves through configuration and back into gameplay. Server-directed transfers also keep it while connecting to the destination. Scope the temporary transfer disconnect screen around its display operation and release that scope in a `finally` block.
- Distinguish connection preparation with an already empty world from an ended connection. Address-resolution and login failures that return to a disconnect screen, and explicit cancellation of a connection attempt, apply the menu setting.
- Extend selective world-sound cleanup to menu departure and transition paths where vanilla skips its stop operation. Retain the current valid music handle and release the other world sounds and delayed tasks.
- During menu ticks, recover retired sounds and release a finished current instance. A playlist with remaining tracks or looping enabled retains the saved vanilla music-volume lock while waiting in the menu and resumes automatic advancement after entering a world. When the last track of a non-looping playlist finishes in the menu, clear the playlist session and restore the saved vanilla music volume immediately. A completed single track also restores that volume. A paused current instance keeps its pause intent throughout the transition.
- Integrated the setting into configuration persistence, missing-field migration, import previews, export, Apply, draft discard, and reset to defaults. Existing configurations receive OFF for the new field; partial imports retain the current draft value when the field is absent. The discard-warning change list now includes the continuation setting, showing the applied and draft ON/OFF values in settings-screen order.
- Added English and Simplified Chinese labels and hover descriptions, and standardized player address to “您” in the Simplified Chinese Tips & Tricks entries. The continuation toggle occupies one standard-width cell in the two-column settings grid; its Simplified Chinese label is “退出世界时保持音乐播放”.

### Verification record

- `compileClientJava --offline` passed using Java 25, Minecraft 26.3, Fabric Loader 0.19.5, and Fabric API `0.161.0+26.3`.
- On October 2, `compileClientJava --offline` passed after integrating selective world-change cleanup and execution-time cancellation checks for static and streamed sounds.
- On October 2, `compileClientJava --offline` passed after integrating configurable disconnect continuation, automatic-transition handling, connection-failure and cancellation handling, and menu session maintenance. Language JSON validation passed for both locales.
- On October 2, in-game testing confirmed that the current track continues after disconnecting and the retained playlist advances after re-entering a world. `compileClientJava --offline` passed after extending vanilla music suppression across the playlist's menu waiting period.
- On October 2, `compileClientJava --offline` passed after adding menu-side completion handling for the final track of a non-looping playlist.
- On October 2, in-game reproduction testing confirmed that elytra wind is cleared after an ender-pearl transfer through a Nether portal while mod music is playing, resolving the persistent wind sound after flight ends.
- On October 2, packaged testing through a third-party launcher on Minecraft 26.3 with Fabric Loader 0.19.5 confirmed that manually paused music stays paused when closing a screen.

### Development runtime alignment with a verified third-party launcher environment

- On October 2, a development launch terminated after world entry with a Windows native access violation at Oracle Java 25.0.2 `jvm.dll + 0x9e7b97`. Used successful packaged testing through a third-party launcher as the reference environment for development launch configuration.
- Aligned development startup with the reference environment: JetBrains Runtime 25.0.2, a 5 GiB maximum heap, G1 settings, and compact object headers. Added the Minecraft 26.3 manifest parameters `-XX:StackShadowPages=32` and the `java.base/jdk.internal.misc` export.
- Configured both Gradle `runClient` and IntelliJ Minecraft Client with the aligned runtime. The local `dev-runtime.local.json` stores the Java executable and additional JVM parameters. Source launches use the Fabric development arguments and source classpath.
- Copied 12 native-library files from the reference environment into an independent project-local directory. SHA-256 checks matched every copied file to its source. Configured the development native extraction paths to use that directory and backed up the original IntelliJ configuration under `.gradle/runtime-backups`.
- After configuration, `compileClientJava --offline` passed and Gradle `runClient --offline` completed resource loading, OpenAL initialization, sound-engine startup, and atlas creation. Live JVM inspection confirmed JBR, the 5 GiB heap, G1, compact object headers, and `StackShadowPages=32`.
