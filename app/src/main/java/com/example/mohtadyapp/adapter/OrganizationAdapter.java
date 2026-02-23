package com.example.mohtadyapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.mohtadyapp.R;
import com.example.mohtadyapp.model.Organization;

import java.util.ArrayList;
import java.util.List;

public class OrganizationAdapter extends RecyclerView.Adapter<OrganizationAdapter.ViewHolder> {

    private List<Organization> list = new ArrayList<>();
    private OnApplyClickListener listener;

    public interface OnApplyClickListener {
        void onApply(Organization org);
    }

    public void setOnApplyClickListener(OnApplyClickListener listener) {
        this.listener = listener;
    }

    public void setList(List<Organization> list) {
        this.list = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_organization, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Organization org = list.get(position);
        holder.tvName.setText(org.getName());
        holder.tvAddress.setText(org.getAddress());
        holder.tvHours.setText(org.getVolunteerDays() + " - " + org.getVolunteerHours() + " (" + org.getTotalHours() + " ساعة)");
        if (org.getImageUrl() != null && !org.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(org.getImageUrl())
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        holder.btnApply.setOnClickListener(v -> {
            if (listener != null) listener.onApply(org);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvAddress, tvHours;
        Button btnApply;

        ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_org_image);
            tvName = itemView.findViewById(R.id.tv_org_name);
            tvAddress = itemView.findViewById(R.id.tv_org_address);
            tvHours = itemView.findViewById(R.id.tv_org_hours);
            btnApply = itemView.findViewById(R.id.btn_apply);
        }
    }
}
