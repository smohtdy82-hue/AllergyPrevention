package com.example.volunteerapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.volunteerapp.R;
import com.example.volunteerapp.model.Organization;
import com.example.volunteerapp.model.VolunteerHour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * محوّل لعرض سجل الساعات التطوعية الخاصة بالطالب.
 * يعرض اسم المؤسسة وعدد الساعات وحالة الطلب (قيد المراجعة / مقبولة / مرفوضة)
 * مع الوصف وسبب الرفض إن وُجدا.
 */
public class VolunteerHourAdapter extends RecyclerView.Adapter<VolunteerHourAdapter.ViewHolder> {

    private List<VolunteerHour> list = new ArrayList<>();
    private Map<String, Organization> orgMap = new HashMap<>();
    private OnRegisterHoursClickListener listener;

    /** واجهة استماع لحدث تسجيل ساعات تطوعية جديدة لدى مؤسسة معيّنة. */
    public interface OnRegisterHoursClickListener {
        void onRegisterHours(Organization org);
    }

    public void setOnRegisterHoursClickListener(OnRegisterHoursClickListener listener) {
        this.listener = listener;
    }

    /**
     * يُحدّث قائمة الساعات التطوعية ويبني خريطة المؤسسات للوصول السريع.
     *
     * @param list قائمة سجلات الساعات التطوعية
     * @param orgs قائمة المؤسسات المرتبطة
     */
    public void setList(List<VolunteerHour> list, List<Organization> orgs) {
        this.list = list != null ? list : new ArrayList<>();
        orgMap.clear();
        if (orgs != null) {
            for (Organization o : orgs) orgMap.put(o.getId(), o);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_volunteer_hour, parent, false);
        return new ViewHolder(v);
    }

    /**
     * يربط بيانات سجل الساعات بعناصر الواجهة.
     * يُترجم الحالة إلى نص عربي، ويُظهر الوصف وسبب الرفض عند توفرهما.
     * زر التسجيل يكون مخفيًا لأن هذه الشاشة للعرض فقط.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VolunteerHour h = list.get(position);
        Organization org = orgMap.get(h.getOrganizationId());
        holder.tvOrgName.setText(org != null ? org.getName() : "مؤسسة");
        holder.tvHours.setText(h.getHours() + " ساعة");

        String statusText = h.getStatus();
        if (VolunteerHour.STATUS_PENDING.equals(h.getStatus())) statusText = "قيد المراجعة";
        else if (VolunteerHour.STATUS_ACCEPTED.equals(h.getStatus())) statusText = "مقبولة";
        else if (VolunteerHour.STATUS_REJECTED.equals(h.getStatus())) statusText = "مرفوضة";
        holder.tvStatus.setText(statusText);

        if (h.getDescription() != null && !h.getDescription().isEmpty()) {
            holder.tvDescription.setVisibility(View.VISIBLE);
            holder.tvDescription.setText(h.getDescription());
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        holder.btnRegister.setVisibility(View.GONE);

        if (h.getRejectReason() != null && !h.getRejectReason().isEmpty()) {
            holder.tvRejectReason.setVisibility(View.VISIBLE);
            holder.tvRejectReason.setText("سبب الرفض: " + h.getRejectReason());
        } else {
            holder.tvRejectReason.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrgName, tvHours, tvStatus, tvDescription, tvRejectReason;
        Button btnRegister;

        ViewHolder(View itemView) {
            super(itemView);
            tvOrgName = itemView.findViewById(R.id.tv_org_name);
            tvHours = itemView.findViewById(R.id.tv_hours);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvRejectReason = itemView.findViewById(R.id.tv_reject_reason);
            btnRegister = itemView.findViewById(R.id.btn_register_hours);
        }
    }
}
