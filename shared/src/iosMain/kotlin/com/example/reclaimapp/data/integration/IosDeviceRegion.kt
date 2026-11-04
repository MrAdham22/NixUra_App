package com.example.reclaimapp.data.integration

import com.example.reclaimapp.domain.integration.DeviceRegion
import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

/** The phone's region setting (iOS gives apps no network country without extra frameworks). */
class IosDeviceRegion : DeviceRegion {
    override fun countryCode(): String? = NSLocale.currentLocale.countryCode?.uppercase()
}
