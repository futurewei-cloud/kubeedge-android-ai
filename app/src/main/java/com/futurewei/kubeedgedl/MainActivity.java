package com.futurewei.kubeedgedl;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final int MAX_IMAGE_DIMENSION = 4096;
    private RVAdapter rvAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        OpenCVLoader.initDebug();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_photo:
                sendMessage();
                break;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE) {

            if(data == null) return;
            Uri selectedImage = data.getData();

            Bitmap bitmap = null;

            try {
                bitmap = getCorrectlyOrientedImage(this, selectedImage);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Mat img = new Mat ();
            Utils.bitmapToMat(bitmap, img);

            Size sz = new Size(FrameRequest.MODEL_INPUT_WIDTH, FrameRequest.MODEL_INPUT_HEIGHT);
            Imgproc.resize(img, img, sz);
            //Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);

            callEdgeAPI(img);

            ImageView imageView = findViewById(R.id.imageView);
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(bitmap);
        }
    }

    private void initUI() {
        Toolbar myToolbar = findViewById(R.id.toolbar);
        myToolbar.setNavigationIcon(null);
        setSupportActionBar(myToolbar);
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setVisibility(View.INVISIBLE);
        RecyclerView rv = findViewById(R.id.rvEmotions);
        rv.setVisibility(View.INVISIBLE);
    }

    private void sendMessage() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");

        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

        startActivityForResult(chooserIntent, PICK_IMAGE);
    }

    private void callEdgeAPI(Mat img) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.145.83.59:8501")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ModelInference service = retrofit.create(ModelInference .class);

        Call<FrameResult> call = service.doModelInference(new FrameRequest(img));

        call.enqueue(new Callback<FrameResult>() {

            @Override
            public void onResponse(Call<FrameResult> call, Response<FrameResult> response) {
                FrameResult res = response.body();

                runOnUiThread(() -> {
                    RecyclerView rv = findViewById(R.id.rvEmotions);
                    rv.setVisibility(View.VISIBLE);

                    List<String> emotionScores = new ArrayList<>();

                    for(int i=0; i < FrameResult.Emotion.values().length; ++i) {
                        String label = FrameResult.Emotion.values()[i].toString();
                        NumberFormat fm = new DecimalFormat("#0.00000");
                        String score = fm.format(res.getPredictions().get(0).get(i));
                        String str = String.format("%s: %s", label, score);
                        emotionScores.add(str);
                    }

                    rv.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                    rvAdapter = new RVAdapter(emotionScores);
                    rv.setAdapter(rvAdapter);
                });
            }

            @Override
            public void onFailure(Call<FrameResult> call, Throwable t) {
                toastMessage("Failure calling Edge model inference API: " + t.getLocalizedMessage());
            }
        });
    }

    private void toastMessage(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private Bitmap getCorrectlyOrientedImage(Context context, Uri photoUri) throws IOException {
        InputStream is = context.getContentResolver().openInputStream(photoUri);
        BitmapFactory.Options dbo = new BitmapFactory.Options();
        dbo.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, dbo);
        is.close();

        int rotatedWidth, rotatedHeight;
        int orientation = getOrientation(context, photoUri);

        if (orientation == 90 || orientation == 270) {
            rotatedWidth = dbo.outHeight;
            rotatedHeight = dbo.outWidth;
        } else {
            rotatedWidth = dbo.outWidth;
            rotatedHeight = dbo.outHeight;
        }

        Bitmap srcBitmap;
        is = context.getContentResolver().openInputStream(photoUri);
        if (rotatedWidth > MAX_IMAGE_DIMENSION || rotatedHeight > MAX_IMAGE_DIMENSION) {
            float widthRatio = ((float) rotatedWidth) / ((float) MAX_IMAGE_DIMENSION);
            float heightRatio = ((float) rotatedHeight) / ((float) MAX_IMAGE_DIMENSION);
            float maxRatio = Math.max(widthRatio, heightRatio);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = (int) maxRatio;
            srcBitmap = BitmapFactory.decodeStream(is, null, options);
        } else {
            srcBitmap = BitmapFactory.decodeStream(is);
        }
        is.close();

        if (orientation > 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);

            srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(),
                    srcBitmap.getHeight(), matrix, true);
        }

        return srcBitmap;
    }

    private int getOrientation(Context context, Uri photoUri) {
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

        if (cursor.getCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();
        return cursor.getInt(0);
    }
}