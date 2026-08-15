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
