package com.kin.ecosystem.history.presenter

import com.kin.ecosystem.base.IBasePresenter
import com.kin.ecosystem.history.view.IOrderHistoryView
import com.kin.ecosystem.widget.KinEcosystemTabs

interface IOrderHistoryPresenter : IBasePresenter<IOrderHistoryView> {

    fun onEnterTransitionEnded()

    fun onResume()

    fun onPause()

    fun onBackButtonClicked()

    fun onSettingsButtonClicked()

    fun onTabSelected(tab: KinEcosystemTabs.Tab)

}
