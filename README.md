# Linkora

Save, organize, and sync your links between Android and desktop. Whether you're quickly bookmarking
something or managing a structured folder hierarchy with tags, Linkora handles it all with optional
self-hosted sync.

A browser extension is available for saving web links directly to Linkora via sync server.

[sync server](https://github.com/LinkoraApp/sync-server) | [Extension repo](https://github.com/LinkoraApp/browser-extension)

## Download

[<img src="https://github.com/user-attachments/assets/a50513b3-dbf8-48c1-bff8-1f4215fefbb9"
alt="Get it on GitHub"
height="80">](https://github.com/sakethpathike/Linkora/releases) [<img src="https://f-droid.org/badge/get-it-on.png"
alt="Get it on F-Droid"
height="80">](https://f-droid.org/packages/com.sakethh.linkora) 

Get it on Arch Linux:

![AUR Version](https://img.shields.io/aur/version/linkora-bin?style=flat&logo=archlinux&label=linkora-bin&link=https%3A%2F%2Faur.archlinux.org%2Fpackages%2Flinkora-bin) ![AUR Maintainer](https://img.shields.io/aur/maintainer/linkora-bin)

## Features

- **Organize links** with unlimited folders and subfolders, tags, and easy copying/moving between
  folders
- **Multiple view layouts** (Grid, List, Staggered views) with AMOLED theme support
- **Highlight important links** and archive old ones for clean organization
- **Customize link names** and auto-recognize images/titles from web pages
- **Share from other apps** (Android) and add folders to **_Panels_** for quick access
- **Sort, search, import/export** data in JSON and HTML formats with auto-backups
- **Keep your links in sync** across devices with
  optional [self-hostable server](https://github.com/LinkoraApp/sync-server)

[How sync works](https://sakethpathike.github.io/blog/synchronization-in-linkora) · [Server setup instructions](docs/ServerConnectionSetup.md) 

## Screenshots

### Mobile

|                    |                    |                    |                    |
|--------------------|--------------------|--------------------|--------------------|
| ![](assets/m1.png) | ![](assets/m2.png) | ![](assets/m3.png) | ![](assets/m4.png) |
| ![](assets/m5.png) | ![](assets/m6.png) | ![](assets/m7.png) | ![](assets/m8.png) |

### Desktop

|                    |                    |
|--------------------|--------------------|
| ![](assets/t1.png) | ![](assets/t2.png) |
| ![](assets/t3.png) | ![](assets/t4.png) |

## Built with

- Kotlin Multiplatform + Compose Multiplatform + Material 3
- SQLite with Room (local storage) + Ktor (networking)
- Coroutines and Flows for async operations
- jsoup for HTML parsing and metadata extraction
- Coil for image loading

### Localization

Linkora supports multiple languages with remote strings that can be updated without requiring an app
update. If you'd like to help translate Linkora into your language or improve existing translations,
please go through the [localization server](https://github.com/LinkoraApp/localization-server)'s
README to learn more about how localization is handled and how you can contribute.

## Support

**Star the repo** if you find Linkora useful

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/sakethpathike)

## Community

[![Discord](https://discord.com/api/guilds/1214971383352664104/widget.png?style=banner2)](https://discord.gg/ZDBXNtv8MD)

---

**Contributing?** See the [contributing guide](CONTRIBUTING.md)

**License:** MIT
