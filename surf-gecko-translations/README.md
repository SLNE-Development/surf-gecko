# surf-gecko translations

`lang/<language>/*.json` — one folder per language (`de_de`, `en_us`). All files of a language are merged, so
keys are namespaced (`game.`, `shop.`, `lobby.`, …). A value is a string or, for multi-line texts such as item lore,
an array of strings. Missing keys fall back to `de_de`.

The server fetches these files from GitHub on startup and on `/i18n reload` (repository, branch and path are set in
`config.yml` under `translations`). If GitHub cannot be reached, the last fetched copy (`translations/` next to the
server) is used, otherwise the copy bundled into the server jar.

## Format

Values are [MiniMessage](https://docs.advntr.dev/minimessage/format.html). Colors are not written directly — use the
semantic tags below; the actual colors are defined in the server code.

| Tag | Use |
|---|---|
| `<primary>` `<secondary>` `<highlight>` `<muted>` | Gecko palette |
| `<info>` `<note>` `<success>` `<warning>` `<error>` | status colors |
| `<var>` `<var_key>` | variable value / key |
| `<spacer>` `<dark_spacer>` | separators, gray text |
| `<seeker>` `<hider>` `<spectator>` | role colors |
| `<prefix>` | Gecko chat prefix |
| `<info_prefix>` `<success_prefix>` `<warning_prefix>` `<error_prefix>` | status prefixes |
| `<small_caps>…</small_caps>` | small caps font |

Placeholders like `<player>` or `<seconds>` are filled in by the server; keep their names unchanged.
Formatting tags such as `<b>`, `<i>` and `<newline>` can be used freely.
