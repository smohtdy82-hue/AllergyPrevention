package com.example.volunteerapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.volunteerapp.R;
import com.example.volunteerapp.model.VolunteerOpportunity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * محوّل لعرض قائمة الفرص التطوعية المتاحة.
 * يعرض عنوان الفرصة واسم المؤسسة والموقع وعدد الساعات،
 * مع إمكانية عرض حالة الفرصة (نشطة / مغلقة) حسب الإعداد.
 * يدعم النقر على العنصر لعرض تفاصيل الفرصة.
 */
public class OpportunityAdapter extends RecyclerView.Adapter<OpportunityAdapter.ViewHolder> {

    private List<VolunteerOpportunity> list = new ArrayList<>();
    private Map<String, String> orgNames = new HashMap<>();
    private OnItemClickListener listener;
    private boolean showStatus = false;

    /** واجهة استماع لحدث النقر على فرصة تطوعية لعرض تفاصيلها. */
    public interface OnItemClickListener {
        void onItemClick(VolunteerOpportunity opportunity);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /** يتحكّم بإظهار أو إخفاء حالة الفرصة (نشطة / مغلقة) في كل عنصر. */
    public void setShowStatus(boolean show) {
        this.showStatus = show;
    }

    public void setList(List<VolunteerOpportunity> list) {
        this.list = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * يُعيّن خريطة أسماء المؤسسات المفهرسة بمعرّف المؤسسة،
     * لعرض اسم المؤسسة بجانب كل فرصة دون الحاجة لجلب الكائن الكامل.
     */
    public void setOrgNames(Map<String, String> orgNames) {
        this.orgNames = orgNames != null ? orgNames : new HashMap<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_opportunity, parent, false);
        return new ViewHolder(v);
    }

    /**
     * يربط بيانات الفرصة بعناصر الواجهة ويعرض الحالة بلون مميّز
     * (أخضر للنشطة، أحمر للمغلقة) عند تفعيل خيار {@code showStatus}.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        VolunteerOpportunity opp = list.get(position);
        h.tvTitle.setText(opp.getTitle());
        String orgName = orgNames.get(opp.getOrganizationId());
        h.tvOrgName.setText(orgName != null ? orgName : "");
        h.tvLocation.setText(opp.getLocation() != null ? opp.getLocation() : "");
        h.tvHours.setText(opp.getHours() + " ساعة");

        if (showStatus && opp.getStatus() != null) {
            h.tvStatus.setVisibility(View.VISIBLE);
            boolean active = VolunteerOpportunity.STATUS_ACTIVE.equals(opp.getStatus());
            h.tvStatus.setText(active ? "نشطة" : "مغلقة");
            h.tvStatus.setTextColor(active ? 0xFF388E3C : 0xFFD32F2F);
        } else {
            h.tvStatus.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(opp);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvOrgName, tvLocation, tvHours, tvStatus;

        ViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tv_title);
            tvOrgName = v.findViewById(R.id.tv_org_name);
            tvLocation = v.findViewById(R.id.tv_location);
            tvHours = v.findViewById(R.id.tv_hours);
            tvStatus = v.findViewById(R.id.tv_status);
        }
    }
}
