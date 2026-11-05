# Word-level lyrics: enhanced LRC

`LyricsProvider.getAllLyrics()` values stay plain `String`s. Word timing is carried **inside**
that String as enhanced LRC, so there is no interface or Zipline ABI change.

## Grammar

```
line      = line-tag+ body
line-tag  = "[" mm ":" ss [ ("." | ":") frac ] "]"          ; same as line LRC
body      = [lead-text] ( word-stamp text )* [ word-stamp ]   ; trailing stamp = end of last word
word-stamp= "<" mm ":" ss [ ("." | ":") frac ] ">"
mm = 1-3 digits; ss = 2 digits; frac = 1-3 digits (1 = tenths, 2 = centis, 3 = millis)
```

Example (what the extensions emit):

```
[00:02.73]<00:02.73>I <00:03.06>found <00:03.51>a <00:03.57>love<00:09.09>
```

Rules (the app's parser, `LyricsUtils.parseLine`, is the reference):

- A stamp marks the **start** of the text after it. That text runs until the next stamp. The final
  stamp with no text after it is the **end** of the last word. Without it, the last word ends at
  the next line (see the tail lead below).
- Whitespace belongs to the preceding word (`"I "`), so concatenating the word texts gives the
  displayed line. The first word is left-trimmed and the last right-trimmed.
- Text before the first stamp starts at the line time.
- Word stamps are **absolute** times. `[offset:]` shifts them like line tags. A line under several
  line tags (`[00:10][00:50]chorus`) moves its words by the difference to the first tag.
- A line whose stamps carry no text is dropped, the same as an empty LRC line.
- **Not supported (yet):** duet / voice markers (`v1:`/`v2:`), background-vocal lines, and syllable
  stamps inside a word. Parsers that do not know them would show them as text, so providers must
  not emit them until the grammar is extended here and in the lib doc.

App-side copy (with rendering details): SoundBound `Docs/ARCHITECTURE/lyrics.md`.

## Backward compatibility: capability key

An app that predates C-31 would show `<00:02.73>` stamps as literal text. Providers therefore send
enhanced LRC **only** when the app says it can parse it:

- The app answers `DevicePreferences.getBooleanOrNull("soundbound.lyrics.wordLrc") == true`
  (the app answers it in code, it is not stored).
- Extensions check it (soundbound-extensions `WordLrc.CAPABILITY_KEY`). An older
  app returns null, so they send plain line LRC.
- An older extension bundle running on a new app just sends line LRC, which renders as before.

`DevicePreferences` is an existing bridged service, so this needs no new `bind`/`take`.

