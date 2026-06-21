package com.example.volunteerapp.fragment;

import android.Manifest;
import android.app.AlertDialog;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.volunteerapp.Hellper.DALAppWriteConnection;
import com.example.volunteerapp.Hellper.OrgVolunteerScheduleHelper;
import com.example.volunteerapp.Hellper.VolunteerAppHelper;
import com.example.volunteerapp.R;
import com.example.volunteerapp.model.Organization;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * شاشة تسجيل مؤسسة جديدة.
 * <p>
 * تجمع بيانات المؤسسة (الاسم، العنوان، البريد، كلمة السر، أيام وساعات التطوع،
 * معلومات التواصل) بالإضافة إلى ثلاث صور اختيارية، ثم ترفعها إلى الخادم
 * عبر {@link VolunteerAppHelper}.
 * تدعم التقاط الصور من الكاميرا أو اختيارها من المعرض مع إدارة الأذونات.
 * </p>
 */
public class OrgRegisterFragment extends Fragment {

    private static final String TAG = "OrgRegisterFragment";
    private static final int PHOTO_SLOTS = 3;
    private static final int[] SLOT_FRAME_IDS = {
            R.id.slot_org_photo_0, R.id.slot_org_photo_1, R.id.slot_org_photo_2
    };
    private static final int[] SLOT_IV_IDS = {
            R.id.iv_org_photo_0, R.id.iv_org_photo_1, R.id.iv_org_photo_2
    };
    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** يُستدعى عند إتمام تسجيل المؤسسة بنجاح وإنشاء حسابها. */
    public interface OnRegisterSuccess {
        void onOrgRegistered(Organization org);
    }

    private OnRegisterSuccess onRegisterSuccess;
    private final byte[][] slotImageData = new byte[PHOTO_SLOTS][];
    private final String[] slotFileNames = new String[PHOTO_SLOTS];
    private int pendingPhotoSlot = -1;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                int slot = pendingPhotoSlot;
                try {
                    if (slot < 0 || slot >= PHOTO_SLOTS) return;
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null && extras.containsKey("data")) {
                            Bitmap bitmap = (Bitmap) extras.get("data");
                            if (bitmap != null) {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                                slotImageData[slot] = baos.toByteArray();
                                slotFileNames[slot] = "org_" + System.currentTimeMillis() + "_" + slot + ".jpg";
                                updateRegisterSlotPreview(slot, bitmap);
                            }
                        }
                    }
                } finally {
                    pendingPhotoSlot = -1;
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
                int slot = pendingPhotoSlot;
                try {
                    if (slot < 0 || slot >= PHOTO_SLOTS) return;
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                                slotImageData[slot] = baos.toByteArray();
                                slotFileNames[slot] = "org_" + System.currentTimeMillis() + "_" + slot + ".jpg";
                                updateRegisterSlotPreview(slot, bitmap);
                            } catch (IOException e) {
                                Toast.makeText(requireContext(), "فشل تحميل الصورة", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                } finally {
                    pendingPhotoSlot = -1;
                }
            });

    public void setOnRegisterSuccess(OnRegisterSuccess listener) { this.onRegisterSuccess = listener; }

    /**
     * يبني واجهة التسجيل، ويُعدّ خانات الصور وجدول التطوع والتحقق الفوري من الحقول.
     * عند الضغط على زر التسجيل يتم رفع الصور ثم إنشاء حساب المؤسسة في خيط خلفي.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        executor = Executors.newSingleThreadExecutor();
        View v = inflater.inflate(R.layout.fragment_org_register, container, false);

        for (int i = 0; i < PHOTO_SLOTS; i++) {
            final int slot = i;
            v.findViewById(SLOT_FRAME_IDS[i]).setOnClickListener(v1 -> showRegisterPhotoMenu(v, slot));
        }

        OrgVolunteerScheduleHelper.setupScheduleUi(requireContext(), v, () -> validateDaysAndHours(v));
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

            int totalHours = 0;
            try {
                totalHours = Integer.parseInt(etTotalHours.getText() != null ? etTotalHours.getText().toString() : "0");
            } catch (NumberFormatException ignored) {}
            String contactPhone = etContactPhone.getText() != null ? etContactPhone.getText().toString().trim() : "";
            String contactDetails = etContactDetails.getText() != null ? etContactDetails.getText().toString().trim() : "";

            if (!validateAllFields(v)) return;
            String schedErr = OrgVolunteerScheduleHelper.validateSchedule(v);
            if (schedErr != null) {
                Toast.makeText(requireContext(), schedErr, Toast.LENGTH_SHORT).show();
                return;
            }
            String volunteerDays = OrgVolunteerScheduleHelper.collectVolunteerDays(v);
            String volunteerHours = OrgVolunteerScheduleHelper.collectVolunteerHours(v);

            Organization org = new Organization(name, "", address, email, password, volunteerDays, volunteerHours, totalHours, contactPhone, contactDetails);
            VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());

            v.findViewById(R.id.btn_register).setEnabled(false);
            executor.execute(() -> {
                try {
                    for (int i = 0; i < PHOTO_SLOTS; i++) {
                        if (slotImageData[i] != null && slotImageData[i].length > 0
                                && slotFileNames[i] != null && !slotFileNames[i].isEmpty()) {
                            DALAppWriteConnection.OperationResult<DALAppWriteConnection.FileInfo> uploadResult =
                                    helper.uploadImage(slotImageData[i], slotFileNames[i]);
                            if (uploadResult.success && uploadResult.data != null
                                    && uploadResult.data.fileId != null && !uploadResult.data.fileId.isEmpty()) {
                                if (i == 0) org.setImageUrl(uploadResult.data.fileId);
                                else if (i == 1) org.setImageUrl2(uploadResult.data.fileId);
                                else org.setImageUrl3(uploadResult.data.fileId);
                            }
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

    /** يعرض قائمة خيارات الصورة (كاميرا / معرض / حذف) لخانة التسجيل المحددة. */
    private void showRegisterPhotoMenu(View root, int slotIndex) {
        boolean has = slotImageData[slotIndex] != null && slotImageData[slotIndex].length > 0;
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        labels.add(getString(R.string.take_photo));
        actions.add(() -> {
            pendingPhotoSlot = slotIndex;
            requestCameraAndLaunch();
        });
        labels.add(getString(R.string.choose_from_gallery));
        actions.add(() -> {
            pendingPhotoSlot = slotIndex;
            requestGalleryAndLaunch();
        });
        if (has) {
            labels.add(getString(R.string.delete_photo));
            actions.add(() -> clearRegisterSlot(root, slotIndex));
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.photo_actions_title)
                .setItems(labels.toArray(new String[0]), (d, which) -> actions.get(which).run())
                .show();
    }

    private void clearRegisterSlot(View root, int slot) {
        slotImageData[slot] = null;
        slotFileNames[slot] = null;
        ImageView iv = root.findViewById(SLOT_IV_IDS[slot]);
        if (iv != null) {
            iv.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private void updateRegisterSlotPreview(int slot, Bitmap bitmap) {
        View root = getView();
        if (root == null) return;
        ImageView iv = root.findViewById(SLOT_IV_IDS[slot]);
        if (iv != null) iv.setImageBitmap(bitmap);
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

    /** يُعدّ مراقبات النصوص للتحقق الفوري من صحة جميع حقول الإدخال. */
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

    /**
     * يتحقق من صحة حقل واحد ويعرض رسالة الخطأ المناسبة.
     * يتعامل مع حالات خاصة كتأكيد كلمة السر وصيغة البريد الإلكتروني.
     */
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

    /** يتحقق من جميع الحقول الإلزامية دفعةً واحدة قبل إرسال نموذج التسجيل. */
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

    /** يتحقق من اختيار أيام وساعات التطوع ويُفعّل/يُعطّل زر التسجيل وفقاً لذلك. */
    private void validateDaysAndHours(View v) {
        TextView tvError = v.findViewById(R.id.tv_validation_error);
        android.widget.Button btnRegister = v.findViewById(R.id.btn_register);
        String error = OrgVolunteerScheduleHelper.validateSchedule(v);
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
