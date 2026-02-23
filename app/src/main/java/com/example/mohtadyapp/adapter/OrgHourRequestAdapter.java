package com.example.mohtadyapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mohtadyapp.R;
import com.example.mohtadyapp.model.Student;
import com.example.mohtadyapp.model.VolunteerHour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrgHourRequestAdapter extends RecyclerView.Adapter<OrgHourRequestAdapter.ViewHolder> {

    private List<VolunteerHour> list = new ArrayList<>();
    private Map<String, Student> studentMap = new HashMap<>();
    private OnAcceptClickListener onAccept;
    private OnRejectClickListener onReject;

    public interface OnAcceptClickListener {
        void onAccept(VolunteerHour hour);
    }

    public interface OnRejectClickListener {
        void onReject(VolunteerHour hour);
    }

    public void setOnAcceptClickListener(OnAcceptClickListener listener) {
        this.onAccept = listener;
    }

    public void setOnRejectClickListener(OnRejectClickListener listener) {
        this.onReject = listener;
    }

    public void setList(List<VolunteerHour> list, List<Student> students) {
        this.list = list != null ? list : new ArrayList<>();
        studentMap.clear();
        if (students != null) {
            for (Student s : students) studentMap.put(s.getId(), s);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_org_hour_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VolunteerHour h = list.get(position);
        Student student = studentMap.get(h.getStudentId());
        holder.tvStudentName.setText(student != null ? student.getName() : "طالب");
        holder.tvHours.setText(h.getHours() + " ساعة");
        if (h.getDescription() != null && !h.getDescription().isEmpty()) {
            holder.tvDescription.setVisibility(View.VISIBLE);
            holder.tvDescription.setText(h.getDescription());
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }
        holder.btnAccept.setOnClickListener(v -> {
            if (onAccept != null) onAccept.onAccept(h);
        });
        holder.btnReject.setOnClickListener(v -> {
            if (onReject != null) onReject.onReject(h);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvHours, tvDescription;
        Button btnAccept, btnReject;

        ViewHolder(View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tv_student_name);
            tvHours = itemView.findViewById(R.id.tv_hours);
            tvDescription = itemView.findViewById(R.id.tv_description);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}
