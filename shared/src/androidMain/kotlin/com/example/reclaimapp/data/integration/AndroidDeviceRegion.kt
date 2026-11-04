package com.example.reclaimapp.data.integration

import android.content.Context
import android.telephony.TelephonyManager
import com.example.reclaimapp.domain.integration.DeviceRegion
import java.util.Locale

/** The mobile network's country (where the phone is), else the SIM's, else the phone's region setting. No permission needed. */
class AndroidDeviceRegion(private val context: Context) : DeviceRegion {
    override fun countryCode(): String? {
        val phone = runCatching { context.getSystemService(TelephonyManager::class.java) }.getOrNull()
        return listOfNotNull(phone?.networkCountryIso, phone?.simCountryIso, Locale.getDefault().country)
            .firstOrNull { it.length == 2 }
            ?.uppercase(Locale.ROOT)
    }
}
