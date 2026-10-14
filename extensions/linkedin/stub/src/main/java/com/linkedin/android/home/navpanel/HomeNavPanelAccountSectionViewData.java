package com.linkedin.android.home.navpanel;

public final class HomeNavPanelAccountSectionViewData {
    public final AccountSectionType accountSectionType;
    public final CharSequence premiumText;

    public HomeNavPanelAccountSectionViewData(AccountSectionType accountSectionType, String premiumText) {
        this.accountSectionType = accountSectionType;
        this.premiumText = premiumText;
    }
}
