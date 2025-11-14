package com.calai.app.ui.home.ui.weight

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.calai.app.R
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.ui.home.ui.weight.model.WeightViewModel
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordWeightScreen(
    vm: WeightViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit          // ★ 新增參數，與 Nav 呼叫保持一致
) {
    val ui by vm.ui.collectAsState()
    var date by remember { mutableStateOf(LocalDate.now()) }
    var showDateSheet by remember { mutableStateOf(false) }
    var photoFile by remember { mutableStateOf<File?>(null) }
    var wheelValue by remember { mutableStateOf(70.0) } // kg
    val context = LocalContext.current

    // ★ 安全 PhotoPicker：預防沒有 ActivityResultRegistry 的場景（Preview/某些容器）
    val registryOwner = LocalActivityResultRegistryOwner.current
    val photoPicker = if (registryOwner != null) {
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            // TODO: 將 uri 存成 File（或改傳 InputStream 至後端）
            // photoFile = ...
        }
    } else null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Record Weight") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                onClick = {
                    val kg = if (ui.unit == UserProfileStore.WeightUnit.KG) wheelValue
                    else wheelValue / 2.20462262
                    vm.save(kg, date, photoFile)
                    onDone()
                }
            ) { Text("Save") }
        }
    ) { inner ->
        Column(
            Modifier.padding(inner).padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 日期列
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(date.toString(), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showDateSheet = true }) { Text("Change date") }
            }
            // 照片
            Box(
                Modifier.fillMaxWidth().height(160.dp).clickable {
                    if (photoPicker != null) {
                        photoPicker.launch("image/*")
                    } else {
                        Toast.makeText(context, "Photo picker unavailable in this environment.", Toast.LENGTH_SHORT).show()
                    }
                },
                contentAlignment = Alignment.Center
            ) {
                if (photoFile == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painterResource(R.drawable.weight_upload_1), contentDescription = null)
                        Text("Tap to add photo")
                    }
                } else {
                    Text("Photo selected")
                }
            }
            // 簡化輸入：以 Slider 模擬數字輪
            Text(
                if (ui.unit == UserProfileStore.WeightUnit.KG)
                    String.format("%.1f kg", wheelValue)
                else
                    String.format("%d lb", (wheelValue * 2.20462262).toInt())
            )
            Slider(
                value = wheelValue.toFloat(),
                onValueChange = { wheelValue = (it.toDouble()).coerceIn(20.0, 800.0) }, // 20~800 kg
                valueRange = 20f..800f
            )
        }
    }

    if (showDateSheet) {
        ModalBottomSheet(onDismissRequest = { showDateSheet = false }) {
            DatePickerStub(current = date, onPick = { date = it; showDateSheet = false })
        }
    }
}

@Composable private fun DatePickerStub(current: LocalDate, onPick: (LocalDate) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(current.toString())
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onPick(current) }) { Text("Save") }
    }
}
