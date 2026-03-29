package com.abast.homebot.actions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.abast.homebot.FlashlightService
import com.abast.homebot.MainActivity
import com.abast.homebot.R
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.File
import java.lang.ref.SoftReference
import java.net.URISyntaxException
import kotlin.reflect.KClass

@JsonTypeInfo(
    use = JsonTypeInfo.Id.MINIMAL_CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@class"
)
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(OpenWeb::class),
        JsonSubTypes.Type(LaunchApp::class),
        JsonSubTypes.Type(ToggleBrightness::class),
        JsonSubTypes.Type(OpenRecentApps::class),
        JsonSubTypes.Type(LaunchShortcut::class),
        JsonSubTypes.Type(ToggleFlashlight::class),
        JsonSubTypes.Type(Folder::class),
        JsonSubTypes.Type(QuickSearch::class),
        JsonSubTypes.Type(Calculator::class)
    ]
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class HomeAction {
    @get:JsonIgnore
    @get:StringRes
    abstract val titleRes: Int

    fun title(context: Context): String = context.getString(titleRes)

    abstract fun label(context: Context): String

    abstract fun icon(context: Context): Drawable

    abstract fun run(context: Context)

    /**
     * Launches MainActivity. Used as fallback for any errors that might occur.
     */
    protected fun launchMainActivity(context: Context) {
        val i = Intent(context, MainActivity::class.java)
        val activity = context as Activity
        activity.finish()
        activity.startActivity(i)
    }
}

fun KClass<out HomeAction>.dumbInstance(): HomeAction = when (this) {
    ToggleFlashlight::class -> ToggleFlashlight
    ToggleBrightness::class -> ToggleBrightness
    OpenRecentApps::class -> OpenRecentApps
    LaunchApp::class -> LaunchApp("")
    LaunchShortcut::class -> LaunchShortcut("", "", null)
    OpenWeb::class -> OpenWeb("")
    Folder::class -> Folder("", "", listOf(ToggleFlashlight))
    QuickSearch::class -> QuickSearch
    Calculator::class -> Calculator
    else -> throw IllegalStateException()
}

object ToggleFlashlight : HomeAction() {
    override fun icon(context: Context): Drawable =
        context.getDrawable(R.drawable.ic_flashlight)!!

    override fun label(context: Context): String = title(context)
    override fun run(context: Context) {
        val flashlightIntent = Intent(context, FlashlightService::class.java)
        ContextCompat.startForegroundService(context, flashlightIntent)
        (context as Activity).finish()
    }

    override val titleRes: Int
        get() = R.string.pref_title_flashlight
}

object ToggleBrightness : HomeAction() {
    override fun icon(context: Context): Drawable =
        context.getDrawable(R.drawable.ic_brightness)!!

    override fun label(context: Context): String = title(context)
    override fun run(context: Context) {
        val activity = context as Activity
        if (Settings.System.canWrite(context)) {
            val cResolver = activity.contentResolver
            try {
                // To handle the auto
                Settings.System.putInt(
                    cResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                //Get the current system brightness
                val brightness =
                    Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS)
                if (brightness > 0) {
                    Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
                } else {
                    Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 255)
                }
                activity.finish()
            } catch (e: Settings.SettingNotFoundException) {
                Log.e("Error", context.getString(R.string.error_brightness))
                Toast.makeText(context, R.string.error_brightness, Toast.LENGTH_SHORT).show()
                launchMainActivity(context)
            }
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:" + context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Please grant permission to modify system settings", Toast.LENGTH_LONG).show()
            activity.finish()
        }
    }

    override val titleRes: Int
        get() = R.string.pref_title_brightness
}

object OpenRecentApps : HomeAction() {
    override fun icon(context: Context): Drawable =
        context.getDrawable(R.drawable.ic_recent_apps)!!

    override fun label(context: Context): String = title(context)
    override fun run(context: Context) {
        val intent = Intent("com.android.systemui.recent.action.TOGGLE_RECENTS")
        intent.setPackage("com.android.systemui")
        context.sendBroadcast(intent)
        (context as Activity).finish()
    }

    override val titleRes: Int
        get() = R.string.pref_title_recents
}

object QuickSearch : HomeAction() {
    override fun icon(context: Context): Drawable =
        context.getDrawable(android.R.drawable.ic_menu_search)!!

    override fun label(context: Context): String = title(context)

    override fun run(context: Context) {
        val intent = Intent(context, com.abast.homebot.QuickSearchActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
        (context as? Activity)?.finish()
    }

    override val titleRes: Int
        get() = R.string.pref_title_quick_search
}

data class LaunchApp(val uri: String) : HomeAction() {
    private var iconCache: SoftReference<Drawable>? = null

    override fun icon(context: Context): Drawable {
        val cache = iconCache?.get()
        return if (cache == null) {
            val newIcon = context.packageManager.getActivityIcon(Intent.parseUri(uri, 0))
            iconCache = SoftReference(newIcon)
            newIcon
        } else {
            cache
        }
    }

    override fun label(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("HBT", "uri is $uri")
            context.packageManager.resolveActivity(
                Intent.parseUri(uri, 0),
                PackageManager.ResolveInfoFlags.of(0L)
            )!!
        } else {
            context.packageManager.resolveActivity(
                Intent.parseUri(uri, 0),
                0
            )!!
        }.activityInfo.loadLabel(context.packageManager).toString()

    override fun run(context: Context) {
        val activity = context as Activity
        try {
            val shortcut = Intent.parseUri(uri, 0).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            activity.finish()
            activity.startActivity(shortcut)
            activity.overridePendingTransition(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_top)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            launchMainActivity(context)
        }
    }

    override val titleRes: Int
        get() = R.string.pref_title_app
}

data class LaunchShortcut(val uri: String, val name: String, val iconFile: String? = null) : HomeAction() {
    override fun icon(context: Context): Drawable {
        if (iconFile != null) {
            val file = File(context.filesDir, iconFile)
            if (file.exists()) {
                val drawable = Drawable.createFromPath(file.absolutePath)
                if (drawable != null) return drawable
            }
        }
        return context.packageManager.getActivityIcon(Intent.parseUri(uri, 0))
    }

    override fun label(context: Context): String = name
    override fun run(context: Context) {
        val activity = context as Activity
        try {
            val shortcut = Intent.parseUri(uri, 0).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            activity.finish()
            activity.startActivity(shortcut)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            launchMainActivity(context)
        }
    }

    override val titleRes: Int
        get() = R.string.pref_title_shortcut
}

data class OpenWeb(val address: String) : HomeAction() {
    override fun icon(context: Context): Drawable =
        context.getDrawable(R.drawable.ic_web)!!

    override fun label(context: Context): String = address
    override fun run(context: Context) {
        val activity = context as Activity
        var finalUrl = address
        if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://"))
            finalUrl = "http://$address"
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, Uri.parse(finalUrl))
        activity.finish()
    }

    override val titleRes: Int
        get() = R.string.pref_title_web
}

object Calculator : HomeAction() {
    override fun icon(context: Context): Drawable =
        context.getDrawable(R.drawable.ic_calculator)!!

    override fun label(context: Context): String = title(context)

    override fun run(context: Context) {
        val activity = context as Activity
        try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
            activity.finish()
        } catch (e: Exception) {
            // Fallback: search for common calculator packages
            val calculatorPackages = listOf(
                "com.android.calculator2",
                "com.google.android.calculator",
                "com.sec.android.app.popupcalculator",
                "com.miui.calculator",
                "com.huawei.calculator"
            )
            val pm = activity.packageManager
            for (pkg in calculatorPackages) {
                val intent = pm.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    activity.startActivity(intent)
                    activity.finish()
                    return
                }
            }
            Toast.makeText(context, "Calculator not found", Toast.LENGTH_SHORT).show()
            launchMainActivity(context)
        }
    }

    override val titleRes: Int
        get() = R.string.pref_title_calculator
}

data class Folder(val iconFile: String, val name: String, val actions: List<HomeAction>) :
    HomeAction() {
    init {
        require(actions.isNotEmpty())
    }

    override val titleRes: Int = R.string.pref_title_folder

    override fun label(context: Context): String = name

    override fun icon(context: Context): Drawable =
        Drawable.createFromPath(File(context.filesDir, iconFile).absolutePath)!!

    override fun run(context: Context) {}
}
