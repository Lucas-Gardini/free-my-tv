package android.widget;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;
public class VideoView extends View {
    public int starts, stops, audioFocus = 1;
    public MediaPlayer.OnPreparedListener prepared;
    public MediaPlayer.OnInfoListener info;
    public MediaPlayer.OnErrorListener error;
    public VideoView(Context context) {}
    public void setAudioFocusRequest(int value) { audioFocus = value; }
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener value) { prepared = value; }
    public void setOnInfoListener(MediaPlayer.OnInfoListener value) { info = value; }
    public void setOnErrorListener(MediaPlayer.OnErrorListener value) { error = value; }
    public void setVideoURI(Uri uri) {}
    public void start() { starts++; }
    public void stopPlayback() { stops++; }
}
