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

import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * 示例页冒烟测试：浅色、深色下都能启动，扩大区域内的点击、开关和隐藏按钮生效
 *
 * @author venshine
 */
@RunWith(RobolectricTestRunner::class)
class DemoSmokeTest {

    @After
    fun tearDown() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun tap(host: View, x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        host.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0))
        host.dispatchTouchEvent(MotionEvent.obtain(now, now + 10, MotionEvent.ACTION_UP, x, y, 0))
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun counterText(activity: MainActivity) = activity.findViewById<TextView>(R.id.counter).text.toString()

    @Test
    fun launchesInLightAndNightMode() {
        listOf(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES).forEach { mode ->
            AppCompatDelegate.setDefaultNightMode(mode)
            val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
            assertEquals("Dot A: 0 / Dot B: 0", counterText(activity))
            activity.finish()
        }
    }

    @Test
    fun tapNearDotHitsItOnlyWhileExpanded() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val host = activity.findViewById<ViewGroup>(R.id.host)
        val dotA = activity.findViewById<View>(R.id.dot_a)
        // 圆点左侧 20dp 处，在 40dp 的扩大区域内
        val x = dotA.left - 20 * activity.resources.displayMetrics.density
        val y = (dotA.top + dotA.bottom) / 2f

        tap(host, x, y)
        assertEquals("Dot A: 1 / Dot B: 0", counterText(activity))

        activity.findViewById<SwitchCompat>(R.id.switch_expand).isChecked = false
        tap(host, x, y)
        assertEquals("Dot A: 1 / Dot B: 0", counterText(activity))
    }

    @Test
    fun hiddenDotNoLongerHits() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val host = activity.findViewById<ViewGroup>(R.id.host)
        val dotB = activity.findViewById<View>(R.id.dot_b)
        val x = dotB.right + 20 * activity.resources.displayMetrics.density
        val y = (dotB.top + dotB.bottom) / 2f

        activity.findViewById<Button>(R.id.button_hide_b).performClick()
        tap(host, x, y)
        assertTrue(counterText(activity).endsWith("Dot B: 0"))
    }
}
