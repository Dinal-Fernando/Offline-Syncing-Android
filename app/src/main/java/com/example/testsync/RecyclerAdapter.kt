package com.example.testsync

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.testsync.roomDb.entity.Contact

import java.util.ArrayList

class RecyclerAdapter(arrayList: ArrayList<Contact>) : RecyclerView.Adapter<RecyclerAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<Contact>()

    init {
        this.arrayList = arrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view, parent, false)

        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        holder.Name.text = arrayList[position].name
        val sync_status = arrayList[position].syncstatus
        if (sync_status == DbContract.SYNC_STATUS_OK) {
            holder.Sync_Status.setImageResource(R.drawable.ok)
        } else {
            holder.Sync_Status.setImageResource(R.drawable.sync)
        }
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var Sync_Status: ImageView
        internal var Name: TextView

        init {
            Sync_Status = itemView.findViewById<View>(R.id.imgSync) as ImageView
            Name = itemView.findViewById<View>(R.id.txtName) as TextView
        }
    }

}
