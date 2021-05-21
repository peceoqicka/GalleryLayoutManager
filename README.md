# GalleryLayoutManager - 画廊布局管理器

![JitPack](https://img.shields.io/badge/JitPack-v1.0.0-blue)
![MinSdk](https://img.shields.io/badge/MinSdk-19-green)

> GalleryLayoutManager是为轮播控件设计的支持无限循环滑动的画廊布局管理器，支持ItemDecoration定制装饰样式。	

## 注意

这是旧版本，后续的维护更新将主要以新项目[GalleryLayoutManagerX](https://github.com/peceoqicka/GalleryLayoutManagerX)为中心，**此项目将逐渐停止维护**，目前仍会修复少量BUG。由于`JCenter即将关闭`，遂重新发布到[JitpackIO](https://www.jitpack.io/)。

## 特点
- [x] Kotlin编写
- [x] 无限循环
- [x] 自动补充重复Item以满足无限循环
- [x] 支持仿ViewPager选中效果
- [x] 支持按比例缩放Item和透明度变化
- [x] 支持完全自定义Item变化（ViewTransformListener）

##  效果预览

<img src="/previews/glm_show.gif" alt="效果预览" style="zoom:30%;" />

## DEMO

克隆此项目运行，或下载[demo-apk](https://github.com/peceoqicka/GalleryLayoutManager/blob/master/app/release/app-release.apk)

## 使用方法
在**root**的**build.gradle**中添加：

```kotlin
allprojects {
    repositories {
        //...
        maven { url 'https://jitpack.io' }//引用Jitpack IO仓库
    }
}
```

在**app**的**build.gradle**中添加依赖：
```groovy
implementation 'com.github.peceoqicka:GalleryLayoutManager:1.0.0'
```

**你需要添加的额外的依赖库：**
```groovy
implementation 'com.android.support:design:28.0.0'
```
代码调用即可
```kotlin
bannerLayoutManager = GalleryLayoutManager.create {
    itemSpace = 120
    onScrollListener = mOnScrollListener
    viewTransformListener = SimpleViewTransformListener(1.2f, 1.2f)
}
	
mRecyclerView.layoutManager = layoutManager
```
推荐使用**GalleryLayoutManager.Builder**，也可以直接使用**构造方法**。

### 首项居中

```java
//默认为BASE_POSITION_CENTER，可以不用调用
//选择BASE_POSIITON_START会将布局起始点设置为左边
basePosition = GalleryLayoutManager.BASE_POSITION_CENTER
```
在第一次布局的时候，将原本的第0项Item移动到可视区域正中间。

### ViewTransformListener

```kotlin
viewTransformListener = SimpleViewTransformListener(1.2f, 1.2f)
```
库中已经提供基础的`缩放实现类SimpleViewTransformListener`，可满足基本需求，如需定制可自行实现`ViewTransformListener`

### SnapHelper与选中Item

调用`setSnapHelper`传入需要的SnapHelper，默认为`GalleryLayoutManager.GallerySnapHelper`，交互效果同`ViewPager`，即滑动翻页单张。如果不在这里传入将无法使用选中Item的回调：

```kotlin
snapHelper = LinearSnapHelper()
//snapHelper = null 在此置空将禁用选中效果，且变形和滑动回调均会失效
```

### 滑动回调

```kotlin
GalleryLayoutManager.create {
    onScrollListener = object: OnScrollListener() {
        @Override
        public void onIdle(int snapViewPosition) {
			//选中的Item的位置，在这里做通知Indicator改变选中位置等操作
        }

        @Override
        public void onScrolling(float scrollingPercentage, int fromPosition, int toPosition) {
			//滑动的监听，从当前选中位置到下一个位置的百分比变化
        }
    })
}
```

`onIdle`方法在滚动停止后回调，返回当前选中的Item；`onScrolling`方法在滚动时回调，返回当前所处的位置，参数的**fromPosition**和**toPosition**都是在适配器中的位置，即**Adapter Position**。