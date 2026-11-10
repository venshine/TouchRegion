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
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.wx.touchregion.TouchRegion
import kotlin.math.PI
import kotlin.math.sin

/**
 * 首页：两个小圆点 + 呼吸的热区预览 + 示例入口 + 接入方式
 *
 * @author venshine
 */
class MainActivity : BaseActivity() {

    private var tick: Runnable? = null

    /** 示例 App 与库同版本发布，版本号只在 sample/build.gradle 的 versionName 维护一处 */
    private val version: String by lazy {
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.source_button).setOnClickListener { openUrl(SOURCE_URL) }
        findViewById<TextView>(R.id.version_chip).text = "v$version"
        initPreview()
        initCards()
        initInstall()
    }

    override fun onDestroy() {
        tick?.let { findViewById<View>(R.id.preview_zone).removeCallbacks(it) }
        super.onDestroy()
    }

    /** 两个 24dp 圆点各自扩大；扩大范围在 [MIN_DP]..[MAX_DP] 之间随时间起伏，热区实时跟着画 */
    private fun initPreview() {
        val host = findViewById<ZoneLayout>(R.id.preview_zone)
        val dots = listOf(R.id.preview_dot_a, R.id.preview_dot_b).map { findViewById<View>(it) }
        val count = findViewById<TextView>(R.id.preview_count)
        val region = TouchRegion(host)
        var clicks = 0
        var margin = dp(MAX_DP)

        fun render() {
            count.text = getString(R.string.clicks_format, clicks)
        }
        dots.forEach { dot ->
            dot.setOnClickListener { clicks++; host.markHit(); render() }
        }
        host.zones = { dots.map { host.rectOf(it, margin) } }
        render()

        val start = SystemClock.uptimeMillis()
        val runnable = object : Runnable {
            override fun run() {
                val phase = (SystemClock.uptimeMillis() - start) % BREATH_MS / BREATH_MS.toFloat()
                val wave = (sin(phase * 2 * PI) + 1) / 2
                margin = dp((MIN_DP + (MAX_DP - MIN_DP) * wave).toFloat())
                dots.forEach { region.expandViewTouchRegion(it, margin) }
                host.invalidate()
                host.postDelayed(this, FRAME_MS)
            }
        }
        tick = runnable
        runnable.run()
    }

    private fun initCards() {
        // 第一张强调色大卡的内容已在布局里写好，只绑定点击
        findViewById<View>(R.id.card_zone).setOnClickListener { open(ZoneActivity::class.java) }
        bindCard(R.id.card_row, R.string.demo_row, R.string.demo_row_desc, R.drawable.ic_row, RowActivity::class.java)
        bindCard(R.id.card_hide, R.string.demo_hide, R.string.demo_hide_desc, R.drawable.ic_eye, HideActivity::class.java)
    }

    private fun bindCard(@IdRes id: Int, @StringRes title: Int, @StringRes desc: Int, @DrawableRes icon: Int,
                         target: Class<out Activity>) {
        val card = findViewById<View>(id)
        card.findViewById<ImageView>(R.id.card_icon).setImageResource(icon)
        card.findViewById<TextView>(R.id.card_title).setText(title)
        card.findViewById<TextView>(R.id.card_desc).setText(desc)
        card.setOnClickListener { open(target) }
    }

    private fun initInstall() {
        val dependency = "com.github.venshine:TouchRegion:$version"
        val code = "// settings.gradle\nmaven { url = 'https://jitpack.io' }\n\n// build.gradle\nimplementation '$dependency'"
        findViewById<TextView>(R.id.install_code).text =
            CodeHighlighter.highlight(this, code, listOf("implementation", "maven"))
        findViewById<TextView>(R.id.copy_dependency).apply {
            setStartIcon(tintedIcon(R.drawable.ic_copy, colorOf(R.color.text), 16f))
            setOnClickListener {
                copyToClipboard("implementation '$dependency'")
                flashCopied(R.string.home_copied_dependency, R.string.home_copy_dependency)
            }
        }
    }

    private fun open(target: Class<out Activity>) = startActivity(Intent(this, target))

    private companion object {
        const val SOURCE_URL = "https://github.com/venshine/TouchRegion"
        const val MIN_DP = 10f
        const val MAX_DP = 26f
        const val BREATH_MS = 2400L
        const val FRAME_MS = 33L
    }
}
