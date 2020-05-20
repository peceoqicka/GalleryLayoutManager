package com.peceoqicka.demox.gallerylayoutmanager.activity.first

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.peceoqicka.demox.gallerylayoutmanager.R
import com.peceoqicka.demox.gallerylayoutmanager.data.NewsModel
import com.peceoqicka.demox.gallerylayoutmanager.databinding.ActivityFirstScaleBinding
import com.peceoqicka.demox.gallerylayoutmanager.util.addSwipeSlidingBack
import com.peceoqicka.demox.gallerylayoutmanager.util.toList
import com.peceoqicka.x.gallerylayoutmanager.GalleryLayoutManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.dimen
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

class FirstScaleActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_TYPE = "FirstScaleActivity.EXTRA_TYPE"
    }

    //0表示第一项缩放，1表示自定义缩放
    private var type: Int = 0
    private lateinit var bindModel: ViewModel
    private lateinit var dataList: ArrayList<ItemFirstScaleViewModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = intent.getIntExtra(EXTRA_TYPE, 0)

        val binding = DataBindingUtil.setContentView<ActivityFirstScaleBinding>(
            this,
            R.layout.activity_first_scale
        )
        binding.model = ViewModel().apply {
            itemDecoration = FirstScaleItemDecoration(
                dimen(R.dimen.px_90),
                dimen(R.dimen.px_90),
                dimen(R.dimen.px_70)
            )
            layoutManager =
                if (type == 1)
                    GalleryLayoutManager.Builder()
                        .setDefaultSnapHelper()
                        .setTransformPosition(GalleryLayoutManager.POSITION_CUSTOMIZED)
                        .setCustomizedTransformPosition(
                            dimen(R.dimen.px_540),
                            0,
                            dimen(R.dimen.px_810)
                        )
                        .setCenterScale(1.3f, 1.3f)
                        .setOnScrollListener(mOnScrollListener)
                        .build()
                else
                    GalleryLayoutManager.Builder()
                        .setDefaultSnapHelper()
                        .setTransformPosition(GalleryLayoutManager.POSITION_START)
                        .setCenterScale(1.2f, 1.2f)
                        .setOnScrollListener(mOnScrollListener)
                        .build()

            bindModel = this
        }
        loadData1()
        addSwipeSlidingBack()
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
                dataList = m.toList { i ->
                    ItemFirstScaleViewModel().apply {
                        this.imageUrl = i.imageUrl
                        this.title = i.title
                    }
                }
                bindModel.adapter = FirstScaleAdapter(dataList)
            }
    }

    private val mOnScrollListener = object : GalleryLayoutManager.OnScrollListener {
        override fun onIdle(snapViewPosition: Int) {
            bindModel.selection = snapViewPosition
        }

        override fun onScrolling(scrollingPercentage: Float, fromPosition: Int, toPosition: Int) {
        }

        override fun onDragging() {
        }

        override fun onSettling() {
        }

    }

    class ViewModel : BaseObservable() {
        lateinit var layoutManager: RecyclerView.LayoutManager
        lateinit var itemDecoration: RecyclerView.ItemDecoration

        var adapter: FirstScaleAdapter? = null
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
    }
}
