package com.peceoqicka.demox.gallerylayoutmanager.activity.center

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.peceoqicka.demox.gallerylayoutmanager.activity.main.SquareAdapter
import com.peceoqicka.demox.gallerylayoutmanager.activity.main.SquareItemViewModel
import com.peceoqicka.demox.gallerylayoutmanager.R
import com.peceoqicka.demox.gallerylayoutmanager.data.NewsModel
import com.peceoqicka.demox.gallerylayoutmanager.databinding.ActivityCenterScaleBinding
import com.peceoqicka.demox.gallerylayoutmanager.util.addSwipeSlidingBack
import com.peceoqicka.x.gallerylayoutmanager.GalleryLayoutManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

class CenterScaleActivity : AppCompatActivity() {
    private lateinit var bindModel: ViewModel
    private lateinit var dataList: ArrayList<SquareItemViewModel>
    private lateinit var binding: ActivityCenterScaleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_center_scale
        )
        binding.model = ViewModel().apply {
            layoutManager = GalleryLayoutManager.Builder()
                .setDefaultSnapHelper()
                .setExtraMargin(80)
                .setTransformPosition(GalleryLayoutManager.POSITION_CENTER)
                .setCenterScale(1.2f, 1.2f)
                .setOnScrollListener(object : GalleryLayoutManager.OnScrollListener {
                    override fun onIdle(snapViewPosition: Int) {
                        binding.sivCenterScaleIndicator.setSelectedIndex(snapViewPosition)
                    }

                    override fun onScrolling(
                        scrollingPercentage: Float,
                        fromPosition: Int,
                        toPosition: Int
                    ) {
                        binding.sivCenterScaleIndicator.updateScrollingPercentage(
                            scrollingPercentage,
                            fromPosition,
                            toPosition
                        )
                    }

                    override fun onDragging() {
                    }

                    override fun onSettling() {
                    }
                })
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
                dataList = m.toItemViewModel()
                var checkList = ArrayList<SquareItemViewModel>()
                checkList.add(dataList[0])
                bindModel.adapter = SquareAdapter(checkList)
                //binding.sivCenterScaleIndicator.setItemCount(dataList.size)
            }
    }

    class ViewModel : BaseObservable() {
        lateinit var layoutManager: RecyclerView.LayoutManager

        var adapter: SquareAdapter? = null
            @Bindable
            get
            set(value) {
                field = value;notifyPropertyChanged(BR.adapter)
            }
    }
}
