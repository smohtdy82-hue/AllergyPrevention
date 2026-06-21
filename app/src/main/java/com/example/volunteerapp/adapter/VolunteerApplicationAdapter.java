package com.example.volunteerapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.volunteerapp.R;
import com.example.volunteerapp.model.Organization;
import com.example.volunteerapp.model.VolunteerApplication;
import com.example.volunteerapp.model.VolunteerOpportunity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * محوّل لعرض طلبات الانضمام التطوعية الخاصة بالطالب.
 * يعرض اسم الفرصة أو المؤسسة وحالة الطلب (قيد الانتظار / مقبول / مرفوض)
 * مع سبب الرفض إن وُجد.
 * يدعم ربط الطلبات بالمؤسسات والفرص التطوعية عبر خريطتين منفصلتين.
 */
public class VolunteerApplicationAdapter extends RecyclerView.Adapter<VolunteerApplicationAdapter.ViewHolder> {

    private List<VolunteerApplication> list = new ArrayList<>();
    private Map<String, Organization> orgMap = new HashMap<>();
    private Map<String, VolunteerOpportunity> oppMap = new HashMap<>();

    /** نسخة مختصرة بدون قائمة الفرص التطوعية. */
    public void setList(List<VolunteerApplication> list, List<Organization> orgs) {
        setList(list, orgs, null);
    }

    /**
     * يُحدّث قائمة الطلبات ويبني خرائط الوصول السريع للمؤسسات والفرص.
     *
     * @param list قائمة طلبات الانضمام
     * @param orgs قائمة المؤسسات
     * @param opps قائمة الفرص التطوعية (اختياري)
     */
    public void setList(List<VolunteerApplication> list, List<Organization> orgs, List<VolunteerOpportunity> opps) {
        this.list = list != null ? list : new ArrayList<>();
        orgMap.clear();
        if (orgs != null) {
            for (Organization o : orgs) orgMap.put(o.getId(), o);
        }
        oppMap.clear();
        if (opps != null) {
            for (VolunteerOpportunity o : opps) oppMap.put(o.getId(), o);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_volunteer_application, parent, false);
        return new ViewHolder(v);
    }

    /**
     * يربط بيانات الطلب بعناصر الواجهة.
     * يُحدّد العنوان بناءً على وجود فرصة تطوعية أو مؤسسة مرتبطة،
     * ويُترجم حالة الطلب إلى نص عربي ويعرض سبب الرفض إن وُجد.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VolunteerApplication app = list.get(position);

        String label = "";
        VolunteerOpportunity opp = app.getOpportunityId() != null ? oppMap.get(app.getOpportunityId()) : null;
        Organization org = orgMap.get(app.getOrganizationId());
        if (opp != null) {
            label = opp.getTitle();
            if (org != null) label += " — " + org.getName();
        } else if (org != null) {
            label = org.getName();
        } else {
            label = "طلب";
        }
        holder.tvOrgName.setText(label);

        String statusText = app.getStatus();
        if (VolunteerApplication.STATUS_PENDING.equals(app.getStatus())) statusText = "قيد الانتظار";
        else if (VolunteerApplication.STATUS_ACCEPTED.equals(app.getStatus())) statusText = "مقبول";
        else if (VolunteerApplication.STATUS_REJECTED.equals(app.getStatus())) statusText = "مرفوض";
        holder.tvStatus.setText(statusText);

        if (app.getRejectReason() != null && !app.getRejectReason().isEmpty()) {
            holder.tvRejectReason.setVisibility(View.VISIBLE);
            holder.tvRejectReason.setText("سبب الرفض: " + app.getRejectReason());
        } else {
            holder.tvRejectReason.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrgName, tvStatus, tvRejectReason;

        ViewHolder(View itemView) {
            super(itemView);
            tvOrgName = itemView.findViewById(R.id.tv_org_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvRejectReason = itemView.findViewById(R.id.tv_reject_reason);
        }
    }
}
