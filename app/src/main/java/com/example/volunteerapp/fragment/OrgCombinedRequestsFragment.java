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
 * حاوية تجمع شاشات الطلبات الثلاث في تبويبات ({@link ViewPager2} + {@link TabLayout}).
 * <p>
 * التبويبات:
 * <ol>
 *   <li>طلبات الانضمام ({@link OrgRequestsFragment})</li>
 *   <li>طلبات الساعات ({@link OrgHoursReviewFragment})</li>
 *   <li>طلبات التقارير ({@link OrgReportRequestsFragment})</li>
 * </ol>
 * </p>
 */
public class OrgCombinedRequestsFragment extends Fragment {

    private static final String ARG_ORG_ID = "orgId";

    public static OrgCombinedRequestsFragment newInstance(String orgId) {
        OrgCombinedRequestsFragment f = new OrgCombinedRequestsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ORG_ID, orgId);
        f.setArguments(b);
        return f;
    }

    /**
     * يبني واجهة التبويبات ويربط كل تبويب بالجزء المناسب عبر {@link FragmentStateAdapter}.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_org_combined_requests, container, false);
        String orgId = requireArguments().getString(ARG_ORG_ID, "");

        TabLayout tabLayout = v.findViewById(R.id.tabs_requests);
        ViewPager2 pager = v.findViewById(R.id.pager_requests);
        pager.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return 3;
            }

            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return OrgRequestsFragment.newInstance(orgId);
                } else if (position == 1) {
                    return OrgHoursReviewFragment.newInstance(orgId);
                }
                return OrgReportRequestsFragment.newInstance(orgId);
            }
        });

        new TabLayoutMediator(tabLayout, pager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.tab_join_requests);
            } else if (position == 1) {
                tab.setText(R.string.tab_hours_requests);
            } else {
                tab.setText(R.string.tab_report_requests);
            }
        }).attach();

        return v;
    }
}
