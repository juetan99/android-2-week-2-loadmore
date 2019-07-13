package com.ucsdextandroid2.petfinder

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.paging.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*

class PetsActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val adapter = PetsAdapter()

    private val LOCATION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pets)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerview)

        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerView.adapter = adapter

        //LivePagedListBuilder of the PetsDataSourceFactory

        checkforLocationPermission(true)


    }


    fun checkforLocationPermission(showRational: Boolean){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            getLocation()
        }
        else {
            if (!showRational){
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_REQUEST_CODE
                )
            }
            else {
                showPermissionRational()
            }

        }
    }

    private fun showPermissionRationalIfAble(): Boolean{
        val ableToShowRational: Boolean = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (ableToShowRational){
            showPermissionRational()
            return true
        }
        else
            return false
    }

    private fun showPermissionRational(){
        AlertDialog.Builder(this)
            .setTitle("Location")
            .setMessage("We need your location to show you pets in your area")
            .setPositiveButton("OK") { dialog, which ->
                if (which == DialogInterface.BUTTON_POSITIVE)
                    checkforLocationPermission(false)
            }.setNegativeButton("No Thanks"){dialog, which ->
                if(which == DialogInterface.BUTTON_NEGATIVE)
                    getLocationFailed()
            }.show()
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(){
        toast("Getting location")

        LocationServices
            .getFusedLocationProviderClient(this)
            .lastLocation
            .addOnSuccessListener { location: Location ->
                toast("Location found ${location?.latitude}, ${location?.longitude}")

                if (location != null) {
                    onLocationFound(location?.latitude, location?.longitude)
                }
                else {
                    toast("location was null")
                }
            }
            .addOnFailureListener{error ->
                toast(error.message ?:"Get Location failed")
            }

//        val client:FusedLocationProviderClient = LocationServices
//            .getFusedLocationProviderClient(this)
//
//        val locationCallback: LocationCallback = object : LocationCallback(){
//
//            override fun onLocationResult(locationResult: LocationResult?) {
//                super.onLocationResult(locationResult)
//
//                val location = locationResult?.lastLocation
//
//                toast("Location found ${location?.latitude}, ${location?.longitude}")
//            }
//        }
//
//        //client.requestLocationUpdates(LocationRequest().create(), locationCallback, null)
//
//        lifecycle.addObserver(object : DefaultLifecycleObserver {
//            override fun onResume(owner: LifecycleOwner) {
//                super.onResume(owner)
//                client.requestLocationUpdates(LocationRequest(), locationCallback, null)
//            }
//
//            override fun onPause(owner: LifecycleOwner) {
//                super.onPause(owner)
//                client.removeLocationUpdates(locationCallback)
//            }
//        })

    }

    private fun toast(toastMessage: String){
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
    }

    private fun getLocationFailed(){
        if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
            toast("Getting location failed. go to settings to enable this")
        else
            toast("Getting location failed")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_REQUEST_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getLocation()
            }
            else {
                getLocationFailed()
            }
        }
    }


    private fun onLocationFound(lat: Double, lng: Double){
        LivePagedListBuilder<Int, PetModel>(PetsDataSourceFactory(lat, lng), 10)
            .build()
            .observe(this, Observer {
                adapter.submitList(it)
            })
    }

    private class PetsAdapter: PagedListAdapter<PetModel, PetCardViewHolder>(diffCallback){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetCardViewHolder {
            return PetCardViewHolder.inflate(parent)
        }

        override fun onBindViewHolder(holder: PetCardViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
        companion object {
            val diffCallback = object : DiffUtil.ItemCallback<PetModel>(){
                override fun areItemsTheSame(oldItem: PetModel, newItem: PetModel): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(oldItem: PetModel, newItem: PetModel): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }

    private class PetCardViewHolder private constructor(view: View) : RecyclerView.ViewHolder(view) {

        val image: ImageView = itemView.findViewById(R.id.vnc_image)
        val titleView: TextView = itemView.findViewById(R.id.vnc_title)
        val textView: TextView = itemView.findViewById(R.id.vnc_text)

        companion object {
            fun inflate(parent: ViewGroup): PetCardViewHolder = PetCardViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_holder_note_card, parent, false)
            )
        }

        fun bind(pet: PetModel?) {
            image.isVisible = pet?.imageUrl != null
            image.loadImageUrl(pet?.imageUrl)
            titleView.text = pet?.name
            textView.text = pet?.breed
            textView.text = "${pet?.breed}\n ${pet?.location}"

        }

    }


}
