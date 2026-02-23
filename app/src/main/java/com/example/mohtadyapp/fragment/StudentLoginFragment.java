package com.example.mohtadyapp.fragment;

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

import com.example.mohtadyapp.Hellper.DALAppWriteConnection;
import com.example.mohtadyapp.Hellper.VolunteerAppHelper;
import com.example.mohtadyapp.R;
import com.example.mohtadyapp.model.Student;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudentLoginFragment extends Fragment {

    private static final String TAG = "StudentLoginFragment";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnLoginSuccess {
        void onStudentLoggedIn(Student student);
    }

    public interface OnGoRegister {
        void onGoStudentRegister();
    }

    private OnLoginSuccess onLoginSuccess;
    private OnGoRegister onGoRegister;

    public void setOnLoginSuccess(OnLoginSuccess listener) { this.onLoginSuccess = listener; }
    public void setOnGoRegister(OnGoRegister listener) { this.onGoRegister = listener; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_student_login, container, false);
        TextInputEditText etPhone = v.findViewById(R.id.et_phone);
        TextInputEditText etPassword = v.findViewById(R.id.et_password);

        v.findViewById(R.id.btn_login).setOnClickListener(v1 -> {
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "أدخل رقم الهاتف وكلمة السر", Toast.LENGTH_SHORT).show();
                return;
            }
            v1.setEnabled(false);
            VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());
            executor.execute(() -> {
                try {
                    DALAppWriteConnection.OperationResult<Student> result = helper.loginStudent(phone, password);
                    mainHandler.post(() -> {
                        v1.setEnabled(true);
                        if (result.success && result.data != null) {
                            if (onLoginSuccess != null) onLoginSuccess.onStudentLoggedIn(result.data);
                        } else {
                            String msg = result.message != null ? result.message : "فشل تسجيل الدخول";
                            Log.e(TAG, "loginStudent failed: " + msg);
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "loginStudent exception", e);
                    mainHandler.post(() -> {
                        v1.setEnabled(true);
                        Toast.makeText(requireContext(), "خطأ: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        v.findViewById(R.id.btn_go_register).setOnClickListener(v1 -> {
            if (onGoRegister != null) onGoRegister.onGoStudentRegister();
        });

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
