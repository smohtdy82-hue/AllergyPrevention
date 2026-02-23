package com.example.mohtadyapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.mohtadyapp.Hellper.VolunteerAppHelper;
import com.example.mohtadyapp.R;
import com.example.mohtadyapp.model.Student;

public class StudentProfileFragment extends Fragment {

    public interface OnLogout {
        void onLogout();
    }

    private OnLogout onLogout;
    private Student student;

    public static StudentProfileFragment newInstance(Student student) {
        StudentProfileFragment f = new StudentProfileFragment();
        Bundle b = new Bundle();
        b.putString("studentId", student.getId());
        b.putString("studentName", student.getName());
        b.putString("imageUrl", student.getImageUrl() != null ? student.getImageUrl() : "");
        b.putInt("requiredHours", student.getRequiredHours());
        b.putInt("completedHours", student.getCompletedHours());
        f.setArguments(b);
        return f;
    }

    public void setOnLogout(OnLogout listener) { this.onLogout = listener; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_student_profile, container, false);
        Bundle args = getArguments();
        String name = args != null ? args.getString("studentName", "") : "";
        String imageUrl = args != null ? args.getString("imageUrl", "") : "";
        int required = args != null ? args.getInt("requiredHours", 0) : 0;
        int completed = args != null ? args.getInt("completedHours", 0) : 0;

        ((TextView) v.findViewById(R.id.tv_name)).setText(name);
        ((TextView) v.findViewById(R.id.tv_hours_summary)).setText("الساعات: " + completed + " / " + required);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(requireContext()).load(imageUrl).into((ImageView) v.findViewById(R.id.iv_profile));
        }

        v.findViewById(R.id.btn_logout).setOnClickListener(v1 -> {
            if (onLogout != null) onLogout.onLogout();
        });

        return v;
    }
}
