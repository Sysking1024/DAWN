package world.accera.dawn

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.api.services.core.PoiItem

@Composable
fun PoiSearchScreen(
    poiViewModel: PoiSearchViewModel = viewModel()
) {
    var keywordInput by remember { mutableStateOf("肯德基") }
    var cityInput by remember { mutableStateOf("北京") }

    val isLoading by poiViewModel.isLoadingState
    val errorMessage by poiViewModel.errorMessageState
    val poiList = poiViewModel.poiListState
    val suggestionCities = poiViewModel.suggestionCitiesState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        //horizontalAlignment = Alignment.CenterHorizontally,
        //verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "高德POI搜索",
            style = MaterialTheme.typography.headlineMedium, // 使用 Material Design 的标题样式
            modifier = Modifier
                .padding(bottom = 16.dp)
                .align(Alignment.CenterHorizontally)
        )
        OutlinedTextField(
            value = keywordInput,
            onValueChange = { keywordInput = it },
            label = { Text("输入搜索关键字") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = cityInput,
            onValueChange = { cityInput = it },
            label = { Text("输入城市 (可留空)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                poiViewModel.searchPoi(keywordInput, cityInput)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("搜索POI")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        if (suggestionCities.isNotEmpty()) {
            Text(
                text = "或许你想在这些城市中搜索:",
                style =  MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            suggestionCities.forEach { cityName ->
                TextButton(
                    onClick = {
                        cityInput = cityName
                        poiViewModel.searchPoi(keywordInput)
                    },
                    enabled = !isLoading
                ) {
                    Text(cityName)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isLoading && poiList.isEmpty()) {
                Log.d("MainActivity", "准备显示加载进度条")
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (poiList.isNotEmpty()) {
                Log.d("MainActivity", "准备显示结果")
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = poiList,
                        key = { poiItem -> poiItem.poiId }
                    ) { poiItem ->
                        PoiListItemView(poiItem = poiItem)
                        Divider()
                    }
                }
            }

            Text(
                text = "暂无结果或未开始搜索",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PoiListItemView(poiItem: PoiItem, modifier: Modifier = Modifier) {
    Log.d("MainActivity", "地址信息：${poiItem.title}")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(
            text = poiItem.title ?: "无标题信息",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "地址：${poiItem.snippet ?: "无地址信息"}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "类型: ${poiItem.typeDes ?: "未知类型"}",
            style = MaterialTheme.typography.bodySmall
        )
        if (!poiItem.tel.isNullOrBlank()) {
            Text(
                text = "电话: ${poiItem.tel}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}