package com.bennet.wallet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bennet.wallet.R;
import com.bennet.wallet.utils.Utility;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class ShowCardPropertyListItemAdapter extends RecyclerView.Adapter<ShowCardPropertyListItemAdapter.ViewHolder> {

    protected List<Utility.StringPair> cardProperties;

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

    public ShowCardPropertyListItemAdapter(List<Utility.StringPair> cardProperties) {
        this.cardProperties = cardProperties;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.adapter_show_card_property_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Replace the contents of a view (invoked by the layout manager)

        if (position == 0)
            holder.divider.setVisibility(View.GONE);
        else
            holder.divider.setVisibility(View.VISIBLE);

        holder.nameView.setText(cardProperties.get(position).first);
        holder.valueView.setText(cardProperties.get(position).second);
    }

    @Override
    public int getItemCount() {
        return cardProperties.size();
    }

}
