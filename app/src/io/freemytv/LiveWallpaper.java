package io.freemytv;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.widget.VideoView;
import java.util.function.Consumer;

/** Optional background video; its decoder exists only while Home is visible. */
final class LiveWallpaper extends VideoView {
    private final Consumer<Boolean> visible;
    private final Uri source;
    private boolean active, failed;

    LiveWallpaper(Context context, Consumer<Boolean> visible) {
        super(context);
        this.visible = visible;
        source = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.red_eye);
        setFocusable(false);
        setFocusableInTouchMode(false);
        setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE);
        setVisibility(GONE);
        setOnPreparedListener(player -> {
            if (!active) return;
            try {
                player.setLooping(true);
                player.setVolume(0, 0);
                // VideoView normally keeps the panel awake; a wallpaper must allow the screensaver.
                player.setScreenOnWhilePlaying(false);
                setKeepScreenOn(false);
            } catch (RuntimeException e) { fail(); }
        });
        setOnInfoListener((player, what, extra) -> {
            if (active && what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                visible.accept(true);
                AppLog.w("live wallpaper playing");
            }
            return false;
        });
        setOnErrorListener((player, what, extra) -> {
            fail();
            return true;
        });
    }

    void startWallpaper() {
        if (active || failed) return;
        active = true;
        try {
            setVisibility(VISIBLE);
            setVideoURI(source);
            start();
        } catch (RuntimeException e) { fail(); }
    }

    void stopWallpaper() {
        if (!active) return;
        active = false;
        visible.accept(false);
        try { stopPlayback(); } catch (RuntimeException e) {
            AppLog.w("live wallpaper decoder already stopped");
        }
        setVisibility(GONE);
        AppLog.w("live wallpaper stopped");
    }

    private void fail() {
        failed = true;
        stopWallpaper();
        AppLog.w("live wallpaper unavailable; keeping static background");
    }
}
