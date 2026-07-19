package com.luphihung.mhike.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.luphihung.mhike.R;
import com.luphihung.mhike.model.Observation;
import com.luphihung.mhike.util.Formats;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds the observations of a hike to cards on the hike detail screen.
 */
public class ObservationAdapter extends RecyclerView.Adapter<ObservationAdapter.ObservationViewHolder> {

    /** Callbacks for the per-observation options menu. */
    public interface OnObservationActionListener {
        void onEditObservation(Observation observation);

        void onDeleteObservation(Observation observation);
    }

    private final List<Observation> observations = new ArrayList<>();
    private final OnObservationActionListener actionListener;

    public ObservationAdapter(OnObservationActionListener actionListener) {
        this.actionListener = actionListener;
    }

    /** Replaces the whole list, e.g. after a database reload. */
    public void submitList(List<Observation> newObservations) {
        observations.clear();
        observations.addAll(newObservations);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ObservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_observation, parent, false);
        return new ObservationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ObservationViewHolder holder, int position) {
        holder.bind(observations.get(position));
    }

    @Override
    public int getItemCount() {
        return observations.size();
    }

    class ObservationViewHolder extends RecyclerView.ViewHolder {

        private final TextView observationText;
        private final TextView timeText;
        private final TextView commentsText;
        private final ImageButton menuButton;

        ObservationViewHolder(@NonNull View itemView) {
            super(itemView);
            observationText = itemView.findViewById(R.id.text_observation);
            timeText = itemView.findViewById(R.id.text_observation_time);
            commentsText = itemView.findViewById(R.id.text_observation_comments);
            menuButton = itemView.findViewById(R.id.button_observation_menu);
        }

        void bind(Observation observation) {
            observationText.setText(observation.getText());
            timeText.setText(Formats.displayDateTime(observation.getObservedAt()));

            boolean hasComments = observation.getComments() != null
                    && !observation.getComments().trim().isEmpty();
            commentsText.setVisibility(hasComments ? View.VISIBLE : View.GONE);
            commentsText.setText(observation.getComments());

            menuButton.setOnClickListener(v -> showOptionsMenu(observation));
        }

        private void showOptionsMenu(Observation observation) {
            android.widget.PopupMenu popup =
                    new android.widget.PopupMenu(itemView.getContext(), menuButton);
            popup.getMenu().add(R.string.action_edit);
            popup.getMenu().add(R.string.action_delete);
            popup.setOnMenuItemClickListener(item -> {
                String editLabel = itemView.getContext().getString(R.string.action_edit);
                if (editLabel.contentEquals(item.getTitle())) {
                    actionListener.onEditObservation(observation);
                } else {
                    actionListener.onDeleteObservation(observation);
                }
                return true;
            });
            popup.show();
        }
    }
}
