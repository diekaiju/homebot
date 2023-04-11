package com.abast.homebot.pickers

import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abast.homebot.R
import com.abast.homebot.databinding.ActivityRecyclerviewBinding

abstract class BasePickerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecyclerviewBinding
    lateinit var adapter : ActivityInfoAdapter
    var headerItem : ActivityInfo? = null

    abstract fun onItemClick(item: ActivityInfo)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecyclerviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ActivityInfoAdapter(this) {
            onItemClick(it)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this,RecyclerView.VERTICAL,false)
        binding.recyclerView.adapter = adapter
    }

    fun setLoading(enabled: Boolean){
        binding.progressBar.visibility = if(enabled) View.VISIBLE else View.INVISIBLE
    }

    fun setHeader(item : ActivityInfo){
        headerItem = item
        binding.headerTitle.visibility = View.VISIBLE
        binding.header.root.visibility = View.VISIBLE
        binding.header.label.text = item.loadLabel(packageManager)
        binding.header.subtitle.text = getString(R.string.default_activity)
        binding.header.image.setImageDrawable(item.loadIcon(packageManager))
        binding.header.root.setOnClickListener{ onItemClick(item) }
    }

    fun setListItems(items : Array<ActivityInfo>){
        adapter.items = items
        adapter.notifyDataSetChanged()

        if(items.isEmpty()){
            binding.emptyText.visibility = View.VISIBLE
        }else{
            binding.emptyText.visibility = View.GONE
        }
    }

}
