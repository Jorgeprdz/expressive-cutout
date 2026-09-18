package com.ekoehler.expressivecutout.statusbar;

interface IStatusBarTelephonyUserService {
    String dumpTelephonyRegistry() = 1;
    int[] readDisplayInfo() = 2;
    void destroy() = 16777114;
}
