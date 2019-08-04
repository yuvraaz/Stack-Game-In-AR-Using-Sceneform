package com.terminus.stackar;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.CamcorderProfile;
import android.media.SoundPool;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {


    private CustomArFragment arFragment;
    private ModelRenderable modelRenderable;
    private List<Node> nodes;
    private AnchorNode anchorNode;
    private Vector3 baseNodePosition;
    private boolean isOnXAxis = false, shouldMovePositive;
    private float totalMove = -0.45f, sizeX = 0.3f, sizeZ = 0.3f;
    private int nodeToHide = -1;
    private int score = 0;
    private TextView scoreText;
    private boolean isGameOver = false;
    private SoundPool soundPool;
    private int horizontalPlacementSound, verticalPlacementSound, gameOverSound, increaseSizeSound;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private RelativeLayout extraOptionsLayout;
    private ImageView moreOptions;
    private VideoRecorder videoRecorder;
    private float METRES_TO_MOVE;
    private boolean isGameStarted = false;
    private TextView diamondText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game);


        prefs = getSharedPreferences("stack_ar", Context.MODE_PRIVATE);
        editor = prefs.edit();


        switch (prefs.getString("difficulty", "medium")) {

            case "hard":
                METRES_TO_MOVE = 0.0193456789876543204226242f;
                break;
            case "medium":
                METRES_TO_MOVE = 0.0153456789876543204226242f;
                break;
            default:
                METRES_TO_MOVE = 0.0113456789876543204226242f;

        }


        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
        scoreText = findViewById(R.id.score);

        loadSoundPool();
        loadUIActions();



        makeRenderable(sizeX, sizeZ);

        ArSceneView sceneView = arFragment.getArSceneView();
        sceneView.getScene().addOnUpdateListener(this::onUpdateFrame);
        sceneView.setOnClickListener(view1 -> handleGameProcess());

        initializeVideoRecorder();

    }



    private void initializeVideoRecorder() {

        int quality;

        switch (prefs.getString("videoQuality", "fullHD")) {

            case "high":
                quality = CamcorderProfile.QUALITY_HIGH;
                break;
            case "2k":
                quality = CamcorderProfile.QUALITY_2160P;
                break;
            case "fullHD":
                quality = CamcorderProfile.QUALITY_1080P;
                break;
            case "HD":
                quality = CamcorderProfile.QUALITY_720P;
                break;
            default:
                quality = CamcorderProfile.QUALITY_480P;

        }


        videoRecorder = new VideoRecorder();
        videoRecorder.setSceneView(arFragment.getArSceneView());

        int orientation = getResources().getConfiguration().orientation;
        videoRecorder.setVideoQuality(quality, orientation);

    }


    private void handleGameProcess() {

        if (isGameOver || !isGameStarted)
            return;

        if (isOnXAxis) {

            sizeX -= Math.abs(totalMove);

            Node node = nodes.get(nodes.size() - 1);
            Vector3 position = node.getLocalPosition();
            position.x -= (totalMove / 2f);
            node.setLocalPosition(position);

            if (sizeX <= 0) {

                gameOver();
                return;

            }

            hideExtraNodes();
            makeRenderable(sizeX, sizeZ);

            if (prefs.getBoolean("isVolumeOn", true))
                soundPool.play(verticalPlacementSound, 1, 1, 1, 0, 1);

        } else {

            sizeZ -= Math.abs(totalMove);

            Node node = nodes.get(nodes.size() - 1);
            Vector3 position = node.getLocalPosition();
            position.z -= (totalMove / 2f);
            node.setLocalPosition(position);

            if (sizeZ <= 0) {
                gameOver();
                return;
            }

            hideExtraNodes();
            makeRenderable(sizeX, sizeZ);

            if (prefs.getBoolean("isVolumeOn", true))
                soundPool.play(horizontalPlacementSound, 1, 1, 1, 0, 1);

        }

        score++;

        scoreText.setText(score + "");

        totalMove = -0.45f;

        int diamondsReserve = prefs.getInt("diamondsReserve", 0);
        diamondsReserve++;
        setDiamondText(diamondsReserve);
        editor.putInt("diamondsReserve", diamondsReserve);
        editor.apply();

    }


    private void hideExtraNodes() {

        if (nodes != null) {

            if (nodes.size() == 12)
                nodeToHide = 0;

        }

        if (nodeToHide != -1) {
            nodes.get(nodeToHide).setParent(null);

            for (Node node : nodes) {

                if (node.getParent() != null) {

                    new Thread(() -> {

                        for (int i = 0; i < 20; i++) {

                            runOnUiThread(() -> {
                                Vector3 position = node.getLocalPosition();
                                position.y -= 0.003f;
                                node.setLocalPosition(position);
                            });

                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }

                    }).start();

                }

            }

            nodeToHide++;
        }

    }




    private void loadUIActions() {

        extraOptionsLayout = findViewById(R.id.extraOptionsLayout);
        ImageView closeOptions = findViewById(R.id.close);
        ImageView replay = findViewById(R.id.replay);
        ImageView toggleRecording = findViewById(R.id.toggleRecording);
        moreOptions = findViewById(R.id.moreOptions);
        ImageView back = findViewById(R.id.back);
        RelativeLayout increaseBlockSize = findViewById(R.id.increaseBlockSize);
        diamondText = findViewById(R.id.diamondText);

        setDiamondText((prefs.getInt("diamondsReserve", 0)));

        increaseBlockSize.setOnClickListener(v -> {

            if (isGameOver)
                return;

            int diamondsReserve = prefs.getInt("diamondsReserve", 0);

            if (diamondsReserve < 30) {
                Toast.makeText(this, "30 diamonds required for the action", Toast.LENGTH_SHORT).show();
                return;
            }

            diamondsReserve -= 30;
            editor.putInt("diamondsReserve", diamondsReserve);
            editor.apply();
            setDiamondText(diamondsReserve);

            sizeX += 0.05f;
            sizeZ += 0.05f;

            if (modelRenderable != null) {
                modelRenderable = ShapeFactory.makeCube(
                        new Vector3(sizeX, 0.06f, sizeZ),
                        new Vector3(0f, 0.06f, 0f),
                        modelRenderable.getMaterial());
                nodes.get(nodes.size() - 1).setRenderable(modelRenderable);

                soundPool.play(increaseSizeSound, 1, 1, 1, 0, 1);
            }

        });


        back.setOnClickListener(v -> onBackPressed());


        closeOptions.setOnClickListener(view1 -> {

            extraOptionsLayout.setVisibility(View.GONE);
            moreOptions.setVisibility(View.VISIBLE);


        });

        moreOptions.setOnClickListener(view1 -> {

            extraOptionsLayout.setVisibility(View.VISIBLE);
            moreOptions.setVisibility(View.GONE);

        });


        replay.setOnClickListener(view1 -> handleReplayDialog());


        toggleRecording.setOnClickListener(v -> onToggleRecording(toggleRecording));


    }

    private void setDiamondText (int amount) {
        diamondText.setText(amount + "");
    }

    private void handleReplayDialog () {

        View alertView = getLayoutInflater().inflate(R.layout.replay_alert_dialog, null);

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setView(alertView);
        alertDialog.show();

        Button yes = alertView.findViewById(R.id.yes);
        Button no = alertView.findViewById(R.id.no);

        no.setOnClickListener(v -> alertDialog.dismiss());



        yes.setOnClickListener(v -> {

            startActivity(new Intent(this, GameActivity.class));
            finish();

        });

    }


    private void onToggleRecording(ImageView toggleRecording) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return;
        }


        boolean isRecording = videoRecorder.onToggleRecord();

        if (isRecording)
            toggleRecording.setColorFilter(android.graphics.Color.RED);
        else
            toggleRecording.setColorFilter(android.graphics.Color.WHITE);

    }

    private void loadSoundPool() {

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build();

        soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .build();

        horizontalPlacementSound = soundPool.load(this, R.raw.horizontal_placement, 1);
        verticalPlacementSound = soundPool.load(this, R.raw.vertical_placement, 1);
        gameOverSound = soundPool.load(this, R.raw.game_over, 1);
        increaseSizeSound = soundPool.load(this, R.raw.increase_size, 1);
    }





    private void gameOver() {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("score", score);

            JSONArray jsonArray = new JSONArray(prefs.getString("highScores", "[]"));
            jsonArray.put(jsonObject);

            editor.putString("highScores", jsonArray.toString());
            editor.apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }

        nodes.get(nodes.size() - 1).setParent(null);


        soundPool.play(gameOverSound, 1, 1, 1, 0, 1);


        showAllNodes();

        extraOptionsLayout.setVisibility(View.VISIBLE);
        moreOptions.setVisibility(View.GONE);

        isGameOver = true;

    }





    private void showAllNodes() {

        for (int i = 0;i < nodes.size() - 1; i++) {

            Node node = nodes.get(i);

            if (node.getParent() == null)
                node.setParent(anchorNode);
        }

        for (int i = 0; i < nodes.size() - 1; i++) {

            Node node = nodes.get(i);

            Vector3 position = node.getLocalPosition();
            position.y = baseNodePosition.y;

            position.y += i * 0.06f;

            node.setLocalPosition(position);

        }

    }





    private void onUpdateFrame(FrameTime unusedFrameTime) {

        Frame frame = arFragment.getArSceneView().getArFrame();

        Collection<Plane> planes = frame.getUpdatedTrackables(Plane.class);

        if (nodes == null) {
            for (Plane plane : planes) {

                if (plane.getTrackingState() == TrackingState.TRACKING) {

                    startGame(plane.createAnchor(plane.getCenterPose()));
                    moreOptions.setVisibility(View.VISIBLE);
                    extraOptionsLayout.setVisibility(View.GONE);

                    arFragment.getPlaneDiscoveryController().hide();

                    return;
                }

            }
        }

        if (nodes == null || isGameOver)
            return;

        Node activeNode = nodes.get(nodes.size() - 1);


        if (isOnXAxis) {

            Vector3 position = activeNode.getLocalPosition();

            if (totalMove <= -0.45f)
                shouldMovePositive = true;
            else if (totalMove >= 0.45f)
                shouldMovePositive = false;


            if (shouldMovePositive) {

                position.x += METRES_TO_MOVE;
                totalMove += METRES_TO_MOVE;
                activeNode.setLocalPosition(position);

            } else {

                position.x -= METRES_TO_MOVE;
                totalMove -= METRES_TO_MOVE;
                activeNode.setLocalPosition(position);

            }


        } else {

            Vector3 position = activeNode.getLocalPosition();

            if (totalMove <= -0.45f)
                shouldMovePositive = true;
            else if (totalMove >= 0.45f)
                shouldMovePositive = false;


            if (shouldMovePositive) {

                position.z += METRES_TO_MOVE;
                totalMove += METRES_TO_MOVE;
                activeNode.setLocalPosition(position);

            } else {

                position.z -= METRES_TO_MOVE;
                totalMove -= METRES_TO_MOVE;
                activeNode.setLocalPosition(position);

            }
        }




    }





    private void startGame(Anchor anchor) {

        anchorNode = new AnchorNode(anchor);
        arFragment.getArSceneView().getScene().addChild(anchorNode);

        nodes = new ArrayList<>();

        Node node = new Node();
        node.setRenderable(modelRenderable);
        node.setParent(anchorNode);

        baseNodePosition = node.getLocalPosition();

        nodes.add(node);
        addNewNode();

        isGameStarted = true;

    }






    private void addNewNode() {

        Node node = new Node();
        node.setRenderable(modelRenderable);
        node.setParent(anchorNode);

        int totalNodes = nodes.size();

        if (nodeToHide != -1)
            totalNodes = 11;

        Vector3 position = new Vector3();
        position.y = baseNodePosition.y;
        position.x = baseNodePosition.x;
        position.z = baseNodePosition.z;

        position.y += totalNodes * 0.06f;

        Node topNode = nodes.get(nodes.size() - 1);
        position.z = topNode.getLocalPosition().z;
        position.x = topNode.getLocalPosition().x;

        if (isOnXAxis)
            position.x = topNode.getLocalPosition().x;
        else
            position.z = topNode.getLocalPosition().z;

        if (!isOnXAxis) {
            isOnXAxis = true;
            position.x -= 0.45f;

        } else {
            isOnXAxis = false;
            position.z -= 0.45f;
        }

        node.setLocalPosition(position);
        nodes.add(node);
    }






    private void makeRenderable(float sizeX, float sizeZ) {

        int color = getColor();

        if (modelRenderable != null) {

            Material temp = modelRenderable.getMaterial();
            Node node = nodes.get(nodes.size() - 1);
            modelRenderable = ShapeFactory.makeCube(

                    new Vector3(sizeX, 0.06f, sizeZ),
                    new Vector3(0f, 0.06f, 0f),
                    temp

            );
            node.setRenderable(modelRenderable);

        }

        MaterialFactory
                .makeOpaqueWithColor(this, new Color(color))
                .thenAccept(material -> {
                    modelRenderable = ShapeFactory.makeCube(

                            new Vector3(sizeX, 0.06f, sizeZ),
                            new Vector3(0f, 0.06f, 0f),
                            material

                    );

                    addNewNode();
                });
    }





    private int getColor() {

        Random random = new Random();
        int randNum = random.nextInt(18);
        int color;

        switch (randNum) {
            case 0:
                color = getColor(R.color.colorAccent);
                break;
            case 1:
                color = getColor(R.color.colorPrimary);
                break;
            case 3:
                color = getColor(R.color.colorPrimaryDark);
                break;
            case 4:
                color = getColor(R.color.theme1Accent);
                break;
            case 5:
                color = getColor(R.color.theme1Primary);
                break;
            case 6:
                color = getColor(R.color.theme1PrimaryDark);
                break;
            case 7:
                color = getColor(R.color.theme2Accent);
                break;
            case 8:
                color = getColor(R.color.theme2Primary);
                break;
            case 9:
                color = getColor(R.color.theme2PrimaryDark);
                break;
            case 10:
                color = getColor(R.color.theme3Accent);
                break;
            case 11:
                color = getColor(R.color.theme3Primary);
                break;
            case 12:
                color = getColor(R.color.theme3PrimaryDark);
                break;
            case 13:
                color = getColor(R.color.theme4Accent);
                break;
            case 14:
                color = getColor(R.color.theme4Primary);
                break;
            case 15:
                color = getColor(R.color.theme4PrimaryDark);
                break;
            case 16:
                color = getColor(R.color.theme5Accent);
                break;
            case 17:
                color = getColor(R.color.theme5Primary);
                break;
            default:
                color = getColor(R.color.theme5PrimaryDark);

        }

        return color;

    }


    @Override
    protected void onPause() {
        super.onPause();

        if (videoRecorder.isRecording())
            videoRecorder.onToggleRecord();

    }
}
