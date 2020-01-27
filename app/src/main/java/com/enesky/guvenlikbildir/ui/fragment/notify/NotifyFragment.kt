package com.enesky.guvenlikbildir.ui.fragment.notify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import com.enesky.guvenlikbildir.R
import com.enesky.guvenlikbildir.ui.fragment.BaseFragment
import com.enesky.guvenlikbildir.ui.login.activity.ui.home.NotifyFragmentVM
import com.enesky.guvenlikbildir.utils.getViewModel

class NotifyFragment : BaseFragment() {

    private lateinit var notifyFragmentVM: NotifyFragmentVM

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        notifyFragmentVM = getViewModel()
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
        notifyFragmentVM.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }
}