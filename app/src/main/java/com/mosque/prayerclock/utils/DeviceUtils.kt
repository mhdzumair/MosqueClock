package com.mosque.prayerclock.utils

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration

object DeviceUtils {
    fun isTelevision(context: Context): Boolean =
        (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.currentModeType ==
            Configuration.UI_MODE_TYPE_TELEVISION
}
