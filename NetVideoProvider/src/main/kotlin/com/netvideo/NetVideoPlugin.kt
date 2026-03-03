package com.netvideo

import com.lagradost.cloudstream3.CloudstreamPlugin
import com.lagradost.cloudstream3.DeviceType

class NetVideoPlugin: CloudstreamPlugin() {
    override fun load(context: android.content.Context) {
        // Registra el nuevo nombre de la clase aquí
        registerMainAPI(NetVideoProvider())
    }
}
