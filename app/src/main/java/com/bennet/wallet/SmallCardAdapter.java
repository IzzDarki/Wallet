package com.bennet.wallet;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class SmallCardAdapter extends RecyclerView.Adapter<SmallCardAdapter.ViewHolder> {
    static public final double cardWidthToHeightRatio = 85.6 / 53.98 ;

    protected List<HomeActivity.SmallCard> cards;
    protected double cardWidth;
    protected @ColorInt int expectedParentBackgroundColor;
    protected Context context;
    protected OnItemClickListener itemClickListener;


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        protected MaterialCardView cardView;
        protected MaterialTextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.small_card_item_card_view);
            textView = itemView.findViewById(R.id.small_card_item_text_view);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // forward click event to the callback set by parent
            if (itemClickListener != null)
                itemClickListener.onItemClick(view, getAdapterPosition());
        }
    }


    public interface OnItemClickListener {
        /**
         * Callback for Clicks on adapter items
         * @param view View that has been clicked
         * @param position Position of the View that has been clicked
         */
        void onItemClick(View view, int position);
    }

    public SmallCardAdapter(Context context, List<HomeActivity.SmallCard> cards, double expectedCardWidth) {
        this.context = context;
        this.cards = cards;
        this.cardWidth = expectedCardWidth;
        TypedValue val = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorBackground, val, true); // kind of works
        this.expectedParentBackgroundColor = val.data;
    }

    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    @Override @NonNull
    public SmallCardAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // inflates item
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.small_card_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SmallCardAdapter.ViewHolder holder, int position) {
        // binds the data to the item
        
        holder.cardView.setMinimumHeight((int) (cardWidth / cardWidthToHeightRatio));
        holder.cardView.setRadius((float) (cardWidth / ScrollAnimationImageView.widthToCornerRadiusRatio));
        holder.cardView.setCardBackgroundColor(cards.get(position).color);
        if (Utility.areColorsSimilar(expectedParentBackgroundColor, cards.get(position).color)) {
            // draw outline
            holder.cardView.setStrokeWidth(4);
            if (Utility.isColorDark(cards.get(position).color))
                holder.cardView.setStrokeColor(context.getResources().getColor(R.color.onDarkTextColor));
            else
                holder.cardView.setStrokeColor(context.getResources().getColor(R.color.onLightTextColor));
        }
        else {
            // remove outline
            holder.cardView.setStrokeWidth(0);
        }

        holder.textView.setText(cards.get(position).name);
        if (Utility.isColorDark(cards.get(position).color))
            holder.textView.setTextColor(context.getResources().getColor(R.color.onDarkTextColor));
        else
            holder.textView.setTextColor(context.getResources().getColor(R.color.onLightTextColor));
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }
}
