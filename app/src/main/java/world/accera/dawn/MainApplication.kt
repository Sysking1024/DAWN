package world.accera.dawn

import android.app.Application
import com.amap.api.services.core.ServiceSettings

class MainApplication : Application {
    override fun onCreate() {
        super.onCreate()

        ServiceSettings.updatePrivacyShow(this, true, true)
        ServiceSettings.updatePrivacyAgree(this, true)
    }
}