package com.example.tataskyremote;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private HashMap<Integer, String> buttonChannelMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Map button IDs to channel numbers
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

        // Set listeners for all buttons
        for (Integer id : buttonChannelMap.keySet()) {
            Button btn = findViewById(id);
            btn.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        String channelNumber = buttonChannelMap.get(v.getId());
        // TODO: Send channelNumber to Tata Sky set-top box
        // For now, just show channel number
        Toast.makeText(this, "Switching to channel: " + channelNumber, Toast.LENGTH_SHORT).show();

        // Example: sendChannelToTataSky(channelNumber); // Implement as per Tata Sky API
    }
}
