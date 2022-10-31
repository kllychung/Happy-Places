package com.project.happyplaces.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.happyplaces.R
import com.project.happyplaces.activities.AddHappyPlaceActivity
import com.project.happyplaces.activities.MainActivity
import com.project.happyplaces.databases.DatabaseHandler
import com.project.happyplaces.models.HappyPlaceModel
import kotlinx.android.synthetic.main.item_happy_place.view.*

open class HappyPlaceAdapter(
    private val context: Context,
    private var happyPlaceList: ArrayList<HappyPlaceModel>,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_happy_place, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = happyPlaceList[position]
        if (holder is MyViewHolder){
            holder.itemView.iv_place_image.setImageURI(Uri.parse(model.imagePath))
            holder.itemView.tvTitle.text = model.title
            holder.itemView.tvDescription.text = model.description
            holder.itemView.setOnClickListener{
                if (onClickListener!= null){
                    onClickListener!!.onClick(position, model)
                }
            }
        }
    }

    fun notifyEditItem(activity: Activity, position: Int, requestCode: Int){
        val intent = Intent(context, AddHappyPlaceActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS, happyPlaceList[position])
        activity.startActivityForResult(intent, requestCode)
        notifyItemChanged(position)
    }

    fun deleteItem(position: Int){
        val dbHandler = DatabaseHandler(context)
        val isDeleted = dbHandler.deleteHappyPlace(happyPlaceList[position])
        if (isDeleted > 0){
            happyPlaceList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun getItemCount(): Int {
        return happyPlaceList.size
    }

    fun setOnClickLister(onClickListener: OnClickListener){
        this.onClickListener = onClickListener
    }

    private class MyViewHolder(view: View): RecyclerView.ViewHolder(view)

    interface OnClickListener{
        fun onClick(position: Int, model: HappyPlaceModel)
    }
}