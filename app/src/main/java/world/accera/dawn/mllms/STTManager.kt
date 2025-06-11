package world.accera.dawn.mllms

import android.app.Application
import android.content.Context
import com.bytedance.speech.speechengine.SpeechEngine
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.bytedance.speech.speechengine.SpeechEngineGenerator

object STTManager {

    private var engine: SpeechEngine? = null

    fun initialize(applicationContext: Context, application: Application) {
SpeechEngineGenerator.PrepareEnvironment(applicationContext, application)
        engine = SpeechEngineGenerator.getInstance()
        val engineHandler = engine?.createEngine()
        engine?.setOptionString(
            engineHandler!!,
            SpeechEngineDefines.PARAMS_KEY_ENGINE_NAME_STRING,
            SpeechEngineDefines.ASR_ENGINE
        )
        engine?.setOptionString(engineHandler!!, SpeechEngineDefines.PARAMS_KEY_UID_STRING, "XXXXX")
        engine?.setOptionString(engineHandler!!, SpeechEngineDefines.PARAMS_KEY_APP_ID_STRING, "{2831793530}")
        engine?.setOptionString(engineHandler!!, SpeechEngineDefines.PARAMS_KEY_APP_TOKEN_STRING, "Bearer;{TOKEN}")
    }
}