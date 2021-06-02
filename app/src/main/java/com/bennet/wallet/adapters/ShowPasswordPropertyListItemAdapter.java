package com.bennet.wallet.adapters;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bennet.wallet.R;
import com.bennet.wallet.activities.passwords.ShowPasswordActivity;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class ShowPasswordPropertyListItemAdapter extends RecyclerView.Adapter<ShowPasswordPropertyListItemAdapter.ViewHolder> {

    protected List<ShowPasswordActivity.ShowPasswordProperty> passwordProperties;

    static public class ViewHolder extends RecyclerView.ViewHolder {
        protected View divider;
        protected MaterialTextView nameView;
        protected MaterialTextView valueView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            divider = itemView.findViewById(R.id.show_password_properties_list_item_divider);
            nameView = itemView.findViewById(R.id.show_password_properties_list_item_name_view);
            valueView = itemView.findViewById(R.id.show_password_properties_list_item_value_view);
        }
    }

    public ShowPasswordPropertyListItemAdapter(List<ShowPasswordActivity.ShowPasswordProperty> passwordProperties) {
        this.passwordProperties = passwordProperties;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.show_password_property_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShowPasswordPropertyListItemAdapter.ViewHolder holder, int position) {
        // Replace the contents of a view (invoked by the layout manager)

        if (position == 0)
            holder.divider.setVisibility(View.GONE);

        holder.nameView.setText(passwordProperties.get(position).name);
        holder.valueView.setText(passwordProperties.get(position).value);

        boolean hidden = passwordProperties.get(position).hidden;
        if (hidden) {
            holder.valueView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            holder.valueView.setTextIsSelectable(false);
        }
        else {
            holder.valueView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            holder.valueView.setTextIsSelectable(true);
        }
    }

    @Override
    public int getItemCount() {
        return passwordProperties.size();
    }

}
