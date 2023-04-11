package com.abast.homebot

import android.content.ComponentName
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.abast.homebot.databinding.ActivityMainBinding
import com.abast.homebot.settings.HomeBotPreferenceFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.warningButton.setOnClickListener{
            // Opens assistant settings
            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
            val packageManager = packageManager
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this,getString(R.string.error_no_assistant),Toast.LENGTH_LONG).show()
            }
        }

        supportFragmentManager.beginTransaction().replace(R.id.preferences_frame,
            HomeBotPreferenceFragment()
        ).commit()
    }

    override fun onResume() {
        super.onResume()
        if(isAssistApp(this)){
            binding.warningText.visibility = View.GONE
            binding.warningButton.visibility = View.GONE
        }else{
            binding.warningText.visibility = View.VISIBLE
            binding.warningButton.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.licenses){
            startActivity(Intent(this,LicensesActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Checks if app is set as the Assist app.
     */
    private fun isAssistApp(context: Context) : Boolean {
        val assistant = Settings.Secure.getString(context.contentResolver, "assistant")
        var isAssistApp = false
        if (assistant != null) {
            val cn = ComponentName.unflattenFromString(assistant)
            if (cn != null) {
                if (cn.packageName == context.packageName) {
                    isAssistApp = true
                }
            }
        }
        return isAssistApp
    }

}