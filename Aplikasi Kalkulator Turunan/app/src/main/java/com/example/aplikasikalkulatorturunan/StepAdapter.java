package com.example.aplikasikalkulatorturunan;

// StepAdapter.java
// RecyclerView.Adapter untuk menampilkan daftar Stepmodel.
// Disesuaikan dengan item_step.xml versi project kamu:
//   tvStepNumber, tvStepTitle, tvStepBadge, tvStepDetail, ivArrow, stepHeader

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StepAdapter extends RecyclerView.Adapter<StepAdapter.StepViewHolder> {

    private List<Stepmodel> items = new ArrayList<>();
    private final List<Boolean> expanded = new ArrayList<>();

    public void submitList(List<Stepmodel> newItems) {
        items = newItems != null ? newItems : new ArrayList<>();
        expanded.clear();
        for (int i = 0; i < items.size(); i++) expanded.add(false);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_step, parent, false);
        return new StepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        Stepmodel step = items.get(position);
        boolean isExpanded = expanded.get(position);

        holder.tvStepNumber.setText(String.valueOf(step.number));
        holder.tvStepTitle.setText(step.title);

        // Badge: tampilkan formula umum secara singkat (potong jika terlalu panjang)
        String badge = step.formula;
        if (badge != null && badge.length() > 22) {
            badge = badge.substring(0, 20) + "..";
        }
        holder.tvStepBadge.setText(badge);

        holder.tvStepDetail.setText(step.detail);
        holder.tvStepDetail.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.ivArrow.setRotation(isExpanded ? 180f : 0f);

        holder.stepHeader.setOnClickListener(v -> {
            boolean newState = !expanded.get(position);
            expanded.set(position, newState);
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class StepViewHolder extends RecyclerView.ViewHolder {
        LinearLayout stepHeader;
        TextView tvStepNumber, tvStepTitle, tvStepBadge, tvStepDetail;
        ImageView ivArrow;

        StepViewHolder(@NonNull View itemView) {
            super(itemView);
            stepHeader   = itemView.findViewById(R.id.stepHeader);
            tvStepNumber = itemView.findViewById(R.id.tvStepNumber);
            tvStepTitle  = itemView.findViewById(R.id.tvStepTitle);
            tvStepBadge  = itemView.findViewById(R.id.tvStepBadge);
            tvStepDetail = itemView.findViewById(R.id.tvStepDetail);
            ivArrow      = itemView.findViewById(R.id.ivArrow);
        }
    }
}