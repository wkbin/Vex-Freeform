package top.wkbin.freeform

import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.hardware.input.InputManager
import android.os.IBinder
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.FrameLayout
import java.lang.reflect.Field


class FloatingWindowService : Service() {
    private lateinit var container: FrameLayout
    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var virtualDisplay: VirtualDisplay? = null
    private lateinit var textureView: TextureView
    private lateinit var mIntent: Intent
    private lateinit var mComponentName: ComponentName
    private lateinit var activityManager: ActivityManager

    // 添加输入管理器用于转发触摸事件
    private lateinit var inputManager: InputManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        inputManager = getSystemService(INPUT_SERVICE) as InputManager
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager

        // 创建浮动窗口容器
        container = FrameLayout(this)
        container.setBackgroundColor(Color.TRANSPARENT)

        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels

        layoutParams = WindowManager.LayoutParams(
            (width * 0.6f).toInt(),
            (height * 0.6f).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.CENTER

        windowManager.addView(container, layoutParams)

        // 创建 TextureView 用于显示目标应用的内容
        textureView = TextureView(this)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture, width: Int, height: Int
            ) {
                Log.d("FloatingWindowService", "SurfaceTexture 可用")
                createVirtualDisplay(textureView) // SurfaceTexture 准备好后创建 VirtualDisplay
                virtualDisplay?.surface = Surface(surface)
                // 启动目标应用并显示在小窗中
                launchAppAndDisplayInWindow("com.ideaflow.zmcy")
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Log.d("FloatingWindowService", "SurfaceTexture 被销毁")
                virtualDisplay?.release() // 释放 VirtualDisplay
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        container.addView(textureView)
    }

    private fun launchAppAndDisplayInWindow(packageName: String) {
        // 设置启动目标应用的 Intent 和 ComponentName
        mIntent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(packageName, "$packageName.module.splash.SplashActivity")
        }
        mComponentName = mIntent.component!!

        // 使用 startIntent 启动目标应用并确保其显示在虚拟显示器中
        startIntent(mIntent, virtualDisplay?.display?.displayId ?: Display.DEFAULT_DISPLAY)
    }

    private fun createVirtualDisplay(textureView: TextureView) {
        try {
            // 获取显示管理器并创建虚拟显示器
            val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
            virtualDisplay = displayManager.createVirtualDisplay(
                "FloatingAppVirtualDisplay",
                textureView.width, textureView.height,
                resources.displayMetrics.densityDpi,
                null,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
            )
            Log.d("FloatingWindowService", "虚拟显示器创建成功")

        } catch (e: Exception) {
            Log.e("FloatingWindowService", "创建 VirtualDisplay 失败", e)
        }
    }

    private fun startIntent(intent: Intent, displayId: Int) {
        // 设置启动应用的 ActivityOptions，确保应用显示在虚拟显示器上
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent, options.toBundle())
        // 调用 callIntent 启动目标应用，并确保它显示在正确的虚拟显示器上
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(container)
        virtualDisplay?.release() // 释放虚拟显示器资源，如果 virtualDisplay 为空则不会抛异常
    }
}
