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
import android.widget.TextView
import com.wx.touchregion.TouchRegion

/**
 * 隐藏与恢复：圆点隐藏后热区不再命中，[TouchRegion.restoreViewTouchRegion] 还原为圆点自身大小
 *
 * 状态只有 [expanded]、[hidden]、[clicks]，[render] 统一推送。
 *
 * @author venshine
 */
class HideActivity : BaseActivity() {

    private lateinit var host: ZoneLayout
    private lateinit var dot: View
    private lateinit var region: TouchRegion

    private var expanded = true
    private var hidden = false
    private var clicks = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hide)
        setToolbar(R.string.demo_hide, "restoreViewTouchRegion()")

        host = findViewById(R.id.hide_host)
        dot = findViewById(R.id.target_dot)
        region = TouchRegion(host)
        host.zones = {
            if (hidden) emptyList() else listOf(host.rectOf(dot, if (expanded) dp(MARGIN_DP) else 0))
        }
        dot.setOnClickListener { clicks++; host.markHit(); render() }

        findViewById<View>(R.id.expand_button).setOnClickListener { expanded = true; render() }
        findViewById<View>(R.id.restore_button).setOnClickListener { expanded = false; render() }
        findViewById<View>(R.id.hide_button).setOnClickListener { hidden = !hidden; render() }
        render()
    }

    private fun render() {
        if (expanded) region.expandViewTouchRegion(dot, dp(MARGIN_DP)) else region.restoreViewTouchRegion(dot)
        dot.visibility = if (hidden) View.INVISIBLE else View.VISIBLE
        findViewById<TextView>(R.id.hide_button).setText(if (hidden) R.string.hide_show else R.string.hide_hide)
        findViewById<TextView>(R.id.count_text).text = clicks.toString()
        findViewById<TextView>(R.id.state_text).text = getString(
            R.string.hide_state,
            getString(if (expanded) R.string.hide_zone_expanded else R.string.hide_zone_restored),
            getString(if (hidden) R.string.hide_view_hidden else R.string.hide_view_visible),
        )
        host.invalidate()
    }

    private companion object {
        const val MARGIN_DP = 24f
    }
}
