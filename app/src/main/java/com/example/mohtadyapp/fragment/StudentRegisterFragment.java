package com.example.mohtadyapp.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mohtadyapp.Hellper.DALAppWriteConnection;
import com.example.mohtadyapp.Hellper.VolunteerAppHelper;
import com.example.mohtadyapp.R;
import com.example.mohtadyapp.model.Student;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudentRegisterFragment extends Fragment {

    private static final String TAG = "StudentRegisterFragment";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnRegisterSuccess {
        void onStudentRegistered(Student student);
    }

    private OnRegisterSuccess onRegisterSuccess;
    private byte[] profileImageData;
    private String imageFileName = "";

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null && extras.containsKey("data")) {
                        Bitmap bitmap = (Bitmap) extras.get("data");
                        if (bitmap != null) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                            profileImageData = baos.toByteArray();
                            imageFileName = "student_" + System.currentTimeMillis() + ".jpg";
                        }
                    }
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(requireContext(), "يجب منح إذن الكاميرا لالتقاط الصورة", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> galleryPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchGallery();
                } else {
                    Toast.makeText(requireContext(), "يجب منح إذن المعرض لاختيار الصورة", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                            profileImageData = baos.toByteArray();
                            imageFileName = "student_" + System.currentTimeMillis() + ".jpg";
                        } catch (IOException e) {
                            Toast.makeText(requireContext(), "فشل تحميل الصورة", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    public void setOnRegisterSuccess(OnRegisterSuccess listener) { this.onRegisterSuccess = listener; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_student_register, container, false);

        v.findViewById(R.id.btn_camera).setOnClickListener(v1 -> requestCameraAndLaunch());
        v.findViewById(R.id.btn_gallery).setOnClickListener(v1 -> requestGalleryAndLaunch());

        v.findViewById(R.id.btn_register).setOnClickListener(v1 -> {
            TextInputEditText etName = v.findViewById(R.id.et_name);
            TextInputEditText etIdNumber = v.findViewById(R.id.et_id_number);
            TextInputEditText etSchool = v.findViewById(R.id.et_school_address);
            TextInputEditText etPhone = v.findViewById(R.id.et_phone);
            TextInputEditText etPassword = v.findViewById(R.id.et_password);
            TextInputEditText etBirthDate = v.findViewById(R.id.et_birth_date);
            TextInputEditText etRequiredHours = v.findViewById(R.id.et_required_hours);

            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String idNumber = etIdNumber.getText() != null ? etIdNumber.getText().toString().trim() : "";
            String school = etSchool.getText() != null ? etSchool.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
            String birthDate = etBirthDate.getText() != null ? etBirthDate.getText().toString().trim() : "";
            int requiredHours = 0;
            try {
                requiredHours = Integer.parseInt(etRequiredHours.getText() != null ? etRequiredHours.getText().toString() : "0");
            } catch (NumberFormatException ignored) {}

            if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "أدخل الاسم ورقم الهاتف وكلمة السر", Toast.LENGTH_SHORT).show();
                return;
            }

            Student student = new Student(name, "", idNumber, school, phone, password, birthDate, requiredHours);
            VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());

            v.findViewById(R.id.btn_register).setEnabled(false);
            executor.execute(() -> {
                try {
                    if (profileImageData != null && profileImageData.length > 0 && !imageFileName.isEmpty()) {
                        DALAppWriteConnection.OperationResult<DALAppWriteConnection.FileInfo> uploadResult =
                                helper.uploadImage(profileImageData, imageFileName);
                        if (uploadResult.success && uploadResult.data != null && uploadResult.data.fileUrl != null) {
                            student.setImageUrl(uploadResult.data.fileUrl);
                        }
                    }
                    DALAppWriteConnection.OperationResult<Student> result = helper.registerStudent(student);
                    mainHandler.post(() -> {
                        v.findViewById(R.id.btn_register).setEnabled(true);
                        if (result.success && result.data != null) {
                            Toast.makeText(requireContext(), "تم إنشاء الحساب بنجاح", Toast.LENGTH_SHORT).show();
                            if (onRegisterSuccess != null) onRegisterSuccess.onStudentRegistered(result.data);
                        } else {
                            String msg = result.message != null ? result.message : "فشل التسجيل";
                            Log.e(TAG, "registerStudent failed: " + msg);
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "registerStudent exception", e);
                    mainHandler.post(() -> {
                        v.findViewById(R.id.btn_register).setEnabled(true);
                        Toast.makeText(requireContext(), "خطأ: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }

    private void requestCameraAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void requestGalleryAndLaunch() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            launchGallery();
        } else {
            galleryPermissionLauncher.launch(permission);
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }
}
