package com.etg.asr;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

public class AudioBufferReader {
    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = 2000; // 읽어올 버퍼 크기

    private boolean isRecording = false;
    private AudioRecord audioRecord;
    byte[] mAudioBuffer;

    public void setupRecording() {
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        audioRecord = new AudioRecord(AudioSource.MIC,
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

    public String getAudioBuffer() {
        if (isRecording) {
            audioRecord.read(mAudioBuffer, 0, mAudioBuffer.length);
        }
        //return mAudioBuffer;
        int[] intArray = convertByteToIntArray(mAudioBuffer);
        String stringArray = Arrays.stream(intArray)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(", "));

        return stringArray;
    }

    public void stopRecording() {
        if (isRecording) {
            isRecording = false;
            audioRecord.stop();
            //audioRecord.release();
        }
    }

    public int[] convertByteToIntArray(byte[] byteArray) {
        int[] intArray = new int[byteArray.length / 2];

        for (int i = 0; i < intArray.length; i++) {
            short value = 0;
            // 리틀 엔디안으로 바이트 배열을 short로 변환
            value |= (byteArray[i * 2] & 0xFF);
            value |= (byteArray[i * 2 + 1] & 0xFF) << 8;
            intArray[i] = value;
        }
        return intArray;
    }
}
