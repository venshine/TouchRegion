/*
 * Copyright (C) 2016 venshine.cn@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wx.touchregion.demo

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout

/**
 * 示例专用的父 View：把 TouchRegion 扩大后的热区画出来，并给每次按下画一个反馈圈
 *
 * - 热区由 [zones] 在每次绘制时现算，状态只存在调用方，这里不缓存矩形。
 * - 反馈圈画在手指落点：命中（调用过 [markHit]）为绿色，否则为灰色，约 [MARKER_MS] 毫秒淡出。
 *
 * @author venshine
 */
class ZoneLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    /** 当前要画的热区（父 View 坐标系）；隐藏的子 View 不要返回 */
    var zones: () -> List<RectF> = { emptyList() }

    private class Marker(val x: Float, val y: Float, val start: Long) {
        var hit = false
    }

    private val markers = ArrayList<Marker>()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = context.colorOf(R.color.accent_zone)
    }
    private val zoneStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dp(1.5f).toFloat()
        color = context.colorOf(R.color.accent_ink)
        pathEffect = DashPathEffect(floatArrayOf(context.dp(5f).toFloat(), context.dp(4f).toFloat()), 0f)
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = context.dp(2f).toFloat()
    }

    init {
        setWillNotDraw(false)
    }

    /** [view] 的命中范围向四周扩大 [margin] px，并裁到本 View 范围内（超出的触摸到不了父 View） */
    fun rectOf(view: View, margin: Int): RectF {
        val hit = Rect()
        view.getHitRect(hit)
        return RectF(
            (hit.left - margin).coerceAtLeast(0).toFloat(),
            (hit.top - margin).coerceAtLeast(0).toFloat(),
            (hit.right + margin).coerceAtMost(width).toFloat(),
            (hit.bottom + margin).coerceAtMost(height).toFloat(),
        )
    }

    /** 把最近一次按下标记为命中（由目标 View 的点击回调调用） */
    fun markHit() {
        markers.lastOrNull()?.hit = true
        invalidate()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            markers.add(Marker(ev.x, ev.y, SystemClock.uptimeMillis()))
            if (markers.size > MAX_MARKERS) markers.removeAt(0)
            invalidate()
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchDraw(canvas: Canvas) {
        val radius = context.dp(8f).toFloat()
        for (zone in zones()) {
            canvas.drawRoundRect(zone, radius, radius, fillPaint)
            canvas.drawRoundRect(zone, radius, radius, zoneStroke)
        }
        super.dispatchDraw(canvas)
        drawMarkers(canvas)
    }

    private fun drawMarkers(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        markers.removeAll { now - it.start >= MARKER_MS }
        for (marker in markers) {
            val t = (now - marker.start) / MARKER_MS.toFloat()
            val color = context.colorOf(if (marker.hit) R.color.hit else R.color.miss)
            val alpha = ((1f - t) * 255).toInt()
            markerPaint.color = (color and 0x00FFFFFF) or (alpha shl 24)
            markerPaint.style = Paint.Style.STROKE
            canvas.drawCircle(marker.x, marker.y, context.dp(10f) + context.dp(18f) * t, markerPaint)
            markerPaint.style = Paint.Style.FILL
            canvas.drawCircle(marker.x, marker.y, context.dp(3f).toFloat(), markerPaint)
        }
        if (markers.isNotEmpty()) postInvalidateDelayed(FRAME_MS)
    }

    private companion object {
        const val MARKER_MS = 900L
        const val FRAME_MS = 33L
        const val MAX_MARKERS = 8
    }
}
