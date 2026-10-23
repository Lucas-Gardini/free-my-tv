package io.freemytv;
import android.content.Context;
import android.media.MediaPlayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class LiveWallpaperTest {
    public static void main(String[] args) {
        List<Boolean> visible = new ArrayList<>();
        LiveWallpaper video = new LiveWallpaper(new Context(), visible::add);
        video.startWallpaper();
        video.startWallpaper();
        check(video.starts == 1, "Repeated resume must not create another playback session");
        check(visible.isEmpty(), "Keep the static wallpaper until the first video frame");
        MediaPlayer player = new MediaPlayer();
        video.prepared.onPrepared(player);
        check(player.looping && player.volume == 0, "Loop silently");
        check(!player.screenOn && video.audioFocus == 0, "Do not block screensaver or take audio focus");
        video.info.onInfo(player, MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START, 0);
        check(visible.equals(Arrays.asList(true)), "Show video after the first frame");
        video.stopWallpaper();
        check(video.stops == 1 && visible.get(visible.size() - 1) == false, "Release decoder and restore static on pause");
        video.info.onInfo(player, MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START, 0);
        check(!visible.get(visible.size() - 1), "Ignore rendering callbacks after pause");
        video.startWallpaper();
        check(video.starts == 2, "Return to Home can start playback again");
        check(video.error.onError(player, 1, 0), "Consume decoder errors rather than show a video error dialog");
        check(!visible.get(visible.size() - 1) && video.stops == 2, "Failure releases decoder and restores static");
        video.startWallpaper();
        check(video.starts == 2, "Do not repeatedly retry a broken decoder in the same view");
        System.out.println("Live wallpaper: 11 lifecycle checks passed");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
