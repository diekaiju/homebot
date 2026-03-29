package com.abast.homebot

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class QuickSearchActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var resultsView: RecyclerView
    private var mode: String? = null
    private val adapter = SearchAdapter { result, type ->
        when (type) {
            "web" -> {
                val url = "https://www.google.com/search?q=${java.net.URLEncoder.encode(result, "UTF-8")}"
                val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(browserIntent)
                finish()
            }
            "calc" -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Calculation Result", result)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied result: $result", Toast.LENGTH_SHORT).show()
                return@SearchAdapter // don't finish yet, let user copy or continue
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

        mode = intent.getStringExtra("mode")
        searchInput = findViewById(R.id.search_input)
        
        if (mode == "calc") {
            searchInput.hint = getString(R.string.calc_hint)
            searchInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            findViewById<ImageView>(R.id.search_icon).setImageResource(R.drawable.ic_calculator)
        } else {
            searchInput.hint = getString(R.string.search_hint)
        }

        searchInput.post {
            searchInput.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
        resultsView = findViewById(R.id.search_results)
        
        val spanCount = 4
        val layoutManager = GridLayoutManager(this, spanCount)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val type = adapter.items.getOrNull(position)?.type
                return if (type == "web" || type == "calc") {
                    spanCount
                } else {
                    1
                }
            }
        }
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
                val mathResult = evaluateMath(lower)
                if (mathResult != null) {
                    uiObjects.add(0, SearchItem("Result: $mathResult", mathResult, getDrawable(R.drawable.ic_calculator), "calc"))
                }
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

    private fun evaluateMath(query: String): String? {
        val clean = query.replace(" ", "")
        if (clean.isEmpty() || !clean.any { it in "0123456789" }) return null
        if (!clean.all { it in "0123456789.+-*/^()" }) return null
        
        return try {
            object : Any() {
                var pos = -1
                var ch = 0

                fun nextChar() {
                    ch = if (++pos < clean.length) clean[pos].toInt() else -1
                }

                fun eat(charToEat: Int): Boolean {
                    while (ch == ' '.toInt()) nextChar()
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): Double {
                    nextChar()
                    val x = parseExpression()
                    if (pos < clean.length) throw RuntimeException()
                    return x
                }

                fun parseExpression(): Double {
                    var x = parseTerm()
                    while (true) {
                        if (eat('+'.toInt())) x += parseTerm()
                        else if (eat('-'.toInt())) x -= parseTerm()
                        else return x
                    }
                }

                fun parseTerm(): Double {
                    var x = parseFactor()
                    while (true) {
                        if (eat('*'.toInt())) x *= parseFactor()
                        else if (eat('/'.toInt())) x /= parseFactor()
                        else return x
                    }
                }

                fun parseFactor(): Double {
                    if (eat('+'.toInt())) return parseFactor()
                    if (eat('-'.toInt())) return -parseFactor()
                    var x: Double
                    val startPos = pos
                    if (eat('('.toInt())) {
                        x = parseExpression()
                        eat(')'.toInt())
                    } else if (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == '.'.toInt()) {
                        while (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == '.'.toInt()) nextChar()
                        x = clean.substring(startPos, pos).toDouble()
                    } else {
                        throw RuntimeException()
                    }
                    if (eat('^'.toInt())) x = Math.pow(x, parseFactor())
                    return x
                }
            }.parse().let {
                if (it.isInfinite() || it.isNaN()) "Error"
                else if (it == it.toLong().toDouble()) it.toLong().toString()
                else String.format("%.4f", it).trimEnd('0').trimEnd('.')
            }
        } catch (e: Exception) {
            null
        }
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
        
        // Dynamic UI adjustment based on item type
        val layout = holder.itemView as LinearLayout
        if (item.type == "web" || item.type == "calc") {
            layout.orientation = LinearLayout.HORIZONTAL
            layout.gravity = Gravity.CENTER_VERTICAL
            layout.setPadding(32, 16, 32, 16)
            holder.name.apply {
                gravity = Gravity.START
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setPadding(16, 0, 0, 0)
            }
            holder.icon.layoutParams.apply {
                width = 32.px
                height = 32.px
            }
        } else {
            layout.orientation = LinearLayout.VERTICAL
            layout.gravity = Gravity.CENTER
            layout.setPadding(8, 8, 8, 8)
            holder.name.apply {
                gravity = Gravity.CENTER
                textSize = 12f
                setTypeface(null, Typeface.NORMAL)
                setPadding(0, 0, 0, 0)
            }
            holder.icon.layoutParams.apply {
                width = 56.px
                height = 56.px
            }
        }
        
        holder.itemView.setOnClickListener { onClick(item.intentData, item.type) }
    }

    private val Int.px: Int
        get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()

    override fun getItemCount(): Int = items.size
}
