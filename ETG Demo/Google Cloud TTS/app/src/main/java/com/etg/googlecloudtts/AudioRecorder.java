package com.etg.googlecloudtts;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AudioRecorder {
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = SAMPLE_RATE / 4; // 250ms

    private boolean isRecording = false;
    private AudioRecord audioRecord;
    byte[] mAudioBuffer;

    @SuppressLint("MissingPermission")
    public void setupRecording() {
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize * 10);

        mAudioBuffer = new byte[BUFFER_SIZE * 2]; // PCM 16비트이므로 byte 배열 크기는 2배
    }

    public void startRecording() {
        audioRecord.startRecording();
        isRecording = true;
    }

    public byte[] getAudioBuffer() {
        if (isRecording) {
            audioRecord.read(mAudioBuffer, 0, mAudioBuffer.length);
        }
        return mAudioBuffer;
    }

    public void stopRecording() {
        if (isRecording) {
            isRecording = false;
            audioRecord.stop();
            //audioRecord.release();
        }
    }
}
