package com.limber.breach.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.IOException;

/**
 * Plays application sound effects
 */
public class SoundPlayer {

    /**
     * Available sound effects
     * <p>
     * Each one maps to an MP3 file from the assets
     */
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

    private static final String kTHREAD_NAME = "SoundPlayer";

    /**
     * Singleton instance
     */
    static SoundPlayer sInstance;

    /**
     * Executers all commands
     */
    private final Handler mHandler;

    /**
     * Media player used to play assets
     */
    private final MediaPlayer mMediaPlayer = new MediaPlayer();

    /**
     * Indication if all sounds effects are enabled
     */
    private boolean mEnabled = true;

    private SoundPlayer() {
        HandlerThread mThread = new HandlerThread(kTHREAD_NAME);
        mThread.start();

        mHandler = new Handler(mThread.getLooper());
    }

    /**
     * Create the singleton
     */
    public static void create() {
        sInstance = new SoundPlayer();
    }

    /**
     * Get the singleton
     */
    public static SoundPlayer get() {
        return sInstance;
    }

    /**
     * Play an effect
     */
    public void play(Context context, Effect effect) {
        play(context, effect, false);
    }

    /**
     * Play an effect
     */
    public void play(Context context, Effect effect, boolean loop) {
        if (!mEnabled) {
            return;
        }

        mHandler.post(() -> {
            try {
                playPriv(context, effect, loop);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Enable or disable all sound effects
     */
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    private void playPriv(Context context, Effect effect, boolean loop) throws IOException {
        mMediaPlayer.stop();
        mMediaPlayer.reset();

        AssetFileDescriptor descriptor = context.getAssets().openFd(effect.getAssetPath());
        mMediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
        descriptor.close();

        mMediaPlayer.prepare();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setVolume(1f, 1f);
        mMediaPlayer.setLooping(loop);
        mMediaPlayer.start();
    }

    /**
     * Stop playback
     */
    public void stop() {
        mHandler.post(() -> {
            mMediaPlayer.stop();
            mMediaPlayer.reset();

        });
    }
}
