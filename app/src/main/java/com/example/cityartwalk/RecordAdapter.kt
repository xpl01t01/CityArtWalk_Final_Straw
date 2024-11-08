package com.example.cityartwalk

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class RecordAdapter : RecyclerView.Adapter<RecordAdapter.RecordHolder>() {
    private var records: List<Record> = emptyList()
    private var listener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordHolder {
        //inflate the record_item layout to create a new item view
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.record_item, parent, false)
        return RecordHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecordHolder, position: Int) {
        //bind the data for the record at this position
        val currentRecord = records[position]
        holder.bind(currentRecord)
    }

    override fun getItemCount() = records.size //return the total number of records

    fun setRecords(records: List<Record>) {
        //update the list of records and notify the adapter
        this.records = records
        notifyDataSetChanged()
    }

    inner class RecordHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTitle: TextView = itemView.findViewById(R.id.text_view_title)
        val textViewDate: TextView = itemView.findViewById(R.id.text_view_date)
        val textViewAddress: TextView = itemView.findViewById(R.id.text_view_address)
        val thumbnailImageView: ImageView = itemView.findViewById(R.id.thumbnailImageView)

        init {
            //handle item clicks
            itemView.setOnClickListener {
                val position = adapterPosition
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(records[position])
                }
            }
        }

        fun bind(record: Record) {
            //bind record data to the UI components
            textViewTitle.text = record.title
            textViewDate.text = record.date
            textViewAddress.text = record.address

            //load the thumbnail image if available using Glide
            if (record.imagePath != null) {
                val file = itemView.context.getFileStreamPath(record.imagePath)
                Glide.with(itemView.context)
                    .load(file)
                    .placeholder(R.drawable.placeholder_image) //show placeholder while loading
                    .override(thumbnailImageView.width, thumbnailImageView.height) //resize image to fit the view
                    .centerCrop() //crop the image to fill the view
                    .into(thumbnailImageView)
            } else {
                //set placeholder if no image is available
                thumbnailImageView.setImageResource(R.drawable.placeholder_image)
            }
        }
    }

    interface OnItemClickListener {
        //define the interface to handle item clicks
        fun onItemClick(record: Record)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        //set the listener for item click events
        this.listener = listener
    }
}
