package com.peceoqicka.demo.gallerylayoutmanager.activity.main

import android.annotation.SuppressLint
import android.databinding.BaseObservable
import android.databinding.Bindable
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.android.databinding.library.baseAdapters.BR
import com.peceoqicka.demo.gallerylayoutmanager.R
import com.peceoqicka.demo.gallerylayoutmanager.databinding.ActivityMainBinding
import com.peceoqicka.demo.gallerylayoutmanager.util.appMoshi
import com.peceoqicka.demo.gallerylayoutmanager.util.readTextFromAssets
import com.peceoqicka.gallerylayoutmanager.GalleryLayoutManager
import com.peceoqicka.gallerylayoutmanager.OnScrollListener
import com.peceoqicka.gallerylayoutmanager.SimpleViewTransformListener
import com.squareup.moshi.Types
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    private lateinit var bindModel: ViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var mSupplyList: MutableList<SquareItemViewModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_main
        )
        binding.model = ViewModel().apply {
            handler = eventHandler
            layoutManager = GalleryLayoutManager.create {
                itemSpace = 60
                viewTransformListener = SimpleViewTransformListener(1.2f, 1.2f)
                onScrollListener = object : OnScrollListener {
                    override fun onIdle(snapViewPosition: Int) {
                        bindModel.selection = snapViewPosition
                    }
                }
            }
            numberLayoutManager =
                LinearLayoutManager(this@MainActivity, RecyclerView.HORIZONTAL, false)

            bindModel = this
        }

        loadData1()
    }

    @SuppressLint("CheckResult")
    private fun loadData1() {
        Observable.create<String> {
            it.onNext(readTextFromAssets("source.json"))
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val list = appMoshi.adapter<MutableList<SquareItemViewModel>>(
                    Types.newParameterizedType(
                        MutableList::class.java,
                        SquareItemViewModel::class.java
                    )
                ).fromJson(it)
                list?.let { dataList ->
                    if (dataList.isNotEmpty()) {
                        val numberList = arrayListOf<NumberAdapter.ItemViewModel>()
                        dataList.mapIndexedTo(numberList, { index, _ ->
                            NumberAdapter.ItemViewModel().apply {
                                number = index
                            }
                        })
                        bindModel.numberAdapter = NumberAdapter(numberList).apply {
                            simpleOnItemClick = { m ->
                                bindModel.targetDataPosition = m.number
                            }
                        }
                        bindModel.adapter = SquareAdapter(dataList)
                    }
                }
            }
        Observable.create<String> {
            it.onNext(readTextFromAssets("source2.json"))
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val list = appMoshi.adapter<MutableList<SquareItemViewModel>>(
                    Types.newParameterizedType(
                        MutableList::class.java,
                        SquareItemViewModel::class.java
                    )
                ).fromJson(it)
                list?.let { dataList ->
                    mSupplyList = dataList
                }
            }
    }

    private val eventHandler: ViewModel.EventHandler = object : ViewModel.EventHandler {
        override fun onItemClick(v: View, model: ViewModel) {
            when (v.id) {
                R.id.btn_main_insert_item -> {
                    if (this@MainActivity::mSupplyList.isInitialized) {
                        if (bindModel.targetDataPosition in (0..mSupplyList.size)) {
                            val rndDataPosition = (Math.random() * mSupplyList.size).toInt()
                            val insertData = mSupplyList[rndDataPosition]
                            val dataCopy = SquareItemViewModel().apply {
                                this.imageUrl = insertData.imageUrl
                                this.title = insertData.title
                            }
                            bindModel.adapter?.addData(bindModel.targetDataPosition, dataCopy)
                            bindModel.numberAdapter?.addNumber()
                        }
                    }
                }
                R.id.btn_main_delete_item -> {
                    if (this@MainActivity::mSupplyList.isInitialized) {
                        bindModel.adapter?.removeDataAt(bindModel.targetDataPosition)
                        bindModel.numberAdapter?.removeNumber()
                    }
                }
                R.id.btn_main_scroll_to_item -> {
                    binding.rvMainList.scrollToPosition(bindModel.targetDataPosition)
                }
                R.id.btn_main_smooth_scroll_to_item -> {
                    binding.rvMainList.smoothScrollToPosition(bindModel.targetDataPosition)
                }
            }
        }
    }

    class ViewModel : BaseObservable() {
        lateinit var handler: EventHandler
        lateinit var layoutManager: RecyclerView.LayoutManager
        lateinit var numberLayoutManager: RecyclerView.LayoutManager

        @get:Bindable
        var adapter: SquareAdapter? = null
            set(value) {
                field = value;notifyPropertyChanged(BR.adapter)
            }

        @get:Bindable
        var numberAdapter: NumberAdapter? = null
            set(value) {
                field = value;notifyPropertyChanged(BR.numberAdapter)
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
            fun onItemClick(v: View, model: ViewModel) {

            }
        }
    }
}
