package com.example.aplikasikalkulatorturunan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter untuk RecyclerView langkah penyelesaian turunan.
 *
 * Fitur:
 * - Accordion: hanya satu langkah terbuka sekaligus
 * - Chevron berputar saat expand/collapse
 * - submitList() untuk update data + notifyDataSetChanged
 */
public class StepAdapter extends RecyclerView.Adapter<StepAdapter.StepViewHolder> {

    private List<Stepmodel> items          = new ArrayList<>();
    private int             expandedIndex  = -1; // -1 = semua collapsed

    // ------------------------------------------------------------------
    // ViewHolder
    // ------------------------------------------------------------------
    static class StepViewHolder extends RecyclerView.ViewHolder {
        final View     stepHeader;
        final TextView tvNumber;
        final TextView tvTitle;
        final TextView tvBadge;
        final TextView tvDetail;
        final ImageView ivArrow;

        StepViewHolder(@NonNull View itemView) {
            super(itemView);
            stepHeader = itemView.findViewById(R.id.stepHeader);
            tvNumber   = itemView.findViewById(R.id.tvStepNumber);
            tvTitle    = itemView.findViewById(R.id.tvStepTitle);
            tvBadge    = itemView.findViewById(R.id.tvStepBadge);
            tvDetail   = itemView.findViewById(R.id.tvStepDetail);
            ivArrow    = itemView.findViewById(R.id.ivArrow);
        }
    }

    // ------------------------------------------------------------------
    // Adapter overrides
    // ------------------------------------------------------------------
    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_step, parent, false);
        return new StepViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        Stepmodel step       = items.get(position);
        boolean   isExpanded = (position == expandedIndex);

        holder.tvNumber.setText(String.valueOf(step.number));
        holder.tvTitle.setText(step.title);
        holder.tvBadge.setText(step.badge);
        holder.tvDetail.setText(step.detail);

        // Tampilkan / sembunyikan detail
        holder.tvDetail.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Putar chevron
        holder.ivArrow.setRotation(isExpanded ? 180f : 0f);

        // Klik header → toggle accordion
        holder.stepHeader.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;

            int previous = expandedIndex;

            if (expandedIndex == pos) {
                // Tutup yang sudah terbuka
                expandedIndex = -1;
                notifyItemChanged(pos);
            } else {
                // Tutup item lama, buka item baru
                expandedIndex = pos;
                if (previous != -1) notifyItemChanged(previous);
                notifyItemChanged(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /** Ganti data dan reset expanded state */
    public void submitList(List<Stepmodel> newItems) {
        expandedIndex = -1;
        items = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    /** Collapse semua item tanpa ganti data */
    public void collapseAll() {
        int previous = expandedIndex;
        expandedIndex = -1;
        if (previous != -1) notifyItemChanged(previous);
    }
}