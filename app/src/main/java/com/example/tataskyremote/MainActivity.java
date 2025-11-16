package com.example.tataskyremote;

import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private HashMap<Integer, String> buttonChannelMap = new HashMap<>();
    private ConsumerIrManager irManager;
    private static final int CARRIER_FREQUENCY = 56000;

    // IR patterns for digits 0-9
    private final int[] KEY_0 = {2660, 889, 444, 444, 444, 444, 444, 444,
        444, 444, 444, 444, 444, 444, 444, 444,
        444, 444, 444, 444, 444, 444, 444, 444,
        444, 1333};
    private final int[] KEY_1 = {2660, 889, 444, 444, 444, 444, 444, 444,
        444, 1333, 444, 444, 444, 444, 444, 1333,
        444, 1333, 444, 444, 444, 444, 444, 444,
        444, 444};
    private final int[] KEY_2 = {2660, 889, 444, 444, 444, 444, 444, 444,
        444, 1333, 444, 444, 444, 1333, 444, 444,
        444, 1333, 444, 444, 444, 444, 444, 444,
        444, 444};
    private final int[] KEY_3 = {2660, 889, 444, 444, 444, 444, 444, 444,
        444, 1333, 444, 444, 444, 1333, 444, 1333,
        444, 444, 444, 444, 444, 444, 444, 444,
        444, 444};
    private final int[] KEY_4 = {2660, 889, 444, 444, 444, 444, 444, 444,
        444, 1333, 444, 444, 444, 444, 444, 444,
        444, 1333, 444, 1333, 444, 444, 444, 444,
        444, 444};
    private final int[] KEY_5 = {2660, 889, 444, 444, 444, 444, 444, 444,
        444, 1333, 444, 444, 444, 444, 444, 1333,
        444, 444, 444, 1333, 444, 444, 444, 444,
        444, 444};
    private final int[] KEY_6 = {2660, 889, 444, 444, 444, 444, 444, 444,
        444, 1333, 444, 444, 444, 444, 444, 1333,
        444, 1333, 444, 1333, 444, 444, 444, 444,
        444, 444};
    private final int[] KEY_7 = {2660, 889, 444, 444, 444, 444, 444, 444,
        444, 1333, 444, 1333, 444, 444, 444, 444,
        444, 444, 444, 444, 444, 444, 444, 1333,
        444, 444};
    private final int[] KEY_8 = {2660, 889, 444, 444, 444, 444, 444, 444,
        444, 1333, 444, 1333, 444, 444, 444, 1333,
        444, 444, 444, 444, 444, 444, 444, 1333,
        444, 444};
    private final int[] KEY_9 = {2660, 889, 444, 444, 444, 444, 444, 444,
        444, 1333, 444, 1333, 444, 444, 444, 1333,
        444, 1333, 444, 444, 444, 444, 444, 1333,
        444, 444};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        irManager = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);

        buttonChannelMap.put(R.id.btn_gemini_movies, "1431");
        buttonChannelMap.put(R.id.btn_star_maa_movies, "1433");
        buttonChannelMap.put(R.id.btn_star_maa_gold, "1435");
        buttonChannelMap.put(R.id.btn_zee_cinemalu, "1438");
        buttonChannelMap.put(R.id.btn_gemini_comedy, "1442");
        buttonChannelMap.put(R.id.btn_star_maa_music, "1486");
        buttonChannelMap.put(R.id.btn_star_maa, "1409");
        buttonChannelMap.put(R.id.btn_gemini_tv, "1406");
        buttonChannelMap.put(R.id.btn_zee_telugu, "1409");
        buttonChannelMap.put(R.id.btn_etv, "1412");
        buttonChannelMap.put(R.id.btn_gemini_music, "1484");
        buttonChannelMap.put(R.id.btn_etv_plus, "1417");
        buttonChannelMap.put(R.id.btn_etv_cinema, "1440");
        buttonChannelMap.put(R.id.btn_svbc, "1499");
        buttonChannelMap.put(R.id.btn_bhakti_tv, "1490");

        // Null safety for findViewById
        for (Integer id : buttonChannelMap.keySet()) {
            Button btn = findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View v) {
        String channelNumber = buttonChannelMap.get(v.getId());
        if (irManager == null || !irManager.hasIrEmitter()) {
            Toast.makeText(this, "IR emitter not available!", Toast.LENGTH_SHORT).show();
            return;
        }
        sendChannelAsync(channelNumber);
        Toast.makeText(this, "Switching to channel: " + channelNumber, Toast.LENGTH_SHORT).show();
    }

    // Sends IR patterns for each digit with delay, avoiding UI hang/crash
    private void sendChannelAsync(String channel) {
        Handler handler = new Handler();
        char[] digits = channel.toCharArray();
        for (int i = 0; i < digits.length; i++) {
            final int idx = i;
            handler.postDelayed(() -> {
                int[] pattern = getPatternForDigit(digits[idx]);
                if (pattern != null) {
                    irManager.transmit(CARRIER_FREQUENCY, pattern);
                }
            }, idx * 350);
        }
    }

    private int[] getPatternForDigit(char digit) {
        switch (digit) {
            case '0': return KEY_0;
            case '1': return KEY_1;
            case '2': return KEY_2;
            case '3': return KEY_3;
            case '4': return KEY_4;
            case '5': return KEY_5;
            case '6': return KEY_6;
            case '7': return KEY_7;
            case '8': return KEY_8;
            case '9': return KEY_9;
            default: return null;
        }
    }
}
