package com.example.propertymanager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import java.io.File;
import java.io.FileOutputStream;

public class FullScreenImageActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        imageView = findViewById(R.id.imgFullScreen);
        String imageUrl = getIntent().getStringExtra("IMAGE_URL");

        // 1. Load Image
        if (imageUrl != null) {
            Glide.with(this).load(imageUrl).into(imageView);
        }

        // 2. Share Button Logic
        findViewById(R.id.fabShare).setOnClickListener(v -> shareImage());

        // 3. Close Button
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }

    private void shareImage() {
        // Get Bitmap from ImageView
        if (imageView.getDrawable() == null) {
            Toast.makeText(this, "Wait for image to load...", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
            Bitmap bitmap = drawable.getBitmap();

            // Save Bitmap to Cache Directory
            File cachePath = new File(getExternalCacheDir(), "my_images/");
            cachePath.mkdirs();
            File file = new File(cachePath, "shared_image.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // Create Share Intent
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share via"));

        } catch (Exception e) {
            Toast.makeText(this, "Error sharing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}