# GalleryLayoutManager

![Bintray](https://img.shields.io/badge/JCenter-v1.0.0-blue.svg)
![MinSdk](https://img.shields.io/badge/MinSdk-19-green)

> GalleryLayoutManager, which can scroll infinitely, is designed for advertisement view. You can use **ItemDecorations** to style items.

## Features
- [x] android x  compatible
- [x] infinite scrolling
- [x] select item like how a viewpager does
- [x] scale item
- [ ] item's alpha change（developing...）

## Preview



**Sorry, I forgot to translate the preview images, you can download demo to run  yourself, I add English translation in the demo**



| 1 | 2 | 3 |
| :----------------------------------------------------------: | :--: | :--: |
| <img src="/previews/glm01_ItemAnimations.gif" alt="animation support" style="zoom:45%;" /> |<img src="/previews/glm02_Infinite.gif" alt="infinite scrolling" style="zoom:45%;" />|<img src="/previews/glm03_centerScale.gif" alt="center item scale" style="zoom:45%;" />|
| animation support                                                     |infinite scrolling|center item scale|
| <img src="/previews/glm04_firstScale.gif" alt="first item scale" style="zoom:45%;" /> | <img src="/previews/glm05_customScale.gif" alt="custom item scale" style="zoom:45%;" /> |      |
| first item scale | custom item scale |      |

## DEMO

clone this project，or download [demo-apk](https://github.com/peceoqicka/GalleryLayoutManager/blob/master/app/release/app-release.apk), [demo-apk-androidx](https://github.com/peceoqicka/GalleryLayoutManager/blob/master/appx/release/appx-release.apk)

## How to use

add implementations in **app**'s **build.gradle** file：
```groovy
implementation 'com.peceoqicka:gallerylayoutmanager:1.0.0'
```
or **Android X** version（only package name changed, not work with legacy project）:
```groovy
implementation 'com.peceoqicka:gallerylayoutmanagerx:1.0.0'
```
then call this in java:
```java
GalleryLayoutManager layoutManager = new GalleryLayoutManager.Builder()
	.setSnapHelper(new PagerSnapHelper())
	.setInfinityMode(true)
	.setLayoutInCenter(true)
	.setTransformPosition(GalleryLayoutManager.POSITION_CENTER)
	.setCenterScale(1.2f, 1.2f)
	.build();
	
mRecyclerView.setLayoutManager(layoutManager)
```
It's better to use **GalleryLayoutManager.Builder**，or use **constructors** directly。

### Infinite scrolling

```java
new GalleryLayoutManager.Builder()
	.setInfinityMode(true)
	.build();
```

### First item layout in center

```java
new GalleryLayoutManager.Builder()
	.setInfinityMode(true)
	.setLayoutInCenter(true)
	.build();
```
When first-layout, the 0th item will be moved to the center of visible area.(Only works in infinity-mode)

### Transformation position

Transformation-Position is position that item transforms. For example, we invoke `Builder.setCenterScale` to set the target scale value to **1.2**, and transform position is set to `POSITION_CENTER`, in this case, item whose center location reaches the center of the visible area(ordinarly half of the recyclerview's width) should be scale to **1.2**.

Because of the default scale value is 1, there is scale-change range, suppose this: one item's pivotX is growing up from **left threshold** to **center threshold**, at the same time, scale value is growing up from **1** to **1.2**; one item's pivotX is growing up from **center threshold** to **right threshold**, scale value is dropping down from **1.2** to **1**.

If you want to customize transform range, use `setCustomizedTransformPosition(centerX, leftCenterX, rightCenterX)` to set  the transform range, the params are **center threshold**, **left threshold** and **right threshold**.

For now, there's 5 kinds of transform position：

| Variable Name              | Value   | Description                                                         |
| ------------------- | ---- | ------------------------------------------------------------ |
| POSITION_NONE       | 0    | none transformations                                                   |
| POSITION_CENTER     | 1    | item located at center of the visible area transforms to target value, should also call `Builder.setLayoutInCenter(true)` |
| POSITION_START      | 2    | first item located at left of the visible area transforms to target value                                     |
| POSITION_END        | 3    | last item located at right of the visible area transforms to target value                                   |
| POSITION_CUSTOMIZED | 4    | custom transform position, should also call `Builder.setCustomizedTransformPosition`|

### SnapHelper and item-selection

invoke `setSnapHelper`，if not, you cannot get the callback of scrolling：

```java
new GalleryLayoutManager.Builder()
	.setSnapHelper(new LinearSnapHelper())
	.build();
```

It's better to use default snaphelper，call `setDefaultSnapHelper` to use：

```java
new GalleryLayoutManager.Builder()
	.setDefaultSnapHelper()
    .setOnScrollListener(new GalleryLayoutManager.OnScrollListener() {
        @Override
        public void onIdle(int snapViewPosition) {
        	//the position of selected item
        }

        @Override
        public void onScrolling(float scrollingPercentage, int fromPosition, int toPosition) {
			//scrolling position is between previous position and next position
        }

        @Override
        public void onDragging() {

        }

        @Override
        public void onSettling() {

        }
    })
	.build();
```

`onIdle` is called after scrolling complete, you can get selection-position here; `onScrolling` is called when scrolling(drag), **fromPosition** and **toPosition** are **Adapter Position**.