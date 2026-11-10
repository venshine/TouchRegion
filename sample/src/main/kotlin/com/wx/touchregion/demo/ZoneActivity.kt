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

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.wx.touchregion.TouchRegion

/**
 * 热区可视化：开关扩大、调节扩大范围，热区（橙色虚线框）实时画出来，点击计数
 *
 * 状态只有 [expanded]、[marginDp]、[clicks] 三个，[render] 统一推送到 TouchRegion、热区绘制和文字。
 *
 * @author venshine
 */
class ZoneActivity : BaseActivity() {

    private lateinit var host: ZoneLayout
    private lateinit var dot: View
    private lateinit var region: TouchRegion

    private var expanded = true
    private var marginDp = 24
    private var clicks = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zone)
        setToolbar(R.string.demo_zone, "expandViewTouchRegion()")

        host = findViewById(R.id.zone_host)
        dot = findViewById(R.id.target_dot)
        region = TouchRegion(host)
        // 未扩大时画 View 自身的范围，方便和扩大后对比
        host.zones = { listOf(host.rectOf(dot, if (expanded) dp(marginDp.toFloat()) else 0)) }
        dot.setOnClickListener { clicks++; host.markHit(); render() }

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

    private fun render() {
        if (expanded) region.expandViewTouchRegion(dot, dp(marginDp.toFloat())) else region.restoreViewTouchRegion(dot)
        findViewById<View>(R.id.margin_seek).apply {
            isEnabled = expanded
            alpha = if (expanded) 1f else 0.4f
        }
        findViewById<TextView>(R.id.margin_text).text = "$marginDp dp"
        findViewById<TextView>(R.id.count_text).text = clicks.toString()
        val code = if (expanded) "touchRegion.expandViewTouchRegion(dot, ${marginDp}.dp)" else "touchRegion.restoreViewTouchRegion(dot)"
        findViewById<TextView>(R.id.code_text).text = CodeHighlighter.highlight(this, code, emptyList())
        host.invalidate()
    }
}
