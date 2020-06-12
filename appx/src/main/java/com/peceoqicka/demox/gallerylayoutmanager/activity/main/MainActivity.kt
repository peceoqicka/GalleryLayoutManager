package com.peceoqicka.demox.gallerylayoutmanager.activity.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.peceoqicka.demox.gallerylayoutmanager.R
import com.peceoqicka.demox.gallerylayoutmanager.activity.center.CenterScaleActivity
import com.peceoqicka.demox.gallerylayoutmanager.activity.first.FirstScaleActivity
import com.peceoqicka.demox.gallerylayoutmanager.data.NewsModel
import com.peceoqicka.demox.gallerylayoutmanager.databinding.ActivityMainBinding
import com.peceoqicka.x.gallerylayoutmanager.GalleryLayoutManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.startActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {
    private lateinit var bindModel: ViewModel
    private lateinit var dataList: ArrayList<SquareItemViewModel>
    private lateinit var binding: ActivityMainBinding
    private var localIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_main
        )
        binding.model = ViewModel().apply {
            handler = eventHandler
            layoutManager = GalleryLayoutManager.Builder()
                .setDefaultSnapHelper()
                .setExtraMargin(100)
                .setBasePosition(GalleryLayoutManager.BASE_POSITION_CENTER)
                .setTransformPosition(GalleryLayoutManager.POSITION_CENTER)
                .setCenterScale(1.2f, 1.2f)
                .setOnScrollListener(object : GalleryLayoutManager.SimpleScrollListener() {
                    override fun onIdle(snapViewPosition: Int) {
                        bindModel.selection = snapViewPosition
                    }
                })
                .build()

            bindModel = this
        }

        loadData1()
    }

    @SuppressLint("CheckResult")
    private fun loadData1() {
        Observable.create<String> {
            val inputStream = assets.open("source.json")
            val inputStreamReader = InputStreamReader(inputStream, Charset.forName("UTF-8"))
            val bufferedReader = BufferedReader(inputStreamReader)
            val text = bufferedReader.readText()
            bufferedReader.close()
            inputStreamReader.close()
            inputStream.close()
            it.onNext(text)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val m = Gson().fromJson(it, NewsModel::class.java)
                dataList = m.toItemViewModel()
                val afterList = ArrayList<SquareItemViewModel>()
                //afterList.addAll(dataList)
                afterList.addAll(dataList.subList(0, 1))
                bindModel.adapter = SquareAdapter(dataList)
                //bindModel.adapter = SquareAdapter(afterList)
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_center_scale -> {
                startActivity<CenterScaleActivity>()
            }
            R.id.action_first_scale -> {
                startActivity<FirstScaleActivity>()
            }
            R.id.action_custom_scale -> {
                //自定义缩放
                startActivity<FirstScaleActivity>(FirstScaleActivity.EXTRA_TYPE to 1)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private val eventHandler: ViewModel.EventHandler = object : ViewModel.EventHandler {
        override fun onInsert() {
            if (bindModel.targetDataPosition in (0..dataList.size)) {
                val rndDataPosition = (Math.random() * dataList.size).toInt()
                val insertData = dataList[rndDataPosition]
                val dataCopy = SquareItemViewModel().apply {
                    this.imageUrl = insertData.imageUrl
                    this.title = "[${localIndex}]${insertData.title}"
                    localIndex++
                }
                dataList.add(bindModel.targetDataPosition, dataCopy)
                //println("targetDataPosition : ${bindModel.targetDataPosition}")
                bindModel.adapter?.notifyItemInserted(bindModel.targetDataPosition)
            }
        }

        override fun onDelete() {
            if (bindModel.targetDataPosition in (0 until dataList.size)) {
                dataList.removeAt(bindModel.targetDataPosition)
                bindModel.adapter?.notifyItemRemoved(bindModel.targetDataPosition)
            }
        }

        override fun onFirstItem() {
            bindModel.targetDataPosition = 0
        }

        override fun onSecondItem() {
            bindModel.targetDataPosition = 1
        }

        override fun onLastItem() {
            bindModel.targetDataPosition = dataList.size - 1
        }

        override fun onMiddleItem() {
            if (dataList.size % 2 == 1) {
                bindModel.targetDataPosition = (dataList.size - 1) / 2
            } else {
                bindModel.targetDataPosition = dataList.size / 2 - 1
            }
        }

        override fun onScrollToItem() {
            binding.rvMainList.scrollToPosition(bindModel.targetDataPosition)
        }

        override fun onSmoothScrollToItem() {
            binding.rvMainList.smoothScrollToPosition(bindModel.targetDataPosition)
        }
    }

    class ViewModel : BaseObservable() {
        lateinit var handler: EventHandler
        lateinit var layoutManager: RecyclerView.LayoutManager

        var adapter: SquareAdapter? = null
            @Bindable
            get
            set(value) {
                field = value;notifyPropertyChanged(BR.adapter)
            }
        var selection: Int = 0
            @Bindable
            get
            set(value) {
                field = value;notifyPropertyChanged(BR.selection)
            }
        var targetDataPosition: Int = 0
            @Bindable
            get
            set(value) {
                field = value;notifyPropertyChanged(BR.targetDataPosition)
            }

        interface EventHandler {
            fun onInsert()
            fun onDelete()
            fun onFirstItem()
            fun onSecondItem()
            fun onLastItem()
            fun onMiddleItem()
            fun onScrollToItem()
            fun onSmoothScrollToItem()
        }
    }
}
