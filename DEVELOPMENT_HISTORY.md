# Development History

This document is an append-oriented record of user-visible features, compatibility decisions, data migrations, and notable behavioral changes in New Custom Music Client-Side Player.

It complements, rather than replaces, release notes and Git history:

- Release notes provide a concise overview for players.
- This document preserves the complete product-level history behind those highlights.
- Git commits remain the source for implementation details and individual code changes.

## Recording policy

- Version entries are ordered from oldest to newest. New entries are appended to the end of the document.
- Published entries should not be silently removed or rewritten.
- If a historical statement is found to be inaccurate, a dated correction should be appended to the relevant entry.
- Spelling, grammar, formatting, and broken links may be corrected directly when their meaning is unchanged.
- Pure refactors and other internal changes are recorded only when they affect compatibility, maintenance expectations, or observable behavior.

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
- Playback-volume keyboard shortcuts continue to change the value in five-percentage-point steps. When a non-multiple such as 98% is increased, it first reaches the 100% boundary; only a subsequent attempt reports that the maximum has already been reached.
- Playback-volume shortcuts are applied only while no screen is open, preventing configuration drafts and Minecraft's keyboard navigation from interfering with one another.
- Existing playback-volume data is migrated silently on startup. The legacy file is retired only after the unified configuration has successfully taken ownership of the value.
- Migrated and loaded volume values are constrained to the supported 0-100 range.
- Missing fields in an otherwise valid configuration are restored from defaults and written back, so installations created by older versions acquire newly introduced settings without requiring a manual reset.
- Configuration import and export now include playback volume and validate it as an integer from 0 to 100.
- Configuration-import warnings use reliable labels from the active Minecraft language for known settings instead of exposing internal JSON field names.
- Applying a configuration draft updates the active playback volume immediately. Failed persistence retains the in-memory value and can be retried during shutdown.

### Unapplied configuration changes

- Leaving a modified configuration screen through Back or Esc now opens an unapplied-changes warning instead of silently discarding the draft.
- The warning lists the settings that would be discarded in the same order as the configuration screen. Each entry shows the applied value, a right arrow, and the draft value with distinct colors for easier comparison.
- The warning shows only fields whose draft values actually differ from the applied configuration.
- **Keep Editing** returns to the exact draft screen and is the initially focused action. **Discard Changes** immediately confirms the user's choice and exits without applying the draft.
- Esc-based discard is guarded for three seconds. During the countdown, Esc cannot dismiss the warning; after it finishes, pressing Esc again discards the changes directly instead of returning to the draft.
- The centered countdown message identifies the Esc action explicitly, emphasizes the remaining seconds, and changes to a ready-state message when the guard expires.

### Shared warning-screen presentation

- Warning content now uses a dynamically sized bordered panel for up to twelve wrapped lines.
- Longer warning content switches to a scrollable list while keeping the same overall screen structure.
- Short one- or two-line warnings no longer receive a misleading scrollbar or lose part of their final text line because of text-area padding.
- The shared warning-screen foundation now supports an optional centered dynamic subtitle and a screen-defined initial button focus.

### Custom single-track names

- Added import and export icon buttons to the single-track selection screen.
- Export creates a mod-generated text transfer file containing custom track-name mappings. Entries that still use their original IDs are omitted because they require no stored override.
- Import previews and validates the complete file before changing local data. Unreadable, empty, malformed, or wrong-mod files stop without modifying existing mappings.
- Recoverable warnings cover invalid sound-event IDs, duplicate IDs, non-text values, empty or overlong names, redundant default-name entries, same-namespace conflicts, and replacements of existing custom names.
- The warning preview distinguishes namespaces, paths, existing names, and imported names with the same color-oriented presentation used by related music-management screens.
- A mapping is retained when its sound event is absent from the currently loaded resource packs. Availability alone neither generates a warning nor causes the mapping to be skipped, allowing transfer files to preserve names for optional or temporarily disabled packs.
- Confirming an import writes the compatible mappings atomically, refreshes the namespace currently being viewed, and clears the previous row selection so controls cannot operate on stale list state.
- The success toast reports the number of mappings whose stored names actually changed.
- Repository writes use a temporary file and replacement step so an interrupted or failed save does not intentionally overwrite the last valid mapping file.

### Single-track selection interface

- Reorganized the bottom actions into a consistent two-by-two, 150-pixel button layout: **Play Selected Track**, **Rename Track**, **Restore Default Name**, and **Back**.
- Kept Play as the upper-left action to preserve the most familiar primary-action position.
- Added **Restore Default Name**, which removes the selected track's custom mapping and displays its original sound-event ID again.
- Restore is available only when the selected track currently has a custom name. Its explanatory tooltip is shown only while the button is available; routine disabled states do not add redundant tooltips.
- Reduced the search field's vertical footprint to preserve list space after adding the second row of actions.

### Playlist naming and transfer consistency

- Playlist-name conflict checks in graphical create and rename screens now use exact, case-sensitive comparison, matching command and storage semantics.
- Playlist import collision handling follows the same rule. Names that differ only by letter case can therefore coexist consistently instead of being rejected by one interface and accepted by another.
- Replaced the outdated graphical naming hint that implied playlist names were restricted to letters, numbers, and underscores. Graphical names continue to accept spaces, Unicode characters, punctuation, and underscores within the existing length rules.
- Standardized user-visible before-and-after changes on the Unicode right arrow (`→`) instead of mixing it with the older ASCII `->` representation.

### Localization and release metadata

- Added and reviewed English and Simplified Chinese text for playback volume, track-name transfer, restore-default-name behavior, unapplied-change warnings, countdown states, and detailed configuration differences.
- Added dedicated translation keys for new shared behaviors instead of reusing keys whose names incorrectly implied that they belonged to an unrelated screen.
- Updated the project version to `1.4.4-beta2` for community testing before the next stable release.
