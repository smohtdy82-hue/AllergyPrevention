package com.example.volunteerapp.fragment;

import android.Manifest;
import android.app.AlertDialog;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.volunteerapp.Hellper.DALAppWriteConnection;
import com.example.volunteerapp.Hellper.OrgVolunteerScheduleHelper;
import com.example.volunteerapp.Hellper.VolunteerAppHelper;
import com.example.volunteerapp.R;
import com.example.volunteerapp.model.Organization;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * شاشة إدارة ملف المؤسسة (تعديل البيانات).
 * <p>
 * تُحمّل بيانات المؤسسة الحالية من الخادم وتعرضها في نموذج قابل للتعديل
 * يشمل: الاسم، العنوان، جدول التطوع، ساعات التطوع المطلوبة، معلومات التواصل،
 * وثلاث صور. يدعم تغيير كلمة السر وحذف/استبدال الصور مع تنظيف الملفات القديمة
 * من التخزين بعد نجاح التحديث.
 * </p>
 */
public class OrgManageFragment extends Fragment {

    private static final String TAG = "OrgManageFragment";
    private static final String ARG_ORG_ID = "orgId";
    private static final int PHOTO_SLOTS = 3;
    private static final int[] SLOT_FRAME_IDS = {
            R.id.slot_org_photo_0, R.id.slot_org_photo_1, R.id.slot_org_photo_2
    };
    private static final int[] SLOT_IV_IDS = {
            R.id.iv_org_photo_0, R.id.iv_org_photo_1, R.id.iv_org_photo_2
    };

    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private StudentProfileFragment.OnLogout onLogout;
    private String orgId;
    private String storedPassword = "";
    private String loadedEmail = "";

    private final String[] baselinePhotoUrls = new String[PHOTO_SLOTS];
    private final byte[][] slotNewImageData = new byte[PHOTO_SLOTS][];
    private final String[] slotNewFileNames = new String[PHOTO_SLOTS];
    private final boolean[] slotMarkedEmpty = new boolean[PHOTO_SLOTS];
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
                                slotNewImageData[slot] = baos.toByteArray();
                                slotNewFileNames[slot] = "org_" + System.currentTimeMillis() + "_" + slot + ".jpg";
                                slotMarkedEmpty[slot] = false;
                                updateManageSlotPreview(slot, bitmap);
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
                                slotNewImageData[slot] = baos.toByteArray();
                                slotNewFileNames[slot] = "org_" + System.currentTimeMillis() + "_" + slot + ".jpg";
                                slotMarkedEmpty[slot] = false;
                                updateManageSlotPreview(slot, bitmap);
                            } catch (IOException e) {
                                Toast.makeText(requireContext(), "فشل تحميل الصورة", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                } finally {
                    pendingPhotoSlot = -1;
                }
            });

    /** يُنشئ نسخة جديدة من الجزء مع تمرير معرّف المؤسسة كمعامل. */
    public static OrgManageFragment newInstance(String organizationId) {
        OrgManageFragment f = new OrgManageFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ORG_ID, organizationId);
        f.setArguments(b);
        return f;
    }

    public void setOnLogout(StudentProfileFragment.OnLogout listener) {
        this.onLogout = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        executor = Executors.newSingleThreadExecutor();
        return inflater.inflate(R.layout.fragment_org_manage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        orgId = getArguments() != null ? getArguments().getString(ARG_ORG_ID, "") : "";

        for (int i = 0; i < PHOTO_SLOTS; i++) {
            final int slot = i;
            v.findViewById(SLOT_FRAME_IDS[i]).setOnClickListener(x -> showManagePhotoMenu(slot));
        }

        MaterialButton btnSave = v.findViewById(R.id.btn_save);
        btnSave.setOnClickListener(x -> attemptSave());

        v.findViewById(R.id.btn_logout).setOnClickListener(x -> {
            if (onLogout != null) onLogout.onLogout();
        });

        OrgVolunteerScheduleHelper.setupScheduleUi(requireContext(), v, null);
        loadOrganization();
    }

    /**
     * يُحمّل بيانات المؤسسة من الخادم ويعرض مؤشر التحميل أثناء الانتظار.
     * عند النجاح يُملأ النموذج بالبيانات المسترجعة.
     */
    private void loadOrganization() {
        View v = requireView();
        ProgressBar progress = v.findViewById(R.id.progress_load);
        ScrollView scroll = v.findViewById(R.id.scroll_content);
        progress.setVisibility(View.VISIBLE);
        scroll.setVisibility(View.GONE);

        executor.execute(() -> {
            VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());
            DALAppWriteConnection.OperationResult<Organization> result = helper.getOrganizationById(orgId);
            mainHandler.post(() -> {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                if (result.success && result.data != null) {
                    bindOrganization(result.data);
                    scroll.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(requireContext(),
                            result.message != null ? result.message : getString(R.string.loading_data),
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /** يربط بيانات المؤسسة المحمّلة بحقول النموذج وخانات الصور. */
    private void bindOrganization(Organization o) {
        View v = requireView();
        loadedEmail = o.getEmail() != null ? o.getEmail() : "";
        storedPassword = o.getPassword() != null ? o.getPassword() : "";

        baselinePhotoUrls[0] = o.getImageUrl() != null ? o.getImageUrl() : "";
        baselinePhotoUrls[1] = o.getImageUrl2() != null ? o.getImageUrl2() : "";
        baselinePhotoUrls[2] = o.getImageUrl3() != null ? o.getImageUrl3() : "";
        Arrays.fill(slotMarkedEmpty, false);
        Arrays.fill(slotNewImageData, null);
        Arrays.fill(slotNewFileNames, null);

        setText(v, R.id.et_name, o.getName());
        setText(v, R.id.et_address, o.getAddress());
        setText(v, R.id.et_email, loadedEmail);
        OrgVolunteerScheduleHelper.bindStoredSchedule(v, o.getVolunteerDays(), o.getVolunteerHours());
        setText(v, R.id.et_total_hours, String.valueOf(o.getTotalHours()));
        setText(v, R.id.et_contact_phone, o.getContactPhone());
        setText(v, R.id.et_contact_details, o.getContactDetails());

        for (int i = 0; i < PHOTO_SLOTS; i++) {
            ImageView iv = v.findViewById(SLOT_IV_IDS[i]);
            String url = baselinePhotoUrls[i];
            if (url != null && !url.isEmpty()) {
                Glide.with(requireContext()).load(url).into(iv);
            } else {
                iv.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }
    }

    /** يعرض قائمة خيارات الصورة (كاميرا / معرض / حذف) لخانة الإدارة المحددة. */
    private void showManagePhotoMenu(int slotIndex) {
        if (!isAdded()) return;
        View root = requireView();
        boolean has = !slotMarkedEmpty[slotIndex]
                && ((slotNewImageData[slotIndex] != null && slotNewImageData[slotIndex].length > 0)
                || (baselinePhotoUrls[slotIndex] != null && !baselinePhotoUrls[slotIndex].isEmpty()));
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
            actions.add(() -> {
                slotMarkedEmpty[slotIndex] = true;
                slotNewImageData[slotIndex] = null;
                slotNewFileNames[slotIndex] = null;
                ImageView iv = root.findViewById(SLOT_IV_IDS[slotIndex]);
                if (iv != null) iv.setImageResource(android.R.drawable.ic_menu_gallery);
            });
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.photo_actions_title)
                .setItems(labels.toArray(new String[0]), (d, which) -> actions.get(which).run())
                .show();
    }

    private void updateManageSlotPreview(int slot, Bitmap bitmap) {
        View root = getView();
        if (root == null) return;
        ImageView iv = root.findViewById(SLOT_IV_IDS[slot]);
        if (iv != null) iv.setImageBitmap(bitmap);
    }

    private static void setOrgImageAt(Organization org, int index, String url) {
        String v = url != null ? url : "";
        switch (index) {
            case 0:
                org.setImageUrl(v);
                break;
            case 1:
                org.setImageUrl2(v);
                break;
            case 2:
                org.setImageUrl3(v);
                break;
            default:
                break;
        }
    }

    private static void setText(View root, int id, String value) {
        TextInputEditText et = root.findViewById(id);
        if (et != null) {
            et.setText(value != null ? value : "");
        }
    }

    /**
     * يجمع البيانات المُعدّلة، يتحقق من صحتها، يرفع الصور الجديدة،
     * يُحدّث مستند المؤسسة في قاعدة البيانات، ثم يحذف الصور القديمة المُستبدلة.
     */
    private void attemptSave() {
        View v = requireView();
        String name = textOf(v, R.id.et_name);
        String address = textOf(v, R.id.et_address);
        String contactPhone = textOf(v, R.id.et_contact_phone);
        String contactDetails = textOf(v, R.id.et_contact_details);
        String newPass = textOf(v, R.id.et_new_password);
        String confirmPass = textOf(v, R.id.et_confirm_password);

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "أدخل اسم المؤسسة", Toast.LENGTH_SHORT).show();
            return;
        }
        if (address.isEmpty()) {
            Toast.makeText(requireContext(), "أدخل العنوان", Toast.LENGTH_SHORT).show();
            return;
        }
        String schedErr = OrgVolunteerScheduleHelper.validateSchedule(v);
        if (schedErr != null) {
            Toast.makeText(requireContext(), schedErr, Toast.LENGTH_SHORT).show();
            return;
        }
        String volunteerDays = OrgVolunteerScheduleHelper.collectVolunteerDays(v);
        String volunteerHours = OrgVolunteerScheduleHelper.collectVolunteerHours(v);

        int totalHours = 0;
        try {
            totalHours = Integer.parseInt(textOf(v, R.id.et_total_hours));
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "أدخل عدد الساعات بشكل صحيح", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.isEmpty()) {
            if (newPass.length() < 6) {
                Toast.makeText(requireContext(), R.string.password_min_length, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirmPass)) {
                Toast.makeText(requireContext(), R.string.passwords_not_match, Toast.LENGTH_SHORT).show();
                return;
            }
        } else if (!confirmPass.isEmpty()) {
            Toast.makeText(requireContext(), R.string.passwords_not_match, Toast.LENGTH_SHORT).show();
            return;
        }

        Organization org = new Organization();
        org.setId(orgId);
        org.setName(name);
        org.setAddress(address);
        org.setEmail(loadedEmail);
        org.setVolunteerDays(volunteerDays);
        org.setVolunteerHours(volunteerHours);
        org.setTotalHours(totalHours);
        org.setContactPhone(contactPhone);
        org.setContactDetails(contactDetails);

        if (newPass.isEmpty()) {
            org.setPassword(storedPassword);
        } else {
            org.setPassword(newPass);
        }

        final boolean[] markedEmptyCopy = Arrays.copyOf(slotMarkedEmpty, PHOTO_SLOTS);
        final byte[][] newDataCopy = new byte[PHOTO_SLOTS][];
        final String[] newNamesCopy = new String[PHOTO_SLOTS];
        final String[] baselineCopy = Arrays.copyOf(baselinePhotoUrls, PHOTO_SLOTS);
        for (int i = 0; i < PHOTO_SLOTS; i++) {
            if (slotNewImageData[i] != null) {
                newDataCopy[i] = Arrays.copyOf(slotNewImageData[i], slotNewImageData[i].length);
            }
            newNamesCopy[i] = slotNewFileNames[i];
        }

        MaterialButton btnSave = v.findViewById(R.id.btn_save);
        btnSave.setEnabled(false);

        executor.execute(() -> {
            try {
                VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());
                Set<String> storageFilesToDeleteAfterDocUpdate = new LinkedHashSet<>();
                for (int i = 0; i < PHOTO_SLOTS; i++) {
                    if (markedEmptyCopy[i]) {
                        String oldId = Organization.extractStorageFileId(baselineCopy[i]);
                        if (!oldId.isEmpty()) {
                            storageFilesToDeleteAfterDocUpdate.add(oldId);
                        }
                        setOrgImageAt(org, i, "");
                        continue;
                    }
                    if (newDataCopy[i] != null && newDataCopy[i].length > 0
                            && newNamesCopy[i] != null && !newNamesCopy[i].isEmpty()) {
                        DALAppWriteConnection.OperationResult<DALAppWriteConnection.FileInfo> uploadResult =
                                helper.uploadImage(newDataCopy[i], newNamesCopy[i]);
                        if (uploadResult.success && uploadResult.data != null
                                && uploadResult.data.fileId != null && !uploadResult.data.fileId.isEmpty()) {
                            String newId = uploadResult.data.fileId;
                            String oldId = Organization.extractStorageFileId(baselineCopy[i]);
                            if (!oldId.isEmpty() && !oldId.equals(newId)) {
                                storageFilesToDeleteAfterDocUpdate.add(oldId);
                            }
                            setOrgImageAt(org, i, newId);
                        } else {
                            setOrgImageAt(org, i, baselineCopy[i]);
                        }
                    } else {
                        setOrgImageAt(org, i, baselineCopy[i]);
                    }
                }

                DALAppWriteConnection.OperationResult<Organization> result = helper.updateOrganization(org);
                if (result.success) {
                    for (String fileId : storageFilesToDeleteAfterDocUpdate) {
                        helper.deleteStorageFile(fileId);
                    }
                }
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    btnSave.setEnabled(true);
                    if (result.success) {
                        if (!newPass.isEmpty()) {
                            storedPassword = newPass;
                            clearPasswordFields();
                        }
                        baselinePhotoUrls[0] = org.getImageUrl() != null ? org.getImageUrl() : "";
                        baselinePhotoUrls[1] = org.getImageUrl2() != null ? org.getImageUrl2() : "";
                        baselinePhotoUrls[2] = org.getImageUrl3() != null ? org.getImageUrl3() : "";
                        Arrays.fill(slotMarkedEmpty, false);
                        Arrays.fill(slotNewImageData, null);
                        Arrays.fill(slotNewFileNames, null);
                        for (int i = 0; i < PHOTO_SLOTS; i++) {
                            ImageView iv = requireView().findViewById(SLOT_IV_IDS[i]);
                            String url = baselinePhotoUrls[i];
                            if (url != null && !url.isEmpty()) {
                                Glide.with(requireContext()).load(url).into(iv);
                            } else {
                                iv.setImageResource(android.R.drawable.ic_menu_gallery);
                            }
                        }
                        Toast.makeText(requireContext(), R.string.saved_successfully, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),
                                result.message != null ? result.message : "فشل الحفظ",
                                Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "save", e);
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    btnSave.setEnabled(true);
                    Toast.makeText(requireContext(), e.getMessage() != null ? e.getMessage() : "خطأ", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void clearPasswordFields() {
        View v = getView();
        if (v == null) return;
        TextInputEditText et1 = v.findViewById(R.id.et_new_password);
        TextInputEditText et2 = v.findViewById(R.id.et_confirm_password);
        if (et1 != null) et1.setText("");
        if (et2 != null) et2.setText("");
    }

    private static String textOf(View root, int id) {
        TextInputEditText et = root.findViewById(id);
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
