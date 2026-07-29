# Windows desktop bundle

The Windows release bundle contains NCMD libraries and a native Win32 launcher only. It
does not embed Java or .NET.

The launcher accepts a system-installed Java 21+ runtime from `NCMD_JAVA_HOME`, `JAVA_HOME`,
the Windows Java registry keys, or `PATH`. If none is suitable, it displays a dialog and opens
the official JetBrains Runtime download page. This keeps runtime updates independent from NCMD
releases.
