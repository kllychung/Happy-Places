package com.project.happyplaces.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.project.happyplaces.R
import com.project.happyplaces.models.HappyPlaceModel
import kotlinx.android.synthetic.main.activity_map.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mHappyPlaceModel : HappyPlaceModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mHappyPlaceModel = intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS) as
                    HappyPlaceModel?
        }

        if(mHappyPlaceModel != null){
            setSupportActionBar(toolbar_map)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            if (supportActionBar != null){
                toolbar_map.setNavigationOnClickListener {
                    onBackPressed()
                }
            }
            supportActionBar!!.title = "Map"

            val supportMapFragment : SupportMapFragment = supportFragmentManager.findFragmentById(
                R.id.map) as SupportMapFragment

            supportMapFragment.getMapAsync(this)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        val position = LatLng(mHappyPlaceModel!!.latitude, mHappyPlaceModel!!.longitude)
        map.addMarker(MarkerOptions().position(position).title(mHappyPlaceModel!!.location))
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 15f)
        map.animateCamera(newLatLngZoom)
    }
}