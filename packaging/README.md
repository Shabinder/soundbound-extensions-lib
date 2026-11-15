# Packaging

Public package-manager recipes for the SoundBound desktop app. They contain no binaries: each recipe
downloads the official, unmodified release assets from this repository's
[GitHub releases](https://github.com/Shabinder/soundbound-extensions-lib/releases) and verifies their SHA-256.

| Folder | Package manager | Contents |
|---|---|---|
| `windows/chocolatey/` | [Chocolatey](https://community.chocolatey.org/) | `soundbound.nuspec` and `tools/` install/uninstall scripts. Installs for all users (`/ALLUSERS`). |
| `windows/winget/` | [winget](https://github.com/microsoft/winget-pkgs) | Manifest templates for `Shabinder.SoundBound` (version, default locale, installer). Per-user install. |
| `aur/soundbound-bin/` | [AUR](https://aur.archlinux.org/) | `PKGBUILD` that repackages the official AppImage into `/opt/soundbound`. |

`__VERSION__`, `__URL__` and `__SHA256__` (and the AUR `pkgver`/`sha256sums`) are filled in at release time.

SoundBound is proprietary freeware: see the [license](https://soundbound.app/license/) and
[third-party notices](https://soundbound.app/license/third-party/). Redistributing the unmodified official
installers through package managers is permitted by that license.
