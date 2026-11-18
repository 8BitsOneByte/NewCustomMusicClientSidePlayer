# Development History

This document is an append-oriented record of user-visible features, compatibility decisions, data migrations, and notable behavioral changes in New Custom Music Client-Side Player.

It complements, rather than replaces, release notes and Git history:

- Release notes provide a concise overview for players.
- This document preserves the complete product-level history behind those highlights.
- Git commits remain the source for implementation details and individual code changes.

## Recording policy

- Version entries are ordered from oldest to newest. New entries are appended to the end of the document.
- Before a version is published, its draft entry may be expanded or corrected as implementation and testing continue. After publication, the entry follows the historical-preservation rules below.
- Published entries should not be silently removed or rewritten.
- If a historical statement is found to be inaccurate, a dated correction should be appended to the relevant entry.
- Spelling, grammar, formatting, and broken links may be corrected directly when their meaning is unchanged.
- A version entry is complete only when it records every relevant user-visible behavior, data format, migration rule and priority, validation boundary, error or warning branch, conflict-resolution rule, failure protection, and important interaction detail introduced or changed by that version.
- Exact limits, ordering, fallback behavior, and persistence guarantees should be stated whenever users, resource-pack authors, translators, or maintainers could depend on them later.
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
- Changing or applying the volume also updates the base volume of a currently playing mod-owned sound instead of waiting for the next track.
- Existing playback-volume data is migrated silently on startup. If `config.json` has no playback-volume field, the legacy value is used; a valid volume already owned by `config.json` takes priority.
- When migration succeeds, `playback_volume.json` is renamed to `playback_volume.json.migrated`. An existing migrated file is never overwritten, and the legacy source is not deliberately removed until the unified configuration has successfully taken ownership of the value.
- If a new unified configuration cannot be saved, the legacy file is kept, its value remains usable in memory, and migration can be attempted again on a later launch. An unreadable or structurally invalid main configuration likewise does not trigger retirement of the legacy source.
- Migrated and loaded numeric volume values are constrained to the supported 0-100 range. A non-integer or otherwise unreadable stored volume falls back to the default of 100%.
- Missing fields in an otherwise valid configuration are restored from defaults and written back, so installations created by older versions acquire newly introduced settings without requiring a manual reset.
- This missing-field repair covers update checks, the now-playing toast, playback feedback mode, and playback volume while preserving other members of the existing JSON object during the repair write.
- Configuration import and export now include playback volume. Import accepts only an integer from 0 to 100; an invalid value is skipped through the normal recoverable configuration-warning flow rather than replacing the draft.
- Configuration-import warnings use reliable labels from the active Minecraft language for known settings instead of exposing internal JSON field names.
- An unknown skipped field still falls back to its literal transfer-file field name because no reliable localized setting label exists for it.

### Unapplied configuration changes

- Leaving a modified configuration screen through Back or Esc now opens an unapplied-changes warning instead of silently discarding the draft.
- The warning lists the settings that would be discarded in the same order as the configuration screen. Each entry shows the applied value, a right arrow, and the draft value with distinct colors for easier comparison.
- The warning shows only fields whose draft values actually differ from the applied configuration.
- Setting labels are aqua, applied values are gray, arrow punctuation is dark gray, and draft values are yellow. Boolean values reuse Minecraft's localized On/Off text, feedback modes reuse their existing translated labels, and volume values include a percent sign.
- **Keep Editing** returns to the exact draft screen and is the initially focused action. **Discard Changes** immediately confirms the user's choice and exits without applying the draft.
- Explicitly clicking **Discard Changes** is not delayed by the countdown.
- Esc-based discard is guarded for three seconds. During the countdown, additional Esc presses do nothing; after it finishes, pressing Esc discards the changes directly instead of returning to the draft.
- The countdown is centered directly below the title and enclosed in gray parentheses. Remaining seconds are yellow, `Esc` is aqua and bold, and the ready-state instruction is yellow.

### Shared warning-screen presentation

- Warning content now uses a dynamically sized bordered panel for up to twelve wrapped lines.
- Longer warning content switches to a scrollable list while keeping the same overall screen structure.
- Short one- or two-line warnings no longer receive a misleading scrollbar or lose part of their final text line because of text-area padding.
- The shared warning-screen foundation now supports an optional centered dynamic subtitle and a screen-defined initial button focus.
- The sizing correction applies to every screen using this shared presentation, including playlist import, configuration import, track-name import, and the new unapplied-change warning.

### Custom single-track names

- Added import and export icon buttons to the single-track selection screen.
- The transfer buttons are disabled in fullscreen because the operating-system file dialog is unavailable there. Their tooltip changes to a dedicated track-name-transfer instruction to disable fullscreen; this does not reuse the configuration screen's translation key.
- Both dialogs filter for `.txt` files. Closing either dialog without selecting a path leaves all data unchanged, and a dialog result is ignored if the user has already left the single-track screen before the asynchronous result returns.
- The default export filename combines the mod name and mod version with `-track-names.txt`, replacing characters that are unsafe in filenames.
- Export creates a UTF-8, human-readable structured text file containing format version 1, the mod ID, mod version, Minecraft version, export time, and the custom track-name map.
- Entries that still use their original IDs are omitted because they require no stored override. Export remains available when there are no custom mappings and produces a valid transfer file with an empty map.
- Successful export reports the normalized destination path in a success toast. A file-write failure reports the selected path in a failure toast, while failure to open the save dialog uses a separate failure message.
- Import previews and validates the complete file before changing local data. A missing, non-file, empty, inaccessible, or otherwise unreadable source opens the read-error screen.
- A non-object root, malformed or trailing JSON, duplicate top-level `modId` or `trackNames` members, a non-text `modId`, a non-object `trackNames` value, a missing required member, or a different mod ID opens the invalid-file screen.
- Version and export metadata are informational in format 1. Import identifies compatibility from the mod ID and `trackNames` object and ignores other top-level fields, allowing metadata additions without invalidating otherwise compatible files.
- Recoverable warnings are ordered as invalid sound-event IDs, duplicate IDs, non-text values, empty names, names longer than 50 characters, redundant default-name entries, same-namespace name conflicts, and replacements of existing custom names. A category is displayed only when it actually occurs.
- Imported text names are trimmed before empty, length, default-name, and conflict checks. When an ID occurs more than once in the transfer object, every occurrence of that ID is skipped rather than choosing one silently.
- Invalid, duplicate, non-text, empty, overlong, redundant-default, and conflicting entries are skipped while compatible entries remain eligible for import. If no compatible entry remains, the operation stops on a dedicated screen without writing local data.
- Same-namespace display-name conflicts are compared without letter-case differences. Existing or otherwise unchanged names take priority; when only imported candidates collide, the earlier transfer entry keeps its candidate name and later candidates are rejected, with conflicts recalculated until the final set is stable.
- The warning preview distinguishes namespaces, paths, existing names, and imported names with the same color-oriented presentation used by related music-management screens.
- A warning preview requires confirmation before applying compatible entries. A clean preview with compatible entries applies directly without inserting an unnecessary confirmation screen.
- A mapping is retained when its sound event is absent from the currently loaded resource packs. Availability alone neither generates a warning nor causes the mapping to be skipped, allowing transfer files to preserve names for optional or temporarily disabled packs.
- Confirming an import writes the compatible mappings atomically, refreshes the namespace currently being viewed, and clears the previous row selection so controls cannot operate on stale list state.
- The success toast reports the number of mappings whose stored names actually changed. An otherwise compatible mapping that already has the imported value counts as zero changes and does not by itself cause a repository rewrite.
- The track-name repository now reads and writes explicitly as UTF-8. Rename and import operations prepare a separate updated map, write it through a temporary-file replacement, and update the in-memory map only after that write succeeds.
- If loading the existing repository throws a read or parse error, rename, restore, export, and import operations fail instead of treating the repository as empty and overwriting the unreadable data.
- Import-save failure opens a dedicated error screen and leaves the existing stored and in-memory names unchanged. Restore failure sends a translated system message, and export failures use the toast paths described above.

### Single-track selection interface

- Reorganized the bottom actions into a consistent two-by-two, 150-pixel button layout: **Play Selected Track**, **Rename Track**, **Restore Default Name**, and **Back**.
- Kept Play as the upper-left action to preserve the most familiar primary-action position.
- Added **Restore Default Name**, which removes the selected track's custom mapping and displays its original sound-event ID again.
- Restore is available only when the selected track currently has a custom name. Its explanatory tooltip is shown only while the button is available; routine disabled states do not add redundant tooltips.
- Play and Rename require any selected track; Restore additionally requires that selection to have a custom name. Refreshing entries after rename, restore, or import clears stale selection state and recalculates all three button states.
- Fixed the search field at 20 pixels high and increased the footer to accommodate the second action row. The transfer buttons sit beside the search field while balancing space keeps the search control visually centered.
- The track list now fills all remaining content height instead of enlarging the search field or reserving unused space merely to show only complete 36-pixel rows.

### Playlist naming and transfer consistency

- Playlist-name conflict checks in graphical create and rename screens now use exact, case-sensitive comparison, matching command and storage semantics.
- Playlist import collision handling follows the same rule. Names that differ only by letter case can therefore coexist consistently instead of being rejected by one interface and accepted by another.
- A rename that changes only letter case is therefore allowed unless another playlist already has that exact target spelling. An unchanged exact name continues to be rejected as the original name rather than treated as a rename.
- Exact import collisions continue to use the established automatic numeric-suffix renaming. A case-only difference no longer triggers that automatic rename.
- Replaced the outdated graphical naming hint that implied playlist names were restricted to letters, numbers, and underscores, and removed the unused invalid-character translation. Graphical names are trimmed, must contain 1-50 characters, and may contain spaces, Unicode characters, punctuation, and underscores.
- Command arguments continue to use exact, case-sensitive playlist names and suggestions continue to insert the stored spelling, so the graphical and command paths now agree on case-only names.
- Standardized user-visible before-and-after changes on the Unicode right arrow (`→`) instead of mixing it with the older ASCII `->` representation.

### Localization and release metadata

- Added and reviewed English and Simplified Chinese text for playback volume, track-name transfer, restore-default-name behavior, unapplied-change warnings, countdown states, and detailed configuration differences.
- Added dedicated translation keys for new shared behaviors, including the track-name fullscreen requirement, instead of reusing keys whose names incorrectly implied that they belonged to an unrelated screen.
- Updated the project version to `1.4.4-beta2` for community testing before the next stable release.

## 1.4.4 post-beta2 stabilization

**Status:** In progress — between `1.4.4-beta2` and stable `1.4.4`

This entry records small, low-risk maintenance completed while `1.4.4-beta2` remains the community test build and before stable `1.4.4` is published. These changes are part of the existing stabilization period and do not by themselves define or require a separate `1.4.4-beta3` release.

### Repository default-branch migration

- Migrated the main GitHub repository from the legacy `master` branch name to `main` without merging the two unrelated histories or force-overwriting either history.
- Before the migration, preserved the former placeholder `main` at commit `8528521` as `archive/legacy-main-before-migration` and created the annotated `pre-main-migration` tag at `bf3b7a0` to preserve the complete project state immediately before the branch-name change.
- Temporarily selected the complete `master` history as the default, removed the placeholder `main`, and then used GitHub's branch-renaming operation to rename the complete branch to `main`. The repository default branch now points to the complete project history.
- Renamed the local development branch to `main`, updated its upstream to `origin/main`, and updated `origin/HEAD` so new clones and ordinary Git operations consistently select the new default branch.
- Updated the source-code URLs stored in Fabric mod metadata and the in-game configuration screen from `/tree/master` to `/tree/main`.
- Updated the main-repository links on the Wiki home page from `/blob/master` to `/blob/main`. The Wiki remains a separate Git repository whose own branch is intentionally still named `master`.
- Updated the external Modrinth project-level source URL from the branch-specific `/tree/master` path to the repository root. The root URL follows the current default `main` branch automatically and avoids retaining another stale branch-specific link if the default branch name changes again.
- Immediately afterward, made the repository-owned source links equally resilient by replacing the branch-specific `/tree/main` URLs in Fabric mod metadata and the in-game configuration screen with the repository root.
- Changed the Wiki's direct development-history and license links from `/blob/main/...` to `/blob/HEAD/...`, preserving their exact file destinations while allowing GitHub to resolve them through whichever branch is currently the repository default.
- This migration changes repository navigation and source links only. It does not alter playback behavior, configuration data, migrations, transfer formats, translations, or other runtime behavior in the mod.

### Stale Copilot branch cleanup

- Removed the unprotected `copilot/fix-ghost-instances` and `copilot/evaluate-potential-bugs` remote branches after verifying that neither branch was associated with an open, closed, or merged pull request.
- `copilot/fix-ghost-instances` pointed only to the former placeholder repository commit already retained by the migration archive and contained no independent implementation of the later ghost-instance fix.
- `copilot/evaluate-potential-bugs` contained no standalone analysis report or applied bug fix. Its project snapshot was already represented in the formal history by commit `c4301aa`, apart from a redundant additional `LICENSE` file.
- Removing these stale branch names does not change `main`, release tags, the migration recovery points, published artifacts, or any player-facing behavior.

### Playlist playback availability feedback

- Kept the playlist-management screen's **Play Selected Playlist** action available for any selected playlist, including an empty or currently unplayable one. Users can therefore still open the play-instance screen to inspect the saved order, see unavailable tracks marked in the preview, choose a starting index, and review the loop and shuffle controls.
- The play-instance screen now disables only its **Start Playing** action when the selected settings cannot produce a playable track, replacing the normal GUI path's easy-to-miss chat feedback behind the blurred screen with a directly associated button tooltip.
- Preserved the existing empty-playlist and invalid-start-index validation, messages, and tooltip behavior. Availability validation runs only after those structural input checks have succeeded.
- When the complete playlist contains no currently playable sound, **Start Playing** is disabled in both loop modes and its unstyled tooltip reuses the existing localized `no_playable_sound` message.
- When loop mode is disabled and the playlist has playable tracks only before the selected starting index, **Start Playing** is disabled with a dedicated tooltip explaining that no playable track remains between the selected starting position and the end of the playlist.
- Complete unavailability is checked before the narrower non-loop starting-position condition. A playlist with no playable track from index 1 through the end therefore always receives the general no-playable-playlist message instead of being misclassified as only a starting-position problem.
- When loop mode is enabled, any playable track anywhere in the playlist is sufficient because playback may wrap around to an earlier position. When loop mode is disabled, at least one playable track must exist at or after the selected starting index.
- Partially playable playlists remain supported. Unavailable entries continue to be marked in the preview and skipped by the playback queue, while a valid playable candidate keeps **Start Playing** available.
- Availability is recalculated when the screen initializes, the starting-index text changes, or the loop or shuffle state changes. It is also recalculated immediately before the start action invokes the playback controller, preventing a stale enabled button from starting playback after resource availability changes while the screen remains open.
- The playback controller retains its independent empty, availability, queue-position, and final-start checks as defense for unexpected callers or state changes. Under ordinary GUI interaction the screen validation prevents these failure paths, while the controller remains able to reject an invalid request safely if the UI is bypassed.
- Command-based playlist playback retains its chat error feedback. All command paths that report the no-playable-sound condition now apply explicit red styling, while the corresponding disabled-button tooltips remain the normal white tooltip color.
- Added reviewed English and Simplified Chinese text for the non-loop starting-position condition. This change does not modify playlist storage, track ordering, import/export formats, configuration data, or the playback queue's established filtering behavior.

## Minecraft 26.2 backport — Part 1

- Begin backport preparation for Minecraft 26.2 as Minecraft 26.4 approaches and the release timing of 1.5.3 remains uncertain.

### Isolated development environment

- Configure a dedicated Minecraft 26.2 development instance with Java 25, an external game directory, and separate build outputs and project caches.
- Enable the unobfuscated Minecraft mode for Loom 1.15.5 and resolve the Fabric dependencies required by the client runtime.
- Defer run-task validation until execution so IntelliJ's coroutine debugging initialization can resolve the client classpath within Gradle's task configuration context.
- Pass the game directory to Loom as a project-relative path, preventing Windows drive paths from being appended to the project directory twice.
- Reproduce the IntelliJ initialization failure with its debugger scripts, then verify successful task-graph preparation after the adjustment.
- Launch Minecraft 26.2 with Fabric Loader 0.19.3 and mod version 1.4.5-beta1 in the isolated instance; resource loading and sound-engine startup complete, and the client reaches the interactive main menu.

## 1.4.5-beta1 — Minecraft 26.2 backport

### Playback foundation

- Backported the 1.5.2 playback, storage, and interface behavior to Minecraft 26.2. Mod and transfer metadata identify this version as `1.4.5-beta1`.
- Added per-instance filtering to the global resume path. It skips manually paused mod-owned channels and cancelled handles in the captured handle set, and resumes eligible vanilla channels when containers, inventory, chat, or configuration screens close.
- Pause and resume operations publish the instance's playback intent before queuing a native operation. The sound executor reads that intent when executing the task; explicit manual pause queues the native pause operation.
- Added logical manual-pause state covering loading, prepared, playing, and paused channels. Audio prepared during manual pause attaches to its channel and waits for explicit continuation.
- Continuation resumes an already-started paused channel from its current position, starts a prepared initial channel, or permits a loading channel to start after preparation. Repeated resume requests continue the current position.
- Cancelled instances enter a terminal state. Late static-buffer results are discarded; late streamed results are closed on the sound executor before attachment. Channel release owns cleanup of attached streams, while shared static buffers retain their shared lifecycle.
- Starting a replacement track or a new single-track or playlist session clears manual pause. Stopping a session clears the controller's pause state immediately.
- Recover ended instances before the paused-session early return. If a paused session loses its current channel after a sound-engine or output-device reload, clear the session and its vanilla-music suppression instead of advancing the paused playlist.
- Replaced the world-change shortcut that skipped global sound cleanup. Retain the exact current music instance and its active, uncancelled channel handle, then clean up the other sounds; use the original full-stop operation when the retained pair is unavailable.
- Publish cancellation before removing discarded handles from engine indexes. Clear their instance mappings, source entries, delayed playback, deletion deadlines, tickable queues, and cached source gains.
- Sweep the actual handle set on the sound executor, retaining the selected handle and releasing every other handle, including handles absent from engine indexes. Remove a handle from the set before releasing it to share cleanup ownership with queued cancellation.
- Apply selective cleanup to departing-world sounds such as elytra wind, ambience, minecart loops, and entity-bound sounds, including their scheduled restarts. Playing, paused, and loading mod music keeps its channel, position, and pause intent.
- Stop local jukebox sounds on world departure and follow vanilla jukebox playback events on re-entry.
- Added **Keep Playing in Game Menu**, defaulting to OFF. ON retains the current music instance, playback position, pause intent, and playlist queue when leaving a world; OFF stops owned sounds and clears the session.
- Apply the disconnect decision synchronously at teardown, covering pause-menu exits, low-level connection closure, kicks, and connection loss. Reconfiguration and server-directed transfer retain the session while moving to the destination.
- Scope temporary server-transfer disconnect handling around the screen display operation and release it in a `finally` block. Distinguish destination preparation with an empty world from an ended connection.
- Address-resolution failures, login failures, and explicit connection cancellation apply the menu setting. Transition paths where vanilla skips sound stopping still perform selective cleanup.
- In menus, recover retired sounds and release finished instances. A retained playlist waits for world entry before advancing; finishing the last track of a non-looping playlist clears that session on the next client tick. Valid paused instances retain their pause intent.
- Replaced the MUSIC-category volume lock with suppression of the vanilla music manager's scheduled tick and direct start operation. A current managed track or active playlist session keeps suppression active during playback, manual pause, and menu waiting.
- Publish the new managed instance before stopping vanilla music and submitting audio loading. Cancel the vanilla manager's current channel before stopping it so late audio preparation cannot restart it, then request release of the cancelled handle.
- Use vanilla stopping to clear its tracked instance, dismiss its music toast, and reset its scheduling delay. Once the custom session ends, ordinary scheduling resumes with the configured MUSIC volume intact.
- Managed playback uses MASTER combined with the mod's independent playback-volume percentage. The Java runtime dependency now explicitly requires Java 25 or newer.

### Storage, resource sources, and commands

- Applied mandatory atomic replacement to playlist, configuration, and custom-name saves: write complete UTF-8 JSON to a unique temporary file in the destination directory and publish it with an atomic move.
- Unsupported atomic replacement or a failed move reports a save failure and preserves the previous destination. Attempt to remove the temporary file after a failed save; configuration saves attach cleanup failures to the primary exception.
- Configuration Apply retains the draft and latest applied baseline when persistence fails. Custom-name edits publish their in-memory result after successful storage.
- Added an in-memory catalog of effective `sounds/**/*.ogg` resources from vanilla assets and enabled resource packs, including files without a registered sound event. Each entry retains its complete resource ID, namespace, and supplying pack ID.
- Resource-manager priority resolves identical IDs before indexing. Reload publishes a complete new directory during apply, with metadata initially pending; resource collections belong to one generation.
- Parse metadata on one daemon worker, retaining at most one active job and the latest pending job. Replace superseded pending work rather than accumulating requests.
- Read Ogg pages incrementally, checking capture patterns, version, flags, sequence, continuation, checksums, and granule progression. Identify Vorbis headers, sample rate, and channel count, and derive duration from final sample count without PCM decoding.
- Explicit parser buffers total 328,505 bytes under a 1 MiB working-buffer limit. Record `PENDING`, `READY`, `INVALID`, `UNSUPPORTED`, or `IO_ERROR` per resource; unsupported chained or multiplexed layouts and individual file failures do not stop the remaining scan.
- Reparse metadata after reload, reuse immutable identifiers where possible, and cancel obsolete generations. Close resource streams after success, failure, or cancellation, and discard results from obsolete jobs.
- Failed reload and client shutdown cancel their metadata work; shutdown also closes the catalog and worker. Playback opens separate streams, and catalog storage remains in memory.
- Local playlist storage and exports use **format version 3**, grouping playlists by `SOUND_EVENT` and `OGG_RESOURCE`. Names and ordering are independent and case-sensitive within each source; each list retains its source, ordered tracks, and modification time.
- Accept legacy version 1 and version 2 playlists and migrate them into source groups. Legacy arrays and objects become sound-event lists; version 2 entries use their explicit source. Missing local times fall back to the nonnegative source-file modification time.
- Preserve original bytes before migration: version 1 uses `playlists.json.migrated`, and version 2 uses `playlists.json.v2.migrated`. Reuse an identical archive; a conflicting archive rejects migration before replacing the stored document.
- Preserve unknown root members, unknown source groups, unreadable entries, and unedited object members. Legacy entries with unknown sources remain in `UNKNOWN_LEGACY`; exports carry retained raw data.
- Keep readable lists usable when other entries cannot be interpreted. Unreadable names remain occupied and cannot be edited through the typed API; whole-document read failures block ordinary storage operations instead of exposing empty replaceable data.
- Rename preserves list order and checks the destination name in one storage operation. Import rechecks all destination names and publishes one merged batch, protecting names created after the preview.
- Compare the source file with the originally read bytes before replacement; external edits or deletion reject the save. Migration repeats this comparison after archiving and before publication.
- Playlist import accepts legacy, version 2, and version 3 transfers. Unsupported future versions, invalid version values, and repeated `formatVersion` declarations reject the file.
- Skip lists with unusable names or unknown sources while retaining compatible lists. Trim names, enforce the 50-character limit, and resolve exact collisions within each source with `(2)`, `(3)`, and later available suffixes, shortening the base when required.
- Validate track identifiers and their source-specific shape. OGG IDs include `sounds/` and `.ogg`; valid IDs remain importable when their resource pack or event is currently absent. Invalid tracks are removed with preview details while valid order is retained.
- Empty playlists remain importable. Missing `tracks` gives an empty list; a non-array value adds the unreadable-track-list warning. Filtering all invalid tracks still leaves an importable empty list.
- Missing or invalid imported modification times use zero and retain the warning. Nonnegative future timestamps remain supported. An import with no compatible lists opens the existing no-compatible-playlists error.
- Preserve the streaming reader's handling of repeated top-level `playlists` and `modId`: later readable values replace earlier readable values, and wrong-type values are skipped. Repeated list names enter source-specific collision handling.
- Group recoverable warnings with Ogg Files before Sound Events, using gold and green source labels. Display only source sections with warnings; compatible clean-source lists remain part of the same import.
- Keep skipped-list counts above source sections and preserve unreadable-track, invalid-ID, empty-ID, invalid-time, and renamed-list details. Short warnings use the compact panel and long warnings use the scrollable presentation.
- Unsupported future or repeated versions use the version-specific error body under the existing invalid-file title. Resource availability and metadata readiness do not add import warning categories.
- Custom-name storage and exports use **format version 2**, with separate `SOUND_EVENT` and `OGG_RESOURCE` maps in `track_names.json`. The same resource ID can have independent names in both sources.
- Migrate valid unversioned and version 1 local names into the event group, preserving root data and archiving original bytes in `track_names.json.migrated` before atomic publication.
- Invalid legacy names, conflicting migration archives, unsupported local versions, and load failures protect the existing file from replacement. A readable source group remains editable when another known group is unreadable; writes carry other raw data forward.
- Name lookup, rename, restoration, and batch import use the selected source. Older external flat exports become sound-event mappings without modifying the selected external file.
- Retain name validation for non-text, empty, overlong, redundant-default, duplicate-ID, same-namespace conflicts, and replacements. Trim imported names; IDs repeated within one source are all skipped, while the same ID in different sources remains independent.
- Import known name groups as one batch. A validation or save failure prevents partial publication, identical mappings avoid another save, and comparison with original bytes rejects detected external changes.
- Skip unknown groups in external name imports while preserving unknown groups already stored locally. A malformed known group, repeated group declaration, or unsupported version rejects the external file.
- Group name warnings by OGG then sound events, list unknown groups above those sections, and retain the existing no-compatible-names flow. Cancel and Esc leave stored mappings unchanged.
- Added the preferred track-source setting. New installations default to OGG; existing configurations with a missing or invalid choice retain sound events. A valid stored choice takes priority.
- Before initialization, inspect playlist, name, legacy volume, and corresponding migration files for existing-data evidence. Existing or not-confirmed-absent paths select sound events; unreadable or non-object configuration also uses compatible sound-event defaults.
- Include source and menu-continuation settings in persistence, missing-field repair, draft comparison, import, export, Apply, reset, discard details, and widget synchronization. Missing or invalid imported fields retain their draft values.
- Added `/tracksource` query and `sound_events` / `ogg_files` selection with completion. Save a changed preference before applying it; selecting the current value reports that it is already active.
- Adapted single-track and playlist commands to source-specific identifiers, playlist lookup, and completion. Direct OGG playback resolves the exact file and streams it through the managed instance.
- Each playback session captures its source. Later preference changes affect subsequent sessions; pause, resume, stop, looping, relative skips, random skips, and shuffle use the captured queue and source.

### Browsing, editing, and playback presentation

- Single-track and playlist screens capture the applied source on opening. Carry it through selection, renaming, restoration, viewing, editing, playback preview, and playback confirmation.
- Sound-event mode lists registered events; OGG mode lists effective catalog resources. Both use the existing namespace selector, sorted namespace options, search field, and controls.
- Display a two-line source indicator at the upper left: a white heading followed by Sound Events in green or Ogg Files in gold. Row descriptions identify a sound event or music file and retain the namespace argument.
- Generate OGG default names from the final filename without `.ogg`. Detect filename conflicts within a namespace using the complete effective catalog, independently of search and custom names.
- Ambiguous filenames display their full path including `sounds/` and `.ogg`. Identical resource IDs overridden by packs contribute one effective entry; different namespaces do not expand each other's names.
- Sort OGG resources by filename, then nearest parent directory and successive parents. Shorter matching suffix paths come first, with namespace as the final comparison.
- After catalog replacement, refresh open browsers and available editor resources. Preserve the selected namespace when available and otherwise select the first; retain saved playlist order.
- Renaming changes the source-specific display-name mapping. **Restore Default Name** removes that mapping and regenerates the current catalog label without saving it as a custom name or applying custom-name length and conflict validation.
- Display custom track names in yellow in the single-track browser, editor, playback preview, and View Playlist Tracks. The single-track row number also remains yellow; default names use white.
- Highlight visible search matches in green, including overlapping occurrences. Searches are case-insensitive, Unicode-aware, and literal, so punctuation does not become a regular-expression operator.
- Search original resource paths and current source-specific names. Hidden-path-only matches keep the displayed custom name and its ordinary color; no hidden path is added to the row. Row numbers remain outside highlighted text.
- Apply the same matching to playlist names and the editor's available tracks. Green takes precedence inside custom names, and empty or whitespace-only queries show the ordinary list.
- Clip overflowing names at the row's visible boundary in the playlist browser, single-track browser, track view, playback preview, and both editor columns. Intersect the row with the list viewport and screen, and stop at an overlapping visible scrollbar.
- Display a complete-name tooltip only over clipped visible text. Preserve search and custom-name styling; on the name line, the full-name tooltip takes precedence over unavailable-track warnings.
- Use the same bounded 32-by-32 icon regions for playlist-editor hover and click handling, including half-icon controls. Clicking track names or descriptions no longer activates add, remove, or reorder actions.
- Replace the toast toggle with ON, ON++, and OFF. ON shows the temporary song-start toast; ON++ additionally displays the current track at the upper right of the pause menu, with the existing English and Chinese mode tooltips.
- Read the title from the active instance and captured source. Update it when tracks change, hide it during custom pause, and clear it after stopping, finishing, or channel invalidation. Resume restores the pause-menu display without a new temporary toast.
- Reuse the temporary toast on track changes, refreshing its title and duration. The pause-menu card appears immediately and replaces the temporary card while ON++ is shown there; both retain the 200-pixel width limit and ellipsis.
- Store `nowPlayingToastMode`; migrate the old boolean and old configuration imports to ON or OFF. Export the selected mode and retain the draft on invalid imported values.
- All six settings participate in draft editing, dirty detection, applied-to-draft discard details, defaults, imports, and widget synchronization. Keep Editing preserves the draft; successful Apply establishes the baseline restored by later discard.
- Use Minecraft 26.2's fullscreen and link APIs, rendering pipeline, direct-sound resolution, and native file dialogs while retaining source-specific screen and transfer behavior.

### Verification on Minecraft 26.2

- A non-clean build passed with Java 25.0.2, Minecraft 26.2, Fabric Loader 0.19.3, and Fabric API `0.152.1+26.2`.
- The independent 26.2 development client loaded `1.4.5-beta1`, reached the main menu, initialized its sound engine, and entered a test world.
- The configuration, playlist-storage, custom-name, and discard-formatter harnesses passed 155 checks: 62 configuration checks, 21 playlist-storage checks, 32 custom-name checks, and 40 formatter checks.
- In-game configuration testing imported changes to all six fields, opened six applied-to-draft details through Esc, kept the draft, and applied it successfully.
- Resetting after Apply produced six reverse changes through Back. Explicit discard and reopening restored all six values to the latest applied baseline.
- Imported two playlists with the same name in different source groups and started playback from the sound-event playlist.
- Source comparison covered all 113 files under `src`, including added files. The seven substantive differences from the 1.5.2 reference are Minecraft 26.2 platform adaptations; both bundled language files and other resources match the reference.
- Direct OGG browsing filtered by resource path and started a selected vanilla music file. ON++ displayed its title in the pause menu, hid it after manual pause, and restored it after resume.
- Before a subsequent build, the isolated Gradle initialization archived the existing versioned jar and sources jar and verified both SHA-256 hashes; successful builds also completed the existing post-build archive.
- Reimporting same-name lists displayed separate OGG and sound-event rename warnings, then preserved both source groups. The OGG editor retained its two tracks when the track-name area was clicked; its playback preview used the corresponding file names.
- Started an OGG playlist and used `/skip` to advance from `beginning_2` to `floating_trees`. With menu continuation enabled, leaving and re-entering the test world retained the active single-track session.
- Reloading resources during playback completed resource and sound-engine initialization, returned to the world, and cleared the inactive track from the pause-menu display.
- Started `xusong:what_a_surprising_face` in SingleTrack Mode with looping enabled at pitches 1.0 and 0.92.
- Published [1.4.5-beta1](https://modrinth.com/mod/new-custom-music-client-side-player/version/j3EgVfhv) on Modrinth as a Fabric beta for Minecraft 26.2 on November 19, 2026 (UTC+8).

### Minecraft version branches and backports

- On September 23, 2026, we preserved the final Minecraft 26.2 source on the local `26.2` branch before moving development to Minecraft 26.3. On September 24, we documented this approach for future migrations.
- On November 14, 2026, we adopted a branch for each Minecraft version as its main development cycle ends. `main` tracks the latest Minecraft version from Mojang. We maintain older versions on `mc-<version>` branches for backports and fixes. We created `mc-26.1.2` and `mc-26.2` that day.
- On November 19, 2026, we completed the backport of the mod’s 1.5.2 feature set from Minecraft 26.3 to Minecraft 26.2 as `1.4.5-beta1`.
