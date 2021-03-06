package com.kin.ecosystem.marketplace.presenter;

import com.kin.ecosystem.base.IBasePresenter;
import com.kin.ecosystem.main.INavigator;
import com.kin.ecosystem.marketplace.view.IMarketplaceView;

public interface IMarketplacePresenter extends IBasePresenter<IMarketplaceView> {

    void onResume();

    void onPause();

    void getOffers();

    void onItemClicked(int position);

    void showOfferActivityFailed();

    void setNavigator(INavigator navigator);

	void closeClicked();

    void myKinCLicked();
}
