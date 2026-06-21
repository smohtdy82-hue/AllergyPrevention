package com.example.volunteerapp.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.volunteerapp.Hellper.DALAppWriteConnection;
import com.example.volunteerapp.Hellper.VolunteerAppHelper;
import com.example.volunteerapp.R;
import com.example.volunteerapp.model.Student;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * واجهة تسجيل حساب طالب جديد.
 * <p>
 * يُدخل الطالب اسمه وبريده الإلكتروني وكلمة السر، وعند النجاح
 * يتم إبلاغ النشاط المضيف ببيانات الطالب المُسجَّل.
 */
public class StudentRegisterFragment extends Fragment {

    private static final String TAG = "StudentRegisterFragment";
    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ============================ واجهات الاتصال ============================

    /** يُستدعى عند نجاح التسجيل لإرسال بيانات الطالب الجديد. */
    public interface OnRegisterSuccess {
        void onStudentRegistered(Student student);
    }

    private OnRegisterSuccess onRegisterSuccess;

    public void setOnRegisterSuccess(OnRegisterSuccess listener) { this.onRegisterSuccess = listener; }

    // ============================ دورة حياة الواجهة ============================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        executor = Executors.newSingleThreadExecutor();
        View v = inflater.inflate(R.layout.fragment_student_register, container, false);

        TextInputEditText etName = v.findViewById(R.id.et_name);
        TextInputEditText etEmail = v.findViewById(R.id.et_email);
        TextInputEditText etPassword = v.findViewById(R.id.et_password);
        TextInputEditText etConfirm = v.findViewById(R.id.et_confirm_password);

        v.findViewById(R.id.btn_register).setOnClickListener(v1 -> {
            String name = text(etName);
            String email = text(etEmail);
            String password = text(etPassword);
            String confirm = text(etConfirm);

            if (name.isEmpty()) { etName.setError("مطلوب"); return; }
            if (email.isEmpty()) { etEmail.setError("مطلوب"); return; }
            if (password.isEmpty()) { etPassword.setError("مطلوب"); return; }
            if (password.length() < 6) {
                etPassword.setError(getString(R.string.password_min_length));
                return;
            }
            if (!password.equals(confirm)) {
                etConfirm.setError(getString(R.string.passwords_not_match));
                return;
            }

            v1.setEnabled(false);
            Student student = new Student(name, email, password);
            VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());

            executor.execute(() -> {
                try {
                    DALAppWriteConnection.OperationResult<Student> result = helper.registerStudent(student);
                    mainHandler.post(() -> {
                        v1.setEnabled(true);
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
                        v1.setEnabled(true);
                        Toast.makeText(requireContext(), "خطأ: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        return v;
    }

    // ============================ أدوات مساعدة ============================

    private static String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
