package com.enesky.guvenlikbildir.ui.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.enesky.guvenlikbildir.ui.activity.main.MainActivity
import com.enesky.guvenlikbildir.ui.dialog.InfoCountDownDialog
import com.trendyol.medusalib.navigator.MultipleStackNavigator

abstract class BaseFragment: Fragment() {

    var multipleStackNavigator: MultipleStackNavigator? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        initStackNavigator(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initStackNavigator(context)
    }

    private fun initStackNavigator(context: Context?) {
        if (context is MainActivity && multipleStackNavigator == null)
            multipleStackNavigator = context.navigator
    }

    fun openInfoCountDownDialog(tag: String) {
        InfoCountDownDialog().show(parentFragmentManager, tag)
    }

}