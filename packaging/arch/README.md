# Arch Linux package

The `Package Desktop` GitHub Actions workflow produces an official
`ncmd-<version>-1-x86_64.pkg.tar.zst` artifact. It is built from the Compose Desktop app
image, not converted from a DEB package.

Install a downloaded artifact with:

```bash
sudo pacman -U ncmd-<version>-1-x86_64.pkg.tar.zst
```

The package includes its own trimmed Java runtime. Its runtime dependencies are only audio,
font, OpenGL/X11, and desktop-integration libraries; it must not depend on IntelliJ IDEA,
WPS Office, or a system JDK.

For a local package build, first create the app image and then run `makepkg`:

```bash
./gradlew :desktop:createDistributable
cd packaging/arch
makepkg --cleanbuild --nodeps
```
