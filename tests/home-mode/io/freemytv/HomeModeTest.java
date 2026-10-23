package io.freemytv;

import android.content.Context;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class HomeModeTest {
    private static int passed, failed;
    interface Test { void run(Context c) throws Exception; }
    static void test(String name, Test test) throws Exception {
        Adb.reset();
        Context c = new Context(Files.createTempDirectory("freemytv-test-").toFile());
        c.getFilesDir().deleteOnExit();
        try { test.run(c); passed++; System.out.println("PASS " + name); }
        catch (AssertionError e) { failed++; System.out.println("FAIL " + name + ": " + e.getMessage()); }
        finally {
            File[] files = c.getFilesDir().listFiles();
            if (files != null) for (File file : files) file.deleteOnExit();
        }
    }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    static void unchanged() {
        check(Adb.home.equals(Adb.HOME) && Adb.disabled.isEmpty() && Adb.saver.equals(Adb.DREAM),
                "must not change home, screensaver or packages");
    }
    public static void main(String[] args) throws Exception {
        test("partial Wi-Fi dump aborts before changing the TV", c -> {
            Adb.wifi = "Verbose logging is off\nException occurred while dumping:\njava.lang.NullPointerException\n";
            HomeMode.applyNow(c, HomeMode.FREE); unchanged();
        });
        test("unrecognized Wi-Fi format aborts", c -> {
            Adb.wifi = "a vendor's unsupported dump\nFMTV_WIFI_EXIT:0\n";
            HomeMode.applyNow(c, HomeMode.FREE); unchanged();
        });
        test("failed Wi-Fi command aborts even with network markers", c -> {
            Adb.wifi = Adb.WIFI.replace("FMTV_WIFI_EXIT:0", "FMTV_WIFI_EXIT:1");
            HomeMode.applyNow(c, HomeMode.FREE); unchanged();
        });
        test("unknown network owner aborts", c -> {
            Adb.wifi = Adb.WIFI.replace("cname=com.android.tv.settings", "creatorUid=1000");
            HomeMode.applyNow(c, HomeMode.FREE); unchanged();
        });
        test("home owning a saved network aborts", c -> {
            Adb.wifi = Adb.WIFI.replace("com.android.tv.settings", "com.google.android.apps.tv.launcherx");
            HomeMode.applyNow(c, HomeMode.FREE); unchanged();
        });
        test("null network owner aborts", c -> {
            Adb.wifi = Adb.WIFI.replace("cname=com.android.tv.settings", "cname=null");
            HomeMode.applyNow(c, HomeMode.FREE); unchanged();
        });
        test("valid empty network list permits switching", c -> {
            Adb.wifi = Adb.WIFI.replace("ID: 0 SSID: redacted cname=com.android.tv.settings\n", "");
            HomeMode.applyNow(c, HomeMode.FREE);
            check(Adb.home.equals(HomeMode.HOME_ACTIVITY), "should switch with zero saved networks");
        });
        test("unwritable state aborts before changing the TV", c -> {
            Files.createDirectory(new File(c.getFilesDir(), "home-state.json.tmp").toPath());
            HomeMode.applyNow(c, HomeMode.FREE); unchanged();
        });
        test("failed global recovery backup aborts before changing the TV", c -> {
            Adb.rejectGlobal = true;
            HomeMode.applyNow(c, HomeMode.FREE); unchanged();
        });
        test("Original restores recorded packages after local state is lost", c -> {
            Adb.global.put("free_my_tv_disabled", "com.tcl.miracast");
            Adb.disabled.add("com.tcl.miracast");
            HomeMode.applyNow(c, HomeMode.ORIGINAL);
            check(!Adb.disabled.contains("com.tcl.miracast"), "must consume global disabled record");
        });
        test("Original restores custom screensaver after local state is lost", c -> {
            Adb.saver = "com.google.android.backdrop/.CustomSaver";
            HomeMode.applyNow(c, HomeMode.FREE);
            Files.delete(new File(c.getFilesDir(), "home-state.json").toPath());
            HomeMode.applyNow(c, HomeMode.ORIGINAL);
            check(Adb.saver.equals("com.google.android.backdrop/.CustomSaver"), "must recover exact screensaver");
            check(Adb.disabled.isEmpty(), "must restore all packages we disabled");
        });
        test("interrupted disable is journaled before command executes", c -> {
            Adb.interruptDisable = true;
            HomeMode.applyNow(c, HomeMode.FREE);
            check(!Adb.disabled.isEmpty(), "test must interrupt after an actual disable");
            for (String pkg : Adb.disabled) check(Adb.global.getOrDefault("free_my_tv_disabled", "").contains(pkg),
                    "missing durable record for " + pkg);
            Adb.interruptDisable = false;
            HomeMode.applyNow(c, HomeMode.ORIGINAL);
            check(Adb.disabled.isEmpty(), "must recover interrupted disable");
        });
        test("round trip preserves packages disabled by the user", c -> {
            Adb.disabled.add("com.tcl.miracast");
            HomeMode.applyNow(c, HomeMode.FREE);
            HomeMode.applyNow(c, HomeMode.ORIGINAL);
            check(Adb.disabled.equals(Collections.singleton("com.tcl.miracast")), "must preserve user-disabled packages: " + Adb.disabled + " state=" + HomeMode.state(c));
            check(Adb.home.equals(Adb.HOME) && Adb.saver.equals(Adb.DREAM), "must restore original components");
        });
        test("corrupt local state recovers components and user-disabled packages", c -> {
            Adb.disabled.add("com.tcl.miracast");
            HomeMode.applyNow(c, HomeMode.FREE);
            Files.write(new File(c.getFilesDir(), "home-state.json").toPath(), "broken".getBytes("UTF-8"));
            HomeMode.applyNow(c, HomeMode.ORIGINAL);
            check(Adb.disabled.equals(Collections.singleton("com.tcl.miracast")), "must recover user-disabled list");
            check(Adb.home.equals(Adb.HOME) && Adb.saver.equals(Adb.DREAM), "must recover original components");
        });
        test("boot verification journals new disables before connection loss", c -> {
            HomeMode.applyNow(c, HomeMode.FREE);
            Adb.disabled.clear();
            Adb.global.remove("free_my_tv_disabled");
            Adb.interruptDisable = true;
            HomeMode.verifyNow(c);
            check(!Adb.disabled.isEmpty(), "test must exercise a disable during verification");
            for (String pkg : Adb.disabled) check(Adb.global.getOrDefault("free_my_tv_disabled", "").contains(pkg),
                    "verification lost durable record for " + pkg);
        });
        test("watchdog preserves the package journal after local state loss", c -> {
            HomeMode.applyNow(c, HomeMode.FREE);
            Files.delete(new File(c.getFilesDir(), "home-state.json").toPath());
            HomeMode.revertHomeNow(c);
            check(Adb.global.getOrDefault("free_my_tv_disabled", "").contains("com.tcl.miracast"),
                    "watchdog must preserve records for packages it leaves disabled");
            HomeMode.applyNow(c, HomeMode.ORIGINAL);
            check(Adb.disabled.isEmpty(), "Original must still restore leftover packages after watchdog");
        });
        test("repeat switch preserves the existing package journal after state loss", c -> {
            HomeMode.applyNow(c, HomeMode.FREE);
            Files.delete(new File(c.getFilesDir(), "home-state.json").toPath());
            Adb.interruptDisable = true;
            HomeMode.applyNow(c, HomeMode.FREE);
            check(Adb.global.getOrDefault("free_my_tv_disabled", "").contains("com.tcl.miracast"),
                    "repeat switch must not discard earlier journal entries");
        });
        test("Wi-Fi owner-like text in SSID cannot hide the real owner", c -> {
            Adb.wifi = Adb.WIFI.replace("SSID: redacted cname=com.android.tv.settings",
                    "SSID: \"cname=com.android.tv.settings \" cname=com.google.android.apps.tv.launcherx");
            HomeMode.applyNow(c, HomeMode.FREE); unchanged();
        });
        test("Original restores accessibility after local state loss", c -> {
            Adb.remoteSupported = true;
            HomeMode.applyNow(c, HomeMode.FREE);
            check(Adb.accessibility.equals("1"), "test must enable accessibility");
            Files.delete(new File(c.getFilesDir(), "home-state.json").toPath());
            HomeMode.applyNow(c, HomeMode.ORIGINAL);
            check(Adb.services.isEmpty() && Adb.accessibility.equals("0"), "must restore accessibility baseline");
        });
        test("corrupt optional backup cannot block Original with valid local state", c -> {
            HomeMode.applyNow(c, HomeMode.FREE);
            Adb.global.put("free_my_tv_recovery", "invalid backup");
            HomeMode.applyNow(c, HomeMode.ORIGINAL);
            check(Adb.disabled.isEmpty() && Adb.home.equals(Adb.HOME), "must restore from local recovery state");
        });
        test("upgrade verification backs up existing installations", c -> {
            HomeMode.applyNow(c, HomeMode.FREE);
            Adb.global.remove("free_my_tv_recovery");
            HomeMode.verifyNow(c);
            check(Adb.global.containsKey("free_my_tv_recovery"), "must migrate old local recovery state");
        });
        test("fresh switch recomputes user-disabled packages", c -> {
            Adb.disabled.add("com.tcl.miracast");
            HomeMode.applyNow(c, HomeMode.FREE);
            HomeMode.applyNow(c, HomeMode.ORIGINAL);
            Adb.disabled.clear(); // User enabled Miracast while in Original.
            HomeMode.applyNow(c, HomeMode.FREE);
            check(Adb.disabled.contains("com.tcl.miracast"), "must not import stale user-disabled status");
        });
        test("partially recognized Wi-Fi section aborts", c -> {
            Adb.wifi = Adb.WIFI.replace("ID: 0", "NetworkId: 9 cname=com.google.android.apps.tv.launcherx\nID: 0");
            HomeMode.applyNow(c, HomeMode.FREE); unchanged();
        });
        test("malformed owner cannot hide behind a valid owner token", c -> {
            Adb.wifi = Adb.WIFI.replace("cname=com.android.tv.settings", "cname=com.android.tv.settings cname=<unknown>");
            HomeMode.applyNow(c, HomeMode.FREE); unchanged();
        });
        test("verification preserves older journal entries missing from local state", c -> {
            HomeMode.applyNow(c, HomeMode.FREE);
            Adb.global.put("free_my_tv_disabled", "com.example.oldpromo");
            HomeMode.verifyNow(c);
            check(Adb.global.getOrDefault("free_my_tv_disabled", "").contains("com.example.oldpromo"),
                    "must not overwrite earlier global journal entries");
        });
        test("Original retries an interrupted accessibility restore", c -> {
            Adb.remoteSupported = true;
            HomeMode.applyNow(c, HomeMode.FREE);
            Adb.services = ""; // Previous repair stopped between service removal and disabling accessibility.
            HomeMode.applyNow(c, HomeMode.ORIGINAL);
            check(Adb.accessibility.equals("0"), "must complete interrupted accessibility restore");
        });
        test("Google setup goes off before stock home", c -> {
            Adb.setupHome = true;
            HomeMode.applyNow(c, HomeMode.FREE);
            int setup = Adb.commands.indexOf("pm disable-user --user 0 " + HomeMode.GOOGLE_SETUP);
            int home = Adb.commands.indexOf("pm disable-user --user 0 " + Adb.HOME.split("/")[0]);
            check(setup >= 0 && home > setup, "must disable setup before stock home");
        });
        test("failed setup disable keeps stock home enabled", c -> {
            Adb.setupHome = Adb.rejectSetupDisable = true;
            HomeMode.applyNow(c, HomeMode.FREE);
            check(!Adb.disabled.contains(Adb.HOME.split("/")[0]), "must retain stock home when setup stays on");
        });
        test("Original restores home before enabling setup", c -> {
            Adb.setupHome = true;
            HomeMode.applyNow(c, HomeMode.FREE);
            Adb.commands.clear();
            HomeMode.applyNow(c, HomeMode.ORIGINAL);
            int home = Adb.commands.indexOf("cmd package set-home-activity '" + Adb.HOME + "'");
            int setup = Adb.commands.indexOf("pm enable " + HomeMode.GOOGLE_SETUP);
            check(home >= 0 && setup > home, "must restore the home role before enabling setup");
        });
        test("failed home restore leaves setup off", c -> {
            Adb.setupHome = true;
            HomeMode.applyNow(c, HomeMode.FREE);
            Adb.rejectHomeRestore = true;
            HomeMode.applyNow(c, HomeMode.ORIGINAL);
            check(Adb.disabled.contains(HomeMode.GOOGLE_SETUP), "setup must wait until a home is restored");
            check(Adb.global.getOrDefault("free_my_tv_disabled", "").contains(HomeMode.GOOGLE_SETUP),
                    "must retain recovery intent for setup");
        });
        test("watchdog restores home before enabling setup", c -> {
            Adb.setupHome = true;
            HomeMode.applyNow(c, HomeMode.FREE);
            Adb.commands.clear();
            HomeMode.revertHomeNow(c);
            check(Adb.commands.indexOf("cmd package set-home-activity '" + Adb.HOME + "'")
                    < Adb.commands.indexOf("pm enable " + HomeMode.GOOGLE_SETUP), "watchdog setup order");
        });
        test("boot verification disables setup before stock home", c -> {
            Adb.setupHome = true;
            HomeMode.applyNow(c, HomeMode.FREE);
            Adb.disabled.clear(); Adb.commands.clear();
            HomeMode.verifyNow(c);
            int setup = Adb.commands.indexOf("pm disable-user --user 0 " + HomeMode.GOOGLE_SETUP);
            int home = Adb.commands.indexOf("pm disable-user --user 0 " + Adb.HOME.split("/")[0]);
            check(setup >= 0 && home > setup, "boot repair must retain setup-first order");
        });
        test("input and remote package dependencies stay enabled", c -> {
            Adb.needed.add("com.tcl.miracast");
            HomeMode.applyNow(c, HomeMode.FREE);
            check(!Adb.disabled.contains("com.tcl.miracast"), "input dependency must stay enabled");
            check(!Adb.disabled.isEmpty(), "unrelated targets should still be disabled");
        });
        test("incomplete dependency inspection disables no packages", c -> {
            Adb.unknownPackageNeeds = true;
            HomeMode.applyNow(c, HomeMode.FREE);
            check(Adb.disabled.isEmpty(), "must fail closed for package inspection");
        });
        test("boot repair restores a newly discovered input dependency", c -> {
            HomeMode.applyNow(c, HomeMode.FREE);
            Adb.needed.add("com.tcl.miracast");
            HomeMode.verifyNow(c);
            check(!Adb.disabled.contains("com.tcl.miracast"), "must restore required input package");
            check(!Adb.global.getOrDefault("free_my_tv_disabled", "").contains("com.tcl.miracast"),
                    "must remove restored package from journal");
        });
        test("Kalley model guard changes nothing", c -> {
            android.os.Build.DEVICE = "R3_GTV";
            HomeMode.applyNow(c, HomeMode.FREE); unchanged();
        });
        test("verification retries packages present only in the durable journal", c -> {
            HomeMode.applyNow(c, HomeMode.FREE);
            Adb.extraPackages.add("com.example.oldpromo");
            Adb.global.put("free_my_tv_disabled", "com.example.oldpromo");
            HomeMode.verifyNow(c);
            check(Adb.disabled.contains("com.example.oldpromo"), "must retry journal entries absent from local state");
        });
        test("failed dumpsys cannot pass the package inspection sentinel", c -> {
            Adb.realPackageInspection = true; Adb.packageDumpExit = 1;
            Adb.packageDump = "Permission Denial: package inspection";
            HomeMode.applyNow(c, HomeMode.FREE);
            check(Adb.inspectionOutput.contains("checked"), "must execute actual inspection shell: " + Adb.inspectionOutput);
            check(Adb.disabled.isEmpty(), "failed package dump must never authorize a disable");
        });
        test("empty dumpsys cannot authorize package disables", c -> {
            Adb.realPackageInspection = true;
            HomeMode.applyNow(c, HomeMode.FREE);
            check(Adb.inspectionOutput.contains("checked"), "must execute actual inspection shell: " + Adb.inspectionOutput);
            check(Adb.disabled.isEmpty(), "empty package dump must be unknown");
        });
        test("reapplying Free preserves disabled setup HOME capability", c -> {
            Adb.setupHome = true;
            HomeMode.applyNow(c, HomeMode.FREE);
            HomeMode.applyNow(c, HomeMode.FREE);
            check(HomeMode.state(c).optBoolean("setupHome"), "disabled setup is still a HOME candidate");
            Adb.disabled.clear(); Adb.commands.clear();
            HomeMode.verifyNow(c);
            int setup = Adb.commands.indexOf("pm disable-user --user 0 " + HomeMode.GOOGLE_SETUP);
            int home = Adb.commands.indexOf("pm disable-user --user 0 " + Adb.HOME.split("/")[0]);
            check(setup >= 0 && home > setup, "repeat switch must retain OTA repair order");
        });
        test("reenabled user-disabled setup keeps stock home safe", c -> {
            Adb.setupHome = true; Adb.disabled.add(HomeMode.GOOGLE_SETUP);
            HomeMode.applyNow(c, HomeMode.FREE);
            Adb.disabled.clear(); Adb.commands.clear(); // Firmware reenabled setup and stock home.
            HomeMode.applyNow(c, HomeMode.FREE);
            check(!Adb.disabled.contains(Adb.HOME.split("/")[0]), "user-owned setup still on requires stock home");
        });
        test("empty home role is not proof that stock home recovered", c -> {
            Adb.setupHome = true;
            HomeMode.applyNow(c, HomeMode.FREE);
            Adb.rejectHomeRestore = Adb.emptyHomeRole = true;
            HomeMode.applyNow(c, HomeMode.ORIGINAL);
            check(Adb.disabled.contains(HomeMode.GOOGLE_SETUP), "must not enable setup with only a chooser");
        });
        test("Kalley guard also protects existing Free installations", c -> {
            HomeMode.applyNow(c, HomeMode.FREE);
            android.os.Build.DEVICE = "R3_GTV";
            Adb.disabled.clear(); Adb.commands.clear();
            HomeMode.verifyNow(c);
            check(Adb.disabled.isEmpty(), "boot repair must respect restricted model");
        });
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed != 0) System.exit(1);
    }
}
