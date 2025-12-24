package com.example.propertymanager;

import android.app.DatePickerDialog;
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
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTenantActivity extends AppCompatActivity {

    private EditText etName, etPhone, etParentPhone, etCompany, etRoom, etMoveInDate, etVehicleNumber;
    private Spinner spinnerHostel, spinnerVehicleType;
    private ImageView imgAadharFront, imgAadharBack, imgForm;
    private Button btnSave;
    private ProgressBar progressBar;

    private Uri uriFront, uriBack, uriForm;
    private final int REQ_FRONT = 101, REQ_BACK = 102, REQ_FORM = 103;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tenant);

        initViews();
        setupSpinners();

        Tenant existing = (Tenant) getIntent().getSerializableExtra("TENANT_DATA");
        if (existing != null) {
            prefillData(existing);
        } else if (getIntent().hasExtra("PREFILL_ROOM")) {
            etRoom.setText(getIntent().getStringExtra("PREFILL_ROOM"));
            etName.requestFocus();
        }

        etMoveInDate.setOnClickListener(v -> showDatePicker());
        imgAadharFront.setOnClickListener(v -> pickImage(REQ_FRONT));
        imgAadharBack.setOnClickListener(v -> pickImage(REQ_BACK));
        imgForm.setOnClickListener(v -> pickImage(REQ_FORM));

        btnSave.setOnClickListener(v -> validateAndUpload());
    }

    private void prefillData(Tenant t) {
        etName.setText(t.getName());
        etPhone.setText(t.getPhone());
        etParentPhone.setText(t.getParentPhone());
        etCompany.setText(t.getCompanyName());
        etRoom.setText(t.getRoomNumber());
        etMoveInDate.setText(t.getMoveInDate());
        etVehicleNumber.setText(t.getVehicleNumber());

        if(t.getAadharFrontUrl() != null) com.bumptech.glide.Glide.with(this).load(t.getAadharFrontUrl()).into(imgAadharFront);
        if(t.getAadharBackUrl() != null) com.bumptech.glide.Glide.with(this).load(t.getAadharBackUrl()).into(imgAadharBack);
        if(t.getFormPhotoUrl() != null) com.bumptech.glide.Glide.with(this).load(t.getFormPhotoUrl()).into(imgForm);

        btnSave.setText("Update Tenant");
    }

    private void validateAndUpload() {
        String name = etName.getText().toString().trim();
        String roomStr = etRoom.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String parentPhone = etParentPhone.getText().toString().trim();
        String selectedProperty = spinnerHostel.getSelectedItem().toString();

        if (name.isEmpty() || roomStr.isEmpty()) {
            Toast.makeText(this, "Name and Room are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.length() != 10 || !phone.matches("\\d+")) {
            etPhone.setError("Must be exactly 10 digits");
            etPhone.requestFocus();
            return;
        }
        if (parentPhone.length() != 10 || !parentPhone.matches("\\d+")) {
            etParentPhone.setError("Must be exactly 10 digits");
            etParentPhone.requestFocus();
            return;
        }

        if (!isValidRoomRange(selectedProperty, roomStr)) {
            etRoom.setError("Invalid Room");
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Invalid " + selectedProperty + " Room")
                    .setMessage(getRangeErrorMessage(selectedProperty))
                    .setPositiveButton("OK", (dialog, which) -> {
                        etRoom.setText("");
                        etRoom.requestFocus();
                    })
                    .show();
            return;
        }

        toggleLoading(true);

        Tenant existing = (Tenant) getIntent().getSerializableExtra("TENANT_DATA");
        String fUrl = (existing != null) ? existing.getAadharFrontUrl() : "";
        String bUrl = (existing != null) ? existing.getAadharBackUrl() : "";
        String formUrl = (existing != null) ? existing.getFormPhotoUrl() : "";

        handleUpload(uriFront, "front", fUrl, finalFront -> {
            handleUpload(uriBack, "back", bUrl, finalBack -> {
                handleUpload(uriForm, "form", formUrl, finalForm -> {
                    saveTenantToSupabase(finalFront, finalBack, finalForm);
                });
            });
        });
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

    private void handleUpload(Uri newUri, String type, String oldUrl, OnImageUploadListener listener) {
        if (newUri != null) {
            uploadToSupabase(newUri, type, listener);
        } else {
            listener.onUpload(oldUrl);
        }
    }

    private void saveTenantToSupabase(String f, String b, String form) {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String parent = etParentPhone.getText().toString().trim();
        String company = etCompany.getText().toString().trim();
        String room = etRoom.getText().toString().trim();
        String date = etMoveInDate.getText().toString().trim();
        String hostel = spinnerHostel.getSelectedItem().toString();
        String vNum = etVehicleNumber.getText().toString().trim();
        String vType = spinnerVehicleType.getSelectedItem().toString();

        String floor = (room.length() >= 3) ? room.substring(0, 1) : "Ground";
        Tenant tenant = new Tenant(name, phone, hostel, floor, room, f, b, form, date, parent, company);
        tenant.setVehicleNumber(vNum);
        tenant.setVehicleType(vType.equals("None") ? "" : vType);

        Tenant existing = (Tenant) getIntent().getSerializableExtra("TENANT_DATA");

        if (existing != null && existing.getId() != null) {
            SupabaseClient.getService().updateTenant(SupabaseClient.getKey(), SupabaseClient.getAuth(), "eq." + existing.getId(), tenant)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AddTenantActivity.this, "Updated Successfully!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else { toggleLoading(false); }
                        }
                        @Override public void onFailure(Call<Void> call, Throwable t) { toggleLoading(false); }
                    });
        } else {
            SupabaseClient.getService().addTenant(SupabaseClient.getKey(), SupabaseClient.getAuth(), "return=minimal", tenant)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AddTenantActivity.this, "Added Successfully!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else { toggleLoading(false); }
                        }
                        @Override public void onFailure(Call<Void> call, Throwable t) { toggleLoading(false); }
                    });
        }
    }

    // --- UPDATED UPLOAD METHOD WITH COMPRESSION ---
    private void uploadToSupabase(Uri uri, String type, OnImageUploadListener listener) {
        try {
            // STEP 1: Compress the image first
            File file = compressImage(uri);

            // Fallback: If compression fails for some reason, try getting original file
            if (file == null) {
                file = getFileFromUri(uri);
            }

            if (file == null) {
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                toggleLoading(false);
                return;
            }

            // STEP 2: Upload the compressed file
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("", file.getName(), requestFile);
            String filename = System.currentTimeMillis() + "_" + type + ".jpg";

            SupabaseClient.getService().uploadImage(SupabaseClient.getKey(), SupabaseClient.getAuth(), filename, body)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                String fullUrl = SupabaseClient.BASE_URL + "/storage/v1/object/public/tenant_docs/" + filename;
                                listener.onUpload(fullUrl);
                            } else { toggleLoading(false); }
                        }
                        @Override public void onFailure(Call<ResponseBody> call, Throwable t) { toggleLoading(false); }
                    });
        } catch (Exception e) { toggleLoading(false); }
    }

    private void setupSpinners() {
        String[] hostels = {"Hostel", "Home", "Rizz"};
        spinnerHostel.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, hostels));
        String[] vehicleTypes = {"None", "Bike", "Scooty", "Car"};
        spinnerVehicleType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vehicleTypes));
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etParentPhone = findViewById(R.id.etParentPhone);
        etCompany = findViewById(R.id.etCompany);
        etRoom = findViewById(R.id.etRoom);
        etMoveInDate = findViewById(R.id.etMoveInDate);
        spinnerHostel = findViewById(R.id.spinnerHostel);
        etVehicleNumber = findViewById(R.id.etVehicleNumber);
        spinnerVehicleType = findViewById(R.id.spinnerVehicleType);
        imgAadharFront = findViewById(R.id.imgAadharFront);
        imgAadharBack = findViewById(R.id.imgAadharBack);
        imgForm = findViewById(R.id.imgForm);
        btnSave = findViewById(R.id.btnSaveTenant);
        progressBar = findViewById(R.id.progressBar);
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                etMoveInDate.setText(String.format("%02d-%02d-%d", dayOfMonth, month + 1, year)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
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
            Uri imageUri = data.getData();
            if (requestCode == REQ_FRONT) { uriFront = imageUri; imgAadharFront.setImageURI(imageUri); }
            else if (requestCode == REQ_BACK) { uriBack = imageUri; imgAadharBack.setImageURI(imageUri); }
            else if (requestCode == REQ_FORM) { uriForm = imageUri; imgForm.setImageURI(imageUri); }
        }
    }

    // --- NEW IMAGE COMPRESSOR FUNCTION ---
    private File compressImage(Uri imageUri) {
        try {
            // 1. Convert Uri to Bitmap
            android.graphics.Bitmap original = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            // 2. Resize the image first (Max 800px)
            int maxWidth = 800;
            int maxHeight = 800;
            float scale = Math.min(((float)maxWidth / original.getWidth()), ((float)maxHeight / original.getHeight()));

            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postScale(scale, scale);

            android.graphics.Bitmap resized = android.graphics.Bitmap.createBitmap(
                    original, 0, 0, original.getWidth(), original.getHeight(), matrix, true
            );

            // 3. Compress loop (Reduce quality until < 50KB)
            java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
            int quality = 95;

            do {
                stream.reset();
                resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, stream);
                quality -= 5;
            } while (stream.size() > 51200 && quality > 10); // 51200 bytes = 50KB

            // 4. Save the compressed data to a temporary file
            File file = new File(getCacheDir(), "compressed_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(stream.toByteArray());
            fos.flush();
            fos.close();

            return file;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Fallback method (used if compression fails)
    private File getFileFromUri(Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File file = new File(getCacheDir(), "temp_" + System.currentTimeMillis());
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, length);
        outputStream.close(); inputStream.close();
        return file;
    }

    private void toggleLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!isLoading);
        btnSave.setText(isLoading ? "Processing..." : (getIntent().hasExtra("TENANT_DATA") ? "Update Tenant" : "Save Tenant"));
    }

    interface OnImageUploadListener { void onUpload(String url); }
}