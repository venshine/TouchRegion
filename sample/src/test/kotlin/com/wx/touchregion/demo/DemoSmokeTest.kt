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

import android.app.Activity
import android.content.ClipboardManager
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import java.time.Duration

/**
 * 示例页冒烟测试：每个页面在浅色、深色下都能启动，在父 View 上派发真实触摸事件校验热区交互
 *
 * @author venshine
 */
@RunWith(RobolectricTestRunner::class)
class DemoSmokeTest {

    private val controllers = mutableListOf<ActivityController<*>>()

    @After
    fun tearDown() {
        // 销毁启动过的 Activity，停掉首页的循环任务
        controllers.forEach { it.destroy() }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun idle(millis: Long = 1000) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))
    }

    private fun <T : Activity> launch(type: Class<T>): T {
        val controller = Robolectric.buildActivity(type)
        controllers += controller
        val activity = controller.setup().get()
        idle()
        return activity
    }

    /** 在父 View 的坐标 (x, y) 处按下再抬起 */
    private fun tap(host: View, x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        host.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0))
        host.dispatchTouchEvent(MotionEvent.obtain(now, now + 10, MotionEvent.ACTION_UP, x, y, 0))
        idle(100)
    }

    /** 点在 [view] 右边缘外 [gapDp] 处（垂直居中），即只有扩大后的热区才能命中的位置 */
    private fun tapBesideRight(host: View, view: View, gapDp: Float) {
        val density = host.resources.displayMetrics.density
        tap(host, view.right + gapDp * density, (view.top + view.bottom) / 2f)
    }

    private fun tapInside(host: View, view: View) = tap(host, (view.left + view.right) / 2f, (view.top + view.bottom) / 2f)

    private fun text(activity: Activity, id: Int) = activity.findViewById<TextView>(id).text.toString()

    private val screens = listOf(
        MainActivity::class.java, ZoneActivity::class.java, RowActivity::class.java, HideActivity::class.java,
    )

    @Test
    fun allScreensLaunchInLightMode() {
        screens.forEach { type ->
            assertFalse("${type.simpleName} 应为浅色", DemoTheme.isNight(launch(type)))
        }
    }

    @Test
    fun allScreensLaunchInDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        screens.forEach { type ->
            assertTrue("${type.simpleName} 应为深色", DemoTheme.isNight(launch(type)))
        }
    }

    @Test
    fun themeToggleSwitchesToDark() {
        val activity = launch(MainActivity::class.java)
        activity.findViewById<View>(R.id.theme_toggle).performClick()
        idle()
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun homeShowsVersionAndCopiesDependency() {
        val activity = launch(MainActivity::class.java)
        assertEquals("v2.0.0", text(activity, R.id.version_chip))
        activity.findViewById<View>(R.id.copy_dependency).performClick()
        val clip = activity.getSystemService(ClipboardManager::class.java).primaryClip!!
        assertEquals("implementation 'com.github.venshine:TouchRegion:2.0.0'", clip.getItemAt(0).text.toString())
    }

    @Test
    fun homeCardsOpenDemoPages() {
        val activity = launch(MainActivity::class.java)
        mapOf(
            R.id.card_zone to ZoneActivity::class.java,
            R.id.card_row to RowActivity::class.java,
            R.id.card_hide to HideActivity::class.java,
        ).forEach { (id, target) ->
            activity.findViewById<View>(id).performClick()
            assertEquals(target.name, shadowOf(activity).nextStartedActivity.component!!.className)
        }
    }

    @Test
    fun homePreviewHotZoneAlwaysExpanded() {
        val activity = launch(MainActivity::class.java)
        val host = activity.findViewById<View>(R.id.preview_zone)
        val dot = activity.findViewById<View>(R.id.preview_dot_a)
        assertEquals("点击 0 次", text(activity, R.id.preview_count))
        // 呼吸范围最小 10dp，点在圆点外 8dp 处任何时刻都应命中
        repeat(3) {
            idle(700)
            tapBesideRight(host, dot, 8f)
        }
        assertEquals("点击 3 次", text(activity, R.id.preview_count))
    }

    @Test
    fun zoneSwitchAndRangeChangeHits() {
        val activity = launch(ZoneActivity::class.java)
        val host = activity.findViewById<View>(R.id.zone_host)
        val dot = activity.findViewById<View>(R.id.target_dot)
        tapBesideRight(host, dot, 20f)            // 默认扩大 24dp，命中
        assertEquals("1", text(activity, R.id.count_text))

        activity.findViewById<SeekBar>(R.id.margin_seek).progress = 10
        tapBesideRight(host, dot, 20f)            // 缩到 10dp，不再命中
        assertEquals("1", text(activity, R.id.count_text))
        assertEquals("10 dp", text(activity, R.id.margin_text))
        tapBesideRight(host, dot, 6f)
        assertEquals("2", text(activity, R.id.count_text))

        activity.findViewById<SwitchCompat>(R.id.expand_switch).isChecked = false
        tapBesideRight(host, dot, 6f)             // 关闭扩大，只有圆点自身能点
        assertEquals("2", text(activity, R.id.count_text))
        assertFalse(activity.findViewById<View>(R.id.margin_seek).isEnabled)
        tapInside(host, dot)
        assertEquals("3", text(activity, R.id.count_text))
    }

    @Test
    fun rowIconsExpandTogetherWithoutInterfering() {
        val activity = launch(RowActivity::class.java)
        val host = activity.findViewById<View>(R.id.row_host)
        val heart = activity.findViewById<View>(R.id.icon_heart)
        val star = activity.findViewById<View>(R.id.icon_star)

        tapBesideRight(host, heart, 8f)           // 默认扩大 12dp，点在喜欢右侧 8dp
        assertEquals(activity.getString(R.string.row_last_on, "喜欢"), text(activity, R.id.tap_text))
        tapInside(host, star)
        assertEquals(activity.getString(R.string.row_last_on, "星标"), text(activity, R.id.tap_text))
        tapBesideRight(host, heart, 8f)           // 再点喜欢取消，星标不受影响
        assertEquals(activity.getString(R.string.row_last_off, "喜欢"), text(activity, R.id.tap_text))

        activity.findViewById<SwitchCompat>(R.id.expand_switch).isChecked = false
        tapBesideRight(host, heart, 8f)           // 关闭扩大后点在图标外不再命中
        assertEquals(activity.getString(R.string.row_last_off, "喜欢"), text(activity, R.id.tap_text))
    }

    @Test
    fun hideAndRestoreChangeHits() {
        val activity = launch(HideActivity::class.java)
        val host = activity.findViewById<View>(R.id.hide_host)
        val dot = activity.findViewById<View>(R.id.target_dot)

        tapBesideRight(host, dot, 20f)
        assertEquals("1", text(activity, R.id.count_text))

        activity.findViewById<View>(R.id.hide_button).performClick()
        assertEquals(View.INVISIBLE, dot.visibility)
        tapBesideRight(host, dot, 20f)            // 隐藏后不命中
        tapInside(host, dot)
        assertEquals("1", text(activity, R.id.count_text))

        activity.findViewById<View>(R.id.hide_button).performClick()
        tapBesideRight(host, dot, 20f)            // 显示后热区自动回来
        assertEquals("2", text(activity, R.id.count_text))

        activity.findViewById<View>(R.id.restore_button).performClick()
        tapBesideRight(host, dot, 20f)            // 还原后只剩圆点自身
        assertEquals("2", text(activity, R.id.count_text))
        tapInside(host, dot)
        assertEquals("3", text(activity, R.id.count_text))

        activity.findViewById<View>(R.id.expand_button).performClick()
        tapBesideRight(host, dot, 20f)
        assertEquals("4", text(activity, R.id.count_text))
    }
}
