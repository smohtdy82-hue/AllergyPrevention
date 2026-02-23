package com.example.mohtadyapp.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.example.mohtadyapp.model.Organization;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrgRegisterFragment extends Fragment {

    private static final String TAG = "OrgRegisterFragment";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnRegisterSuccess {
        void onOrgRegistered(Organization org);
    }

    private OnRegisterSuccess onRegisterSuccess;
    private byte[] logoImageData;
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
                            logoImageData = baos.toByteArray();
                            imageFileName = "org_" + System.currentTimeMillis() + ".jpg";
                            View view = getView();
                            if (view != null) {
                                ImageView iv = view.findViewById(R.id.iv_logo);
                                if (iv != null) iv.setImageBitmap(bitmap);
                            }
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
                            logoImageData = baos.toByteArray();
                            imageFileName = "org_" + System.currentTimeMillis() + ".jpg";
                            View view = getView();
                            if (view != null) {
                                ImageView iv = view.findViewById(R.id.iv_logo);
                                if (iv != null) iv.setImageBitmap(bitmap);
                            }
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
        View v = inflater.inflate(R.layout.fragment_org_register, container, false);

        v.findViewById(R.id.btn_camera).setOnClickListener(v1 -> requestCameraAndLaunch());
        v.findViewById(R.id.btn_gallery).setOnClickListener(v1 -> requestGalleryAndLaunch());

        MaterialCheckBox cb24Hours = v.findViewById(R.id.cb_24_hours);
        View layoutTimeRange = v.findViewById(R.id.layout_time_range);
        cb24Hours.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutTimeRange.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            validateDaysAndHours(v);
        });
        layoutTimeRange.setVisibility(cb24Hours.isChecked() ? View.GONE : View.VISIBLE);

        setupDayClickListeners(v);
        setupTimeSpinners(v);
        setupFieldValidation(v);
        validateDaysAndHours(v);

        v.findViewById(R.id.btn_register).setOnClickListener(v1 -> {
            TextInputEditText etName = v.findViewById(R.id.et_name);
            TextInputEditText etAddress = v.findViewById(R.id.et_address);
            TextInputEditText etEmail = v.findViewById(R.id.et_email);
            TextInputEditText etPassword = v.findViewById(R.id.et_password);
            TextInputEditText etPasswordConfirm = v.findViewById(R.id.et_password_confirm);
            TextInputEditText etTotalHours = v.findViewById(R.id.et_total_hours);
            TextInputEditText etContactPhone = v.findViewById(R.id.et_contact_phone);
            TextInputEditText etContactDetails = v.findViewById(R.id.et_contact_details);

            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

            String passwordConfirm = etPasswordConfirm.getText() != null ? etPasswordConfirm.getText().toString() : "";

            ArrayList<String> daysList = new ArrayList<>();
            if (((MaterialCheckBox) v.findViewById(R.id.cb_sat)).isChecked()) daysList.add("السبت");
            if (((MaterialCheckBox) v.findViewById(R.id.cb_sun)).isChecked()) daysList.add("الأحد");
            if (((MaterialCheckBox) v.findViewById(R.id.cb_mon)).isChecked()) daysList.add("الإثنين");
            if (((MaterialCheckBox) v.findViewById(R.id.cb_tue)).isChecked()) daysList.add("الثلاثاء");
            if (((MaterialCheckBox) v.findViewById(R.id.cb_wed)).isChecked()) daysList.add("الأربعاء");
            if (((MaterialCheckBox) v.findViewById(R.id.cb_thu)).isChecked()) daysList.add("الخميس");
            if (((MaterialCheckBox) v.findViewById(R.id.cb_fri)).isChecked()) daysList.add("الجمعة");
            String volunteerDays = String.join("، ", daysList);

            String volunteerHours;
            if (((MaterialCheckBox) v.findViewById(R.id.cb_24_hours)).isChecked()) {
                volunteerHours = "24 ساعة";
            } else {
                Spinner spinnerFrom = v.findViewById(R.id.spinner_from_time);
                Spinner spinnerTo = v.findViewById(R.id.spinner_to_time);
                String fromTime = spinnerFrom.getSelectedItem() != null ? spinnerFrom.getSelectedItem().toString() : "";
                String toTime = spinnerTo.getSelectedItem() != null ? spinnerTo.getSelectedItem().toString() : "";
                volunteerHours = (!fromTime.isEmpty() && !toTime.isEmpty()) ? (fromTime + " - " + toTime) : "";
            }
            int totalHours = 0;
            try {
                totalHours = Integer.parseInt(etTotalHours.getText() != null ? etTotalHours.getText().toString() : "0");
            } catch (NumberFormatException ignored) {}
            String contactPhone = etContactPhone.getText() != null ? etContactPhone.getText().toString().trim() : "";
            String contactDetails = etContactDetails.getText() != null ? etContactDetails.getText().toString().trim() : "";

            if (!validateAllFields(v)) return;
            if (daysList.isEmpty()) {
                Toast.makeText(requireContext(), "اختر يوم واحد على الأقل", Toast.LENGTH_SHORT).show();
                return;
            }
            if (volunteerHours.isEmpty() && !((MaterialCheckBox) v.findViewById(R.id.cb_24_hours)).isChecked()) {
                Toast.makeText(requireContext(), "أدخل أوقات التطوع أو اختر مفتوح 24 ساعة", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!((MaterialCheckBox) v.findViewById(R.id.cb_24_hours)).isChecked()) {
                Spinner spinnerFrom = v.findViewById(R.id.spinner_from_time);
                Spinner spinnerTo = v.findViewById(R.id.spinner_to_time);
                int fromPos = spinnerFrom.getSelectedItemPosition();
                int toPos = spinnerTo.getSelectedItemPosition();
                if (fromPos >= toPos) {
                    Toast.makeText(requireContext(), "ساعة البداية يجب أن تكون قبل ساعة النهاية", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Organization org = new Organization(name, "", address, email, password, volunteerDays, volunteerHours, totalHours, contactPhone, contactDetails);
            VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());

            v.findViewById(R.id.btn_register).setEnabled(false);
            executor.execute(() -> {
                try {
                    if (logoImageData != null && logoImageData.length > 0 && !imageFileName.isEmpty()) {
                        DALAppWriteConnection.OperationResult<DALAppWriteConnection.FileInfo> uploadResult =
                                helper.uploadImage(logoImageData, imageFileName);
                        if (uploadResult.success && uploadResult.data != null && uploadResult.data.fileUrl != null) {
                            org.setImageUrl(uploadResult.data.fileUrl);
                        }
                    }
                    DALAppWriteConnection.OperationResult<Organization> result = helper.registerOrganization(org, password);
                    mainHandler.post(() -> {
                        v.findViewById(R.id.btn_register).setEnabled(true);
                        if (result.success && result.data != null) {
                            Toast.makeText(requireContext(), "تم إنشاء المؤسسة بنجاح", Toast.LENGTH_SHORT).show();
                            if (onRegisterSuccess != null) onRegisterSuccess.onOrgRegistered(result.data);
                        } else {
                            String msg = result.message != null ? result.message : "فشل التسجيل";
                            Log.e(TAG, "registerOrganization failed: " + msg);
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "registerOrganization exception", e);
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

    private void setupDayClickListeners(View v) {
        int[] dayIds = {R.id.day_sat, R.id.day_sun, R.id.day_mon, R.id.day_tue, R.id.day_wed, R.id.day_thu, R.id.day_fri};
        int[] cbIds = {R.id.cb_sat, R.id.cb_sun, R.id.cb_mon, R.id.cb_tue, R.id.cb_wed, R.id.cb_thu, R.id.cb_fri};
        for (int i = 0; i < dayIds.length; i++) {
            View dayLayout = v.findViewById(dayIds[i]);
            MaterialCheckBox cb = v.findViewById(cbIds[i]);
            final MaterialCheckBox checkbox = cb;
            dayLayout.setOnClickListener(v1 -> checkbox.setChecked(!checkbox.isChecked()));
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> validateDaysAndHours(v));
        }
    }

    private void setupTimeSpinners(View v) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.hours_list, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spinnerFrom = v.findViewById(R.id.spinner_from_time);
        Spinner spinnerTo = v.findViewById(R.id.spinner_to_time);
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);
        spinnerFrom.setSelection(8);
        spinnerTo.setSelection(17);

        AdapterView.OnItemSelectedListener timeListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                validateDaysAndHours(v);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        spinnerFrom.setOnItemSelectedListener(timeListener);
        spinnerTo.setOnItemSelectedListener(timeListener);
    }

    private void setupFieldValidation(View v) {
        addTextWatcher(v, R.id.et_name, R.id.til_name);
        addTextWatcher(v, R.id.et_address, R.id.til_address);
        addTextWatcher(v, R.id.et_email, R.id.til_email);
        addTextWatcher(v, R.id.et_password, R.id.til_password);
        addTextWatcher(v, R.id.et_password_confirm, R.id.til_password_confirm);
        addTextWatcher(v, R.id.et_total_hours, R.id.til_total_hours);
        addTextWatcher(v, R.id.et_contact_phone, R.id.til_contact_phone);
        addTextWatcher(v, R.id.et_contact_details, R.id.til_contact_details);
    }

    private void addTextWatcher(View root, int etId, int tilId) {
        TextInputEditText et = root.findViewById(etId);
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                validateFieldById(root, etId, tilId);
                if (etId == R.id.et_password) {
                    validateFieldById(root, R.id.et_password_confirm, R.id.til_password_confirm);
                }
            }
        });
    }

    private void validateFieldById(View root, int etId, int tilId) {
        TextInputLayout til = root.findViewById(tilId);
        String text = "";
        TextInputEditText et = root.findViewById(etId);
        if (et != null && et.getText() != null) text = et.getText().toString().trim();
        String error = null;
        if (etId == R.id.et_name) {
            if (text.isEmpty()) error = "أدخل اسم المؤسسة";
        } else if (etId == R.id.et_email) {
            if (text.isEmpty()) error = "أدخل البريد الإلكتروني";
            else if (!Patterns.EMAIL_ADDRESS.matcher(text).matches()) error = "بريد إلكتروني غير صحيح";
        } else if (etId == R.id.et_password) {
            if (text.isEmpty()) error = "أدخل كلمة السر";
            else if (text.length() < 6) error = "كلمة السر 6 أحرف على الأقل";
        } else if (etId == R.id.et_password_confirm) {
            String pass = "";
            TextInputEditText etPass = root.findViewById(R.id.et_password);
            if (etPass != null && etPass.getText() != null) pass = etPass.getText().toString();
            if (text.isEmpty()) error = "أكد كلمة السر";
            else if (!text.equals(pass)) error = "كلمة السر غير متطابقة";
        } else if (etId == R.id.et_total_hours) {
            if (text.isEmpty()) error = "أدخل عدد الساعات";
            else try { if (Integer.parseInt(text) < 0) error = "عدد غير صحيح"; } catch (NumberFormatException e) { error = "عدد غير صحيح"; }
        }
        til.setError(error);
        til.setErrorEnabled(error != null);
    }

    private boolean validateAllFields(View v) {
        clearAllErrors(v);
        boolean valid = true;
        String name = ((TextInputEditText) v.findViewById(R.id.et_name)).getText() != null ? ((TextInputEditText) v.findViewById(R.id.et_name)).getText().toString().trim() : "";
        String email = ((TextInputEditText) v.findViewById(R.id.et_email)).getText() != null ? ((TextInputEditText) v.findViewById(R.id.et_email)).getText().toString().trim() : "";
        String password = ((TextInputEditText) v.findViewById(R.id.et_password)).getText() != null ? ((TextInputEditText) v.findViewById(R.id.et_password)).getText().toString() : "";
        String passConfirm = ((TextInputEditText) v.findViewById(R.id.et_password_confirm)).getText() != null ? ((TextInputEditText) v.findViewById(R.id.et_password_confirm)).getText().toString() : "";
        if (name.isEmpty()) { ((TextInputLayout) v.findViewById(R.id.til_name)).setError("أدخل اسم المؤسسة"); valid = false; }
        if (email.isEmpty()) { ((TextInputLayout) v.findViewById(R.id.til_email)).setError("أدخل البريد الإلكتروني"); valid = false; }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { ((TextInputLayout) v.findViewById(R.id.til_email)).setError("بريد إلكتروني غير صحيح"); valid = false; }
        if (password.isEmpty()) { ((TextInputLayout) v.findViewById(R.id.til_password)).setError("أدخل كلمة السر"); valid = false; }
        else if (password.length() < 6) { ((TextInputLayout) v.findViewById(R.id.til_password)).setError("كلمة السر 6 أحرف على الأقل"); valid = false; }
        if (passConfirm.isEmpty()) { ((TextInputLayout) v.findViewById(R.id.til_password_confirm)).setError("أكد كلمة السر"); valid = false; }
        else if (!passConfirm.equals(password)) { ((TextInputLayout) v.findViewById(R.id.til_password_confirm)).setError("كلمة السر غير متطابقة"); valid = false; }
        return valid;
    }

    private void clearAllErrors(View v) {
        ((TextInputLayout) v.findViewById(R.id.til_name)).setError(null);
        ((TextInputLayout) v.findViewById(R.id.til_email)).setError(null);
        ((TextInputLayout) v.findViewById(R.id.til_password)).setError(null);
        ((TextInputLayout) v.findViewById(R.id.til_password_confirm)).setError(null);
        ((TextInputLayout) v.findViewById(R.id.til_total_hours)).setError(null);
        ((TextInputLayout) v.findViewById(R.id.til_address)).setError(null);
        ((TextInputLayout) v.findViewById(R.id.til_contact_phone)).setError(null);
        ((TextInputLayout) v.findViewById(R.id.til_contact_details)).setError(null);
    }

    private void validateDaysAndHours(View v) {
        TextView tvError = v.findViewById(R.id.tv_validation_error);
        android.widget.Button btnRegister = v.findViewById(R.id.btn_register);

        boolean hasDay = ((MaterialCheckBox) v.findViewById(R.id.cb_sat)).isChecked()
                || ((MaterialCheckBox) v.findViewById(R.id.cb_sun)).isChecked()
                || ((MaterialCheckBox) v.findViewById(R.id.cb_mon)).isChecked()
                || ((MaterialCheckBox) v.findViewById(R.id.cb_tue)).isChecked()
                || ((MaterialCheckBox) v.findViewById(R.id.cb_wed)).isChecked()
                || ((MaterialCheckBox) v.findViewById(R.id.cb_thu)).isChecked()
                || ((MaterialCheckBox) v.findViewById(R.id.cb_fri)).isChecked();

        boolean is24Hours = ((MaterialCheckBox) v.findViewById(R.id.cb_24_hours)).isChecked();

        String error = null;
        if (!hasDay) {
            error = "اختر يوم واحد على الأقل";
        } else if (!is24Hours) {
            Spinner spinnerFrom = v.findViewById(R.id.spinner_from_time);
            Spinner spinnerTo = v.findViewById(R.id.spinner_to_time);
            int fromPos = spinnerFrom.getSelectedItemPosition();
            int toPos = spinnerTo.getSelectedItemPosition();
            if (fromPos >= toPos) {
                error = "ساعة البداية يجب أن تكون قبل ساعة النهاية";
            }
        }

        if (error != null) {
            tvError.setText(error);
            tvError.setVisibility(View.VISIBLE);
            btnRegister.setEnabled(false);
        } else {
            tvError.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
        }
    }
}
