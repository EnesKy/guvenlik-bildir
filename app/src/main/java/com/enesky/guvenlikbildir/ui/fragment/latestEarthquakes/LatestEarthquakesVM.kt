package com.enesky.guvenlikbildir.ui.fragment.latestEarthquakes

import androidx.lifecycle.MutableLiveData
import com.enesky.guvenlikbildir.adapter.EarthquakePagingAdapter
import com.enesky.guvenlikbildir.database.entity.Earthquake
import com.enesky.guvenlikbildir.databinding.FragmentLatestEarthquakesBinding
import com.enesky.guvenlikbildir.viewModel.BaseViewModel
import com.google.android.gms.maps.model.LatLng
import com.hadilq.liveevent.LiveEvent

class LatestEarthquakesVM : BaseViewModel(), EarthquakePagingAdapter.EarthquakeItemListener{

    val filterIndex = MutableLiveData<Int>().apply {
        value = 0
    }

    val whereTo = LiveEvent<Any>()
    val onClick = LiveEvent<Any>()
    val onOptionClick = LiveEvent<Any>()
    val onFilterIndexChange = LiveEvent<Int>()

    fun init(binding: FragmentLatestEarthquakesBinding) {
        setViewDataBinding(binding)
    }

    override fun onItemClick(earthquake: Earthquake) {
        onClick.value = earthquake
    }

    override fun onOptionsClick(earthquake: Earthquake) {
        onOptionClick.value = earthquake
    }

    override fun onMapClick(latlng: LatLng, header: String) {
        whereTo.value = "${latlng.latitude},${latlng.longitude}map$header"
    }

    fun onFilterIndexChange(newIndex: Int) {
        onFilterIndexChange.value = newIndex
        filterIndex.value = newIndex
    }

}