# TileLayout
一款按水平垂直方向平铺子View的容器

支持子View先水平平铺，当水平方向放不下子View后，会另起一行继续水平平铺的容器。

控件超过容器时支持滚动。

## Usage
Step 1. Add the JitPack repository to your build file
```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```
Step 2. Add the dependency
```
dependencies {
	 implementation 'com.github.lyucl:TileLayout:v1.0.0'
}
```
