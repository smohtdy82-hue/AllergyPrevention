package com.example.mohtadyapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mohtadyapp.R;
import com.example.mohtadyapp.model.Organization;
import com.example.mohtadyapp.model.VolunteerApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VolunteerApplicationAdapter extends RecyclerView.Adapter<VolunteerApplicationAdapter.ViewHolder> {

    private List<VolunteerApplication> list = new ArrayList<>();
    private Map<String, Organization> orgMap = new HashMap<>();

    public void setList(List<VolunteerApplication> list, List<Organization> orgs) {
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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_volunteer_application, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VolunteerApplication app = list.get(position);
        Organization org = orgMap.get(app.getOrganizationId());
        holder.tvOrgName.setText(org != null ? org.getName() : "مؤسسة");
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
