# TileLayout

一款按水平垂直方向平铺子View的容器

支持子View先水平平铺，当水平方向放不下子View后，会另起一行继续水平平铺的容器。

控件超过容器时支持滚动。

## Usage

Step 1. Add the JitPack repository to your build file

```
allprojects {
    repositories {
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

Step 3
```
<com.lyucl.tilelayout.TileLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:horizontalSpacing="10dp"
    app:verticalSpacing="10dp">
<com.lyucl.tilelayout.TileLayout/>


horizontalSpacing - 水平间距
verticalSpacing   - 垂直间距

```
