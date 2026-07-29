# Arch Linux package

The `Package Desktop` GitHub Actions workflow produces an official
`ncmd-<version>-3-x86_64.pkg.tar.zst` artifact. It contains NCMD's application libraries,
not a private Java runtime.

Install a downloaded artifact with:

```bash
sudo pacman -U ncmd-<version>-1-x86_64.pkg.tar.zst
```

Install a Java 21 runtime through pacman before launching NCMD. This keeps Java security
updates under Arch's package management instead of leaving an untracked runtime in `/opt`.

Native Wayland requires JetBrains Runtime, because ordinary OpenJDK uses XWayland for AWT.
Install a JetBrains Runtime 21 package from the AUR (for example `jre21-jetbrains-git`), and
make it the default Java or start NCMD with `NCMD_JAVA_HOME=/path/to/jbr ncmd`.

For a local package build, first create the app image and then run `makepkg`:

```bash
./gradlew :desktop:createDistributable
cd packaging/arch
makepkg --cleanbuild --nodeps
```
