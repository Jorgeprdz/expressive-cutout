package com.ekoehler.expressivecutout.statusbar;

interface IStatusBarTelephonyUserService {
    String dumpTelephonyRegistry() = 1;
    void destroy() = 16777114;
}
