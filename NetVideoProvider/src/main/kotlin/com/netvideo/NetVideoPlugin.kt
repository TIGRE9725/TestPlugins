package com.netvideo

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class NetVideoPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(NetVideoProvider())
    }
}
