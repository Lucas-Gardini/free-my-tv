package io.freemytv;
import android.content.Context;
import android.media.MediaPlayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class LiveWallpaperTest {
    public static void main(String[] args) {
        List<LiveWallpaper.State> visible = new ArrayList<>();
        LiveWallpaper video = new LiveWallpaper(new Context(), visible::add);
        video.startWallpaper();
        video.startWallpaper();
        check(video.starts == 1, "Repeated resume must not create another playback session");
        check(visible.equals(Arrays.asList(LiveWallpaper.State.WAITING)), "Announce loading before starting the decoder so Home can hide the static image");
        MediaPlayer player = new MediaPlayer();
        video.prepared.onPrepared(player);
        check(player.looping && player.volume == 0, "Loop silently");
        check(!player.screenOn && video.audioFocus == 0, "Do not block screensaver or take audio focus");
        video.info.onInfo(player, MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START, 0);
        check(visible.equals(Arrays.asList(LiveWallpaper.State.WAITING, LiveWallpaper.State.PLAYING)), "Show video after the first frame");
        video.stopWallpaper();
        check(video.stops == 1 && visible.get(visible.size() - 1) == LiveWallpaper.State.WAITING, "Release decoder without flashing static on pause");
        video.info.onInfo(player, MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START, 0);
        check(visible.get(visible.size() - 1) == LiveWallpaper.State.WAITING, "Ignore rendering callbacks after pause");
        video.error.onError(player, 1, 0);
        check(visible.get(visible.size() - 1) == LiveWallpaper.State.WAITING, "A late decoder error must not reveal the static image");
        video.startWallpaper();
        check(video.starts == 2, "Return to Home can start playback again");
        check(visible.get(visible.size() - 1) == LiveWallpaper.State.WAITING, "Resume waits for its new first frame");
        check(video.error.onError(player, 1, 0), "Consume decoder errors rather than show a video error dialog");
        check(visible.get(visible.size() - 1) == LiveWallpaper.State.FALLBACK && video.stops == 2, "Failure releases decoder and restores static");
        video.startWallpaper();
        check(video.starts == 2, "Do not repeatedly retry a broken decoder in the same view");
        System.out.println("Live wallpaper: 13 lifecycle checks passed");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
