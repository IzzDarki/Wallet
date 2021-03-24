package com.bennet.wallet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class ShowCardPropertyListItemAdapter extends RecyclerView.Adapter<ShowCardPropertyListItemAdapter.ViewHolder> {

    protected Context context;
    protected List<ShowCardActivity.CardProperty> cardProperties;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        protected View divider;
        protected MaterialTextView nameView;
        protected MaterialTextView valueView;

        public ViewHolder(View v) {
            super(v);
            divider = v.findViewById(R.id.show_card_property_list_item_divider);
            nameView = v.findViewById(R.id.show_card_property_list_item_name_view);
            valueView = v.findViewById(R.id.show_card_property_list_item_value_view);
        }
    }

    public ShowCardPropertyListItemAdapter(Context context, List<ShowCardActivity.CardProperty> cardProperties) {
        this.context = context;
        this.cardProperties = cardProperties;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.show_card_property_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Replace the contents of a view (invoked by the layout manager)

        if (position == 0)
            holder.divider.setVisibility(View.GONE);

        String name = cardProperties.get(position).name;
        if (CardPreferenceManager.PREFERENCE_CARD_ID.equals(name))
            name = context.getString(R.string.card_id);
        holder.nameView.setText(name);

        holder.valueView.setText(cardProperties.get(position).value);
    }

    @Override
    public int getItemCount() {
        return cardProperties.size();
    }

}
