package com.example.syncapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bunnyha.homeautomationsystem.ui.theme.SyncAppTheme
import com.example.syncapp.ui.SyncViewModel
import com.example.syncapp.ui.green
import com.example.syncapp.ui.red
import java.util.concurrent.Executors


var context: Context? = null

var permissionTag = "Permission Tag"

val ebrima = FontFamily(Font(R.font.ebrima))
val bison = FontFamily(
    Font(R.font.bison, weight = FontWeight.Normal),
    Font(R.font.bison_demibold, weight = FontWeight.SemiBold),
    Font(R.font.bison_bold, weight = FontWeight.Bold))

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
        installSplashScreen()
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
            .fillMaxSize()
            .alpha(0.95f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier
                .weight(0.08f)
                .fillMaxWidth()
                .shadow(8.dp),
            //shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxSize()
            )
            {
                Icon(painter = painterResource(R.drawable.sync_logo_svglike),
                    contentDescription = "app icon", modifier = Modifier.scale(0.5f))
                Text(text= stringResource(id = R.string.app_name),fontSize = 24.sp,
                    fontFamily = bison,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp)
            }
        }
        Spacer(modifier = Modifier.weight(0.05f))
        //  local container
        Card(modifier = Modifier
            .fillMaxWidth(0.95f)
            .weight(0.25f)
            .shadow(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface))
        {
            AttributesContainer("LOCAL", syncViewModel)
        }

        Spacer(modifier = Modifier.weight(0.05f))
        //  global container
        Card(modifier = Modifier
            .fillMaxWidth(0.95f)
            .weight(0.25f)
            .shadow(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface))
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
            .weight(0.1f)
            .shadow(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface))
        {
            DirectoriesLayout()
        }
        Spacer(modifier = Modifier.weight(0.05f))
        Card(modifier = Modifier
            .fillMaxWidth(0.95f)
            .weight(0.1f)
            .shadow(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface))
        {
            Column(verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.weight(0.1f))
                LastSyncLayout(syncViewModel)
                Spacer(modifier = Modifier.weight(0.1f))
            }
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
        AnimatedVisibility(!syncViewModel.isConnected){ ConnectButtonLayout(syncViewModel) }
        AnimatedVisibility(syncViewModel.isConnected){ DisConnectButtonLayout(syncViewModel) }
        Spacer(modifier = Modifier.weight(0.025f))
        /*Card(modifier = Modifier
            .weight(0.04f)
            .fillMaxSize(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        {
            IndicatorLayout(syncViewModel)
        }*/
    }
}

@Composable
fun PullSyncLayout(syncViewModel: SyncViewModel)
{
    /*Button(onClick = {
        Executors.newSingleThreadExecutor().execute {syncViewModel.getLocalFiles()}
    }, modifier = Modifier
        .scale(0.9f)
        .padding(top = 16.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = syncViewModel.isConnected
    ) {
        Text(text = "DIRS", color = Color.LightGray)
    }*/

    OutlinedButton(onClick = { Executors.newSingleThreadExecutor().execute { syncViewModel.makePullRequest() } }, modifier = Modifier
        .scale(0.9f)
        .padding(top = 16.dp),
        shape = RoundedCornerShape(50),
        enabled = syncViewModel.isConnected
    ) {
        Icon(
            painter = painterResource(R.drawable.alt_arrow_down_svgrepo_com),
            contentDescription = "app icon", modifier = Modifier
                .requiredSize(40.dp)
                .padding(end = 16.dp)
        )
        Text(
            text = "PULL", fontFamily = bison, letterSpacing = 2.sp, fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier
        )
    }

    OutlinedButton(onClick = {
        Executors.newSingleThreadExecutor().execute {syncViewModel.makeSyncRequest()}
    }, modifier = Modifier
        .scale(0.9f)
        .padding(top = 16.dp),
        shape = RoundedCornerShape(50),
        enabled = syncViewModel.isConnected
    ) {
        Icon(painter = painterResource(R.drawable.restart_svgrepo_com),
            contentDescription = "app icon", modifier = Modifier
                .requiredSize(40.dp)
                .padding(end = 16.dp))
        Text(text = "SYNC", fontFamily = bison, letterSpacing = 2.sp, fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ConnectButtonLayout(syncViewModel: SyncViewModel)
{
    OutlinedButton(onClick = { Executors.newSingleThreadExecutor().execute { syncViewModel.connectToServer()
    }}  , modifier = Modifier,
        shape = RoundedCornerShape(50)
    ) {
        Icon(painter = painterResource(R.drawable.bolt_svgrepo_com),
            contentDescription = "app icon", modifier = Modifier
                .requiredSize(20.dp)
                .padding(end = 8.dp))
        Text(text = "CONNECT", fontFamily = bison, letterSpacing = 2.sp, fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun DisConnectButtonLayout(syncViewModel: SyncViewModel)
{
    OutlinedButton(onClick = { Executors.newSingleThreadExecutor().execute { syncViewModel.serverDisconnect()
    }}  , modifier = Modifier,
        shape = RoundedCornerShape(50)
    ) {
        Icon(painter = painterResource(R.drawable.bolt_svgrepo_com),
            contentDescription = "app icon", modifier = Modifier
                .requiredSize(20.dp)
                .padding(end = 8.dp))
        Text(text = "DISCONNECT", fontFamily = bison, letterSpacing = 2.sp, fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface)
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
    Row(modifier = Modifier.fillMaxHeight()) {
        Icon(
            painter = painterResource(R.drawable.folder_open_svgrepo_com),
            contentDescription = "device icon", modifier = Modifier
                .scale(0.5f)
                .padding(top = 8.dp)
        )
        Column(modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center)
        {
            Text(
                text = "DIRECTORY",
                modifier = Modifier
                    .padding(start = 4.dp, top = 0.dp)
                    .alpha(0.5f),
                fontSize = 20.sp,
                fontFamily = bison,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
            Text(
                text = ".../SyncApp/Music/",
                modifier = Modifier
                    .padding(start = 4.dp, top = 0.dp),
                fontSize = 20.sp,
                fontFamily = bison,
                fontWeight = FontWeight.Normal,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun LastSyncLayout(syncViewModel: SyncViewModel)
{
    Row(modifier = Modifier.fillMaxHeight()) {
        Icon(
            painter = painterResource(R.drawable.clock_circle_svgrepo_com),
            contentDescription = "device icon", modifier = Modifier
                .scale(0.5f)
                .padding(top = 8.dp)
        )
        Column(modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center)
        {
            Text(
                text = "LAST SYNC",
                modifier = Modifier
                    .padding(start = 4.dp, top = 0.dp)//t26, s4
                    .alpha(0.5f),
                fontSize = 20.sp,
                fontFamily = bison,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
            Text(
                text = syncViewModel.syncDate,
                modifier = Modifier
                    .padding(start = 4.dp, top = 0.dp),
                fontSize = 20.sp,
                fontFamily = bison,
                fontWeight = FontWeight.Normal,
                letterSpacing = 2.sp
            )
        }
    }
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
    var statusText = if(syncViewModel.statusString.length>=35){syncViewModel.statusString.take(32)+".."}else{syncViewModel.statusString}
    Text(text = statusText/*syncViewModel.statusString*/, fontFamily = bison, letterSpacing = 2.sp, fontSize = 20.sp)
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
        Row(horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.8f)
        )
        {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                    .weight(0.1f))
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(0.8f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.folder_svgrepo_com),
                        contentDescription = "folder icon",
                        modifier = Modifier
                            .scale(0.6f)
                            .weight(0.1f)
                            .fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = dirsCount.toString(),
                        fontSize = 40.sp,
                        fontFamily = bison,
                        modifier = Modifier.weight(0.1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )

                }
                Text(
                    text = "folders",
                    modifier = Modifier
                        .weight(0.2f)
                        .alpha(0.5f),
                    fontSize = 18.sp,
                    fontFamily = bison,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 2.sp
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(0.1f))
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(end = 0.dp)
                        .weight(0.8f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.file_text_svgrepo_com),
                        contentDescription = "files icon",
                        modifier = Modifier
                            .scale(0.6f)
                            .weight(0.1f)
                            .rotate(0f)
                            .fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = fileCount.toString(),
                        fontSize = 40.sp,
                        fontFamily = bison,
                        modifier = Modifier.weight(0.1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "files",
                    modifier = Modifier
                        .weight(0.2f)
                        .alpha(0.5f),
                    fontSize = 18.sp,
                    fontFamily = bison,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 2.sp
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(0.1f))
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(end = 0.dp)
                        .weight(0.8f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.server_2_svgrepo_com),
                        contentDescription = "files icon",
                        modifier = Modifier
                            .scale(0.6f)
                            .weight(0.1f)
                            .rotate(-90f)
                            .fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = String.format("%,.3f", sizeCount),
                        fontSize = 40.sp,
                        fontFamily = bison,
                        modifier = Modifier.weight(0.1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "size",
                    modifier = Modifier
                        .weight(0.2f)
                        .alpha(0.5f),
                    fontSize = 18.sp,
                    fontFamily = bison,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 2.sp
                )
            }
        }
        Text(text = fieldTitle, modifier = Modifier
            .fillMaxWidth()
            .weight(0.2f)
            .padding(end = 12.dp), fontSize = 20.sp,
            textAlign = TextAlign.End, fontFamily = bison,
        letterSpacing = 4.sp)
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