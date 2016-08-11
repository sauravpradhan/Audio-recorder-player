
package com.saurav.audiorecorder_player;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.audiorecorder_player.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity {

    private AudioManager mAudioManager = null;
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private int mPlayChannelConfig = AudioFormat.CHANNEL_OUT_MONO;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private AudioTrack mAudioTrack = null;
    private boolean isRecording = false;
    private boolean capRunFlag = false;
    private boolean playRunFlag = false;
    private Thread playThread = null;
    private int bufferSizeInBytes = 640;
    private int AUDIOSOURCE;/* = MediaRecorder.AudioSource.MIC; */
    private int AUDIOMODE;/* = AudioManager.MODE_NORMAL; */
    private int AUDIOSTREAM; /* = AudioManager.STREAM_VOICE_CALL; */
    ToggleButton tbut1;
    Spinner audioSrc, audioStrm, audioMode;
    Button start_rec, start_play, stop_rec, stop_play;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tbut1 = (ToggleButton) findViewById(R.id.toggleButton);
        audioSrc = (Spinner) findViewById(R.id.spinner1);
        audioStrm = (Spinner) findViewById(R.id.spinner2);
        audioMode = (Spinner) findViewById(R.id.spinner3);
        mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setMode(AUDIOMODE);
        addButtonListeners();
        tbut1.setChecked(true);
    }

    public void addButtonListeners()
    {

        // tbut1 = (ToggleButton) findViewById(R.id.toggleButton);
        tbut1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on =
                        tbut1.isChecked();
                if (on) {
                    if (null != mAudioManager) {
                        mAudioManager.setSpeakerphoneOn(true);
                        Log.d("s@urav", "Speaker ON");
                    }
                } else {
                    if (null != mAudioManager) {
                        mAudioManager.setSpeakerphoneOn(false);
                        Log.d("s@urav", "Speaker OFF");
                    }
                } // TODO Auto-generated method stub
            }
        });

        start_rec = (Button) findViewById(R.id.start_rec);
        start_rec.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                start_rec();

            }
        });

        start_play = (Button) findViewById(R.id.start_play);
        start_play.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                start_play();
            }
        });
        stop_rec = (Button) findViewById(R.id.stop_rec);
        stop_rec.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                stop_rec();
            }
        });
        stop_play = (Button) findViewById(R.id.stop_play);
        stop_play.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                stop_play();
            }
        });
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        super.onDestroy();
    }

    private void start_rec()
    {
        // AUDIOSOURCE = (int) audioSrc.getSelectedItem();
        // AUDIOSTREAM = (int) audioStrm.getSelectedItem();
        // AUDIOMODE = (int) audioMode.getSelectedItem();
        if (recorder != null)
        {
            stop_rec();
        }
        AUDIOSOURCE = audioSrc.getSelectedItemPosition();
        AUDIOSTREAM = audioStrm.getSelectedItemPosition();
        AUDIOMODE = audioMode.getSelectedItemPosition();
        mAudioManager.setMode(AUDIOMODE);

        /*
         * if (audioSrc.getSelectedItemPosition() == 2 ||
         * audioSrc.getSelectedItemPosition() == 3 ||
         * audioSrc.getSelectedItemPosition() == 8 ||
         * audioSrc.getSelectedItemPosition() == 4) {
         * Toast.makeText(getApplicationContext(),
         * "Invalid Audio Source. Select Different one!",
         * Toast.LENGTH_LONG).show(); return; }
         */

        Log.d("s@urav", " AudioSource is:" + AUDIOSOURCE + " Stream:" + AUDIOSTREAM + " Mode:"
                + AUDIOMODE);
        /* mAudioManager.setSpeakerphoneOn(false); */
        if (playRunFlag) {
            stop_play();
        }
        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        try {
            recorder = new AudioRecord(AUDIOSOURCE, RECORDER_SAMPLERATE,
                    RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);
        } catch (Exception e)
        {
            Log.d("s@urav", "Exception:" + e);
            Toast.makeText(getApplicationContext(), "Invalid Audio Source. Select Different one!",
                    Toast.LENGTH_LONG).show();
            recorder = null;
            return;

        }
        if(recorder.getState() != AudioRecord.STATE_INITIALIZED){
            Toast.makeText(getApplicationContext(), "Recorder not Initialized!", Toast.LENGTH_SHORT).show();
            recorder.release();
            recorder = null;
            return;
        }
        if (null != recorder && recorder.getState() == AudioRecord.STATE_INITIALIZED) {
            recorder.startRecording();
            recordingThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    capRunFlag = true;
                    try {
                        recordAudio();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, "RECORD THREAD");
            recordingThread.start();
            Toast.makeText(getApplicationContext(), "Recording....", Toast.LENGTH_SHORT).show();
        }
       
    }

    protected void recordAudio() throws IOException {
        byte[] audioData = new byte[bufferSizeInBytes];
        int retVal = 0;
        File capfd = new File("/mnt/sdcard/audioCap.raw");
        FileOutputStream capFile = new FileOutputStream(capfd);
        while (capRunFlag) {

            if (null != recorder) {
                retVal = recorder.read(audioData, 0, bufferSizeInBytes);
                if (retVal > 0 && null != capFile) {
                    capFile.write(audioData, 0, bufferSizeInBytes);
                }
            }
        }

        if (null != capFile) {
            capFile.close();
            capFile = null;
        }
    }

    private void stop_rec()
    {
        capRunFlag = false;
        if (null != recorder) {
            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                recorder.stop();
            }
            recorder.release();
            recorder = null;
        }

        if (null != recordingThread) {
            recordingThread = null;
        }
        Toast.makeText(getApplicationContext(), "Recording Stopped.", Toast.LENGTH_SHORT).show();

    }

    private void start_play()
    {
        /* mAudioManager.setSpeakerphoneOn(true); */
        AUDIOMODE = audioMode.getSelectedItemPosition();
        AUDIOSTREAM = audioStrm.getSelectedItemPosition();
        mAudioManager.setMode(AUDIOMODE);
        if (capRunFlag) {
            stop_rec();
        }
        if (mAudioTrack != null)
        {
            stop_play();
        }
        int minPlayBuf = AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        mAudioTrack = new AudioTrack(AUDIOSTREAM,
                RECORDER_SAMPLERATE, mPlayChannelConfig, RECORDER_AUDIO_ENCODING, minPlayBuf,
                AudioTrack.MODE_STREAM);

        if (null != mAudioTrack) {
            mAudioTrack.play();

            playRunFlag = true;
            playThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        playAudio();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }, "PLAY_THREAD");
            playThread.start();
            Toast.makeText(getApplicationContext(), "Playing......", Toast.LENGTH_SHORT).show();
        }

    }

    public void playAudio() throws IOException
    {
        byte[] audioData = new byte[bufferSizeInBytes];
        int retVal = 0;
        File pbFile = new File("/mnt/sdcard/audioCap.raw");
        FileInputStream pbFileStrm = null;
        if (pbFile.exists()) {
            pbFileStrm = new FileInputStream(pbFile);
        } else {
            pbFile = new File("/mnt/sdcard/audioPB.raw");
            pbFileStrm = new FileInputStream(pbFile);
        }
        // Read data from File.
        while (playRunFlag) {
            if (null != mAudioTrack) {
                if (null != pbFileStrm) {
                    retVal = pbFileStrm.read(audioData, 0, bufferSizeInBytes);
                }
                if (retVal > 0)
                    mAudioTrack.write(audioData, 0, 640);
            }
        }

        if (null != pbFileStrm) {
            pbFileStrm.close();
            pbFileStrm = null;
        }

    }

    private void stop_play()
    {
        playRunFlag = false;
        if (null != mAudioTrack) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
        if (null != recorder) {
            recorder = null;
        }
        Toast.makeText(getApplicationContext(), "Stop Playing......", Toast.LENGTH_SHORT).show();

    }
}
