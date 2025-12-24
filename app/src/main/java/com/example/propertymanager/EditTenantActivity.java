package com.example.propertymanager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditTenantActivity extends AppCompatActivity {

    private EditText etName, etPhone, etParent, etCompany, etRoom, etVehicleNumber;
    private Spinner spinnerVehicleType;
    private ImageView imgFront, imgBack, imgForm;
    private Button btnSave;
    private ProgressBar progressBar;

    private Tenant tenant;
    private Uri newUriFront, newUriBack, newUriForm;
    private final int REQ_FRONT = 201, REQ_BACK = 202, REQ_FORM = 203;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_tenant);

        tenant = (Tenant) getIntent().getSerializableExtra("TENANT_DATA");

        if (tenant == null) {
            Toast.makeText(this, "Data is missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        preFillData();

        imgFront.setOnClickListener(v -> pickImage(REQ_FRONT));
        imgBack.setOnClickListener(v -> pickImage(REQ_BACK));
        imgForm.setOnClickListener(v -> pickImage(REQ_FORM));
        btnSave.setOnClickListener(v -> startSaveProcess());
    }

    private void initViews() {
        etName = findViewById(R.id.editName);
        etPhone = findViewById(R.id.editPhone);
        etParent = findViewById(R.id.editParent);
        etCompany = findViewById(R.id.editCompany);
        etRoom = findViewById(R.id.editRoom);
        etVehicleNumber = findViewById(R.id.editVehicleNumber);
        spinnerVehicleType = findViewById(R.id.spinnerEditVehicleType);
        imgFront = findViewById(R.id.imgEditFront);
        imgBack = findViewById(R.id.imgEditBack);
        imgForm = findViewById(R.id.imgEditForm);
        btnSave = findViewById(R.id.btnSaveChanges);
        progressBar = findViewById(R.id.progressBar);

        String[] vTypes = {"None", "Bike", "Scooty", "Car"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vTypes);
        spinnerVehicleType.setAdapter(adapter);
    }

    private void preFillData() {
        etName.setText(tenant.getName());
        etPhone.setText(tenant.getPhone());
        etParent.setText(tenant.getParentPhone());
        etCompany.setText(tenant.getCompanyName());
        etRoom.setText(tenant.getRoomNumber());
        etVehicleNumber.setText(tenant.getVehicleNumber());

        if (tenant.getVehicleType() != null) {
            ArrayAdapter adapter = (ArrayAdapter) spinnerVehicleType.getAdapter();
            spinnerVehicleType.setSelection(adapter.getPosition(tenant.getVehicleType()));
        }

        loadWithPicasso(tenant.getAadharFrontUrl(), imgFront);
        loadWithPicasso(tenant.getAadharBackUrl(), imgBack);
        loadWithPicasso(tenant.getFormPhotoUrl(), imgForm);
    }

    private void loadWithPicasso(String url, ImageView target) {
        if (url != null && !url.isEmpty()) {
            Picasso.get().load(url).placeholder(android.R.drawable.ic_menu_gallery).into(target);
        }
    }

    private void pickImage(int reqCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), reqCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == REQ_FRONT) { newUriFront = uri; imgFront.setImageURI(uri); }
            else if (requestCode == REQ_BACK) { newUriBack = uri; imgBack.setImageURI(uri); }
            else if (requestCode == REQ_FORM) { newUriForm = uri; imgForm.setImageURI(uri); }
        }
    }

    private void startSaveProcess() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String parent = etParent.getText().toString().trim();
        String roomStr = etRoom.getText().toString().trim();
        String property = tenant.getHostelName();

        if (name.isEmpty() || roomStr.isEmpty()) {
            Toast.makeText(this, "Name and Room are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.length() != 10 || !phone.matches("\\d+")) {
            etPhone.setError("Must be exactly 10 digits");
            etPhone.requestFocus();
            return;
        }
        if (parent.length() != 10 || !parent.matches("\\d+")) {
            etParent.setError("Must be exactly 10 digits");
            etParent.requestFocus();
            return;
        }

        if (!isValidRoomRange(property, roomStr)) {
            etRoom.setError("Invalid Room");
            new AlertDialog.Builder(this)
                    .setTitle("Invalid " + property + " Room")
                    .setMessage(getRangeErrorMessage(property))
                    .setPositiveButton("OK", (dialog, which) -> {
                        etRoom.requestFocus();
                    })
                    .show();
            return;
        }

        toggleLoading(true);
        uploadOrKeep(newUriFront, tenant.getAadharFrontUrl(), "front", urlF ->
                uploadOrKeep(newUriBack, tenant.getAadharBackUrl(), "back", urlB ->
                        uploadOrKeep(newUriForm, tenant.getFormPhotoUrl(), "form", urlForm ->
                                saveChangesToDb(urlF, urlB, urlForm))));
    }

    private boolean isValidRoomRange(String property, String roomStr) {
        try {
            int room = Integer.parseInt(roomStr);
            if (property.equalsIgnoreCase("Hostel")) {
                return (room >= 100 && room <= 119) || (room >= 201 && room <= 215) || (room >= 301 && room <= 318);
            } else if (property.equalsIgnoreCase("Home")) {
                return (room >= 101 && room <= 105) || (room >= 201 && room <= 204);
            } else if (property.equalsIgnoreCase("Rizz")) {
                return (room >= 101 && room <= 104) || (room >= 201 && room <= 205);
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String getRangeErrorMessage(String property) {
        if (property.equalsIgnoreCase("Hostel")) return "Hostel Valid Rooms:\n100-119\n201-215\n301-318";
        if (property.equalsIgnoreCase("Home")) return "Home Valid Rooms:\n101-105\n201-204";
        if (property.equalsIgnoreCase("Rizz")) return "Rizz Valid Rooms:\n101-104\n201-205";
        return "Invalid Room Number";
    }

    private void uploadOrKeep(Uri newUri, String oldUrl, String type, OnUrlReadyListener listener) {
        if (newUri == null) listener.onReady(oldUrl);
        else uploadImage(newUri, type, listener);
    }

    // --- UPDATED UPLOAD LOGIC WITH COMPRESSION ---
    private void uploadImage(Uri uri, String type, OnUrlReadyListener listener) {
        try {
            // STEP 1: Compress Image
            File file = compressImage(uri);
            if (file == null) file = getFileFromUri(uri); // Fallback

            if (file == null) {
                Toast.makeText(this, "Image Error", Toast.LENGTH_SHORT).show();
                toggleLoading(false);
                return;
            }

            // STEP 2: Upload Compressed File
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("", file.getName(), requestFile);
            String filename = System.currentTimeMillis() + "_" + type + "_EDIT.jpg";

            SupabaseClient.getService().uploadImage(SupabaseClient.getKey(), SupabaseClient.getAuth(), filename, body)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                listener.onReady(SupabaseClient.BASE_URL + "/storage/v1/object/public/tenant_docs/" + filename);
                            } else { toggleLoading(false); }
                        }
                        @Override public void onFailure(Call<ResponseBody> call, Throwable t) { toggleLoading(false); }
                    });
        } catch (Exception e) { toggleLoading(false); }
    }

    private void saveChangesToDb(String fUrl, String bUrl, String formUrl) {
        tenant.setName(etName.getText().toString());
        tenant.setPhone(etPhone.getText().toString());
        tenant.setParentPhone(etParent.getText().toString());
        tenant.setCompanyName(etCompany.getText().toString());
        tenant.setRoomNumber(etRoom.getText().toString());
        tenant.setVehicleNumber(etVehicleNumber.getText().toString());
        tenant.setVehicleType(spinnerVehicleType.getSelectedItem().toString());
        tenant.setAadharFrontUrl(fUrl);
        tenant.setAadharBackUrl(bUrl);
        tenant.setFormPhotoUrl(formUrl);

        String room = tenant.getRoomNumber();
        tenant.setFloor((room.length() >= 3) ? room.substring(0, 1) : "Ground");

        SupabaseClient.getService().updateTenant(SupabaseClient.getKey(), SupabaseClient.getAuth(), "eq." + tenant.getId(), tenant)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(EditTenantActivity.this, "Updated!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(EditTenantActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        } else { toggleLoading(false); }
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) { toggleLoading(false); }
                });
    }

    // --- NEW IMAGE COMPRESSOR FUNCTION ---
    private File compressImage(Uri imageUri) {
        try {
            android.graphics.Bitmap original = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            int maxWidth = 800; int maxHeight = 800;
            float scale = Math.min(((float)maxWidth / original.getWidth()), ((float)maxHeight / original.getHeight()));
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postScale(scale, scale);
            android.graphics.Bitmap resized = android.graphics.Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
            java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
            int quality = 95;
            do {
                stream.reset();
                resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, stream);
                quality -= 5;
            } while (stream.size() > 102400 && quality > 10);
            File file = new File(getCacheDir(), "compressed_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(stream.toByteArray());
            fos.flush(); fos.close();
            return file;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    private File getFileFromUri(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        File f = new File(getCacheDir(), "temp_edit_" + System.currentTimeMillis());
        FileOutputStream os = new FileOutputStream(f);
        byte[] b = new byte[1024]; int l;
        while ((l = is.read(b)) > 0) os.write(b, 0, l);
        os.close(); is.close();
        return f;
    }

    private void toggleLoading(boolean isLoad) {
        progressBar.setVisibility(isLoad ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!isLoad);
        btnSave.setText(isLoad ? "Updating..." : "Save Changes");
    }

    interface OnUrlReadyListener { void onReady(String url); }
}