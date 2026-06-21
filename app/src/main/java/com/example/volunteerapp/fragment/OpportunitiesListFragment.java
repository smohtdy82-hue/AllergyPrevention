package com.example.volunteerapp.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.volunteerapp.Hellper.DALAppWriteConnection;
import com.example.volunteerapp.Hellper.VolunteerAppHelper;
import com.example.volunteerapp.R;
import com.example.volunteerapp.adapter.OpportunityAdapter;
import com.example.volunteerapp.model.Organization;
import com.example.volunteerapp.model.VolunteerOpportunity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * شاشة عرض قائمة فرص التطوع النشطة المتاحة للطالب.
 * تجلب الفرص وأسماء المؤسسات من قاعدة البيانات وتعرضها في قائمة قابلة للنقر.
 */
public class OpportunitiesListFragment extends Fragment {

    private static final String ARG_STUDENT_ID = "studentId";
    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String studentId;
    private OpportunityAdapter adapter;

    /**
     * واجهة رد الفعل عند اختيار فرصة تطوعية من القائمة.
     */
    public interface OnOpportunitySelected {
        void onOpportunitySelected(VolunteerOpportunity opportunity, String studentId);
    }

    private OnOpportunitySelected listener;

    public void setOnOpportunitySelected(OnOpportunitySelected l) { this.listener = l; }

    /**
     * إنشاء نسخة جديدة من الشاشة مع تمرير معرّف الطالب.
     *
     * @param studentId معرّف الطالب الحالي
     * @return نسخة مُهيّأة من OpportunitiesListFragment
     */
    public static OpportunitiesListFragment newInstance(String studentId) {
        OpportunitiesListFragment f = new OpportunitiesListFragment();
        Bundle b = new Bundle();
        b.putString(ARG_STUDENT_ID, studentId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        executor = Executors.newSingleThreadExecutor();
        View v = inflater.inflate(R.layout.fragment_org_opportunities, container, false);
        studentId = getArguments() != null ? getArguments().getString(ARG_STUDENT_ID, "") : "";

        v.findViewById(R.id.fab_add).setVisibility(View.GONE);

        RecyclerView rv = v.findViewById(R.id.rv_opportunities);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OpportunityAdapter();
        adapter.setOnItemClickListener(opp -> {
            if (listener != null) listener.onOpportunitySelected(opp, studentId);
        });
        rv.setAdapter(adapter);

        loadData(v);
        return v;
    }

    /**
     * تحميل الفرص التطوعية النشطة وأسماء المؤسسات من الخادم بشكل غير متزامن.
     */
    private void loadData(View v) {
        ProgressBar pb = v.findViewById(R.id.progress_bar);
        TextView tvEmpty = v.findViewById(R.id.tv_empty);
        pb.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        executor.execute(() -> {
            VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerOpportunity>> oppResult =
                    helper.getActiveOpportunities();
            DALAppWriteConnection.OperationResult<ArrayList<Organization>> orgResult =
                    helper.getOrganizations();

            Map<String, String> orgNames = new HashMap<>();
            if (orgResult.success && orgResult.data != null) {
                for (Organization o : orgResult.data) {
                    orgNames.put(o.getId(), o.getName());
                }
            }

            mainHandler.post(() -> {
                if (!isAdded()) return;
                pb.setVisibility(View.GONE);
                if (oppResult.success && oppResult.data != null) {
                    adapter.setOrgNames(orgNames);
                    adapter.setList(oppResult.data);
                    tvEmpty.setVisibility(oppResult.data.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
