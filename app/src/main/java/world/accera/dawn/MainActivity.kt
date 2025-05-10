package world.accera.dawn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.viewModelFactory
import world.accera.dawn.ui.theme.DAWNTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DAWNTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PoiSearchScreen()
                }
            }
        }
    }
}

@Composable
fun PoiSearchScreen(
    poiViewModel: PoiSearchViewModel = viewModel()
) {
    var keywordInput by remember { mutableStateOf("肯德基") }
    var cityInput by remember { mutableStateOf("北京") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "高德POI搜索",
            style = MaterialTheme.typography.headlineMedium, // 使用 Material Design 的标题样式
            modifier = Modifier.padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = keywordInput,
            onValueChange = { keywordInput = it },
                    label = { Text("输入搜索关键字") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = cityInput,
            onValueChange = { cityInput = it },
            label = { Text("输入城市 (可留空)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                poiViewModel.searchPoi(keywordInput, cityInput)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("搜索POI") }

        // TODO: 在接下来的步骤中，我们将在这里添加加载指示器、错误信息和结果显示区域。
    }
}