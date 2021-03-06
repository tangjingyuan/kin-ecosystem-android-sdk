package com.kin.ecosystem.settings.view

import android.content.Intent
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.kin.ecosystem.R
import com.kin.ecosystem.core.accountmanager.AccountManagerImpl
import com.kin.ecosystem.core.bi.EventLoggerImpl
import com.kin.ecosystem.core.data.blockchain.BlockchainSourceImpl
import com.kin.ecosystem.core.data.settings.SettingsDataSourceImpl
import com.kin.ecosystem.core.data.settings.SettingsDataSourceLocal
import com.kin.ecosystem.main.INavigator
import com.kin.ecosystem.settings.BackupManagerImpl
import com.kin.ecosystem.settings.presenter.ISettingsPresenter
import com.kin.ecosystem.settings.presenter.SettingsPresenter

class SettingsFragment : Fragment(), ISettingsView, OnClickListener {

    private var navigator: INavigator? = null
    private var settingsPresenter: ISettingsPresenter? = null

    private lateinit var backupItem: SettingsItem
    private lateinit var restoreItem: SettingsItem

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.kinecosystem_activity_settings, container, false)
        initViews(root)
        val settingsDataSource = SettingsDataSourceImpl(
                SettingsDataSourceLocal(context))
        settingsPresenter = SettingsPresenter(settingsDataSource, BlockchainSourceImpl.getInstance(),
                BackupManagerImpl(activity, AccountManagerImpl.getInstance(), EventLoggerImpl.getInstance(),
                        BlockchainSourceImpl.getInstance(), settingsDataSource), navigator ,EventLoggerImpl.getInstance())
        settingsPresenter?.onAttach(this@SettingsFragment)
        return root
    }

    fun initViews(root: View) {
        root.findViewById<ImageView>(R.id.back_btn).apply {
            setOnClickListener(this@SettingsFragment)
        }
        backupItem = root.findViewById<SettingsItem>(R.id.keep_your_kin_safe).apply {
            setOnClickListener(this@SettingsFragment)
        }
        restoreItem = root.findViewById<SettingsItem>(R.id.restore_prev_wallet).apply {
            setOnClickListener(this@SettingsFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        settingsPresenter?.onResume()
    }

    override fun onPause() {
        super.onPause()
        settingsPresenter?.onPause()
    }

    override fun onDestroyView() {
        settingsPresenter?.onDetach()
        navigator = null
        super.onDestroyView()
    }

    override fun onClick(v: View) {
        val vId = v.id
        when(vId) {
            R.id.back_btn -> settingsPresenter?.backClicked()
            R.id.keep_your_kin_safe -> settingsPresenter?.backupClicked()
            R.id.restore_prev_wallet -> settingsPresenter?.restoreClicked()
        }
    }

    override fun setIconColor(item: ISettingsView.Item , color: ISettingsView.IconColor) {
        getSettingsItem(item)?.let {
            @ColorRes val colorRes = getColorRes(color)
            if (colorRes != -1) {
                it.changeIconColor(colorRes)
            }
        }
    }

    override fun changeTouchIndicatorVisibility(item: ISettingsView.Item , isVisible: Boolean) {
        val settingsItem = getSettingsItem(item)
        settingsItem?.setTouchIndicatorVisibility(isVisible)
    }

    override fun showCouldNotImportAccount() {
        Toast.makeText(context, R.string.kinecosystem_could_not_restore_the_wallet, Toast.LENGTH_SHORT).show()
    }

    private fun getSettingsItem(item: ISettingsView.Item): SettingsItem? {
        return when (item) {
            ISettingsView.Item.ITEM_BACKUP -> backupItem
            ISettingsView.Item.ITEM_RESTORE -> restoreItem
        }
    }

    private fun getColorRes(color: ISettingsView.IconColor): Int {
        return when (color) {
            ISettingsView.IconColor.PRIMARY -> R.color.kinecosystem_primary
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        settingsPresenter?.onActivityResult(requestCode, resultCode, data)
    }

    companion object {

        fun newInstance(navigator: INavigator): SettingsFragment {
            val settingsFragment = SettingsFragment()
            settingsFragment.navigator = navigator
            return settingsFragment
        }
    }
}
