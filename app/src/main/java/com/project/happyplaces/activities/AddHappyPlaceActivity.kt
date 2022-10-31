package com.project.happyplaces.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.project.happyplaces.R
import com.project.happyplaces.databases.DatabaseHandler
import com.project.happyplaces.models.HappyPlaceModel
import com.project.happyplaces.utils.GetAddressFromLatLng
import kotlinx.android.synthetic.main.activity_add_happy_place.*
import kotlinx.android.synthetic.main.activity_add_happy_place.iv_place_image
import kotlinx.android.synthetic.main.activity_happy_place_detail.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener{

    private  var cal = Calendar.getInstance()
    private lateinit var dateSetListener : DatePickerDialog.OnDateSetListener
    private var saveImageToInternalStorage: Uri? = null
    private var longitude: Double = 0.0
    private var latitude: Double = 0.0
    private var mHappyPlaceDetails: HappyPlaceModel? = null
    private lateinit var mFusedLocationClient : FusedLocationProviderClient

    companion object{
        private const val GALLERY_REQUEST_CODE = 1
        private const val CAMERA_REQUEST_CODE = 2
        private const val IMAGE_DIRECTORY = "HappyPlacesImages"
        private const val AUTO_COMPLETE_REQUEST_CODE = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_happy_place)

        setSupportActionBar(toolbar_add_place)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar_add_place.setNavigationOnClickListener {
            onBackPressed()
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!Places.isInitialized()){
            Places.initialize(this@AddHappyPlaceActivity,
                resources.getString(R.string.google_maps_api_key))
        }

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
           mHappyPlaceDetails = intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS)
                   as HappyPlaceModel?

           if (mHappyPlaceDetails != null){
               supportActionBar?.title = "Edit Happy Place"
               et_title.setText(mHappyPlaceDetails!!.title)
               et_description.setText(mHappyPlaceDetails!!.description)
               et_date.setText(mHappyPlaceDetails!!.date)
               et_location.setText(mHappyPlaceDetails!!.location)
               latitude = mHappyPlaceDetails!!.latitude
               longitude = mHappyPlaceDetails!!.longitude
               saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.imagePath)
               iv_place_image.setImageURI(saveImageToInternalStorage)
               btn_save.text = "UPDATE"
           }
       }

        et_date.setOnClickListener(this);
        tv_add_image.setOnClickListener(this);
        btn_save.setOnClickListener(this)
        et_location.setOnClickListener(this)
        dateSetListener = DatePickerDialog.OnDateSetListener { view, year, month, day->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            updateDateInView()
        }
        tv_select_current_location.setOnClickListener(this)
        updateDateInView()
    }

    private fun isLocationEnabled():Boolean{
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE)
                as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        @Suppress("DEPRECATION")
        var locationRequest = LocationRequest.create()?.apply {
            interval = 1000
            fastestInterval = 1
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val mLocationCallback = object: LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                val mLastLocation: Location = locationResult!!.lastLocation!!
                latitude = mLastLocation.latitude
                longitude = mLastLocation.longitude
//                Log.i("Location", latitude.toString() + " - " + longitude)
                val addressTask = GetAddressFromLatLng(this@AddHappyPlaceActivity,
                    latitude, longitude)
                addressTask.setAddressListener(object : GetAddressFromLatLng.AddressListener{
                    override fun onAddressFound(address: String?) {
                        et_location.setText(address)
                    }
                    override fun onError() {
                        Log.e("GET ADRRESS:: ", "Something went wrong")
                    }
                })
                addressTask.getAddress()
            }
        }

        mFusedLocationClient.requestLocationUpdates(locationRequest,
            mLocationCallback, Looper.myLooper())

    }

    override fun onClick(view: View?) {
        when(view!!.id){
            R.id.et_date ->{
                DatePickerDialog(
                    this@AddHappyPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            R.id.tv_add_image ->{
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf("Select photo from Gallery",
                    "Capture photo from camera")
                pictureDialog.setItems(pictureDialogItems){
                    _, which->
                    when(which){
                        0->choosePhotoFromGallery()
                        1->takePictureFromCamera()
                    }
                }
                pictureDialog.show()
            }
            R.id.et_location->{
                try {
                    val fieldList = listOf(
                        Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                        Place.Field.ADDRESS)

                    //Start autoComplete with intent + unique request code
                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN,
                        fieldList).build(this@AddHappyPlaceActivity)
                    startActivityForResult(intent, AUTO_COMPLETE_REQUEST_CODE)
                }
                catch (e: Exception){
                    e.printStackTrace()
                }
            }
            R.id.btn_save->{
                //Save data to db
                when{
                    et_title.text.isNullOrEmpty()->{
                        Toast.makeText(this, "Please enter title",
                            Toast.LENGTH_SHORT).show()
                    }
                    et_description.text.isNullOrEmpty()->{
                        Toast.makeText(this, "Please enter description",
                            Toast.LENGTH_SHORT).show()
                    }
                    et_location.text.isNullOrEmpty()->{
                        Toast.makeText(this, "Please enter location",
                            Toast.LENGTH_SHORT).show()
                    }
                    saveImageToInternalStorage==null->{
                        Toast.makeText(this, "Please select image",
                            Toast.LENGTH_SHORT).show()
                    }

                    else->{
                        val happyPlaceModel = HappyPlaceModel(
                            if (mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,
                            et_title.text.toString(),
                            saveImageToInternalStorage.toString(),
                            et_description.text.toString(),
                            et_date.text.toString(),
                            et_location.text.toString(),
                            latitude,
                            longitude)

                        val dbHandler = DatabaseHandler(this@AddHappyPlaceActivity)

                        if (mHappyPlaceDetails == null){
                            val addHappyPlace = dbHandler.insertHappyPlace(happyPlaceModel)
                            if (addHappyPlace > 0){
                                setResult(Activity.RESULT_OK)
                                Toast.makeText(this, "Data inserted successfully",
                                    Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                        else {
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)
                            if (updateHappyPlace > 0){
                                setResult(Activity.RESULT_OK)
                                Toast.makeText(this, "Data updated successfully",
                                    Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }

                    }

                }
            }
            R.id.tv_select_current_location->{
                if (!isLocationEnabled()){
                    Toast.makeText(this@AddHappyPlaceActivity,
                        "Your location provider is turned off, please switch it on",
                        Toast.LENGTH_SHORT).show()

                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
                else{
                    Dexter.withActivity(this).withPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ).withListener(
                        object : MultiplePermissionsListener{
                            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                if (report!!.areAllPermissionsGranted()){
                                    requestNewLocationData()
                                }
                            }
                            override fun onPermissionRationaleShouldBeShown(
                                permissions: MutableList<PermissionRequest>?,
                                token: PermissionToken?
                            ){
                                showRationalDialogForPermissions()
                            }
                        }
                    ).onSameThread().check()
                }
            }
        }
    }

    private fun choosePhotoFromGallery(){
        Dexter.withContext(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report : MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()) {
                        val galleryIntent = Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        @Suppress("DEPRECATION")
                        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
                    }
                }
                override fun onPermissionRationaleShouldBeShown(permissions:List<PermissionRequest>,
                                                                token: PermissionToken){
                   showRationalDialogForPermissions()
                }
            }).onSameThread().check()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == GALLERY_REQUEST_CODE){
               if (data != null){
                   val contentUri = data.data
                   try {
                       @Suppress("DEPRECATION")
                       val selectedImage: Bitmap =
                           MediaStore.Images.Media.getBitmap(this.contentResolver, contentUri)
                       iv_place_image.setImageBitmap(selectedImage)
                       saveImageToInternalStorage  = saveImageToDirectory(selectedImage)
                   }
                   catch (e: IOException){
                       e.printStackTrace()
                       Toast.makeText(this@AddHappyPlaceActivity,
                           "Failed to load image from gallery", Toast.LENGTH_SHORT).show()
                   }
               }
            }
            else if (requestCode == CAMERA_REQUEST_CODE){
                val thumbnail: Bitmap = data!!.extras!!.get("data") as Bitmap
                iv_place_image.setImageBitmap(thumbnail)
                saveImageToInternalStorage  = saveImageToDirectory(thumbnail)
            }
            else if (requestCode == AUTO_COMPLETE_REQUEST_CODE){
                val place:Place = Autocomplete.getPlaceFromIntent(data)
                et_location.setText(place.address)
                latitude = place.latLng.latitude
                longitude = place.latLng.longitude
            }
        }
    }

    private fun takePictureFromCamera(){
        Dexter.withContext(this@AddHappyPlaceActivity)
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(
                object: MultiplePermissionsListener{
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()){
                            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            @Suppress("Deprecation")
                            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
                        }
                    }
                    override fun onPermissionRationaleShouldBeShown
                                (permissions: List<PermissionRequest>, token: PermissionToken) {
                        showRationalDialogForPermissions()
                    }
                }
            ).onSameThread().check()
    }

    private fun updateDateInView(){
         val myFormat = "MM.dd.yyyy"
         val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
         et_date.setText(sdf.format(cal.time).toString())
    }

    private fun showRationalDialogForPermissions(){
        AlertDialog.Builder(this).setMessage("It looks like you have turned off permission " +
                "required for this feature. It can be enabled under Application Settings" +
                "")
            .setPositiveButton("GO TO SETTINGS"){
                    _,_ ->
                try{
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
                catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel"){
                dialog,_->dialog.dismiss()
            }.show()
    }

    private fun saveImageToDirectory(bitmap: Bitmap): Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file =  File(file, "${UUID.randomUUID()}.jpg")

        try{
            val stream : OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        }
        catch (e: IOException){
            e.printStackTrace()
        }

        return Uri.parse(file.absolutePath)
    }

}