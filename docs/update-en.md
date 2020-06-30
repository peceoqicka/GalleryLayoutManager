# Update Logs
## 1.0.6

### BUGs

* Fix a problem that scroll to left when data length is 2, and each ItemView's width can fill up visible area.

## 1.0.5

### New Features

* Add **Force-To-Scroll-To-Right-Mode**, when call **smoothScrollTo**, it will be forced to scroll to right, even if the target  view is in the left of the visible area. This mode is defaultly open

## 1.0.4

### BUGs

* Fix a problem when datalist has only one data, calling **smoothScrollTo** has no effect

## 1.0.3

### New Features

* Add **Force To Scroll To Right** mode, when call smoothScrollTo method, scroll to right (default open)

## 1.0.2

### New Features

* When items are not enough to fill the visible area, duplicated items will be added;
* Add **Layout Base Position**, replace of **setLayoutInCenter**;
* Add **extra margin**, replace of the way adding margins in xml file or ItemDecoration;

## 1.0.1

### BUGs

* Fix a problem that recyclerview won't scroll when fills a empty list in  at first time;