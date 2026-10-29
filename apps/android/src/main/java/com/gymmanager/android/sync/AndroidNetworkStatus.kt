package com.gymmanager.android.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.gymmanager.core.application.NetworkStatus

class AndroidNetworkStatus(
    context: Context,
) : NetworkStatus {
    private val connectivity =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun isOnline(): Boolean {
        val network = connectivity.activeNetwork ?: return false
        val caps = connectivity.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
