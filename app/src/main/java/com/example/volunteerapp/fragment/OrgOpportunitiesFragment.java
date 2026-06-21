package com.example.volunteerapp.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.volunteerapp.Hellper.DALAppWriteConnection;
import com.example.volunteerapp.Hellper.VolunteerAppHelper;
import com.example.volunteerapp.R;
import com.example.volunteerapp.adapter.OpportunityAdapter;
import com.example.volunteerapp.model.VolunteerOpportunity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * شاشة عرض فرص التطوع الخاصة بالمؤسسة.
 * <p>
 * تعرض قائمة {@link RecyclerView} بالفرص المتاحة التي أنشأتها المؤسسة،
 * مع زر عائم لإضافة فرصة جديدة عبر {@link OnAddOpportunity}.
 * تُحدَّث القائمة تلقائياً عند العودة إلى الشاشة.
 * </p>
 */
public class OrgOpportunitiesFragment extends Fragment {

    private static final String ARG_ORG_ID = "orgId";
    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String orgId;
    private OpportunityAdapter adapter;

    /** يُستدعى عند رغبة المؤسسة في إضافة فرصة تطوع جديدة. */
    public interface OnAddOpportunity {
        void onAddOpportunity(String orgId);
    }

    private OnAddOpportunity onAdd;

    public void setOnAddOpportunity(OnAddOpportunity l) { this.onAdd = l; }

    public static OrgOpportunitiesFragment newInstance(String orgId) {
        OrgOpportunitiesFragment f = new OrgOpportunitiesFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ORG_ID, orgId);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) orgId = getArguments().getString(ARG_ORG_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        executor = Executors.newSingleThreadExecutor();
        View v = inflater.inflate(R.layout.fragment_org_opportunities, container, false);
        RecyclerView rv = v.findViewById(R.id.rv_opportunities);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OpportunityAdapter();
        adapter.setShowStatus(true);
        rv.setAdapter(adapter);

        FloatingActionButton fab = v.findViewById(R.id.fab_add);
        fab.setOnClickListener(x -> {
            if (onAdd != null) onAdd.onAddOpportunity(orgId);
        });

        loadData(v);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) loadData(getView());
    }

    /** يجلب فرص التطوع من الخادم ويُحدّث القائمة أو يعرض رسالة "لا توجد فرص". */
    private void loadData(View v) {
        ProgressBar pb = v.findViewById(R.id.progress_bar);
        TextView tvEmpty = v.findViewById(R.id.tv_empty);
        pb.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        executor.execute(() -> {
            VolunteerAppHelper helper = new VolunteerAppHelper(requireContext());
            DALAppWriteConnection.OperationResult<ArrayList<VolunteerOpportunity>> result =
                    helper.getOrganizationOpportunities(orgId);
            mainHandler.post(() -> {
                if (!isAdded()) return;
                pb.setVisibility(View.GONE);
                if (result.success && result.data != null) {
                    adapter.setOrgNames(new HashMap<>());
                    adapter.setList(result.data);
                    tvEmpty.setVisibility(result.data.isEmpty() ? View.VISIBLE : View.GONE);
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
