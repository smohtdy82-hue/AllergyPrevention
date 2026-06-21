package com.example.volunteerapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.volunteerapp.R;

/**
 * شاشة اختيار نوع المستخدم (طالب أو مؤسسة) مع خيارات تسجيل الدخول والتسجيل الجديد.
 * تُعرض كأول واجهة عند فتح التطبيق بدون جلسة محفوظة.
 */
public class ChooseUserFragment extends Fragment {

    /**
     * واجهة رد الفعل عند اختيار نوع المستخدم وطريقة الدخول.
     */
    public interface OnUserTypeSelected {
        void onStudentLogin();
        void onStudentRegister();
        void onOrgLogin();
        void onOrgRegister();
    }

    private OnUserTypeSelected listener;

    /** تعيين المستمع لأحداث اختيار نوع المستخدم. */
    public void setOnUserTypeSelected(OnUserTypeSelected listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_choose_user, container, false);
        v.findViewById(R.id.btn_student_login).setOnClickListener(v1 -> {
            if (listener != null) listener.onStudentLogin();
        });
        v.findViewById(R.id.btn_student_register).setOnClickListener(v1 -> {
            if (listener != null) listener.onStudentRegister();
        });
        v.findViewById(R.id.btn_org_login).setOnClickListener(v1 -> {
            if (listener != null) listener.onOrgLogin();
        });
        v.findViewById(R.id.btn_org_register).setOnClickListener(v1 -> {
            if (listener != null) listener.onOrgRegister();
        });
        return v;
    }
}
