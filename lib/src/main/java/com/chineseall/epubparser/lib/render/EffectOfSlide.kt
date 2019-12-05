package com.chineseall.epubparser.lib.render

import android.animation.Animator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import com.chineseall.epubparser.lib.util.LogUtil
import kotlin.math.abs

/**
 * 水平滑动切换页面
 */
class EffectOfSlide(context: Context) {

    var effectWidth = 0
    var effectHeight = 0
    var curPageBitmap: Bitmap? = null
    var preORnextPageBitmap: Bitmap? = null
    var velocityTracker = VelocityTracker.obtain()
    var longClickTime = 0
    var touchSlop = 0
    var effectReceiver: EffectReceiver? = null

    var downTime: Long = -1L
    var downX: Float = -1f
    // 滑动是否生效
    var isMoveStart = false
    // 开始滑动起始位置
    var startMoveX: Float = -1f
    // 初始滑动向量 决定页面加载方向
    var startMoveVector: Float = 0f
    // 当前滑动向量
    var curMoveVector: Float = 0f
    // 当前页偏移距离 由向量curMoveVector累加得到
    var curPageOffset = 0f
    // 页面加载是否成功
    var loadSuccess = false
    var anim_rate = 0f
    var scrollAnim: ValueAnimator? = null

    init {
        val viewConfiguration = ViewConfiguration.get(context)
        longClickTime = ViewConfiguration.getLongPressTimeout()
        touchSlop = viewConfiguration.scaledPagingTouchSlop
    }

    fun config(
        effectWidth: Int,
        effectHeight: Int,
        curPageBitmap: Bitmap?,
        preORnextPageBitmap: Bitmap?
    ) {
        this.effectWidth = effectWidth
        this.effectHeight = effectHeight
        this.curPageBitmap = curPageBitmap
        this.preORnextPageBitmap = preORnextPageBitmap
        this.anim_rate = effectWidth / 600f
    }

    /**
     * 自动翻页
     */
    fun autoTurnPage(pre: Boolean) {
        // 只绘制当前页
        resetData()
        effectReceiver?.drawCurPage()
        effectReceiver?.invalidate()
        var toX = 0f
        if (pre) {
            curMoveVector = 0.1f
            loadSuccess = effectReceiver?.drawPrePage() ?: false
            if (loadSuccess) {
                toX = effectWidth.toFloat()
                effectReceiver?.toPrePage()
                scrollMoveVector(0f, toX)
            }
        } else {
            curMoveVector = -0.1f
            loadSuccess = effectReceiver?.drawNextPage() ?: false
            if (loadSuccess) {
                toX = -effectWidth.toFloat()
                effectReceiver?.toNextPage()
                scrollMoveVector(0f, toX)
            }
        }
    }

    fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            if (event.pointerCount > 1) {
                return false
            }
            val touchX = event.x
            val touchY = event.y
            velocityTracker.addMovement(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (scrollAnim?.isRunning == true) {
                        // 结束滚动动画
                        scrollAnim?.cancel()
                    }
                    resetData()
                    // 只绘制当前页
                    effectReceiver?.drawCurPage()
                    effectReceiver?.invalidate()
                    downX = touchX
                    downTime = System.currentTimeMillis()
                }
                MotionEvent.ACTION_MOVE -> {
                    val disFromDown = touchX - downX
                    if (abs(disFromDown) > touchSlop) {
                        if (!isMoveStart) {
                            // 记录初始滑动位置 初始滑动向量
                            isMoveStart = true
                            startMoveX = touchX
                            startMoveVector = startMoveX - downX
                            // 根据初始向量决定加载哪一页
                            if (startMoveVector > 0) {
                                LogUtil.d("意图加载上一页")
                                loadSuccess = effectReceiver?.drawPrePage() ?: false
                            } else if (startMoveVector < 0) {
                                LogUtil.d("意图加载下一页")
                                loadSuccess = effectReceiver?.drawNextPage() ?: false
                            }
                        }
                        if (isMoveStart) {
                            curMoveVector = touchX - startMoveX
                            if (loadSuccess && curMoveVector * startMoveVector > 0) {
                                // 表示单向加载页面
                                curPageOffset = +curMoveVector
                                effectReceiver?.invalidate()
                            }
                        }
                    }
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    // 得到抬起时刻滑动速度
                    if (loadSuccess) {
                        velocityTracker.computeCurrentVelocity(100)
                        val xMoveVelocity = velocityTracker.xVelocity
                        if (startMoveVector * xMoveVelocity > 0 || abs(curPageOffset) > effectWidth / 4f) {
                            // 初始滑动方向和抬起时刻方向相同、当前页偏移超过阈值 按初始滑动方向决定翻页
                            if (startMoveVector > 0) {
                                LogUtil.d("滑动到上一页 $startMoveVector $curMoveVector $xMoveVelocity")
                                scrollMoveVector(curMoveVector, effectWidth.toFloat())
                                effectReceiver?.toPrePage()
                            } else if (startMoveVector < 0) {
                                LogUtil.d("滑动到下一$startMoveVector $curMoveVector $xMoveVelocity")
                                scrollMoveVector(curMoveVector, -effectWidth.toFloat())
                                effectReceiver?.toNextPage()
                            }
                        } else {
                            // 否则回退到当前页
                            LogUtil.d("回到当前页  $startMoveVector $curMoveVector $xMoveVelocity")
                            scrollMoveVector(curPageOffset, 0f)
                        }
                    } else if (!isMoveStart) {
                        if (System.currentTimeMillis() - downTime > longClickTime) {
                            // 长按
                            effectReceiver?.onPageLongClick(touchX, touchY)
                        } else {
                            // 点击
                            effectReceiver?.onPageClick(touchX, touchY)
                        }
                    }
                }
            }
            return true
        } else {
            return false
        }
    }

    private fun scrollMoveVector(from: Float, to: Float) {
        val animDiff = abs(to - from)
        scrollAnim = ValueAnimator.ofFloat(from, to)
        scrollAnim?.interpolator = DecelerateInterpolator()
        scrollAnim?.duration = (animDiff / anim_rate).toLong()
        scrollAnim?.setEvaluator(object : TypeEvaluator<Float> {
            override fun evaluate(fraction: Float, startValue: Float?, endValue: Float?): Float {
                val tmp = startValue!! + fraction * (endValue!! - startValue!!)
                curMoveVector = tmp
                effectReceiver?.invalidate()
                return tmp
            }
        })
        scrollAnim?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(p0: Animator?) {

            }

            override fun onAnimationEnd(p0: Animator?) {
                LogUtil.d("动画结束")
            }

            override fun onAnimationCancel(p0: Animator?) {
                LogUtil.d("动画取消")
            }

            override fun onAnimationStart(p0: Animator?) {

            }
        })
        scrollAnim?.start()
    }

    fun resetData() {
        downX = -1f
        isMoveStart = false
        startMoveX = -1f
        startMoveVector = 0f
        curPageOffset = 0f
        curMoveVector = 0f
        loadSuccess = false
    }

    fun onDraw(canvas: Canvas?) {
        drawCurPage(canvas)
        drawPreORnextPage(canvas)
    }

    private fun drawCurPage(canvas: Canvas?) {
        curPageBitmap?.let {
            canvas?.save()
            canvas?.drawBitmap(it, curMoveVector, 0f, null)
            canvas?.restore()
        }
    }

    private fun drawPreORnextPage(canvas: Canvas?) {
        preORnextPageBitmap?.let {
            if (curMoveVector > 0) {
                canvas?.save()
                canvas?.drawBitmap(it, -effectWidth + curMoveVector, 0f, null)
                canvas?.restore()
            } else if (curMoveVector < 0) {
                canvas?.save()
                canvas?.drawBitmap(it, effectWidth + curMoveVector, 0f, null)
                canvas?.restore()
            } else {

            }
        }
    }
}