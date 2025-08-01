package top.wkbin.freeform

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import top.wkbin.freeform.ui.theme.VexFreeformTheme
import java.io.IOException
import androidx.core.net.toUri


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRootAccess()
        setContent {
            VexFreeformTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun requestRootAccess(){
        try {
            val process = Runtime.getRuntime().exec("su")
        }catch (e: IOException){

        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        Text(text = "Hello $name!")
        Button(onClick = {
            if (!Settings.canDrawOverlays(context)) {
                // 请求权限
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    ("package:" + context.packageName).toUri()
                )
                context.startActivity(intent)
            } else {
                // 已有权限，启动服务
                context.startService(Intent(context, FloatingWindowService::class.java))
            }
        }) {
            Text("打开小窗造梦次元")
        }
    }

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VexFreeformTheme {
        Greeting("Android")
    }
}