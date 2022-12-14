package com.project.happyplaces.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.AsyncTask
import android.util.Log
import java.lang.Exception
import java.util.*

class GetAddressFromLatLng(
    context: Context, private val latitude: Double, private  val longitude: Double)
    : AsyncTask<Void, String, String>() {
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())
    private lateinit var mAddressListener: AddressListener

    override fun doInBackground(vararg params: Void?): String {
        try {
            Log.i("Background", latitude.toString() + " - " + longitude)
            val addressList : List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (addressList != null && addressList.isNotEmpty()){
                val address = addressList[0]
                val sb = StringBuilder()
                for (i in 0..address.maxAddressLineIndex){
                    Log.i("Background Line", address.getAddressLine(i))
                    sb.append(address.getAddressLine(i)).append(" ")
                }
                sb.deleteCharAt(sb.length -1)
                return sb.toString()
            }
        }
        catch(e: Exception){
            e.printStackTrace()
        }
        return ""
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)

        if (result == null){
            mAddressListener.onError()
        }
        else mAddressListener.onAddressFound(result)
    }

    fun setAddressListener(addressListener: AddressListener){
        mAddressListener = addressListener
    }

    fun getAddress(){
        execute()
    }

    interface AddressListener{
        fun onAddressFound(address: String?)
        fun onError()
    }
}