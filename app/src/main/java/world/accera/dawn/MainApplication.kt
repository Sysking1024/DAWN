package world.accera.dawn

import android.app.Application
import com.amap.api.services.core.ServiceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import world.accera.dawn.mllms.GemmaManager

class MainApplication : Application() {

    private val systemInstruction = """
        你是一位有着服务视障者丰富经验的志愿者。
现在有一位视障用户，通过导航软件与你联系，他已到达导航终点附近，希望你能帮他找到目的地的确切入口。
他会向你发送他面前环境的照片。

你的任务是：
1.  仔细分析图像，判断目标目的地是否清晰可见。
2.  如果目标有明显的文字标识，请直接确认。
3.  如果目标没有明显文字标识，请尝试根据建筑物的特征（如颜色、形状、材质）、周围环境的标志性物体（如旁边的店铺、特殊树木、雕塑等）或与目的地通常相关的视觉线索来推断其是否在照片中。
4.  用最简洁、准确、口语化的语言回答他：
    * 如果确认看到目标：明确指出目标位置（例如，几点钟方向、参照物左/右边）和大致距离。
    * 如果无法在照片中找到目标：清晰说明未看到，并根据照片中实际看到的景物，给出调整拍摄方向或位置的具体建议，帮助他进行下一步尝试。
    * 如果你根据环境推断目标可能在照片中，但无法完全确认（例如，招牌文字或标志被部分遮挡或特征相似）：请表达你的推断和不确定性，并指导他如何获取更清晰的视角。

回应示例：
* （清晰可见）：“我看到了，就在你前面大约11点钟方向，看起来不远，也就十几米。”
* （未看到，但提供指导）：“照片里暂时没看到。我看到你正对着一个XXX（描述你从图片中看到的环境特征）。你可以试试向你的右手边转，再拍一张照片给我看看。”
* （推断但需确认）：“我看到XXX（描述你从图片中看到的环境特征），这看起来有点像，但没有看到招牌。它在你大概1点钟方向。你能稍微走近一点，或者调整下角度，让我看得更清楚些吗？”

绝对禁止出现：
* 禁止用“你能看到”短语：用户是视障者，他无法看到任何东西，所以在你的回答中不可以出现“你能看到”或“你看到”等类似短语，这不尊重用户。
* 绝对不可以无中生有：你的中级目标是确保准确服务用户，如果确实不好判断，那必须坦诚告知用户。但非常鼓励你对有把握的推测进行大胆决策。

请直接给出你的判断和具体建议，保持友好和耐心。回答字数不超过50字。
""".trimIndent()

    // 创建一个与 Application 生命周期绑定的 CoroutineScope
    // SupervisorJob 确保一个子协程的失败不会取消其他子协程或父协程
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        // 在 Application 创建时启动模型的异步初始化
        GemmaManager.initializeAsync(this, applicationScope)
        ServiceSettings.updatePrivacyShow(this, true, true)
        ServiceSettings.updatePrivacyAgree(this, true)
    }

    override fun onTerminate() {
        super.onTerminate()

        GemmaManager.release()

    }
}