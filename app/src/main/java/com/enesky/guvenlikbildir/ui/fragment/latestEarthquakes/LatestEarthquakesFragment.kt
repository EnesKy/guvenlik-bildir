package com.enesky.guvenlikbildir.ui.fragment.latestEarthquakes

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.enesky.guvenlikbildir.App
import com.enesky.guvenlikbildir.BuildConfig
import com.enesky.guvenlikbildir.R
import com.enesky.guvenlikbildir.databinding.FragmentLastestEarthquakesBinding
import com.enesky.guvenlikbildir.extensions.*
import com.enesky.guvenlikbildir.model.EarthquakeOA
import com.enesky.guvenlikbildir.model.GenericResponse
import com.enesky.guvenlikbildir.network.Result
import com.enesky.guvenlikbildir.network.Status
import com.enesky.guvenlikbildir.ui.fragment.BaseFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.fragment_lastest_earthquakes.*
import kotlinx.coroutines.*
import okhttp3.Response
import kotlin.math.abs

@Suppress("UNCHECKED_CAST")
class LatestEarthquakesFragment: BaseFragment(), AppBarLayout.OnOffsetChangedListener,
    ViewTreeObserver.OnGlobalLayoutListener, SearchView.OnQueryTextListener {

    private lateinit var binding: FragmentLastestEarthquakesBinding
    private lateinit var latestEarthquakesVM: LatestEarthquakesVM
    private var isAppBarExpanded: Boolean = false
    private var listExpand: Int = 5

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_lastest_earthquakes, container,false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        latestEarthquakesVM = getViewModel()
        binding.apply {
            viewModel = latestEarthquakesVM
            lifecycleOwner = this@LatestEarthquakesFragment
        }
        latestEarthquakesVM.init(binding)
        app_bar_layout.addOnOffsetChangedListener(this)

        ConnectionLiveData(requireContext()).observe(this, Observer { isOnline ->
            if (isOnline)
                fab_synchronize.setImageResource(R.drawable.ic_sync)
            else
                fab_synchronize.setImageResource(R.drawable.ic_sync_problem)
        })

        latestEarthquakesVM.responseHandler.addObserver{ _, response ->
            GlobalScope.launch {
                withContext(Dispatchers.Main) {
                    if (response != null && response is Result<*>) {
                        when (response.status) {
                            Status.SUCCESS -> ""
                            Status.FAILURE -> requireContext().showToast(response.data.toString())
                            Status.EXCEPTION -> requireContext().showToast(response.data.toString())
                        }
                    }
                }
            }
        }

        latestEarthquakesVM.whereTo.observe(viewLifecycleOwner, Observer {
            if (it is String)
                openInfoCountDownDialog(Constants.map + it)
        })

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fab_synchronize.setOnClickListener {
            refresh()
        }

        iv_filter.setOnClickListener {
            if (!isAppBarExpanded) {
                if (app_bar_layout.isVisible)
                    app_bar_layout.setExpanded(true, true)
                else {
                    GlobalScope.launch(Dispatchers.Main) {
                        app_bar_layout.makeItVisible()
                        delay(50)
                        app_bar_layout.setExpanded(true, true)
                    }
                }
            } else {
                app_bar_layout.setExpanded(false, true)
                GlobalScope.launch(Dispatchers.Main) {
                    delay(500)
                    app_bar_layout.makeItGone()
                }
            }
        }

        sv_earthquake.viewTreeObserver.addOnGlobalLayoutListener(this)
        sv_earthquake.setOnQueryTextListener(this)

        /**init yenilemesi*/
        refresh()
    }

    fun refresh() {
        val objectAnimator = ObjectAnimator
            .ofFloat(fab_synchronize, "rotation", 360f, 0f)

        objectAnimator.start()
        pb_loading.makeItVisible()
        GlobalScope.launch(Dispatchers.Main) {
            delay(100)

            if (BuildConfig.DEBUG) {
                latestEarthquakesVM.earthquakeAdapter.value!!.update(App.managerInstance.mockEarthquakeList.result.subList(0,listExpand) as MutableList<EarthquakeOA>)
                listExpand += 5
            } else {
                latestEarthquakesVM.getLastEarthquakes("7")
            }

            delay(2000)
            objectAnimator.cancel()
            pb_loading.makeItGone()
        }
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        if (verticalOffset == 0)
            isAppBarExpanded = true
        else if (abs(verticalOffset) >= appBarLayout!!.totalScrollRange)
            isAppBarExpanded = false
    }

    override fun onGlobalLayout() {
        sv_earthquake.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        latestEarthquakesVM.earthquakeAdapter.value!!.filter.filter(newText)
        return true
    }

}