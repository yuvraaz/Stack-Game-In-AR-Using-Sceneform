package com.terminus.stackar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {


    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;


    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        start();
    }

    private void start() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);



        prefs = getSharedPreferences("stack_ar", Context.MODE_PRIVATE);
        editor = prefs.edit();


        selectRandomBackground();

        handleUIActions();

    }


    private void handleUIActions() {


        ImageView settings = findViewById(R.id.settings);
        ImageView play = findViewById(R.id.playButton);
        ImageView scoreBoard = findViewById(R.id.scoreboard);
        ImageView volumeControl = findViewById(R.id.volumeControl);
        ImageView share = findViewById(R.id.share);
        TextView diamondText = findViewById(R.id.diamondText);

        diamondText.setText(prefs.getInt("diamondsReserve", 0) + "");

        if (!prefs.getBoolean("isVolumeOn", true))
            volumeControl.setImageResource(R.drawable.ic_volume_off_black_24dp);
        else
            volumeControl.setImageResource(R.drawable.ic_volume_up_black_24dp);


        settings.setOnClickListener(v -> handleSettings());

        volumeControl.setOnClickListener(view1 -> controlVolumeSettings(volumeControl));

        scoreBoard.setOnClickListener(view1 -> showHighScoresInList());

        play.setOnClickListener(v -> startActivity(new Intent(this, GameActivity.class)));

        share.setOnClickListener(v -> {

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "http://market.android.com/details?id=com.terminus.stackar");
            startActivity(Intent.createChooser(intent, "Share Stack AR"));

        });

    }



    private void handleSettings () {

        View settingsView = getLayoutInflater().inflate(R.layout.settings, null);

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setView(settingsView);
        alertDialog.show();

        RadioButton qualityHigh = settingsView.findViewById(R.id.qualityHigh);
        RadioButton twoK = settingsView.findViewById(R.id.twoK);
        RadioButton fullHD = settingsView.findViewById(R.id.fullHD);
        RadioButton HD = settingsView.findViewById(R.id.HD);
        RadioButton qualityMed = settingsView.findViewById(R.id.qualityMedium);
        Button close = settingsView.findViewById(R.id.close);


        close.setOnClickListener(v -> alertDialog.dismiss());

        switch (prefs.getString("videoQuality", "fullHD")) {


            case "high":
                qualityHigh.setChecked(true);
                break;
            case "2k":
                twoK.setChecked(true);
                break;
            case "fullHD":
                fullHD.setChecked(true);
                break;
            case "HD":
                HD.setChecked(true);
                break;
            case "medium":
                qualityMed.setChecked(true);
                break;


        }


        qualityHigh.setOnCheckedChangeListener((compoundButton, b) -> {

            if (b)
                editor.putString("videoQuality", "high");

        });


        twoK.setOnCheckedChangeListener((compoundButton, b) -> {

            if (b)
                editor.putString("videoQuality", "2k");

        });


        fullHD.setOnCheckedChangeListener((compoundButton, b) -> {

            if (b)
                editor.putString("videoQuality", "fullHD");

        });


        HD.setOnCheckedChangeListener((compoundButton, b) -> {

            if (b)
                editor.putString("videoQuality", "HD");

        });

        qualityMed.setOnCheckedChangeListener((compoundButton, b) -> {

            if (b)
                editor.putString("videoQuality", "medium");

        });

        alertDialog.setOnDismissListener(dialogInterface -> editor.apply());


        RadioButton hard = settingsView.findViewById(R.id.hard);
        RadioButton medium = settingsView.findViewById(R.id.medium);
        RadioButton easy = settingsView.findViewById(R.id.easy);

        String difficulty = prefs.getString("difficulty", "medium");

        switch (difficulty) {

            case "hard":
                hard.setChecked(true);
                break;
            case "medium":
                medium.setChecked(true);
                break;
            default:
                easy.setChecked(true);

        }

        hard.setOnCheckedChangeListener((compoundButton, b) -> {

            if (b)
                editor.putString("difficulty", "hard");

        });

        medium.setOnCheckedChangeListener((compoundButton, b) -> {

            if (b)
                editor.putString("difficulty", "medium");

        });

        easy.setOnCheckedChangeListener((compoundButton, b) -> {

            if (b)
                editor.putString("difficulty", "easy");

        });

    }



    private void controlVolumeSettings(ImageView volumeControl) {


        if (prefs.getBoolean("isVolumeOn", true)) {

            volumeControl.setImageResource(R.drawable.ic_volume_off_black_24dp);
            editor.putBoolean("isVolumeOn", false);

        }
        else {

            volumeControl.setImageResource(R.drawable.ic_volume_up_black_24dp);
            editor.putBoolean("isVolumeOn", true);

        }

        editor.apply();
    }





    private void showHighScoresInList() {

        try {
            JSONArray jsonArray = new JSONArray(prefs.getString("highScores", "[]"));

            List<Integer> scores = new ArrayList<>();

            for (int i = 0;i < jsonArray.length();i++) {
                scores.add(jsonArray.getJSONObject(i).getInt("score"));
            }

            Collections.sort(scores);
            Collections.reverse(scores);

            List<Integer> updatedList = new ArrayList<>();


            for (int i = 0;i < 20;i++) {

                if (i == scores.size())
                    break;

                updatedList.add(scores.get(i));
            }


            View builderView = getLayoutInflater().inflate(R.layout.high_scores, null);

            AlertDialog alertDialog = new AlertDialog.Builder(this).create();

            ListView highScoresList = builderView.findViewById(R.id.scoresList);
            TextView close = builderView.findViewById(R.id.close);
            highScoresList.setAdapter(new ScoresAdapter(updatedList));

            alertDialog.setView(builderView);
            alertDialog.show();

            close.setOnClickListener(v -> alertDialog.dismiss());


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }




    private void selectRandomBackground() {


        RelativeLayout background = findViewById(R.id.backgroundLayout);


        Random rand = new Random();

        switch (rand.nextInt(6)) {
            case 1:
                background.setBackgroundResource(R.drawable.gradient_1);

                break;
            case 2:
                background.setBackgroundResource(R.drawable.gradient_2);

                break;
            case 3:
                background.setBackgroundResource(R.drawable.gradient_3);

                break;
            case 4:
                background.setBackgroundResource(R.drawable.gradient_4);

                break;
            case 5:
                background.setBackgroundResource(R.drawable.gradient_5);

                break;
            default:
                background.setBackgroundResource(R.drawable.default_gradient);
        }

    }

    private class ScoresAdapter extends BaseAdapter {

        List<Integer> scores;

        ScoresAdapter (List<Integer> scores) {
            this.scores = scores;
        }

        @Override
        public int getCount() {
            return scores.size();
        }

        @Override
        public Object getItem(int i) {
            return scores.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {


            if (view == null)
                view = getLayoutInflater().inflate(R.layout.score_list_item, viewGroup, false);

            TextView rank = view.findViewById(R.id.rank);
            rank.setText((i + 1)+ ".  ");

            TextView score = view.findViewById(R.id.score);
            score.setText(scores.get(i) + "");

            return view;
        }
    }


}