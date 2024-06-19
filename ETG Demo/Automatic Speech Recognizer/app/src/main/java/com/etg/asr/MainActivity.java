package com.etg.asr;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    SpeechRecognizer mAsr;
    TextView mTextViewState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mTextViewState = findViewById(R.id.text_status_val);

        requestPermissions();

        // 1. Create SpeechRecognizer Intent
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        //intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");

        // 2. Setup and start new SpeechRecognizer (push to start)
        mAsr = SpeechRecognizer.createSpeechRecognizer(this);
        mAsr.setRecognitionListener(asrListener);

        Button buttonStart = findViewById(R.id.button_start_asr);
        buttonStart.setOnClickListener(v ->
                mAsr.startListening(intent)
        );
    }

    void requestPermissions() {
        if ((ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO}, 0);
        }
    }

    RecognitionListener asrListener = new RecognitionListener() {
        // Called when ready to start speaking
        @Override
        public void onReadyForSpeech(Bundle params) {
            Toast.makeText(getApplicationContext(), "Start speech recognition", Toast.LENGTH_SHORT).show();
            mTextViewState.setText("Speak now!");
        }

        // Called when speech begins
        @Override
        public void onBeginningOfSpeech() {
            mTextViewState.setText("Listening...");
        }

        // Notifies when sound level changes
        @Override
        public void onRmsChanged(float rmsdB) {
            // Not implemented
        }

        // Stores words in buffer as speech starts
        @Override
        public void onBufferReceived(byte[] buffer) {
            // Not implemented
        }

        // Called when speech stops
        @Override
        public void onEndOfSpeech() {
            mTextViewState.setText("End!");
        }

        // Called when an error occurs
        @Override
        public void onError(int error) {
            String message;
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "Audio error";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "Client error";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "Insufficient permissions";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "Network error";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "Network timeout";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "No match found";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "Recognizer busy";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "Server error";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "Speech timeout";
                    break;
                default:
                    message = "Unknown error";
                    break;
            }
            mTextViewState.setText("Error: " + message);
        }

        // Called when recognition results are ready
        @Override
        public void onResults(Bundle results) {
            // Stores spoken words in an ArrayList and displays them in textView
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null) {
                for (int i = 0; i < matches.size(); i++) {
                    mTextViewState.setText(matches.get(i));
                }
            }
        }

        // Called when partial recognition results are available
        @Override
        public void onPartialResults(Bundle partialResults) {
            // Not implemented
        }

        // Reserved for adding future events
        @Override
        public void onEvent(int eventType, Bundle params) {
            // Not implemented
        }
    };
}