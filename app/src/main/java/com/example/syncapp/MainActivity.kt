package com.example.syncapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.syncapp.ui.SyncViewModel
import com.example.syncapp.ui.green
import com.example.syncapp.ui.red
import com.example.syncapp.ui.theme.SyncAppTheme
import java.util.concurrent.Executors
import android.provider.Settings
import androidx.compose.ui.input.key.Key.Companion.Home
import androidx.compose.ui.platform.LocalContext


var context: Context? = null

var permissionTag = "Permission Tag"

val ebrima = FontFamily(Font(R.font.ebrima))

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  set app to run in background regardless of power restrictions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
        setContent {
            SyncAppTheme() {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    context = LocalContext.current      //  store applicationContext

                    HomeLayout()

                }
            }
        }
    }
}


@Composable
fun HomeLayout(syncViewModel: SyncViewModel = viewModel())
{
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier
                .weight(0.1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            )
            {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "SOFIA",
                        modifier = Modifier.padding(start = 16.dp, bottom = 20.dp),
                        fontFamily = ebrima,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(
                            letterSpacing = 4.sp, fontSize = 28.sp
                        )
                    )
                    Text(
                        text = "SYNC-CLIENT",
                        modifier = Modifier.padding(start = 20.dp, top = 20.dp),
                        fontSize = 16.sp,
                        fontFamily = ebrima
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(0.05f))
        //  local container
        Card(modifier = Modifier
            .fillMaxWidth(0.95f)
            .weight(0.2f))
        {
            AttributesContainer("LOCAL", syncViewModel)
        }

        Spacer(modifier = Modifier.weight(0.05f))
        //  global container
        Card(modifier = Modifier
            .fillMaxWidth(0.95f)
            .weight(0.2f))
        {
            AttributesContainer("GLOBAL", syncViewModel)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .weight(0.1f),
            horizontalArrangement = Arrangement.End,
        )
        {
            PullSyncLayout(syncViewModel = syncViewModel)
        }
        Spacer(modifier = Modifier.weight(0.05f))
        Card(modifier = Modifier
            .fillMaxWidth(0.95f)
            .weight(0.1f))
        {
            DirectoriesLayout()
        }
        Spacer(modifier = Modifier.weight(0.05f))
        Card(modifier = Modifier
            .fillMaxWidth(0.95f)
            .weight(0.1f))
        {
            LastSyncLayout(syncViewModel)
        }

        Spacer(modifier = Modifier.weight(0.05f))

        //  for loading bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        )
        {
            ProgressBarLayout(syncViewModel)
        }
        Spacer(modifier = Modifier.weight(0.025f))
        ConnectButtonLayout(syncViewModel)
        Spacer(modifier = Modifier.weight(0.025f))
        Card(modifier = Modifier
            .weight(0.04f)
            .fillMaxSize(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        {
            IndicatorLayout(syncViewModel)
        }
    }
}

@Composable
fun PullSyncLayout(syncViewModel: SyncViewModel)
{
    Button(onClick = {
        Executors.newSingleThreadExecutor().execute {syncViewModel.getLocalFiles()}
    }, modifier = Modifier
        .scale(0.9f)
        .padding(top = 16.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = syncViewModel.isConnected
    ) {
        Text(text = "DIRS", color = Color.LightGray)
    }

    Button(onClick = { Executors.newSingleThreadExecutor().execute { syncViewModel.makePullRequest() } }, modifier = Modifier
        .scale(0.9f)
        .padding(top = 16.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = syncViewModel.isConnected
    ) {
        Text(text = "PULL")
    }

    Button(onClick = {
        Executors.newSingleThreadExecutor().execute {syncViewModel.makeSyncRequest()}
    }, modifier = Modifier
        .scale(0.9f)
        .padding(top = 16.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = syncViewModel.isConnected
    ) {
        Text(text = "SYNC")
    }
}

@Composable
fun ConnectButtonLayout(syncViewModel: SyncViewModel)
{
    Button(onClick = { Executors.newSingleThreadExecutor().execute { syncViewModel.connectToServer()
    }}  , modifier = Modifier,
        shape = RoundedCornerShape(12.dp),
        enabled = !syncViewModel.isConnected
    ) {
        Text(text = "CONNECT")
    }
}

@Composable
fun IndicatorLayout(syncViewModel: SyncViewModel)
{
    Row(modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.off),
            contentDescription = "connection indicator",
            modifier = Modifier
                .scale(0.5f, 0.4f),
            tint = if(syncViewModel.isConnected) green else red
        )
    }
}

@Composable
fun DirectoriesLayout()
{
    Text(text = "Directory", modifier = Modifier.padding(start = 12.dp, top=10.dp), fontSize = 20.sp, fontFamily = ebrima)
    Text(text = ".../SyncApp/Music/", modifier = Modifier.padding(start = 12.dp), fontSize = 16.sp, fontFamily = ebrima)
}

@Composable
fun LastSyncLayout(syncViewModel: SyncViewModel)
{
    Text(text = "Last Sync", modifier = Modifier.padding(start = 12.dp, top=10.dp), fontSize = 20.sp, fontFamily = ebrima)
    Text(text = syncViewModel.syncDate, modifier = Modifier.padding(start = 12.dp), fontSize = 16.sp, fontFamily = ebrima)
}
@Composable
fun ProgressBarLayout(syncViewModel: SyncViewModel)
{
    if (syncViewModel.isWorking)
    {
        LinearProgressIndicator(modifier = Modifier
            .fillMaxHeight(0.01f)
            .fillMaxWidth(0.7f)
            .padding(bottom = 8.dp)
        )
    }
    Text(text = syncViewModel.statusString, fontSize = 20.sp)
}

@Composable
fun AttributesContainer(fieldTitle: String, syncViewModel: SyncViewModel)
{
    var fileCount = 0
    var dirsCount = 0
    var sizeCount = 0.0
    when(fieldTitle)
    {
        "LOCAL" -> {fileCount = syncViewModel.localFilesCount; dirsCount = syncViewModel.localDirsCount;
            sizeCount = syncViewModel.localSize.toDouble()}
        "GLOBAL" -> {fileCount = syncViewModel.globalFilesCount; dirsCount = syncViewModel.globalDirsCount;
            sizeCount = syncViewModel.globalSize.toDouble()}          //  1073741824 = 1024*1024*1024 to get size in GB
    }
    if (sizeCount>0)
    {
        sizeCount /= 1073741824
    }
    Log.d("files: ", fileCount.toString())
    Log.d("dirs: ", dirsCount.toString())
    Log.d("size: ", sizeCount.toString())
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.1f))
        Row(horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        )
        {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.folder_fill0_wght400_grad0_opsz48),
                    contentDescription = "folder icon",
                    modifier = Modifier
                        .scale(1f)
                        .rotate(-90f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = dirsCount.toString(), fontSize = 50.sp, fontFamily = ebrima)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,

                ) {
                Icon(
                    painter = painterResource(id = R.drawable.draft_fill0_wght400_grad0_opsz48),
                    contentDescription = "files icon",
                    modifier = Modifier
                        .scale(1f)
                        .rotate(0f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = fileCount.toString(), fontSize = 50.sp, fontFamily = ebrima)
            }

            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end=12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.hard_drive_fill0_wght400_grad0_opsz48),
                    contentDescription = "files icon",
                    modifier = Modifier
                        .scale(1f)
                        .rotate(-90f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = String.format("%,.2f", sizeCount), fontSize = 50.sp, fontFamily = ebrima)
            }
        }
        Text(text = fieldTitle, modifier = Modifier
            .fillMaxWidth()
            .padding(end = 12.dp), fontSize = 30.sp, textAlign = TextAlign.End)
        Spacer(modifier = Modifier.weight(0.1f))
    }
}

@Composable
fun requestPermission()
{
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(), onResult = {
            isGranted -> if(isGranted) Log.d(permissionTag, "is granted")
    })
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(
    showBackground = true,
    showSystemUi = true,
)
@Composable
fun GreetingPreview() {
    SyncAppTheme {
        HomeLayout()
    }
}