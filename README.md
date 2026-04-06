# Linkora

Save, organize, and sync your links between Android, desktop, and web. Whether you're quickly
bookmarking something or managing a structured folder hierarchy with tags, Linkora handles it all
with optional self-hosted sync.

A browser extension is available for saving web links directly to Linkora via the sync-server.

> Linkora on web is currently
> experimental. [linkora-app.netlify.app](https://linkora-app.netlify.app) is the only site maintained
> by me. Anything else is unrelated.

Other repos in the Linkora ecosystem:
[sync-server](https://github.com/LinkoraApp/sync-server) | [browser-extension](https://github.com/LinkoraApp/browser-extension) | [proxy](https://github.com/LinkoraApp/proxy)

**Contributing?** See
the [Contributing Guide](CONTRIBUTING.md) | [Code of Conduct](https://github.com/LinkoraApp/.github/blob/main/CODE_OF_CONDUCT.md)

## Download

[<img src="https://github.com/user-attachments/assets/a50513b3-dbf8-48c1-bff8-1f4215fefbb9"
alt="Get it on GitHub"
height="80">](https://github.com/sakethpathike/Linkora/releases) [<img src="https://f-droid.org/badge/get-it-on.png"
alt="Get it on F-Droid"
height="80">](https://f-droid.org/packages/com.sakethh.linkora)

&nbsp;&nbsp;&nbsp;[<img src="https://github.com/user-attachments/assets/7b6e7704-7f39-49a9-b868-556ebea5fc76"
alt="Get it on Google Play"
height="58">](https://play.google.com/store/apps/details?id=com.sakethh.linkora)

Get it on Arch Linux:

[<img src="https://img.shields.io/aur/version/linkora-bin?style=flat&logo=archlinux&label=linkora-bin" alt="AUR version">](https://aur.archlinux.org/packages/linkora-bin) ![AUR Maintainer](https://img.shields.io/aur/maintainer/linkora-bin)

## Features

- **Unlimited folders and subfolders** with tag support and easy copying/moving of links & folders
  between them
- **Multiple view layouts** (Grid, List, Staggered views) with AMOLED theme support
- **Highlight important links** and archive old ones for clean organization
- **Customize link metadata** and auto-recognize images/titles from web pages
- **Share from other apps** (Android) and add folders to **_Panels_** for quick access
- **Sort, search, import/export** data in JSON and HTML formats with **auto-backups**
- **Keep your data in sync across devices** with
  optional [self-hostable sync-server](https://github.com/LinkoraApp/sync-server)

[How sync works (Technical write-up)](https://sakethpathike.github.io/blog/synchronization-in-linkora) · [Server setup instructions](docs/ServerConnectionSetup.md)

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
- SQLite with Room (local storage) + Ktor (networking) + Custom cursor-based reactive and
  resource-aware paginator
- Coroutines and Flows for async operations
- [Ksoup](https://github.com/fleeksoft/ksoup) for HTML parsing and metadata extraction
- [Coil](https://github.com/coil-kt/coil) for image loading
- Custom syncing mechanisms for handling syncing with remote server
- [Android-specific] WorkManager for snapshots and bulk metadata refresh for links

Full dependency list
in [libs.versions.toml](/gradle/libs.versions.toml).

Linkora's improved UI components are inspired and based on designs created
by [LOLCATpl](https://discord.com/users/494115165927637007) across all platforms. The icon, painted
by [mondstern](https://pixelfed.social/mondstern), is used as the app icon on all platforms and also
on the internet.

### Localization

Linkora supports multiple languages with remote strings that can be updated without requiring an app
update, i.e. over-the-air updates. If you'd like to help translate Linkora into your language or
improve existing translations, please go through
the [localization server](https://github.com/LinkoraApp/localization-server)'s README to learn how
localization is handled and how you can contribute.

## Community

[![Discord](https://discord.com/api/guilds/1214971383352664104/widget.png?style=banner2)](https://discord.gg/ZDBXNtv8MD)

---

**License:** MIT
