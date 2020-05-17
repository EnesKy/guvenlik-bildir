package com.enesky.guvenlikbildir.ui.fragment.notify

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.enesky.guvenlikbildir.App
import com.enesky.guvenlikbildir.R
import com.enesky.guvenlikbildir.database.AppDatabase
import com.enesky.guvenlikbildir.database.entity.Contact
import com.enesky.guvenlikbildir.databinding.FragmentNotifyBinding
import com.enesky.guvenlikbildir.extensions.getViewModel
import com.enesky.guvenlikbildir.extensions.setTouchAnimation
import com.enesky.guvenlikbildir.extensions.showToast
import com.enesky.guvenlikbildir.others.Constants
import com.enesky.guvenlikbildir.ui.activity.main.MainVM
import com.enesky.guvenlikbildir.ui.fragment.BaseFragment
import kotlinx.android.synthetic.main.fragment_notify.*

class NotifyFragment : BaseFragment() {

    private lateinit var binding: FragmentNotifyBinding
    private val mainVM by lazy {
        getViewModel {
            MainVM(AppDatabase.getDatabaseManager(activity!!.application))
        }
    }

    private var selectedContactList : List<Contact> = listOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_notify, container, false)
        App.mAnalytics.setCurrentScreen(activity!!, "fragment", this.javaClass.simpleName)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainVM.init(binding)

        cl_polis.setTouchAnimation {
            openInfoCountDownDialog(Constants.polis)
        }

        cl_yardım.setTouchAnimation {
            openInfoCountDownDialog(Constants.acilYardım)
        }

        cl_iftaiye.setTouchAnimation {
            openInfoCountDownDialog(Constants.itfaiye)
        }

        iv_safe.setTouchAnimation {
            if (selectedContactList.isNullOrEmpty())
                showInfo()
            else
                openInfoCountDownDialog(Constants.safeSms)
        }

        iv_unsafe.setTouchAnimation {
            if (selectedContactList.isNullOrEmpty())
                showInfo()
            else
                openInfoCountDownDialog(Constants.unsafeSms)
        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mainVM.getSelectedContactList().observe(viewLifecycleOwner, Observer { it ->
            selectedContactList = it
        })

    }

    private fun showInfo() {
        requireContext().showToast("Kayıtlı kullanıcı bulunamadı.\n" +
                "Lütfen Seçenekler sekmesinden kullanıcı seçimi yapınız.")
    }

}