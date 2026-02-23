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
import com.example.mohtadyapp.model.Organization;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrgLoginFragment extends Fragment {

    private static final String TAG = "OrgLoginFragment";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnLoginSuccess {
        void onOrgLoggedIn(Organization org);
    }

    public interface OnGoRegister {
        void onGoOrgRegister();
    }

    private OnLoginSuccess onLoginSuccess;
    private OnGoRegister onGoRegister;

    public void setOnLoginSuccess(OnLoginSuccess listener) { this.onLoginSuccess = listener; }
    public void setOnGoRegister(OnGoRegister listener) { this.onGoRegister = listener; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_org_login, container, false);
        TextInputEditText etEmail = v.findViewById(R.id.et_email);
        TextInputEditText etPassword = v.findViewById(R.id.et_password);

        v.findViewById(R.id.btn_login).setOnClickListener(v1 -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "أدخل البريد الإلكتروني وكلمة السر", Toast.LENGTH_SHORT).show();
                return;
            }
            v1.setEnabled(false);
            VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());
            executor.execute(() -> {
                try {
                    DALAppWriteConnection.OperationResult<Organization> loginResult = helper.loginOrganization(email, password);
                    mainHandler.post(() -> {
                        v1.setEnabled(true);
                        if (loginResult.success && loginResult.data != null) {
                            if (onLoginSuccess != null) onLoginSuccess.onOrgLoggedIn(loginResult.data);
                        } else {
                            String msg = loginResult.message != null ? loginResult.message : "فشل تسجيل الدخول";
                            Log.e(TAG, "loginOrganization failed: " + msg);
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "loginOrganization exception", e);
                    mainHandler.post(() -> {
                        v1.setEnabled(true);
                        Toast.makeText(requireContext(), "خطأ: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        v.findViewById(R.id.btn_go_register).setOnClickListener(v1 -> {
            if (onGoRegister != null) onGoRegister.onGoOrgRegister();
        });

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
