package com.abast.homebot

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import com.abast.homebot.actions.HomeAction
import com.abast.homebot.databinding.ActivityLauncherBinding
import com.abast.homebot.views.QuickActionButton
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.util.TypedValue
import android.view.View
import android.view.Gravity
import android.widget.TextView
import kotlin.math.roundToInt

class ActionLauncherActivity : AppCompatActivity() {
    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private val mapper: ObjectMapper by lazy { jacksonObjectMapper() }
    private val actionsReader: ObjectReader by lazy {
        mapper.readerFor(object : TypeReference<List<HomeAction>>() {})
    }

    private val actions: List<HomeAction> by lazy {
        val string = sharedPrefs.getString(getString(R.string.actions_setting_key), "[]")
        actionsReader.readValue<List<HomeAction>>(string)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.launcherBackground.setOnClickListener {
            finish()
        }
        when (actions.size) {
            0 -> launchMainActivity()
            1 -> actions.first().run(this)
            else -> {
                if (actions.size > 5) {
                    binding.launcherBackground.visibility = View.GONE
                    binding.launcherGrid.apply {
                        visibility = View.VISIBLE
                        layoutManager = GridLayoutManager(this@ActionLauncherActivity, 4)
                        adapter = LauncherGridAdapter(actions)
                    }
                } else {
                    actions.map {
                        QuickActionButton(this).apply {
                            setAction(it)
                        }
                    }.also {
                        binding.launcherBackground.setButtons(it)
                    }
                }
            }
        }
    }

    private inner class LauncherGridAdapter(private val actions: List<HomeAction>) :
        RecyclerView.Adapter<LauncherGridAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val button: QuickActionButton = view.findViewById(R.id.button)
            val label: TextView = view.findViewById(R.id.label)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_launcher_grid, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val action = actions[position]
            holder.button.setAction(action)
            holder.label.text = action.label(this@ActionLauncherActivity)
            holder.itemView.setOnClickListener {
                action.run(this@ActionLauncherActivity)
            }
        }

        override fun getItemCount(): Int = actions.size
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            finish()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        finish()
    }

    /**
     * Launches MainActivity. Used as fallback for any errors that might occur.
     */
    private fun launchMainActivity() {
        val i = Intent(this, MainActivity::class.java)
        finish()
        startActivity(i)
    }

}
