package world.accera.dawn

import android.app.Application
import com.amap.api.services.core.ServiceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import world.accera.dawn.mllms.ModelManager

class MainApplication : Application() {

    // 创建一个与 Application 生命周期绑定的 CoroutineScope
    // SupervisorJob 确保一个子协程的失败不会取消其他子协程或父协程
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        // 在 Application 创建时启动模型的异步初始化
        ModelManager.initializeAsync(this, applicationScope)

        ServiceSettings.updatePrivacyShow(this, true, true)
        ServiceSettings.updatePrivacyAgree(this, true)
    }

    override fun onTerminate() {
        super.onTerminate()

        ModelManager.release()
    }
}