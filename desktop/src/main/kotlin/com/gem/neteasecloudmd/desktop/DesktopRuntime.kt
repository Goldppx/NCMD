package com.gem.neteasecloudmd.desktop

/**
 * Configures AWT before Compose creates its first window. JetBrains Runtime 21 exposes the native
 * Wayland toolkit. Other JVMs continue through XWayland when the compositor exposes DISPLAY.
 */
object DesktopRuntime {
    fun configure() {
        System.setProperty("sun.java2d.uiScale.enabled", "true")

        val shouldUseWayland = !shouldForceX11Fallback() &&
            System.getenv("XDG_SESSION_TYPE").equals("wayland", ignoreCase = true) &&
            !System.getenv("WAYLAND_DISPLAY").isNullOrBlank() &&
            System.getProperty("awt.toolkit.name").isNullOrBlank() &&
            hasJetBrainsWaylandToolkit()
        if (shouldUseWayland) System.setProperty("awt.toolkit.name", WAYLAND_TOOLKIT)
    }

    private fun shouldForceX11Fallback(): Boolean =
        System.getenv("NCMD_FORCE_X11") == "1"

    private fun hasJetBrainsWaylandToolkit(): Boolean = runCatching {
        Class.forName("sun.awt.wl.WLToolkit")
    }.isSuccess

    private const val WAYLAND_TOOLKIT = "WLToolkit"
}
