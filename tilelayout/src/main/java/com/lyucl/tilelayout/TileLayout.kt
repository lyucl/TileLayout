package com.lyucl.tilelayout

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.OverScroller
import android.widget.RadioGroup
import kotlin.math.abs
import kotlin.math.max


/**
 * 一个支持子View先水平平铺，当水平方向放不下子View后，会另起一行继续水平平铺的容器
 * 继承自RadioGroup，方便配合RadioButton进行开发
 *
 * @author: LCL
 * @date: 2021/6/11
 */
open class TileLayout(context: Context?, var attrs: AttributeSet? = null) :
    RadioGroup(context, attrs) {

    private var mSelfWidth = 0// 自己实际的宽
    private var mSelfHeight = 0// 自己实际的高
    private var mInterceptTouchEvent = false// 是否拦截Touch事件
    private lateinit var mOverScroller: OverScroller// 滑动控制类
    private var mVelocityTracker: VelocityTracker? = null// 系统速度追踪器
    private var mMaxFlintVelocity = 0// 系统最大的滑动速度

    private var mDownX = 0f// down点X
    private var mDownY = 0f// down点Y
    private var mLastX = 0f// 上次触摸时的X值
    private var mLastY = 0f// 上次触摸时的Y值

    var verticalSpacing = 0// 每个item纵向间距
    var horizontalSpacing = 0// 每个item横向间距

    init {
        initView()
    }

    /**
     * 初始化View
     */
    private fun initView() {
        mOverScroller = OverScroller(context)
        mVelocityTracker = VelocityTracker.obtain()

        val viewConfiguration = ViewConfiguration.get(context)
        mMaxFlintVelocity = viewConfiguration.scaledMaximumFlingVelocity

        handleAttrs()
    }

    private fun handleAttrs() {
        attrs?.apply {
            val obtainStyledAttributes =
                context.obtainStyledAttributes(this, R.styleable.TileLayout)
            verticalSpacing = obtainStyledAttributes.getDimensionPixelOffset(
                R.styleable.TileLayout_verticalSpacing,
                0
            )
            horizontalSpacing = obtainStyledAttributes.getDimensionPixelOffset(
                R.styleable.TileLayout_horizontalSpacing,
                0
            )
            obtainStyledAttributes.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 获得此ViewGroup上级容器为其推荐的宽和高，以及计算模式
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        // 自己的最大宽度
        var maxWidth = 0
        // 自己的高
        var height = 0
        // 每一行最大高度
        var lineHeight = 0
        // 记录当前childView的左侧的位置
        var childLeft = paddingLeft
        // 记录当前childView的上侧的位置
        var childTop = paddingTop
        // 遍历子View
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            // 测量child的宽和高
            measureChild(childView, widthMeasureSpec, heightMeasureSpec)
            // 获取测量后的子View的宽度
            val childWidth = childView.measuredWidth
            // 获取测量后的子View的高度
            val childHeight = childView.measuredHeight
            // 如果加入当前childView，超出最大宽度，则将目前最大宽度给width，类加height 然后开启新行
            if (childWidth + childLeft + paddingRight > widthSize) {
                maxWidth = max(maxWidth, childLeft)
                childLeft = paddingLeft + childWidth + horizontalSpacing// 重新开启新行，开始记录childLeft
                childTop += verticalSpacing + lineHeight // 叠加当前的高度
                lineHeight = childHeight
            } else {
                // 否则累加当前childView的宽度
                childLeft += childWidth + horizontalSpacing
                maxWidth = max(maxWidth, childLeft)
                lineHeight = max(childHeight, lineHeight) // 取最大值
            }
        }
        height += childTop + lineHeight + paddingBottom
        maxWidth += paddingRight

        mSelfWidth = maxWidth
        mSelfHeight = height

        val dimensionWidth =
            if (widthMode == MeasureSpec.EXACTLY) {
                mSelfWidth = widthSize
                widthSize
            } else {
                maxWidth
            }
        val dimensionHeight =
            if (heightMode == MeasureSpec.EXACTLY) {
                mSelfHeight = heightSize
                heightSize
            } else {
                height
            }
        // 设置自己的宽高
        setMeasuredDimension(dimensionWidth, dimensionHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        var childLeft = paddingLeft
        var childTop = paddingTop
        var lineHeight = 0
        //遍历所有childView根据其宽和高，计算子控件应该出现的位置
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            if (childView.visibility == GONE) {
                continue
            }
            val childWidth = childView.measuredWidth
            val childHeight = childView.measuredHeight
            // 如果已经需要换行
            if (childLeft + childWidth + paddingRight > width) {
                childLeft = paddingLeft
                childTop += verticalSpacing + lineHeight
                lineHeight = childHeight
            } else {
                lineHeight = max(childHeight, lineHeight)
            }
            childView.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)
            childLeft += childWidth + horizontalSpacing
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return if (mInterceptTouchEvent) true else super.onInterceptTouchEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mInterceptTouchEvent = false
                mVelocityTracker?.addMovement(event)
                mDownX = x
                mDownY = y
                mLastX = x
                mLastY = y
                if (!mOverScroller.isFinished) {
                    mOverScroller.abortAnimation()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // 计算第一个Move时点与Down点的距离差
                val diffDownX: Float = abs(x - mDownX)
                val diffDownY: Float = abs(y - mDownY)
                // 当拖动值大于5时，此时应该滚动，拦截子控件的事件
                if (diffDownX > 5 || diffDownY > 5) {
                    mInterceptTouchEvent = true
                }
                val dx = x - mLastX
                val dy = y - mLastY
                mVelocityTracker?.addMovement(event)
                scrollBy(-dx.toInt(), -dy.toInt())
                mLastX = x
                mLastY = y
            }
            MotionEvent.ACTION_UP -> {
                // 手指抬起，计算当前速率
                mVelocityTracker?.computeCurrentVelocity(500, mMaxFlintVelocity.toFloat())
                val xVelocity = mVelocityTracker?.xVelocity?.toInt() ?: 0
                val yVelocity = mVelocityTracker?.yVelocity?.toInt() ?: 0
                mOverScroller.fling(
                    scrollX,
                    scrollY,
                    if (mSelfWidth > getVisibleWidth()) -xVelocity else 0,// 如果View的实际宽度大于可见宽度，此时可以左右滑动
                    if (mSelfHeight > getVisibleHeight()) -yVelocity else 0,// 如果View的实际高度大于可见高度，此时可以上下滑动
                    0,
                    mSelfWidth,
                    0,
                    mSelfHeight
                )
                invalidate()
                mVelocityTracker?.clear()
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return true
    }

    override fun computeScroll() {
        super.computeScroll()
        if (mOverScroller.computeScrollOffset()) {
            scrollTo(mOverScroller.currX, mOverScroller.currY)
            invalidate()
        }
    }

    /**
     * 重写scrollTo方法，避免滑动越界
     */
    override fun scrollTo(oriX: Int, oriY: Int) {
        var x = oriX
        var y = oriY
        // 如果滑动的值越左边界，x赋值0
        if (x < 0) {
            x = 0
        }
        // 如果滑动的值越右边界，x赋值可滑动的最大值
        if (x > mSelfWidth - getVisibleWidth()) {
            x = mSelfWidth - getVisibleWidth()
        }
        // 如果滑动的值越上边界，y赋值0
        if (y < 0) {
            y = 0
        }
        // 如果滑动的y值越下边界，y赋值可滑动的最大值
        if (y > mSelfHeight - getVisibleHeight()) {
            y = mSelfHeight - getVisibleHeight()
        }
        super.scrollTo(x, y)
    }

    /**
     * 是否滑动到顶部
     */
    private fun isSlideToLeft(): Boolean {
        return scrollX <= 0
    }

    /**
     * 是否滑动到底部
     */
    private fun isSlideToRight(): Boolean {
        return getVisibleWidth() + scrollX >= mSelfWidth
    }

    /**
     * 是否滑动到顶部
     */
    private fun isSlideToTop(): Boolean {
        return scrollY <= 0
    }

    /**
     * 是否滑动到底部
     */
    private fun isSlideToBottom(): Boolean {
        return getVisibleHeight() + scrollY >= mSelfHeight
    }

    /**
     * 获取View可见区域的宽度
     */
    private fun getVisibleWidth(): Int {
        val rect = Rect()
        getLocalVisibleRect(rect)
        return rect.width()
    }

    /**
     * 获取View可见区域的高度
     */
    private fun getVisibleHeight(): Int {
        val rect = Rect()
        getLocalVisibleRect(rect)
        return rect.height()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mVelocityTracker?.recycle()
        mVelocityTracker = null
    }
}