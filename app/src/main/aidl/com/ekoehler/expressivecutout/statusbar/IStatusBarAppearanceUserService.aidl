package com.ekoehler.expressivecutout.statusbar;

import com.ekoehler.expressivecutout.statusbar.IStatusBarAppearanceCallback;

interface IStatusBarAppearanceUserService {
    String dumpWindowPolicy() = 1;
    String dumpWindow() = 2;
    void startMonitor(IStatusBarAppearanceCallback callback) = 3;
    void stopMonitor() = 4;
    void destroy() = 16777114;
}
