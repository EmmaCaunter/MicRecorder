package recorder.appix.com.micrecorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private boolean isRecording;
    private ImageButton playButton;

    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private static int SAMPLE_RATE = 44100;
    private static String LOG_TAG = "Mic Recorder";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playButton = findViewById(R.id.play_button);
        playButton.setOnClickListener(v -> startRecording());
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        findViewById(R.id.stop_button).setOnClickListener(v -> stopRecording());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    private File getFile() {
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("hh_mm_ss");
        String dateString = df.format(c);
        String fileName = "sample_" + dateString + ".pcm";
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), fileName);
        return file;
    }

    private void startRecording() {
        if (!isRecording) {
            recordAudio();
            isRecording = true;

            playButton.setBackgroundResource(R.drawable.active_circle_background);
        }

    }

    void recordAudio() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                    // buffer size in bytes
                    int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);

                    if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                        bufferSize = SAMPLE_RATE * 2;
                    }

                    short[] audioBuffer = new short[bufferSize / 2];

                    AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                            SAMPLE_RATE,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            bufferSize);

                    if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                        Log.e(LOG_TAG, "Audio Record can't initialize!");
                        return;
                    }

                    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(getFile())));

                    record.startRecording();

                    Log.v(LOG_TAG, "Start recording");

                    long shortsRead = 0;
                    while (isRecording) {
                        int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
                        shortsRead += numberOfShort;
                        for (int i = 0; i < numberOfShort; i++) {
                            dos.writeShort(audioBuffer[i]);
                        }


                        // Do something with the audioBuffer
                    }

                    record.stop();
                    record.release();
                    dos.close();
                    Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead));

                }catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }).start();
    }

    private void stopRecording() {
        if (isRecording) {
            isRecording = false;
            playButton.setBackgroundResource(R.drawable.circle_background);
        }
    }
}
