# GalleryLayoutManager - 画廊布局管理器

![Bintray](https://img.shields.io/badge/JCenter-v1.0.2-blue)
![MinSdk](https://img.shields.io/badge/MinSdk-19-green)

> GalleryLayoutManager是为轮播控件设计的支持无限循环滑动的画廊布局管理器，支持ItemDecoration定制装饰样式。	

[English](/README-EN.md)

## 特点
- [x] 支持Android X
- [x] 无限循环（1.0.2版本仅支持无限循环）
- [x] 自动补充重复Item以满足无限循环
- [x] 支持仿ViewPager选中效果（设置默认的SnapHelper即可，详情请参考下方说明）
- [x] 支持按比例缩放Item
- [ ] 支持透明度变化（开发中）

## 更新日志

[更新日志](/docs/update.md)

## 样式演示

| 1 | 2 | 3 |
| :----------------------------------------------------------: | :--: | :--: |
| <img src="/previews/glm01_ItemAnimations.gif" alt="动画支持" style="zoom:45%;" /> |<img src="/previews/glm02_Infinite.gif" alt="无限循环" style="zoom:45%;" />|<img src="/previews/glm03_centerScale.gif" alt="中间项缩放" style="zoom:45%;" />|
| 动画支持                                                     |无限循环|中间项缩放|
| <img src="/previews/glm04_firstScale.gif" alt="第一项缩放" style="zoom:45%;" /> | <img src="/previews/glm05_customScale.gif" alt="自定义缩放" style="zoom:45%;" /> |      |
| 第一项缩放 | 自定义缩放 |      |

## DEMO

克隆此项目运行，或下载[demo-apk](https://github.com/peceoqicka/GalleryLayoutManager/blob/master/app/release/app-release.apk)，[demo-apk-androidx](https://github.com/peceoqicka/GalleryLayoutManager/blob/master/appx/release/appx-release.apk)

## 使用方法

在**app**的**build.gradle**中添加依赖：
```groovy
implementation 'com.peceoqicka:gallerylayoutmanager:1.0.2'
```
**Android X**版本（仅对包名做更改，不能与旧版本兼容）：
```groovy
implementation 'com.peceoqicka:gallerylayoutmanagerx:1.0.2'
```
**你需要添加的额外的依赖库：**
```groovy
//旧版本
implementation 'com.android.support:design:28.0.0'
//AndroidX
implementation 'com.google.android.material:material:1.1.0'
```
Java调用即可
```java
GalleryLayoutManager layoutManager = new GalleryLayoutManager.Builder()
	.setSnapHelper(new PagerSnapHelper())
	.setBasePosition(GalleryLayoutManager.BASE_POSITION_CENTER)
	.setTransformPosition(GalleryLayoutManager.POSITION_CENTER)
	.setCenterScale(1.2f, 1.2f)
	.build();
	
mRecyclerView.setLayoutManager(layoutManager)
```
推荐使用**GalleryLayoutManager.Builder**，也可以直接使用**构造方法**。

### 无限循环

```java
new GalleryLayoutManager.Builder()
	.build();
```

### 首项居中

```java
new GalleryLayoutManager.Builder()
	.setBasePosition(GalleryLayoutManager.BASE_POSITION_CENTER)
    //默认为BASE_POSITION_CENTER，可以不用调用
    //选择BASE_POSIITON_START会将布局起始点设置为左边
	.build();
```
在第一次布局的时候，将原本的第0项Item移动到可视区域正中间。

### 变形位置

变形位置即Item产生变形的位置，例如调用`Builder.setCenterScale`设置了目标缩放值为**1.2**，且变形位置设置为`POSITION_CENTER`，即会将**Item中心坐标**到达可视区域中间时（通常为RecyclerView的宽度的一半处），将此Item缩放为1.2。

由于默认的缩放值为1，由此可以产生一个缩放值变化的范围，假定Item中心点从**左边阈值**向**中心阈值**变化，缩放值则会从1逐渐增大到1.2；Item中心点从**中间阈值**向**右边阈值**变化，缩放值则会从1.2逐渐减小到1。

以自定义变形范围为例，调用这个方法`setCustomizedTransformPosition(centerX, leftCenterX, rightCenterX)`设置自定义范围，传入的参数依次为**中心阈值**、**左边阈值**、**右边阈值**。

目前的变形位置有5种：

| 常量名              | 值   | 说明                                                         |
| ------------------- | ---- | ------------------------------------------------------------ |
| POSITION_NONE       | 0    | 不进行变形                                                   |
| POSITION_CENTER     | 1    | 可视区域中间的Item按照设置的目标变化值进行变化，需搭配`Builder.setLayoutInCenter(true)`使用 |
| POSITION_START      | 2    | 可视区域中第一个Item变化                                     |
| POSITION_END        | 3    | 可视区域中最后一个Item变化                                   |
| POSITION_CUSTOMIZED | 4    | 自定义变形范围，需搭配`Builder.setCustomizedTransformPosition`使用 |

### SnapHelper与选中Item

调用`setSnapHelper`传入需要的SnapHelper，不在这里传入将无法使用选中Item的回调：

```java
new GalleryLayoutManager.Builder()
	.setSnapHelper(new LinearSnapHelper())
	.build();
```

推荐使用默认的SnapHelper，调用`setDefaultSnapHelper`即可：

```java
new GalleryLayoutManager.Builder()
	.setDefaultSnapHelper()
    .setOnScrollListener(new GalleryLayoutManager.SimpleScrollListener() {
        @Override
        public void onIdle(int snapViewPosition) {
			//选中的Item的位置，在这里做通知Indicator改变选中位置等操作
        }

        @Override
        public void onScrolling(float scrollingPercentage, int fromPosition, int toPosition) {
			//滑动的监听，从当前选中位置到下一个位置的百分比变化
        }
    })
	.build();
```

`onIdle`方法在滚动停止后回调，返回当前选中的Item；`onScrolling`方法在滚动时回调，返回当前所处的位置，参数的**fromPosition**和**toPosition**都是在适配器中的位置，即**Adapter Position**。