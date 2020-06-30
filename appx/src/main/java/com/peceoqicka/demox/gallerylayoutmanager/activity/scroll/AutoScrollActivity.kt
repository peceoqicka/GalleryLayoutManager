package com.peceoqicka.demox.gallerylayoutmanager.activity.scroll

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.peceoqicka.demox.gallerylayoutmanager.BR
import com.peceoqicka.demox.gallerylayoutmanager.R
import com.peceoqicka.demox.gallerylayoutmanager.activity.center.CenterScaleActivity
import com.peceoqicka.demox.gallerylayoutmanager.activity.first.FirstScaleActivity
import com.peceoqicka.demox.gallerylayoutmanager.activity.main.MainActivity
import com.peceoqicka.demox.gallerylayoutmanager.activity.main.SquareAdapter
import com.peceoqicka.demox.gallerylayoutmanager.activity.main.SquareItemViewModel
import com.peceoqicka.demox.gallerylayoutmanager.data.NewsModel
import com.peceoqicka.demox.gallerylayoutmanager.databinding.ActivityAutoScrollBinding
import com.peceoqicka.demox.gallerylayoutmanager.util.addSwipeSlidingBack
import com.peceoqicka.x.gallerylayoutmanager.GalleryLayoutManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.startActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class AutoScrollActivity : AppCompatActivity() {
    private lateinit var bindModel: ViewModel
    private lateinit var dataList: ArrayList<ItemViewModel>
    private lateinit var binding: ActivityAutoScrollBinding
    private var localIndex: Int = 0
    private var mDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auto_scroll)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_auto_scroll
        )
        binding.model = ViewModel().apply {
            layoutManager = GalleryLayoutManager.Builder()
                .setDefaultSnapHelper()
                .setBasePosition(GalleryLayoutManager.BASE_POSITION_CENTER)
                .setOnScrollListener(object : GalleryLayoutManager.SimpleScrollListener() {
                    override fun onIdle(snapViewPosition: Int) {
                        bindModel.selection = snapViewPosition
                    }
                })
                .build()
            //layoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.HORIZONTAL, false)

            bindModel = this
        }

        binding.cvActivityAutoScroll.viewTreeObserver.addOnGlobalLayoutListener {
            val fixedWidth = binding.cvActivityAutoScroll.width
            val lp = binding.rvActivityMainList.layoutParams
            if (lp.width != fixedWidth) {
                lp.width = fixedWidth
                binding.rvActivityMainList.layoutParams = lp
            }
        }

        loadData()
        addSwipeSlidingBack()
    }

    @SuppressLint("CheckResult")
    private fun loadData() {
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
                val sourceList = m.toModelList()
                dataList = ArrayList()
                //dataList.addAll(sourceList.subList(0, 2))
                dataList.addAll(sourceList)
                bindModel.adapter = ItemAdapter(dataList)
                startAutoScroll()
            }
    }

    private fun NewsModel.toModelList(): ArrayList<ItemViewModel> {
        val list = arrayListOf<ItemViewModel>()
        data.mapTo(list) { item ->
            ItemViewModel().apply {
                this.imageUrl = item.imageUrl
                this.title = item.title
            }
        }
        return list
    }

    override fun onDestroy() {
        mDisposable?.run {
            dispose()
        }
        super.onDestroy()
    }

    private var mTime: Int = 0;
    private var mIsBannerRunning = true;

    private fun startAutoScroll() {
        mDisposable = Observable.interval(1000, 1000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { _ ->
                if (!mIsBannerRunning) {
                    return@subscribe
                }
                if (mTime >= 3) {
                    mTime = 0
                    val selectedPosition: Int = bindModel.scrollPosition
                    val bannerAdapter: ItemAdapter? = bindModel.adapter
                    if (bannerAdapter != null) {
                        var newPosition = selectedPosition + 1
                        if (newPosition == bannerAdapter.itemCount) {
                            newPosition = 0
                        }
                        bindModel.scrollPosition = newPosition
                    }
                }
                mTime++
            }
    }

    class ViewModel : BaseObservable() {
        lateinit var layoutManager: RecyclerView.LayoutManager

        var adapter: ItemAdapter? = null
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
        var scrollPosition: Int = 0
            @Bindable
            get
            set(value) {
                field = value;notifyPropertyChanged(BR.scrollPosition)
            }

    }
}
