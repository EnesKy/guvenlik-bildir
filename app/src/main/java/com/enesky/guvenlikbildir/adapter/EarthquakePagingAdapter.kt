package com.enesky.guvenlikbildir.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import androidx.core.animation.doOnStart
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.enesky.guvenlikbildir.R
import com.enesky.guvenlikbildir.database.entity.Earthquake
import com.enesky.guvenlikbildir.databinding.ItemEarthquakeBinding
import com.enesky.guvenlikbildir.extensions.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.samlss.broccoli.Broccoli

/**
 * Created by Enes Kamil YILMAZ on 25.04.2020
 */

class EarthquakePagingAdapter(context: Context,
                              val earthquakeItemListener: EarthquakeItemListener
) : PagedListAdapter<Earthquake, EarthquakePagingAdapter.EarthquakeViewHolder>(DIFF_CALLBACK) {

    private lateinit var recyclerView: RecyclerView
    private var expandedItemPos: Int? = null
    private var expandedItemId: Int? = null

    private val originalWidth = context.screenWidth - 24.dp
    private val expandedWidth = context.screenWidth - 8.dp
    private var originalHeight = -1
    private var expandedHeight = -1
    private val listItemExpandDuration = 300L
    private val listItemPadding = context.resources.getDimension(R.dimen.default_margin_16)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EarthquakeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemEarthquakeBinding.inflate(inflater, parent, false)
        return EarthquakeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EarthquakeViewHolder, position: Int) = holder.bind(getItem(position))

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    inner class EarthquakeViewHolder(val binding: ItemEarthquakeBinding) :
        RecyclerView.ViewHolder(binding.root), OnMapReadyCallback {
        var mGoogleMap: GoogleMap? = null
        var mEarthquake: Earthquake? = null
        val cvMap = binding.cvMap
        val map = binding.map
        val ivIndicator = binding.ivIndicator
        val cardContainer = binding.cardContainer
        val scaleContainer = binding.scaleContainer
        private val broccoli = Broccoli()

        init {
            map?.onCreate(null)
        }

        override fun onMapReady(googleMap: GoogleMap?) {
            MapsInitializer.initialize(binding.root.context)
            mGoogleMap = googleMap
            setupMap(googleMap, mEarthquake!!, binding.pbLoading)
        }

        fun bind(earthquake: Earthquake?) {

            when (earthquake) {
                null -> {
                    broccoli.addPlaceholders(
                        binding.tvLocation,
                        binding.tvDepth,
                        binding.tvDate,
                        binding.tvShortDate,
                        binding.map)
                    broccoli.show()
                }
                else -> {
                    broccoli.removeAllPlaceholders()

                    mEarthquake = earthquake
                    binding.earthquake = earthquake

                    setMagBackgroundTint(binding.tvMag, earthquake.magML)

                    setCollapsingProgress(this, adapterPosition)
                    toggleItem(this, false, animate = false)

                    if (earthquake.id == expandedItemId) {
                        map.getMapAsync(this)
                        toggleItem(this, true, animate = false)
                    }

                    binding.rootLayout.setOnClickListener {
                        when (expandedItemPos) {
                            null -> {
                                expandedItemPos = adapterPosition
                                // expand clicked item
                                toggleItem(this, expand = true, animate = true)
                                map.getMapAsync(this)
                            }
                            adapterPosition -> {
                                if (earthquake.id == expandedItemId) {
                                    //collapse clicked item
                                    collapseLastExpandedItem(this)
                                    expandedItemPos = null
                                } else {
                                    // expand clicked item
                                    toggleItem(this, expand = true, animate = true)
                                    map.getMapAsync(this)
                                }
                            }
                            else -> {
                                collapseLastExpandedItem()
                                expandedItemPos = adapterPosition
                                // expand clicked item
                                toggleItem(this, expand = true, animate = true)
                                map.getMapAsync(this)
                            }
                        }
                        expandedItemId = earthquake.id
                        //earthquakeItemListener.onItemClick(earthquake)
                    }

                    binding.ivOptions.setOnClickListener {
                        earthquakeItemListener.onOptionsClick(earthquake)
                    }

                }
            }

            binding.executePendingBindings()
        }

    }

    override fun onCurrentListChanged(
        previousList: PagedList<Earthquake>?,
        currentList: PagedList<Earthquake>?
    ) {
        super.onCurrentListChanged(previousList, currentList)

    }

    override fun onViewAttachedToWindow(holder: EarthquakeViewHolder) {
        super.onViewAttachedToWindow(holder)
        // get originalHeight & expandedHeight if not gotten before
        if (expandedHeight < 0) {
            expandedHeight = 0 // so that this block is only called once

            holder.cardContainer.doOnLayout { view ->
                originalHeight = view.height
                holder.cvMap.makeItVisible()
                view.doOnPreDraw {
                    expandedHeight = view.height
                    holder.cvMap.makeItGone()
                }
            }
        }
    }

    /*
    override fun onViewRecycled(holder: EarthquakeViewHolder) {
        super.onViewRecycled(holder)

        if (holder.googleMap != null && !isItemVisible()) {
            // Clear the map and free up resources
            holder.googleMap!!.clear()
            holder.googleMap!!.mapType = GoogleMap.MAP_TYPE_NONE
        }
    }
    */

    override fun getItemId(position: Int): Long = position.toLong()

    private fun getVisibleItemsRange(): IntRange {
        val firstItemPos = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val lastItemPos = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        return firstItemPos..lastItemPos
    }

    private fun isItemVisible(): Boolean {
        return getVisibleItemsRange().contains(expandedItemPos)
    }

    fun setupMap(map: GoogleMap?, earthquake: Earthquake, progressBar: ProgressBar) {
        if (map == null) return
        val loc = LatLng(earthquake.lat.toDouble(), earthquake.lng.toDouble())
        map.setOnMapClickListener {
            earthquakeItemListener.onMapClick(loc, earthquake.location)
        }
        map.setOnMapLoadedCallback {
            map.addMarker(MarkerOptions().position(loc))
            progressBar.makeItGone()
        }
        map.moveCamera(CameraUpdateFactory.newLatLng(loc))
        map.uiSettings.isMapToolbarEnabled = false
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        //TODO: map.addCircle() ile dairesel bi animasyon çizebilirsin???

    }

    fun toggleItem(holder: EarthquakeViewHolder, expand: Boolean, animate: Boolean) {
        if (animate) {
            val animator = getValueAnimator(
                expand, listItemExpandDuration, AccelerateDecelerateInterpolator()
            ) { progress -> setExpandingProgress(holder, progress) }

            if (expand)
                animator.doOnStart {
                    GlobalScope.launch(Dispatchers.Main) {
                        delay(250)
                        holder.cvMap.makeItVisible()
                    }
                }
            else
                animator.doOnStart {
                    GlobalScope.launch(Dispatchers.Main) {
                        delay(100)
                        holder.cvMap.makeItGone()
                    }
                }

            animator.start()
        } else {
            // show expandView only if we have expandedHeight (onViewAttached)
            holder.cvMap.isVisible = expand && expandedHeight >= 0
            setExpandingProgress(holder, if (expand) 1f else 0f)
        }
    }

    fun collapseLastExpandedItem(viewHolder: EarthquakeViewHolder? = null) {
        if (expandedItemPos != null) {
            val oldViewHolder: EarthquakeViewHolder? =
                viewHolder ?: recyclerView.findViewHolderForAdapterPosition(expandedItemPos!!) as? EarthquakeViewHolder
            if (oldViewHolder != null) {
                toggleItem(oldViewHolder, expand = false, animate = true)
                oldViewHolder.mGoogleMap?.clear()
                oldViewHolder.mGoogleMap?.mapType = GoogleMap.MAP_TYPE_NONE
            }
        }
    }

    private fun setExpandingProgress(holder: EarthquakeViewHolder, progress: Float) {
        if (expandedHeight > 0 && originalHeight > 0)
            holder.cardContainer.layoutParams.height = (originalHeight + (expandedHeight - originalHeight) * progress).toInt()

        holder.cardContainer.layoutParams.width = (originalWidth + (expandedWidth - originalWidth) * progress).toInt()
        holder.cardContainer.requestLayout()
        holder.ivIndicator.rotation = 180 * progress
    }

    private fun setCollapsingProgress(holder: EarthquakeViewHolder, position: Int, progress: Float = 0f) {
        val itemExpanded = position >= 0 && position == expandedItemPos
        holder.cardContainer.layoutParams.apply {
            width = ((if (itemExpanded) expandedWidth else originalWidth) * (1 - 0.1f * progress)).toInt()
            height = ((if (itemExpanded) expandedHeight else originalHeight) * (1 - 0.1f * progress)).toInt()
        }
        holder.cardContainer.requestLayout()

        holder.scaleContainer.scaleX = 1 - 0.05f * progress
        holder.scaleContainer.scaleY = 1 - 0.05f * progress

        holder.scaleContainer.setPadding(
            (listItemPadding * (1 - 0.2f * progress)).toInt(),
            (listItemPadding * (1 - 0.2f * progress)).toInt(),
            (listItemPadding * (1 - 0.2f * progress)).toInt(),
            (0 * (1 - 0.2f * progress)).toInt()
        )
    }

    fun setMagBackgroundTint(view: View, magnitude: Double) {
        val color = when {
            magnitude < 3 -> R.color.greeny
            (magnitude >= 3) && (magnitude < 4.5) -> R.color.colorSecondary
            else -> R.color.red
        }
        view.setBackgroundTint(color)
    }

    companion object {
        private val DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<Earthquake>() {
            override fun areItemsTheSame(oldEarthquake: Earthquake,
                                         newEarthquake: Earthquake) =
                oldEarthquake.dateTime == newEarthquake.dateTime &&
                        oldEarthquake.id == newEarthquake.id

            override fun areContentsTheSame(oldEarthquake: Earthquake,
                                            newEarthquake: Earthquake) =
                oldEarthquake.dateTime == newEarthquake.dateTime &&
                        oldEarthquake.id == newEarthquake.id
        }
    }

    interface EarthquakeItemListener {
        fun onItemClick(earthquake: Earthquake)
        fun onOptionsClick(earthquake: Earthquake)
        fun onMapClick(latlng: LatLng, header: String)
    }

}