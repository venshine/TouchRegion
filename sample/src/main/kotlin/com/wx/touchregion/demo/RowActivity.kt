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

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.wx.touchregion.TouchRegion

/**
 * 多个子 View：一行 5 个 28dp 图标共用一个父 View，同时扩大、互不干扰；点击切换选中
 *
 * @author venshine
 */
class RowActivity : BaseActivity() {

    private lateinit var host: ZoneLayout
    private lateinit var icons: List<ImageView>
    private lateinit var region: TouchRegion

    private var expanded = true
    private var marginDp = 12
    private val selected = HashSet<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_row)
        setToolbar(R.string.demo_row, "TouchRegion")

        host = findViewById(R.id.row_host)
        icons = ICON_IDS.map { findViewById(it) }
        region = TouchRegion(host)
        host.zones = { icons.map { host.rectOf(it, if (expanded) dp(marginDp.toFloat()) else 0) } }
        findViewById<TextView>(R.id.tap_text).setText(R.string.row_last_idle)
        icons.forEach { icon ->
            icon.setOnClickListener { toggle(icon) }
        }

        findViewById<SwitchCompat>(R.id.expand_switch).apply {
            isChecked = expanded
            setOnCheckedChangeListener { _, checked -> expanded = checked; render() }
        }
        findViewById<SeekBar>(R.id.margin_seek).apply {
            progress = marginDp
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    marginDp = progress
                    render()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }
        render()
    }

    private fun toggle(icon: ImageView) {
        val on = selected.add(icon.id)
        if (!on) selected.remove(icon.id)
        host.markHit()
        findViewById<TextView>(R.id.tap_text).text =
            getString(if (on) R.string.row_last_on else R.string.row_last_off, icon.contentDescription)
        render()
    }

    private fun render() {
        // 每次都按固定顺序重新设置，保证重叠时「先添加的优先」的顺序稳定
        icons.forEach {
            if (expanded) region.expandViewTouchRegion(it, dp(marginDp.toFloat())) else region.restoreViewTouchRegion(it)
        }
        icons.forEach {
            it.imageTintList = ColorStateList.valueOf(colorOf(if (it.id in selected) R.color.accent_ink else R.color.text_dim))
        }
        findViewById<View>(R.id.margin_seek).apply {
            isEnabled = expanded
            alpha = if (expanded) 1f else 0.4f
        }
        findViewById<TextView>(R.id.margin_text).text = "$marginDp dp"
        host.invalidate()
    }

    private companion object {
        val ICON_IDS = listOf(R.id.icon_heart, R.id.icon_star, R.id.icon_bell, R.id.icon_bookmark, R.id.icon_send)
    }
}
