package com.chineseall.epubparser.lib.render

import android.animation.Animator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import com.chineseall.epubparser.lib.util.LogUtil
import kotlin.math.abs

class EffectOfCover(context: Context) {
    var effectWidth = 0
    var effectHeight = 0
    var curPageBitmap: Bitmap? = null
    var preORnextPageBitmap: Bitmap? = null
    var velocityTracker = VelocityTracker.obtain()
    var longClickTime = 0
    var touchSlop = 0
    var effectReceiver: EffectReceiver? = null
    val shadowPaint = Paint()
    val shadowWidth = 10f

    var downX: Float = -1f
    // 滑动是否生效
    var isMoveStart = false
    // 开始滑动起始位置
    var startMoveX: Float = -1f
    // 初始滑动向量 决定页面加载方向
    var startMoveVector: Float = 0f
    // 当前滑动向量
    var curMoveVector: Float = 0f
    // 页面加载是否成功
    var loadSuccess = false
    var anim_rate = 0f
    var scrollAnim: ValueAnimator? = null
    // 两页分界位置
    var curDivideX = -1f
    // 滑向终点位置
    var slideTargetX = -1f

    var moveDiff = 0f

    init {
        val viewConfiguration = ViewConfiguration.get(context)
        longClickTime = ViewConfiguration.getLongPressTimeout()
        touchSlop = viewConfiguration.scaledPagingTouchSlop
        shadowPaint.isAntiAlias = true
        shadowPaint.shader = LinearGradient(
            0f,
            0f,
            shadowWidth,
            0f,
            0x55111111,
            0x00111111,
            Shader.TileMode.MIRROR
        )
        shadowPaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.XOR))
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

    fun autoTurnPage(pre: Boolean) {
        resetData()
        effectReceiver?.drawCurPage()
        effectReceiver?.invalidate()
        var divideFrom = -1f
        var divideTo = -1f
        if (pre) {
            startMoveVector = 0.1f
            divideFrom = 0f
            divideTo = effectWidth.toFloat()
            loadSuccess = effectReceiver?.drawPrePage() ?: false
            if (loadSuccess) {
                effectReceiver?.toPrePage()
                scroll(divideFrom, divideTo)
            }
        } else {
            startMoveVector = -0.1f
            divideFrom = effectWidth.toFloat()
            divideTo = -shadowWidth
            loadSuccess = effectReceiver?.drawNextPage() ?: false
            if (loadSuccess) {
                effectReceiver?.toNextPage()
                scroll(divideFrom, divideTo)
            }
        }
    }

    fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            if (event.pointerCount > 1) {
                return false
            }
            val touchX = event.x
            velocityTracker.addMovement(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (scrollAnim?.isRunning == true) {
                        // 结束滚动动画
                        scrollAnim?.cancel()
                    }
                    downX = touchX
                    resetData()
                    effectReceiver?.drawCurPage()
                    effectReceiver?.invalidate()
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
                                slideTargetX = effectWidth.toFloat()
                            } else if (startMoveVector < 0) {
                                LogUtil.d("意图加载下一页")
                                loadSuccess = effectReceiver?.drawNextPage() ?: false
                                slideTargetX = 0f - shadowWidth
                            }
                        }
                        if (isMoveStart) {
                            curMoveVector = touchX - startMoveX
                            if (loadSuccess) {
                                if (startMoveVector > 0) {
                                    // 上一页覆盖在当前页上
                                    if (touchX == curDivideX) {
                                        LogUtil.d("junk")
                                    }
                                    curDivideX = touchX
                                    moveDiff = curDivideX
                                    effectReceiver?.invalidate()
                                } else if (startMoveVector < 0 && curMoveVector < 0) {
                                    // 当前页覆盖在下一页上
                                    curDivideX = effectWidth + curMoveVector
                                    moveDiff = curMoveVector
                                    effectReceiver?.invalidate()
                                }
                            }
                        }
                    }
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    if (loadSuccess) {
                        velocityTracker.computeCurrentVelocity(100)
                        val xMoveVelocity = velocityTracker.xVelocity
                        if (startMoveVector * xMoveVelocity > 0 || abs(moveDiff) > effectWidth / 4f) {
                            if (startMoveVector > 0) {
                                effectReceiver?.toPrePage()
                            } else {
                                effectReceiver?.toNextPage()
                            }
                            scroll(curDivideX, slideTargetX)
                        } else {
                            LogUtil.d("回退到当前页")
                            scroll(
                                curDivideX,
                                if (startMoveVector > 0) 0f - shadowWidth else effectWidth.toFloat()
                            )
                        }
                    }
                }
            }
            return true
        } else {
            return false
        }
    }

    private fun scroll(from: Float, to: Float) {
        val animDiff = abs(to - from)
        scrollAnim = ValueAnimator.ofFloat(from, to)
        scrollAnim?.interpolator = DecelerateInterpolator()
        scrollAnim?.duration = (animDiff / anim_rate).toLong()
        scrollAnim?.setEvaluator(object : TypeEvaluator<Float> {
            override fun evaluate(fraction: Float, startValue: Float?, endValue: Float?): Float {
                val tmp = startValue!! + fraction * (endValue!! - startValue!!)
                curDivideX = tmp
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
        startMoveX = -1f
        startMoveVector = 0f
        curMoveVector = 0f
        loadSuccess = false
        isMoveStart = false
        moveDiff = 0f
    }

    fun onDraw(canvas: Canvas?) {
        drawCurPage(canvas)
        drawPreORnextPage(canvas)
        if (startMoveVector != 0f) {
            drawShadow(canvas)
        }
    }

    private fun drawCurPage(canvas: Canvas?) {
        curPageBitmap?.let {
            if (startMoveVector > 0) {
                canvas?.save()
                canvas?.clipRect(curDivideX, 0f, effectWidth.toFloat(), effectHeight.toFloat())
                canvas?.drawBitmap(it, 0f, 0f, null)
                canvas?.restore()
            } else if (startMoveVector < 0) {
                canvas?.save()
                canvas?.drawBitmap(it, curDivideX - effectWidth, 0f, null)
                canvas?.restore()
            } else {
                canvas?.save()
                canvas?.drawBitmap(it, 0f, 0f, null)
                canvas?.restore()
            }
        }
    }

    private fun drawPreORnextPage(canvas: Canvas?) {
        preORnextPageBitmap?.let {
            if (startMoveVector > 0) {
                canvas?.save()
                canvas?.drawBitmap(it, curDivideX - effectWidth, 0f, null)
                canvas?.restore()
            } else if (startMoveVector < 0) {
                canvas?.save()
                canvas?.clipRect(curDivideX, 0f, effectWidth.toFloat(), effectHeight.toFloat())
                canvas?.drawBitmap(it, 0f, 0f, null)
                canvas?.restore()
            } else {

            }
        }
    }

    private fun drawShadow(canvas: Canvas?) {
        canvas?.save()
        canvas?.translate(curDivideX, 0f)
        canvas?.drawRect(
            0f,
            0f,
            shadowWidth,
            effectHeight.toFloat(),
            shadowPaint
        )
        canvas?.restore()
    }


}