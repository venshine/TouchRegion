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
package com.wx.touchregion

import android.graphics.Rect
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup

/**
 * 扩大 View 的触摸和点击区域：把 [view] 周围一圈的触摸转发给它。
 *
 * 一个父 View 下的多个子 View 共用同一个 [TouchDelegateGroup]（系统的 `View.setTouchDelegate`
 * 只能设置一个），所以每个父 View 创建一个 [TouchRegion] 即可。
 *
 * 扩大后的区域最大不超过父 View 范围（超出的触摸到不了父 View）。
 * 区域在每次触摸时按子 View 当前位置计算，布局变化后自动跟随；子 View 隐藏（`INVISIBLE` / `GONE`）
 * 或被移出父 View 后不再命中。多个子 View 的扩大区域重叠时，先扩大的优先。
 *
 * @author venshine
 */
class TouchRegion(private val host: ViewGroup) {

    /**
     * @param view 要扩大点击区域的子 View，必须已加入父 View；等价于 `TouchRegion(view.parent)`
     */
    constructor(view: View) : this(
        view.parent as? ViewGroup
            ?: throw IllegalArgumentException("view must be attached to a parent ViewGroup."),
    )

    private val group = TouchDelegateGroup(host)
    private val delegates = HashMap<View, TouchDelegate>()

    /**
     * 扩大 [view] 的触摸和点击范围，四个方向都扩大 [margin]（px）。再次调用会替换之前的设置。
     */
    fun expandViewTouchRegion(view: View, margin: Int) {
        expandViewTouchRegion(view, margin, margin, margin, margin)
    }

    /**
     * 扩大 [view] 的触摸和点击范围，四个方向分别扩大 [left]、[top]、[right]、[bottom]（px，负数按 0 处理）。
     * 再次调用会替换之前的设置。
     *
     * @throws IllegalArgumentException [view] 不是构造时传入的父 View 的直接子 View
     */
    fun expandViewTouchRegion(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        require(view.parent === host) { "view must be a direct child of the ViewGroup given to TouchRegion." }
        delegates.remove(view)?.let { group.removeTouchDelegate(it) }
        val delegate = ExpandedTouchDelegate(
            host, view,
            left.coerceAtLeast(0), top.coerceAtLeast(0), right.coerceAtLeast(0), bottom.coerceAtLeast(0),
        )
        delegates[view] = delegate
        group.addTouchDelegate(delegate)
        if (host.touchDelegate !== group) {
            host.touchDelegate = group
        }
    }

    /**
     * 恢复 [view] 的触摸和点击范围，最小不小于 View 自身范围。没有扩大过时什么也不做。
     */
    fun restoreViewTouchRegion(view: View) {
        delegates.remove(view)?.let { group.removeTouchDelegate(it) }
        if (delegates.isEmpty() && host.touchDelegate === group) {
            host.touchDelegate = null
        }
    }
}

/**
 * 单个子 View 的扩大触摸区域。
 *
 * 系统 [TouchDelegate] 的区域在构造时固定，布局变化后会过期，也不检查 View 是否可见；
 * 这里在每次 `ACTION_DOWN` 时按 View 当前位置新建一个系统 [TouchDelegate]，后续事件交给它处理。
 */
internal class ExpandedTouchDelegate(
    private val host: ViewGroup,
    private val view: View,
    private val left: Int,
    private val top: Int,
    private val right: Int,
    private val bottom: Int,
) : TouchDelegate(Rect(), view) {

    private var current: TouchDelegate? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            current = null
            if (view.parent !== host || view.visibility != View.VISIBLE) return false
            val bounds = Rect()
            view.getHitRect(bounds)
            bounds.left -= left
            bounds.top -= top
            bounds.right += right
            bounds.bottom += bottom
            val delegate = TouchDelegate(bounds, view)
            if (!delegate.onTouchEvent(event)) return false
            current = delegate
            return true
        }
        val delegate = current ?: return false
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            current = null
        }
        return delegate.onTouchEvent(event)
    }
}
