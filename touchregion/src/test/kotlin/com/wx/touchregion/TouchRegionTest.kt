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

import android.app.Activity
import android.graphics.Rect
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * TouchRegion 测试：在真实的父子 View 上派发触摸事件，校验命中、不命中和多个子 View 互不干扰
 *
 * 布局（父 View 400x400，坐标相对父 View）：
 * A 在 (50,50)-(90,90)，B 在 (250,50)-(290,90)，都是 40x40；默认给 A、B 各扩大 30。
 *
 * @author venshine
 */
@RunWith(RobolectricTestRunner::class)
class TouchRegionTest {

    private lateinit var activity: Activity
    private lateinit var host: FrameLayout
    private lateinit var a: View
    private lateinit var b: View
    private var clicksA = 0
    private var clicksB = 0

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        host = FrameLayout(activity)
        a = addChild(50, 50) { clicksA++ }
        b = addChild(250, 50) { clicksB++ }
        activity.setContentView(host, FrameLayout.LayoutParams(400, 400))
        relayout()
    }

    private fun addChild(x: Int, y: Int, onClick: () -> Unit): View {
        val view = View(activity)
        view.isClickable = true
        view.setOnClickListener { onClick() }
        host.addView(view, FrameLayout.LayoutParams(40, 40).apply { leftMargin = x; topMargin = y })
        return view
    }

    private fun relayout() {
        host.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY))
        host.layout(0, 0, 400, 400)
    }

    private fun tap(x: Float, y: Float) {
        // 先跑完排队的任务，保证不依赖 expand / restore 是同步还是异步生效
        shadowOf(Looper.getMainLooper()).idle()
        val now = SystemClock.uptimeMillis()
        host.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0))
        host.dispatchTouchEvent(MotionEvent.obtain(now, now + 10, MotionEvent.ACTION_UP, x, y, 0))
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun region(): TouchRegion = TouchRegion(host).also {
        it.expandViewTouchRegion(a, 30)
        it.expandViewTouchRegion(b, 30)
    }

    @Test
    fun withoutRegionTapOutsideViewMisses() {
        tap(35f, 70f)
        assertEquals(0, clicksA)
        tap(70f, 70f)
        assertEquals(1, clicksA)
    }

    @Test
    fun tapInsideExpandedAreaHits() {
        region()
        tap(35f, 70f)   // A 左侧 15
        tap(70f, 115f)  // A 下方 25
        tap(115f, 30f)  // A 右上角外侧，在 (20..120, 20..120) 内
        assertEquals(3, clicksA)
        assertEquals(0, clicksB)
    }

    @Test
    fun tapOutsideExpandedAreaMisses() {
        region()
        tap(15f, 70f)    // A 左侧 35 > 30
        tap(70f, 125f)   // A 下方 35 > 30
        tap(130f, 70f)   // A 右侧 40
        assertEquals(0, clicksA)
        assertEquals(0, clicksB)
    }

    @Test
    fun tapOnViewItselfStillWorks() {
        region()
        tap(70f, 70f)
        assertEquals(1, clicksA)
    }

    @Test
    fun asymmetricMarginsAreHonored() {
        TouchRegion(host).expandViewTouchRegion(a, 100, 0, 0, 0)
        tap(-0f + 1f, 70f)    // A 左侧 49，在 100 内
        tap(70f, 45f)         // 上方 5，top=0 不扩大
        assertEquals(1, clicksA)
    }

    @Test
    fun siblingsDoNotInterfere() {
        region()
        tap(235f, 70f)   // B 左侧 15
        assertEquals(0, clicksA)
        assertEquals(1, clicksB)
        tap(35f, 70f)    // A 左侧 15
        assertEquals(1, clicksA)
        assertEquals(1, clicksB)
        tap(180f, 70f)   // 两个扩大区域之间的空隙
        assertEquals(1, clicksA)
        assertEquals(1, clicksB)
    }

    @Test
    fun hiddenViewNoLongerHitsAndComesBack() {
        region()
        a.visibility = View.INVISIBLE
        tap(35f, 70f)
        a.visibility = View.GONE
        tap(35f, 70f)
        assertEquals(0, clicksA)
        a.visibility = View.VISIBLE
        relayout()
        tap(35f, 70f)
        assertEquals(1, clicksA)
        // B 不受 A 影响
        tap(235f, 70f)
        assertEquals(1, clicksB)
    }

    @Test
    fun removedViewNoLongerHits() {
        region()
        host.removeView(a)
        tap(35f, 70f)
        assertEquals(0, clicksA)
        tap(235f, 70f)
        assertEquals(1, clicksB)
    }

    @Test
    fun restoreOnlyAffectsThatView() {
        val region = region()
        region.restoreViewTouchRegion(a)
        tap(35f, 70f)
        assertEquals(0, clicksA)
        tap(235f, 70f)
        assertEquals(1, clicksB)
        // 恢复后可以再次扩大
        region.expandViewTouchRegion(a, 30)
        tap(35f, 70f)
        assertEquals(1, clicksA)
    }

    @Test
    fun restoreAllClearsHostDelegate() {
        val region = region()
        assertTrue(host.touchDelegate != null)
        region.restoreViewTouchRegion(a)
        region.restoreViewTouchRegion(b)
        assertNull(host.touchDelegate)
        // 没扩大过的 View 恢复不报错
        region.restoreViewTouchRegion(a)
    }

    @Test
    fun expandingAgainReplacesTheEarlierMargin() {
        val region = TouchRegion(host)
        region.expandViewTouchRegion(a, 30)
        region.expandViewTouchRegion(a, 10)
        tap(35f, 70f)   // 距 15，超出新的 10
        assertEquals(0, clicksA)
        tap(45f, 70f)   // 距 5
        assertEquals(1, clicksA)
    }

    @Test
    fun regionFollowsLayoutChanges() {
        region()
        a.layoutParams = (a.layoutParams as FrameLayout.LayoutParams).apply { leftMargin = 150; topMargin = 250 }
        relayout()
        tap(35f, 70f)    // 旧位置
        assertEquals(0, clicksA)
        tap(135f, 270f)  // 新位置左侧 15
        assertEquals(1, clicksA)
    }

    @Test
    fun dragOutAfterDownDoesNotClick() {
        region()
        val now = SystemClock.uptimeMillis()
        host.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 35f, 70f, 0))
        host.dispatchTouchEvent(MotionEvent.obtain(now, now + 10, MotionEvent.ACTION_MOVE, 380f, 380f, 0))
        host.dispatchTouchEvent(MotionEvent.obtain(now, now + 20, MotionEvent.ACTION_UP, 380f, 380f, 0))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, clicksA)
    }

    @Test
    fun constructorWithViewUsesItsParent() {
        val region = TouchRegion(a)
        region.expandViewTouchRegion(a, 30)
        tap(35f, 70f)
        assertEquals(1, clicksA)
        assertSame(host, a.parent)
    }

    @Test
    fun viewWithoutParentIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { TouchRegion(View(activity)) }
        val region = TouchRegion(host)
        assertThrows(IllegalArgumentException::class.java) { region.expandViewTouchRegion(View(activity), 10) }
    }

    // TouchDelegateGroup 本身的分发规则

    private class RecordingDelegate(view: View, private val claims: Rect) : TouchDelegate(Rect(), view) {
        val actions = ArrayList<Int>()
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN && !claims.contains(event.x.toInt(), event.y.toInt())) return false
            actions.add(event.action)
            return true
        }
    }

    private fun event(action: Int, x: Float, y: Float) = MotionEvent.obtain(0, 0, action, x, y, 0)

    @Test
    fun groupRoutesAWholeGestureToTheDelegateThatClaimedDown() {
        val group = TouchDelegateGroup(host)
        val first = RecordingDelegate(a, Rect(0, 0, 100, 100))
        val second = RecordingDelegate(b, Rect(200, 0, 300, 100))
        group.addTouchDelegate(first)
        group.addTouchDelegate(second)

        assertTrue(group.onTouchEvent(event(MotionEvent.ACTION_DOWN, 250f, 50f)))
        assertTrue(group.onTouchEvent(event(MotionEvent.ACTION_MOVE, 10f, 10f)))
        assertTrue(group.onTouchEvent(event(MotionEvent.ACTION_UP, 10f, 10f)))
        assertEquals(emptyList<Int>(), first.actions)
        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP), second.actions)
        // 手势结束后不再转发
        assertFalse(group.onTouchEvent(event(MotionEvent.ACTION_MOVE, 10f, 10f)))
    }

    @Test
    fun groupOverlapGoesToTheFirstAddedAndRemoveClearClaims() {
        val group = TouchDelegateGroup(host)
        val first = RecordingDelegate(a, Rect(0, 0, 100, 100))
        val second = RecordingDelegate(b, Rect(0, 0, 100, 100))
        group.addTouchDelegate(first)
        group.addTouchDelegate(second)
        assertTrue(group.onTouchEvent(event(MotionEvent.ACTION_DOWN, 50f, 50f)))
        group.onTouchEvent(event(MotionEvent.ACTION_UP, 50f, 50f))
        assertEquals(2, first.actions.size)
        assertEquals(0, second.actions.size)

        group.removeTouchDelegate(first)
        assertTrue(group.onTouchEvent(event(MotionEvent.ACTION_DOWN, 50f, 50f)))
        assertEquals(1, second.actions.size)

        group.clearTouchDelegates()
        // 清空时丢弃正在进行的手势
        assertFalse(group.onTouchEvent(event(MotionEvent.ACTION_UP, 50f, 50f)))
        assertFalse(group.onTouchEvent(event(MotionEvent.ACTION_DOWN, 50f, 50f)))
    }
}
