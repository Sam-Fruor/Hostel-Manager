package com.example.propertymanager;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

    private Context context;
    private List<RoomData> roomList;
    private OnRoomActionListener listener;

    // Interface for communication with MainActivity
    public interface OnRoomActionListener {
        void onCollectRoomRent(RoomData room);
        void onUpdateRent(RoomData room);
        void onTenantMoveOut(RoomData room);
    }

    public RoomAdapter(Context context, List<RoomData> roomList, OnRoomActionListener listener) {
        this.context = context;
        this.roomList = roomList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        RoomData room = roomList.get(position);

        // 1. Set Title
        holder.tvRoomTitle.setText(room.getPropertyName() + " - " + room.getRoomNumber());

        // 2. Occupancy & Rent Text
        holder.tvOccupancy.setText(room.isVacant() ? "Vacant" : "Occupied");
        holder.tvRoomRent.setText("Rent: ₹" + (int)room.getStandardRent());

        // 3. Due Date & Total Due Logic
        if (room.isVacant()) {
            // VACANT STATE
            holder.tvDueDate.setVisibility(View.GONE);
            holder.tvDueStatus.setText("No Dues");
            holder.tvDueStatus.setTextColor(Color.GRAY);
            holder.btnCollect.setVisibility(View.GONE);
        } else {
            // OCCUPIED STATE
            double totalDue = room.getCurrentDue();
            if (totalDue > 0) {
                // --- HAS DUES (RED STATE) ---
                holder.tvDueDate.setVisibility(View.VISIBLE);
                holder.tvDueDate.setText("Due Date: " + room.getDueDate());
                holder.tvDueDate.setTextColor(Color.RED);

                holder.tvDueStatus.setText("Total Due: ₹" + (int)totalDue);
                holder.tvDueStatus.setTextColor(Color.RED);
                holder.btnCollect.setVisibility(View.VISIBLE);
            } else {
                // --- PAID (GREEN STATE) ---
                holder.tvDueDate.setVisibility(View.GONE);

                holder.tvDueStatus.setText("Paid / No Dues");
                holder.tvDueStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                holder.btnCollect.setVisibility(View.GONE);
            }
        }

        // --- CLICK LISTENERS ---
        holder.btnCollect.setOnClickListener(v -> {
            if (room.isVacant()) {
                Toast.makeText(v.getContext(), "Cannot collect rent: Room is Vacant!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) listener.onCollectRoomRent(room);
        });

        holder.btnEditRent.setOnClickListener(v -> {
            if(listener != null) listener.onUpdateRent(room);
        });

        // --- HIDE DELETE BUTTON PERMANENTLY ---
        if (holder.btnDelete != null) {
            holder.btnDelete.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            RoomDetailActivity.currentRoomData = room;
            android.content.Intent intent = new android.content.Intent(context, RoomDetailActivity.class);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomTitle, tvOccupancy, tvRoomRent, tvDueStatus, tvDueDate;
        Button btnCollect, btnEditRent;
        ImageView btnDelete;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomTitle = itemView.findViewById(R.id.tvRoomTitle);
            tvOccupancy = itemView.findViewById(R.id.tvOccupancy);
            tvRoomRent = itemView.findViewById(R.id.tvRoomRent);
            tvDueStatus = itemView.findViewById(R.id.tvDueStatus);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            btnCollect = itemView.findViewById(R.id.btnCollectRoomRent);
            btnEditRent = itemView.findViewById(R.id.btnEditRent);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}