package com.example.volunteerapp.fragment;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.bumptech.glide.Glide;
import com.example.volunteerapp.Hellper.DALAppWriteConnection;
import com.example.volunteerapp.Hellper.VolunteerAppHelper;
import com.example.volunteerapp.R;
import com.example.volunteerapp.model.Student;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * واجهة الملف الشخصي للطالب.
 * <p>
 * تعرض بيانات الطالب وتسمح بتعديل الاسم ورقم الهوية والمدرسة
 * وتاريخ الميلاد والصورة الشخصية وعدد الساعات المطلوبة، ثم حفظ التعديلات.
 * كما تتيح تسجيل الخروج عبر أيقونة الشريط العلوي.
 */
public class StudentProfileFragment extends Fragment {

    /** واجهة لإبلاغ النشاط المضيف بطلب تسجيل الخروج. */
    public interface OnLogout {
        void onLogout();
    }

    private static final String ARG_STUDENT_ID = "studentId";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private OnLogout onLogout;
    private String studentId;
    private byte[] newPhotoData;
    private String newPhotoName = "";
    private ImageView ivProfile;
    private Student currentStudent;
    private View rootView;
    private String originalToolbarTitle;

    // ============================ مشغّلات الكاميرا والمعرض ============================

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null && extras.containsKey("data")) {
                        Bitmap bmp = (Bitmap) extras.get("data");
                        if (bmp != null) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                            newPhotoData = baos.toByteArray();
                            newPhotoName = "student_" + System.currentTimeMillis() + ".jpg";
                            if (ivProfile != null) ivProfile.setImageBitmap(bmp);
                        }
                    }
                }
            });

    private final ActivityResultLauncher<String> cameraPermLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), ok -> {
                if (ok) launchCamera();
            });

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            Bitmap bmp = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                            newPhotoData = baos.toByteArray();
                            newPhotoName = "student_" + System.currentTimeMillis() + ".jpg";
                            if (ivProfile != null) ivProfile.setImageBitmap(bmp);
                        } catch (IOException ignored) {}
                    }
                }
            });

    private final ActivityResultLauncher<String> galleryPermLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), ok -> {
                if (ok) launchGallery();
            });

    // ============================ إنشاء النسخة ============================

    public static StudentProfileFragment newInstance(String studentId) {
        StudentProfileFragment f = new StudentProfileFragment();
        Bundle b = new Bundle();
        b.putString(ARG_STUDENT_ID, studentId);
        f.setArguments(b);
        return f;
    }

    @Deprecated
    public static StudentProfileFragment newInstance(Student student) {
        return newInstance(student.getId());
    }

    public void setOnLogout(OnLogout listener) { this.onLogout = listener; }

    // ============================ دورة حياة الواجهة ============================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_student_profile, container, false);
        studentId = getArguments() != null ? getArguments().getString(ARG_STUDENT_ID, "") : "";
        ivProfile = rootView.findViewById(R.id.iv_profile);

        setupToolbar();

        rootView.findViewById(R.id.card_photo).setOnClickListener(x -> showPhotoBottomSheet());

        setupHoursSpinner(rootView);
        setupBirthDatePicker(rootView);
        loadProfile(rootView);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(originalToolbarTitle != null ? originalToolbarTitle : getString(R.string.app_name));
            toolbar.setNavigationIcon(null);
            toolbar.setNavigationOnClickListener(null);
        }
        ivProfile = null;
        rootView = null;
    }

    // ============================ إعداد الشريط العلوي ============================

    /** يُعدّ شريط الأدوات بعنوان «الملف الشخصي» وأيقونة تسجيل الخروج وزر الحفظ. */
    private void setupToolbar() {
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            originalToolbarTitle = toolbar.getTitle() != null ? toolbar.getTitle().toString() : "";
            toolbar.setTitle(R.string.profile);
            toolbar.setNavigationIcon(R.drawable.ic_logout);
            toolbar.setNavigationOnClickListener(x -> {
                if (onLogout != null) onLogout.onLogout();
            });
        }

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_profile, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_save) {
                    performSave();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    // ============================ حفظ البيانات ============================

    /**
     * يتحقق من صحة الحقول ثم يرفع الصورة الجديدة (إن وُجدت) ويحفظ
     * بيانات الطالب المحدّثة في قاعدة البيانات.
     */
    private void performSave() {
        if (currentStudent == null || rootView == null) return;

        TextInputEditText etName = rootView.findViewById(R.id.et_name);
        TextInputEditText etIdNumber = rootView.findViewById(R.id.et_id_number);
        TextInputEditText etSchool = rootView.findViewById(R.id.et_school);
        TextInputEditText etBirthDate = rootView.findViewById(R.id.et_birth_date);
        AutoCompleteTextView spinnerHours = rootView.findViewById(R.id.spinner_required_hours);

        String name = text(etName);
        String idNum = text(etIdNumber);
        String school = text(etSchool);
        String birthDate = text(etBirthDate);
        String hoursText = spinnerHours.getText().toString().trim();

        if (name.isEmpty()) { etName.setError("مطلوب"); return; }
        if (idNum.isEmpty() || idNum.length() != 9) {
            etIdNumber.setError(getString(R.string.id_number_error));
            return;
        }
        if (school.isEmpty()) { etSchool.setError("مطلوب"); return; }
        if (birthDate.isEmpty()) { etBirthDate.setError("مطلوب"); return; }
        if (hoursText.isEmpty()) {
            spinnerHours.setError("مطلوب");
            return;
        }
        if (!currentStudent.hasImage()) {
            if (newPhotoData == null || newPhotoData.length == 0) {
                Toast.makeText(requireContext(), "يجب اختيار صورة", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Student s = currentStudent;
        s.setName(name);
        s.setIdNumber(idNum);
        s.setSchoolAddress(school);
        s.setBirthDate(birthDate);
        try { s.setRequiredHours(Integer.parseInt(hoursText)); } catch (NumberFormatException ignored) {}

        ProgressBar pb = rootView.findViewById(R.id.progress_bar);
        pb.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());

            if (newPhotoData != null && newPhotoData.length > 0 && !newPhotoName.isEmpty()) {
                DALAppWriteConnection.OperationResult<DALAppWriteConnection.FileInfo> uploadResult =
                        helper.uploadImage(newPhotoData, newPhotoName);
                if (uploadResult.success && uploadResult.data != null && uploadResult.data.fileId != null) {
                    s.setImageUrl(uploadResult.data.fileId);
                }
                newPhotoData = null;
                newPhotoName = "";
            }

            DALAppWriteConnection.OperationResult<Student> updateResult = helper.updateStudent(s);
            mainHandler.post(() -> {
                if (!isAdded() || rootView == null) return;
                pb.setVisibility(View.GONE);
                if (updateResult.success) {
                    Toast.makeText(requireContext(), R.string.saved_successfully, Toast.LENGTH_SHORT).show();
                    ((TextView) rootView.findViewById(R.id.tv_hours_summary))
                            .setText("الساعات: " + s.getCompletedHours() + " / " + s.getRequiredHours());
                } else {
                    Toast.makeText(requireContext(),
                            updateResult.message != null ? updateResult.message : "فشل الحفظ",
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // ============================ إعداد عناصر الإدخال ============================

    private void setupHoursSpinner(View v) {
        AutoCompleteTextView spinner = v.findViewById(R.id.spinner_required_hours);
        String[] items = new String[100];
        for (int i = 0; i < 100; i++) items[i] = String.valueOf(i + 1);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, items);
        spinner.setAdapter(adapter);
    }

    /** يفتح منتقي التاريخ مع تقييد الحد الأقصى للعمر (8 سنوات على الأقل). */
    private void setupBirthDatePicker(View v) {
        TextInputEditText etBirth = v.findViewById(R.id.et_birth_date);
        etBirth.setOnClickListener(x -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dpd = new DatePickerDialog(requireContext(), (view, year, month, day) -> {
                String date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                etBirth.setText(date);
            }, now.get(Calendar.YEAR) - 12, now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
            Calendar maxDate = Calendar.getInstance();
            maxDate.add(Calendar.YEAR, -8);
            dpd.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
            dpd.show();
        });
    }

    // ============================ اختيار الصورة ============================

    /** يعرض قائمة سفلية لاختيار مصدر الصورة (كاميرا أو معرض). */
    private void showPhotoBottomSheet() {
        BottomSheetDialog bs = new BottomSheetDialog(requireContext());
        android.widget.LinearLayout ll = new android.widget.LinearLayout(requireContext());
        ll.setOrientation(android.widget.LinearLayout.VERTICAL);
        ll.setPadding(32, 32, 32, 32);

        MaterialButton btnCam = new MaterialButton(requireContext());
        btnCam.setText(R.string.take_photo);
        btnCam.setOnClickListener(x -> { bs.dismiss(); requestCameraAndLaunch(); });
        ll.addView(btnCam);

        MaterialButton btnGal = new MaterialButton(requireContext());
        btnGal.setText(R.string.choose_from_gallery);
        btnGal.setOnClickListener(x -> { bs.dismiss(); requestGalleryAndLaunch(); });
        ll.addView(btnGal);

        bs.setContentView(ll);
        bs.show();
    }

    private void requestCameraAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
    }

    private void requestGalleryAndLaunch() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), perm) == PackageManager.PERMISSION_GRANTED) {
            launchGallery();
        } else {
            galleryPermLauncher.launch(perm);
        }
    }

    private void launchGallery() {
        galleryLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    // ============================ تحميل وربط البيانات ============================

    /** يجلب بيانات الطالب من الخادم ويملأ حقول الواجهة. */
    private void loadProfile(View v) {
        ProgressBar pb = v.findViewById(R.id.progress_bar);
        pb.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());
            DALAppWriteConnection.OperationResult<Student> result = helper.getStudentById(studentId);
            mainHandler.post(() -> {
                if (!isAdded()) return;
                pb.setVisibility(View.GONE);
                if (result.success && result.data != null) {
                    currentStudent = result.data;
                    bindStudent(v, currentStudent);
                }
            });
        });
    }

    private void bindStudent(View v, Student s) {
        TextInputEditText etEmail = v.findViewById(R.id.et_email);
        TextInputEditText etName = v.findViewById(R.id.et_name);
        TextInputEditText etIdNumber = v.findViewById(R.id.et_id_number);
        TextInputEditText etSchool = v.findViewById(R.id.et_school);
        TextInputEditText etBirthDate = v.findViewById(R.id.et_birth_date);
        AutoCompleteTextView spinnerHours = v.findViewById(R.id.spinner_required_hours);

        etEmail.setText(s.getEmail() != null ? s.getEmail() : "");
        etName.setText(s.getName());
        etIdNumber.setText(s.getIdNumber());
        etSchool.setText(s.getSchoolAddress());
        etBirthDate.setText(s.getBirthDate());
        if (s.getRequiredHours() > 0) {
            spinnerHours.setText(String.valueOf(s.getRequiredHours()), false);
        }

        ((TextView) v.findViewById(R.id.tv_hours_summary))
                .setText("الساعات: " + s.getCompletedHours() + " / " + s.getRequiredHours());

        String img = s.getImageUrl();
        if (img != null && !img.isEmpty()) {
            Glide.with(requireContext()).load(img).circleCrop().into(ivProfile);
        }
    }

    // ============================ أدوات مساعدة ============================

    private static String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
