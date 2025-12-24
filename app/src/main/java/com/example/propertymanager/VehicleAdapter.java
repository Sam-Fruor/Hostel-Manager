package com.example.propertymanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.ViewHolder> {

    private List<Tenant> list;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Tenant tenant);
    }

    public VehicleAdapter(List<Tenant> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vehicle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tenant t = list.get(position);

        holder.tvNumber.setText(t.getVehicleNumber().toUpperCase());
        holder.tvOwner.setText(t.getName());

        // --- UPDATED THIS PART ---
        // Combine Property Name and Room Number (e.g., "Hostel 101")
        String fullLocation = t.getHostelName() + " " + t.getRoomNumber();
        holder.tvRoom.setText(fullLocation);
        // -------------------------

        String type = (t.getVehicleType() == null || t.getVehicleType().isEmpty()) ? "Vehicle" : t.getVehicleType();
        holder.tvType.setText(type);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(t);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumber, tvOwner, tvRoom, tvType;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tvVehicleNumber);
            tvOwner = itemView.findViewById(R.id.tvOwnerName);
            tvRoom = itemView.findViewById(R.id.tvRoomNumber);
            tvType = itemView.findViewById(R.id.tvVehicleType);
        }
    }
}