package com.abast.homebot

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class QuickSearchActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var resultsView: RecyclerView
    private val adapter = SearchAdapter { result, type ->
        when (type) {
            "web" -> {
                val browserIntent = Intent(Intent.ACTION_WEB_SEARCH)
                browserIntent.putExtra("query", result)
                startActivity(browserIntent)
            }
            else -> {
                val launchIntent = packageManager.getLaunchIntentForPackage(result)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(launchIntent)
                }
            }
        }
        finish()
    }

    private val disposables = CompositeDisposable()
    private var allApps: List<Pair<String, ResolveInfo>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_search)
        
        // click outside to dismiss
        findViewById<View>(android.R.id.content).setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (event.y < findViewById<View>(R.id.search_results).top) {
                    finish()
                }
            }
            false
        }

        searchInput = findViewById(R.id.search_input)
        resultsView = findViewById(R.id.search_results)

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        layoutManager.stackFromEnd = true
        resultsView.layoutManager = layoutManager
        resultsView.adapter = adapter

        fetchApps()

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = performSearch(s.toString())
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun fetchApps() {
        val disp = Single.fromCallable {
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            val limitApps = packageManager.queryIntentActivities(intent, 0)
            
            limitApps.map { 
                it.loadLabel(packageManager).toString() to it 
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ mapped ->
                allApps = mapped
                performSearch("")
            }, { it.printStackTrace() })
        disposables.add(disp)
    }

    private fun performSearch(query: String) {
        val disp = Single.fromCallable {
            val lower = query.lowercase().trim()
            val filtered = if (lower.isEmpty()) {
                allApps.take(10)
            } else {
                allApps.filter { it.first.lowercase().contains(lower) }.take(10)
            }
            
            val uiObjects = filtered.map { 
                SearchItem(it.first, it.second.activityInfo.packageName, it.second.loadIcon(packageManager), "app")
            }.toMutableList()

            if (lower.isNotEmpty()) {
                uiObjects.add(0, SearchItem("Search Web: \"$query\"", query, getDrawable(android.R.drawable.ic_menu_search), "web"))
            }
            uiObjects
        }.subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ uiObjects ->
                adapter.items = uiObjects
                adapter.notifyDataSetChanged()
            }, { it.printStackTrace() })
        disposables.add(disp)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}

data class SearchItem(val label: String, val intentData: String, val icon: Drawable?, val type: String)

class SearchAdapter(val onClick: (String, String) -> Unit) : RecyclerView.Adapter<SearchAdapter.VH>() {
    var items: List<SearchItem> = emptyList()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.app_name)
        val icon: ImageView = v.findViewById(R.id.app_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app_search, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.label
        holder.icon.setImageDrawable(item.icon)
        holder.itemView.setOnClickListener { onClick(item.intentData, item.type) }
    }

    override fun getItemCount(): Int = items.size
}
