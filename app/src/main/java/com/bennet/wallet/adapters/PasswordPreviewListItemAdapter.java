package com.bennet.wallet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bennet.wallet.fragments.HomePasswordsFragment;
import com.bennet.wallet.R;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class PasswordPreviewListItemAdapter extends RecyclerView.Adapter<PasswordPreviewListItemAdapter.ViewHolder> {

    protected List<HomePasswordsFragment.PasswordPreview> passwords;
    protected OnItemClickListener itemClickListener;

    public interface OnItemClickListener {
        /**
         * Callback for clicks on adapter items
         * @param view View that has been clicked
         * @param position Position of the view that has been clicked
         */
        void onItemClick(View view, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        protected View divider;
        protected MaterialTextView nameView;
        //protected MaterialTextView valueView;

        public ViewHolder(View view) {
            super(view);
            divider = view.findViewById(R.id.password_preview_list_item_divider);
            nameView = view.findViewById(R.id.password_preview_list_item_name_view);
            // valueView = v.findViewById(R.id.password_preview_list_item_value_view);
            view.setOnClickListener(this::forwardClick);
        }

        protected void forwardClick(View view) {
            // forward click event to the callback set by parent
            if (itemClickListener != null)
                itemClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    public PasswordPreviewListItemAdapter(List<HomePasswordsFragment.PasswordPreview> passwordProperties) {
        this.passwords = passwordProperties;
    }

    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.adapter_password_preview_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PasswordPreviewListItemAdapter.ViewHolder holder, int position) {
        // Replace the contents of a view (invoked by the layout manager)

        if (position == 0)
            holder.divider.setVisibility(View.GONE);
        else
            holder.divider.setVisibility(View.VISIBLE);

        String name = passwords.get(position).passwordName;
        holder.nameView.setText(name);
    }

    @Override
    public int getItemCount() {
        return passwords.size();
    }

}
