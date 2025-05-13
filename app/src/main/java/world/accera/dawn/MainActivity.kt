package world.accera.dawn

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import world.accera.dawn.ui.theme.DAWNTheme

class MainActivity : ComponentActivity() {

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var allGranted = true
            permissions.entries.forEach {
                if (!it.value) {
                    allGranted = false
                    Log.w("Permissions", "Permission denied: ${it.key}")
                }
            }

            if (allGranted) {
                Log.d("Permissions", "All requested permissions granted.")
                // 可以在这里触发一次定位，如果ViewModel中因为权限不足而未能启动
                // viewModel.startLocation() // 如果你有一个viewModel实例在这里
            } else {
                Log.e("Permissions", "Some permissions were denied.")
                // 用户拒绝了部分或全部权限，提示用户或引导用户去设置开启
                // Toast.makeText(this, "需要定位权限才能使用此功能", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DAWNTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    //PoiSearchScreen()
                    LocationScreen { permissionsArray ->
                        requestMultiplePermissionsLauncher.launch(permissionsArray)
                    }
                }
            }
        }
    }
}

