package com.example.volunteerapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.volunteerapp.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * واجهة تبويبية تعرض علاقة الطالب بالمؤسسات.
 * <p>
 * تحتوي على تبويبين: «طلباتي» لعرض طلبات الانضمام،
 * و«ساعاتي» لعرض الساعات التطوعية المسجّلة.
 */
public class StudentMyOrgsFragment extends Fragment {

    private static final String ARG_STUDENT_ID = "studentId";

    public static StudentMyOrgsFragment newInstance(String studentId) {
        StudentMyOrgsFragment f = new StudentMyOrgsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_STUDENT_ID, studentId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_student_my_orgs, container, false);
        String studentId = requireArguments().getString(ARG_STUDENT_ID, "");

        TabLayout tabLayout = v.findViewById(R.id.tabs_my_orgs);
        ViewPager2 pager = v.findViewById(R.id.pager_my_orgs);
        pager.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return 2;
            }

            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return MyApplicationsFragment.newInstance(studentId);
                }
                return MyHoursFragment.newInstance(studentId);
            }
        });

        new TabLayoutMediator(tabLayout, pager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.my_applications);
            } else {
                tab.setText(R.string.my_hours);
            }
        }).attach();

        return v;
    }
}
