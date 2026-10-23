package android.media;
public class MediaPlayer {
    public static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;
    public interface OnPreparedListener { void onPrepared(MediaPlayer player); }
    public interface OnInfoListener { boolean onInfo(MediaPlayer player, int what, int extra); }
    public interface OnErrorListener { boolean onError(MediaPlayer player, int what, int extra); }
    public boolean looping, screenOn = true;
    public float volume = 1;
    public void setLooping(boolean value) { looping = value; }
    public void setVolume(float left, float right) { volume = Math.max(left, right); }
    public void setScreenOnWhilePlaying(boolean value) { screenOn = value; }
}
