package com.kin.ecosystem.balance.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Animatable

import android.support.v7.app.AppCompatDelegate
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextSwitcher
import android.widget.TextView
import com.kin.ecosystem.R
import com.kin.ecosystem.balance.presenter.BalancePresenter
import com.kin.ecosystem.balance.presenter.IBalancePresenter
import com.kin.ecosystem.core.data.blockchain.BlockchainSourceImpl
import com.kin.ecosystem.core.data.order.OrderRepository
import com.kin.ecosystem.core.util.StringUtil.getAmountFormatted
import com.kin.ecosystem.obtainAttrs
import com.kin.ecosystem.widget.util.FontUtil


class BalanceView @JvmOverloads constructor(context: Context,
                                            attrs: AttributeSet? = null,
                                            defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr), IBalanceView {
  
    private var kinAVD: Animatable
    private var balanceText: TextSwitcher
    private var presenter: IBalancePresenter? = null


    init {
        LayoutInflater.from(context).inflate(R.layout.kinecosystem_balance_view, this, true)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        val attributes = obtainAttrs(attrs, R.styleable.KinEcosystemBalanceView)
        var textStyle = R.style.KinecosysBalanceTextSmall
        try {
            attributes?.let {
                 textStyle = it.getResourceId(R.styleable.KinEcosystemBalanceView_textStyle, R.style.KinecosysBalanceTextSmall)
            }
        } finally {
            attributes?.recycle()
        }
        kinAVD = getKinLogoAVD()
        balanceText = findViewById<TextSwitcher>(R.id.balance_text).apply {
            setFactory {
                val balanceText = TextView(context)
                balanceText.setTextAppearance(context, textStyle)
                balanceText.typeface = FontUtil.SAILEC_MEDIUM
                balanceText
            }
        }
        presenter = BalancePresenter(BlockchainSourceImpl.getInstance(), OrderRepository.getInstance())
    }

    @SuppressLint("NewApi")
    private fun getKinLogoAVD(): Animatable = findViewById<ImageView>(R.id.avd_kin_logo).drawable as Animatable


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        presenter?.onAttach(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        presenter?.onDetach()
    }

    override fun updateBalance(balance: Int) {
        val balanceString = if (balance == 0) {
            BALANCE_ZERO_TEXT
        } else {
            getAmountFormatted(balance)
        }
        post { balanceText.setText(balanceString) }
    }

    override fun startLoadingAnimation() {
        kinAVD.start()
    }

    override fun stopLoadingAnimation() {
        kinAVD.stop()
    }

    companion object {
        private const val BALANCE_ZERO_TEXT = "0"
    }
}