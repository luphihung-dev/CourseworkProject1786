package com.luphihung.mhike.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import com.luphihung.mhike.R;
import com.luphihung.mhike.model.Hike;
import com.luphihung.mhike.util.Formats;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds hikes to the cards shown on the home screen and in search results.
 */
public class HikeAdapter extends RecyclerView.Adapter<HikeAdapter.HikeViewHolder> {

    /** Callback fired when the user taps a hike card. */
    public interface OnHikeClickListener {
        void onHikeClick(Hike hike);
    }

    private final List<Hike> hikes = new ArrayList<>();
    private final OnHikeClickListener clickListener;

    public HikeAdapter(OnHikeClickListener clickListener) {
        this.clickListener = clickListener;
    }

    /** Replaces the whole list, e.g. after a database reload. */
    public void submitList(List<Hike> newHikes) {
        hikes.clear();
        hikes.addAll(newHikes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HikeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hike, parent, false);
        return new HikeViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull HikeViewHolder holder, int position) {
        holder.bind(hikes.get(position));
    }

    @Override
    public int getItemCount() {
        return hikes.size();
    }

    class HikeViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameText;
        private final TextView locationText;
        private final TextView dateLengthText;
        private final Chip difficultyChip;

        HikeViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_hike_name);
            locationText = itemView.findViewById(R.id.text_hike_location);
            dateLengthText = itemView.findViewById(R.id.text_hike_date_length);
            difficultyChip = itemView.findViewById(R.id.chip_difficulty);
        }

        void bind(Hike hike) {
            nameText.setText(hike.getName());
            locationText.setText(hike.getLocation());
            dateLengthText.setText(itemView.getContext().getString(
                    R.string.format_date_and_length,
                    Formats.displayDate(hike.getDate()),
                    Formats.compactNumber(hike.getLengthKm())));
            difficultyChip.setText(hike.getDifficulty());
            itemView.setOnClickListener(v -> clickListener.onHikeClick(hike));
        }
    }
}
