package com.enesky.guvenlikbildir.ui.activity.main

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.enesky.guvenlikbildir.App
import com.enesky.guvenlikbildir.R
import com.enesky.guvenlikbildir.database.EarthquakeDB
import com.enesky.guvenlikbildir.database.EarthquakeVM
import com.enesky.guvenlikbildir.database.dao.EarthquakeDao
import com.enesky.guvenlikbildir.database.entity.Earthquake
import com.enesky.guvenlikbildir.databinding.ActivityMainBinding
import com.enesky.guvenlikbildir.extensions.*
import com.enesky.guvenlikbildir.network.EarthquakeAPI
import com.enesky.guvenlikbildir.network.Result
import com.enesky.guvenlikbildir.network.Status
import com.enesky.guvenlikbildir.others.ConnectionLiveData
import com.enesky.guvenlikbildir.ui.activity.BaseActivity
import com.enesky.guvenlikbildir.ui.fragment.latestEarthquakes.LatestEarthquakesFragment
import com.enesky.guvenlikbildir.ui.fragment.notify.NotifyFragment
import com.enesky.guvenlikbildir.ui.fragment.options.OptionsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseUser
import com.trendyol.medusalib.navigator.MultipleStackNavigator
import com.trendyol.medusalib.navigator.Navigator
import com.trendyol.medusalib.navigator.NavigatorConfiguration
import com.trendyol.medusalib.navigator.transaction.NavigatorTransaction
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity(), Navigator.NavigatorListener,
    BottomNavigationView.OnNavigationItemSelectedListener,
    BottomNavigationView.OnNavigationItemReselectedListener {

    private lateinit var binding: ActivityMainBinding
    private val mainVM by lazy {
        getViewModel { MainVM() }
    }
    private var locationManager: LocationManager? = null
    private var locationListenerGPS: LocationListener? = null

    private var rootFragmentProvider: List<() -> Fragment> = listOf(
        { LatestEarthquakesFragment() },
        { NotifyFragment() },
        { OptionsFragment() }
    )
    var navigator: MultipleStackNavigator = MultipleStackNavigator(
        supportFragmentManager,
        R.id.container,
        rootFragmentProvider,
        this,
        NavigatorConfiguration(1,false, defaultNavigatorTransaction = NavigatorTransaction.SHOW_HIDE)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        navigator.initialize(savedInstanceState)
        binding.viewModel = mainVM
        mainVM.init(binding)

        val earthquakeDao = EarthquakeDB.getDatabaseManager(application.applicationContext).earthquakeDao()

        val rvViewModel by lazy {
            getViewModel { EarthquakeVM(App.mInstance, earthquakeDao) }
        }

        mainVM.responseHandler.addObserver { _, response ->
            GlobalScope.launch {
                withContext(Dispatchers.Main) {
                    if (response != null && response is Result<*>) {
                        when (response.status) {
                            Status.SUCCESS -> rvViewModel.initAllEarthquakes(response.data as List<Earthquake>)
                            Status.FAILURE, Status.EXCEPTION -> showToast(response.data.toString())
                        }
                    }
                }
            }
        }

        bottom_nav.setOnNavigationItemReselectedListener(this)
        bottom_nav.setOnNavigationItemSelectedListener(this)
    }

    override fun onStart() {
        super.onStart()
        val currentUser: FirebaseUser? = App.mAuth.currentUser
        if (currentUser == null)
            this.openLoginActivity()

        ConnectionLiveData(this)
            .observe(this, Observer { isOnline ->
                if (!isOnline)
                    showToast("İnternet bağlantısı bulunamadı.\nBazı fonksiyonlar pasif durumda olacaktır.")
            })

        if (isFirstTime) {
            requireAllPermissions()
            isFirstTime = false
        }

        requireLocationPermission { requestLocationUpdates() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        navigator.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (navigator.canGoBack())
            navigator.goBack()
        else
            super.onBackPressed()
    }

    override fun onTabChanged(tabIndex: Int) {
        when (tabIndex) {
            0 -> bottom_nav.selectedItemId = R.id.latest_earthquakes
            1 -> bottom_nav.selectedItemId = R.id.notify
            2 -> bottom_nav.selectedItemId = R.id.options
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.latest_earthquakes -> {
                navigator.switchTab(0)
                true
            }
            R.id.notify -> {
                navigator.switchTab(1)
                true
            }
            R.id.options -> {
                navigator.switchTab(2)
                true
            }
            else -> false
        }
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        //ignored
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates() {
        locationListenerGPS = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val latitude = location.latitude
                val longitude = location.longitude
                lastKnownLocation = "$latitude,$longitude"
                Log.d("LocationManager", lastKnownLocation!!)
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String?) {}
            override fun onProviderDisabled(provider: String?) {}
        }

        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager!!.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            10000,
            5f,
            locationListenerGPS!!
        )
    }

}
