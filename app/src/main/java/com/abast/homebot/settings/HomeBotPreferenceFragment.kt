package com.abast.homebot.settings

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.abast.homebot.R
import com.abast.homebot.actions.*
import com.abast.homebot.pickers.AppPickerActivity
import com.google.android.material.snackbar.Snackbar
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

class HomeBotPreferenceFragment : PreferenceFragmentCompat(), AddActionPreference.Listener {
    companion object {
        const val IMPORT_SETTINGS_FILE_REQUEST_CODE = 432
    }

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val appPickerIntent: Intent by lazy {
        Intent(context, AppPickerActivity::class.java).apply {
            putExtra(AppPickerActivity.EXTRA_LABEL, getString(R.string.choose_app))
            putExtra(AppPickerActivity.EXTRA_PICK_TYPE, AppPickerActivity.PICK_TYPE_APP)
        }
    }

    private val shortcutPickerIntent: Intent by lazy {
        Intent(context, AppPickerActivity::class.java).apply {
            putExtra(AppPickerActivity.EXTRA_LABEL, getString(R.string.choose_shortcut))
            putExtra(AppPickerActivity.EXTRA_PICK_TYPE, AppPickerActivity.PICK_TYPE_SHORTCUT)
        }
    }

    private val actionsPreference: ActionsPreference by lazy {
        findPreference<ActionsPreference>(getString(R.string.actions_setting_key))!!
    }

    private val exportSettingsPreference: Preference by lazy {
        findPreference<Preference>(getString(R.string.export_settings_preference_key))!!
    }

    private val importSettingsPreference: Preference by lazy {
        findPreference<Preference>(getString(R.string.import_settings_preference_key))!!
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_general)
        actionsPreference.setOnActionClickListener(::onActionClick)
        exportSettingsPreference.setOnPreferenceClickListener {
            exportSettings()
            true
        }
        importSettingsPreference.setOnPreferenceClickListener {
            importSettings()
            true
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is AddActionPreference) {
            AddActionPreferenceDialogFragment.newInstance(preference.key).let { dialog ->
                dialog.setTargetFragment(this, 0)
                dialog.listener = this
                dialog.show(fragmentManager!!, "androidx.preference.PreferenceFragment.DIALOG")
            }
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun createAction(type: KClass<out HomeAction>) {
        when (type) {
            ToggleBrightness::class, ToggleFlashlight::class, OpenRecentApps::class -> actionsPreference.addAction(
                type.dumbInstance()
            )
            LaunchApp::class -> startActivityForResult(
                appPickerIntent,
                AppPickerActivity.REQUEST_CODE_APP
            )
            LaunchShortcut::class -> startActivityForResult(
                shortcutPickerIntent,
                AppPickerActivity.REQUEST_CODE_SHORTCUT
            )
            OpenWeb::class -> showWebDialog()
            Folder::class ->
                startActivityForResult(
                    Intent(context, EditFolderActivity::class.java),
                    EditFolderActivity.NEW_FOLDER_REQUEST_CODE
                )
            else -> throw IllegalStateException()
        }
    }

    private fun exportSettings() {
        val settings = sharedPrefs.getString(getString(R.string.actions_setting_key), "[]")!!
        try {
            val file = buildExportFile()
            PrintWriter(file).use {
                it.write(settings)
            }
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                putExtra(
                    Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(context!!, "com.abast.homebot", file)
                )
                type = "text/plain"
            }, getString(R.string.export_backup_intent_title)))
        } catch (e: Exception) {
            Snackbar.make(view!!, R.string.export_settings_error_message, Snackbar.LENGTH_SHORT).show()
            Log.e("HBK", "Cannot export settings", e)
        }
    }

    private fun buildExportFile(): File =
        File(
            File(context!!.cacheDir, "/backup/").apply {
                mkdir()
            },
            SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.getDefault()).format(Date())
        )

    private fun importSettings() {
        startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
        }, IMPORT_SETTINGS_FILE_REQUEST_CODE)
    }

    /**
     * Shows dialog with EditText to enter a url
     */
    private fun showWebDialog() {
        context?.let {
            val et = EditText(it)
            et.hint = getString(R.string.web_hint)
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.enter_url)
            builder.setView(et)
            builder.setPositiveButton(R.string.ok) { dialog, _ ->
                actionsPreference.addAction(OpenWeb(et.text.toString()))
                dialog.dismiss()
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                AppPickerActivity.REQUEST_CODE_APP -> {
                    data?.extras?.getParcelable<ActivityInfo>(AppPickerActivity.EXTRA_PICKED_CONTENT)?.let { appData ->
                        val activityIntent = activity!!.packageManager.getLaunchIntentForPackage(appData.packageName)!!
                        val value = activityIntent.toUri(0)
                        actionsPreference.addAction(LaunchApp(value))
                    }
                }
                AppPickerActivity.REQUEST_CODE_SHORTCUT -> {
                    val shortcutIntent = data?.extras?.getParcelable<Intent>(Intent.EXTRA_SHORTCUT_INTENT)!!
                    val shortcutName = data.extras?.getString(Intent.EXTRA_SHORTCUT_NAME)!!
                    val value = shortcutIntent.toUri(Intent.URI_INTENT_SCHEME)
                    actionsPreference.addAction(LaunchShortcut(value, shortcutName))
                }
                HomeBotPreferenceFragment.IMPORT_SETTINGS_FILE_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        val text = BufferedReader(InputStreamReader(context!!.contentResolver.openInputStream(uri))).use {
                            it.readText()
                        }
                        sharedPrefs.edit().putString(getString(R.string.actions_setting_key), text).apply()
                        actionsPreference.refresh()
                    }
                }
            }
        }
    }

    private fun onActionClick(action: HomeAction) {
        if (action is Folder) {
            startActivityForResult(
                Intent(context, EditFolderActivity::class.java),
                EditFolderActivity.EDIT_FOLDER_REQUEST_CODE
            )
        }
    }

}
