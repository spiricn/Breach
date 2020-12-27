package com.limber.breach;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.IOException;

public class SoundPlayer {

    Context mContext;

    public enum Effect {
        error("error"),
        beep("beep"),
        cancel("cancel"),
        short_beep("short"),
        working("working"),
        success("success");

        Effect(String name) {
            mName = name;
        }

        public String getAssetPath() {
            return mName + ".mp3";
        }

        String mName;
    }

    SoundPlayer(Context context) {
        mContext = context;
        mThread = new HandlerThread("SoundPlayer");
        mThread.start();

        mHandler = new Handler(mThread.getLooper());
    }

    static SoundPlayer sInstance;

    HandlerThread mThread;
    Handler mHandler;

    public static void create(Context context) {
        sInstance = new SoundPlayer(context);
    }

    public static SoundPlayer get() {
        return sInstance;
    }


    public void play(Effect effect) {
        play(effect, false);
    }

    public void play(Effect effect, boolean loop) {
        if(!mEnabled) {
            return;
        }

        mHandler.post(() -> {
            try {
                playPriv(effect, loop);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    boolean mEnabled = true;

    private void playPriv(Effect effect, boolean loop) throws IOException {
        mMediaPlayer.stop();
        mMediaPlayer.reset();

        AssetFileDescriptor descriptor = mContext.getAssets().openFd(effect.getAssetPath());
        mMediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
        descriptor.close();

        mMediaPlayer.prepare();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setVolume(1f, 1f);
        mMediaPlayer.setLooping(loop);
        mMediaPlayer.start();
    }

    public void stop() {
        mHandler.post(() -> {
            mMediaPlayer.stop();
            mMediaPlayer.reset();

        });
    }

    MediaPlayer mMediaPlayer = new MediaPlayer();
}
