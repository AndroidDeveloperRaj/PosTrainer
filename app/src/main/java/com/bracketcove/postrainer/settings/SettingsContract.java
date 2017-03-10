package com.bracketcove.postrainer.settings;

import com.bracketcove.postrainer.BasePresenter;
import com.bracketcove.postrainer.BaseView;

/**
 * Created by Ryan on 09/03/2017.
 */

public interface SettingsContract {
    interface View extends BaseView<Presenter> {

    }

    interface Presenter extends BasePresenter {
        void onContactButtonClick();
    }
}
