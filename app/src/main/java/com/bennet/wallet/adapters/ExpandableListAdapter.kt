package com.bennet.wallet.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bennet.wallet.R
import com.google.android.material.textview.MaterialTextView

class ExpandingListAdapter<A: RecyclerView.Adapter<*>>(
    private val groups: List<GroupInfo>,
    private val childLayoutManagerFactory: () -> RecyclerView.LayoutManager,
    private val onBindChildViewHolder: (holder: ExpandingListAdapter<A>.ViewHolder, pos: Int) -> Unit
)
    : RecyclerView.Adapter<ExpandingListAdapter<A>.ViewHolder>()
{

    class GroupInfo(
        var name: String,
        var isExpanded: Boolean = true
    )

    inner class ViewHolder(itemView: View)
        : RecyclerView.ViewHolder(itemView)
    {
        private val expandButton: AppCompatImageView = itemView.findViewById(R.id.adapter_expandable_list_expand_icon)
        val groupHeader: MaterialTextView = itemView.findViewById(R.id.adapter_expandable_list_header)
        val contentsRecyclerView: RecyclerView = itemView.findViewById(R.id.adapter_expandable_list_recycler_view)

        init {
            contentsRecyclerView.layoutManager = childLayoutManagerFactory()

            expandButton.setOnClickListener {
                groups[adapterPosition].isExpanded = !groups[adapterPosition].isExpanded
                updateExpandedStatus(groups[adapterPosition].isExpanded)
            }
        }

        fun updateExpandedStatus(isExpanded: Boolean) {
            if (isExpanded) {
                expandButton.setImageDrawable(
                    AppCompatResources.getDrawable(expandButton.context, R.drawable.icon_expand_more_30dp)
                )
                contentsRecyclerView.visibility = View.VISIBLE
            }
            else {
                expandButton.setImageDrawable(
                    AppCompatResources.getDrawable(expandButton.context, R.drawable.icon_expand_less_30dp)
                )
                contentsRecyclerView.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.adapter_expandable_list, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        holder.groupHeader.text = groups[pos].name
        holder.updateExpandedStatus(groups[pos].isExpanded)
        onBindChildViewHolder(holder, pos)
    }

    override fun getItemCount() = groups.size

}