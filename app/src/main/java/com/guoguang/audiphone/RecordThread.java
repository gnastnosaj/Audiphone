package com.guoguang.audiphone;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

/**
 * Created by jasontsang on 2/25/16.
 */
public class RecordThread extends Thread {

    private static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    private int frequency = 44100;
    private int recBufSize;
    private int plyBufSize;

    public static boolean isCAMCORDER = true;

    private boolean recording = true;
    private boolean restarting = false;

    private AudioRecord audioRecord;
    private AudioTrack audioTrack;

    @Override
    public void run() {
            int plyBufSize = AudioTrack.getMinBufferSize(frequency,
                    channelConfiguration, audioEncoding);

            if (isCAMCORDER) {
                createAudioRecord(MediaRecorder.AudioSource.CAMCORDER);
            } else {
                createAudioRecord(MediaRecorder.AudioSource.MIC);
            }

            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency,
                    channelConfiguration, audioEncoding, plyBufSize, AudioTrack.MODE_STREAM);

            byte[] recBuf = new byte[recBufSize];
            audioRecord.startRecording();
            audioTrack.play();

            while (true) {
                if (restarting) {
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                    if (isCAMCORDER) {
                        audioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, frequency,
                                channelConfiguration, audioEncoding, recBufSize);
                    } else {
                        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,
                                channelConfiguration, audioEncoding, recBufSize);
                    }
                    audioRecord.startRecording();
                    restarting = false;
                    continue;
                }
                if (recording) {
                    int readLen = audioRecord.read(recBuf, 0, recBufSize);
                    audioTrack.write(recBuf, 0, readLen);
                } else {
                    audioTrack.stop();
                    audioTrack.release();
                    audioTrack = null;
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                    break;
                }
            }
    }

    public void cancel() {
        recording = false;
    }

    public void restart() {
        restarting = true;
    }

    public int getAudioSessionId() {
        if(audioTrack != null) {
            return audioTrack.getAudioSessionId();
        }else {
            return 0;
        }
    }

    private void createAudioRecord(int audioSource) {
        try {
            recBufSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
            audioRecord = new AudioRecord(audioSource, frequency, channelConfiguration, audioEncoding, recBufSize);
        }catch (Exception e) {
            frequency = 16000;
            try {
                recBufSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
                audioRecord = new AudioRecord(audioSource, frequency, channelConfiguration, audioEncoding, recBufSize);
            }catch (Exception e1) {
                frequency = 8000;
                try{
                    recBufSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
                    audioRecord = new AudioRecord(audioSource, frequency, channelConfiguration, audioEncoding, recBufSize);
                }catch (Exception e2) {
                    try {
                        recBufSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
                        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, recBufSize);
                    }catch (Exception e3) {
                        try {
                            recBufSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
                            audioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, frequency, channelConfiguration, audioEncoding, recBufSize);
                        }catch (Exception e4) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
