package com.example.propertymanager;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TenantAdapter extends RecyclerView.Adapter<TenantAdapter.TenantViewHolder> {

    private Context context;
    private List<Tenant> tenantList;
    private OnTenantActionListener listener;

    public interface OnTenantActionListener {
        void onItemClick(Tenant tenant);
        void onPayClick(Tenant tenant);
        void onLeaveClick(Tenant tenant);
        void onWhatsAppClick(Tenant tenant);
    }

    public TenantAdapter(Context context, List<Tenant> tenantList, OnTenantActionListener listener) {
        this.context = context;
        this.tenantList = tenantList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TenantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tenant, parent, false);
        return new TenantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TenantViewHolder holder, int position) {
        Tenant tenant = tenantList.get(position);

        if ("VACANT".equals(tenant.getName())) {
            holder.tvName.setText("Vacant Slot");
            holder.tvName.setTextColor(Color.GRAY);
            holder.tvDate.setVisibility(View.GONE);
            holder.tvRent.setVisibility(View.GONE);
            holder.btnPay.setVisibility(View.GONE);
            holder.btnWhatsApp.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.tvName.setText(tenant.getName());
            holder.tvName.setTextColor(Color.BLACK);
            holder.tvDate.setVisibility(View.GONE);

            if (!tenant.isActive()) {
                // --- HISTORY STYLE (VEHICLE RECORD STYLE) ---
                holder.btnPay.setVisibility(View.GONE);
                holder.btnWhatsApp.setVisibility(View.GONE);
                holder.btnDelete.setVisibility(View.GONE);

                // Show combined Property + Room Number Badge
                holder.tvRent.setVisibility(View.VISIBLE);
                String fullLocation = tenant.getHostelName() + " " + tenant.getRoomNumber();
                holder.tvRent.setText(fullLocation);

                // Style to match your Vehicle Record badge
                holder.tvRent.setTextColor(Color.parseColor("#1976D2")); // Blue
                holder.tvRent.setTextSize(14f);
                holder.tvRent.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                // --- ACTIVE TENANT STYLE ---
                holder.tvRent.setVisibility(View.GONE);
                holder.btnPay.setVisibility(View.GONE);
                holder.btnWhatsApp.setVisibility(View.VISIBLE);
                holder.btnDelete.setVisibility(View.VISIBLE);
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(tenant));
        holder.btnDelete.setOnClickListener(v -> listener.onLeaveClick(tenant));
        holder.btnWhatsApp.setOnClickListener(v -> listener.onWhatsAppClick(tenant));
    }

    @Override
    public int getItemCount() { return tenantList.size(); }

    public static class TenantViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRent, tvDate;
        ImageView btnPay, btnWhatsApp, btnDelete;

        public TenantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvRent = itemView.findViewById(R.id.tvRent);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnPay = itemView.findViewById(R.id.btnPay);
            btnWhatsApp = itemView.findViewById(R.id.btnWhatsApp);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}