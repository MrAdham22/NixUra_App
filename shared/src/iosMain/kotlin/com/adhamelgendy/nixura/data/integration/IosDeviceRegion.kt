package com.adhamelgendy.nixura.data.integration

import com.adhamelgendy.nixura.domain.integration.DeviceRegion
import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

/** The phone's region setting (iOS gives apps no network country without extra frameworks). */
class IosDeviceRegion : DeviceRegion {
    override fun countryCode(): String? = NSLocale.currentLocale.countryCode?.uppercase()
}
