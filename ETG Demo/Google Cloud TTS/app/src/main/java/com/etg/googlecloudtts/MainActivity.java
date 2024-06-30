package com.etg.googlecloudtts;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeRequest;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private SpeechClient mSpeechClient;
    private AudioRecorder mAudioRecorder;

    private byte[] mByteArray;
    String mSttResult;
    TextView mResultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initializeSpeechClient();

        findViewById(R.id.start_button).setOnClickListener(view -> {
            if (requestPermissions()){
                mResultText.setText("Listening...");
                runSpeechToText();
            }
        });

        mResultText = findViewById(R.id.result_text_view);
    }

    private void runSpeechToText() {
        mAudioRecorder = new AudioRecorder();
        mAudioRecorder.setupRecording();
        mAudioRecorder.startRecording();

        mByteArray = null;

        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        Runnable runnable = new Runnable() {
            int readCnt = 0;
            @Override
            public void run() {
                mByteArray = appendByteArrays(mByteArray, mAudioRecorder.getAudioBuffer());

                if(++readCnt > 12) { // set reading count (4 * sec)
                    mAudioRecorder.stopRecording();
                    transcribeRecording(mByteArray);

                    service.shutdown();
                }
            }
        };
        service.scheduleWithFixedDelay(runnable, 250, 250, TimeUnit.MILLISECONDS);
    }

    private void initializeSpeechClient() {
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(getResources().openRawResource(R.raw.credentials));
            FixedCredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);
            mSpeechClient = SpeechClient.create(SpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build());
        } catch (IOException e) {

            Log.e("MAIN ACTIVITY", "initializeSpeechClient : " + e.getMessage());
        }
    }

    private void transcribeRecording(byte[] data) {
        try {
            Thread sttThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        RecognizeResponse response = mSpeechClient.recognize(createRecognizeRequestFromVoice(data));
                        mSttResult = "";

                        for (SpeechRecognitionResult result : response.getResultsList()) {
                            mSttResult = result.getAlternativesList().get(0).getTranscript();
                        }
                        runOnUiThread(() -> {
                            mResultText.setText(mSttResult);
                        });
                    } catch (Exception e) {
                        Log.e("MAIN ACTIVITY", "" + e.getMessage());
                    }
                }
            });
            sttThread.start();
        } catch (Exception e) {
            Log.e("MAIN ACTIVITY", "" + e.getMessage());
        }
    }

    private RecognizeRequest createRecognizeRequestFromVoice(byte[] audioData) {
        RecognitionAudio audioBytes = RecognitionAudio.newBuilder().setContent(ByteString.copyFrom(audioData)).build();
        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(16000)
                .setLanguageCode("ko-KR")
                .build();
        return RecognizeRequest.newBuilder()
                .setConfig(config)
                .setAudio(audioBytes)
                .build();
    }

    private byte[] appendByteArrays(byte[] array1, byte[] array2) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            outputStream.write(array1);
            outputStream.write(array2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    boolean requestPermissions() {
        if ((ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO}, 0);
            return false;
        } else {
            return true;
        }
    }
}