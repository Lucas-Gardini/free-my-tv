package io.freemytv;

import android.content.Context;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;

final class Adb implements Closeable {
    interface Approval { void waiting(); }
    static final String HOME = "com.google.android.apps.tv.launcherx/.home.HomeActivity";
    static final String DREAM = "com.google.android.backdrop/.Backdrop";
    static final String WIFI = "WifiConfigManager - Configured networks Begin ----\n"
            + "ID: 0 SSID: redacted cname=com.android.tv.settings\n"
            + "WifiConfigManager - Configured networks End ----\nFMTV_WIFI_EXIT:0\n";
    static final Map<String, String> global = new HashMap<>();
    static final Set<String> disabled = new HashSet<>();
    static final List<String> commands = new ArrayList<>();
    static String wifi, home, saver, services, accessibility;
    static boolean interruptDisable, rejectGlobal, remoteSupported;
    static void reset() {
        global.clear(); disabled.clear(); commands.clear();
        wifi = WIFI; home = HOME; saver = DREAM;
        services = ""; accessibility = "0";
        interruptDisable = rejectGlobal = remoteSupported = false;
    }
    static Adb connect(Context c, int ms, Approval approval) { return new Adb(); }
    String shell(String cmd) throws IOException {
        commands.add(cmd);
        if (cmd.startsWith("dumpsys wifi")) return wifi;
        if (cmd.equals("settings get secure enabled_accessibility_services")) return services;
        if (cmd.equals("settings get secure accessibility_enabled")) return accessibility;
        if (cmd.startsWith("settings put secure enabled_accessibility_services ")) {
            services = cmd.substring("settings put secure enabled_accessibility_services ".length()).replace("'", ""); return "";
        }
        if (cmd.equals("settings delete secure enabled_accessibility_services")) { services = ""; return ""; }
        if (cmd.startsWith("settings put secure accessibility_enabled ")) {
            accessibility = cmd.substring("settings put secure accessibility_enabled ".length()); return "";
        }
        if (cmd.startsWith("settings get global ")) return global.getOrDefault(cmd.substring(20), "null");
        if (cmd.startsWith("settings put global ")) {
            String[] parts = cmd.substring(20).split(" ", 2);
            if (!rejectGlobal) global.put(parts[0], parts[1]);
            return "";
        }
        if (cmd.startsWith("settings delete global ")) { global.remove(cmd.substring(23)); return ""; }
        if (cmd.contains("get-role-holders")) return home.split("/")[0];
        if (cmd.contains("query-activities") || cmd.contains("resolve-activity")) return HOME;
        if (cmd.equals("settings get secure screensaver_components")) return saver;
        if (cmd.equals("settings get secure screensaver_default_component")) return DREAM;
        if (cmd.startsWith("settings put secure screensaver_components ")) {
            saver = cmd.substring("settings put secure screensaver_components ".length()).replace("'", "");
            return "";
        }
        if (cmd.startsWith("cmd package set-home-activity ")) {
            home = cmd.substring("cmd package set-home-activity ".length()).replace("'", ""); return "Success";
        }
        if (cmd.equals("pm list packages -d")) return packageList(disabled);
        if (cmd.equals("pm list packages ")) return packageList(new HashSet<>(Arrays.asList(
                "io.freemytv", "com.google.android.apps.tv.launcherx", "com.google.android.backdrop", "com.tcl.miracast")));
        if (cmd.startsWith("pm path ")) return "package:/system/app/launcher.apk";
        if (cmd.startsWith("pm disable-user --user 0 ")) {
            String pkg = cmd.substring("pm disable-user --user 0 ".length());
            disabled.add(pkg);
            if (interruptDisable) throw new IOException("connection lost after disable");
            return "Package " + pkg + " new state: disabled-user";
        }
        if (cmd.startsWith("pm enable ")) { disabled.remove(cmd.substring(10)); return "new state: enabled"; }
        return "";
    }
    private static String packageList(Set<String> packages) {
        StringBuilder out = new StringBuilder();
        for (String pkg : packages) out.append("package:").append(pkg).append('\n');
        return out.toString();
    }
    public void close() {}
}
final class Ui {
    static boolean ru;
    static String tr(String pt, String en) { return en; }
    static String tr(String pt, String en, String es) { return en; }
}
final class Russian { static String text(String s) { return s; } }
final class AppLog { static void w(String s) { if (Boolean.getBoolean("test.verbose")) System.out.println(s); } }
final class Config { static final boolean DISABLE_ASSISTANT = false; }
final class RemoteKeys { static boolean supported() { return Adb.remoteSupported; } }
final class Watchdog { static void clear(Context c) {} }
final class ModeJob {
    static final String APPLY = "apply", REVERT = "revert", VERIFY = "verify", MEMORY = "memory", REMOTE = "remote";
    static void schedule(Context c, String action, String mode, long delay) {}
}
