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
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.wx.touchregion.TouchRegion

/**
 * Demo：两个 24dp 的小圆点共用一个父 View，用 TouchRegion 各自扩大到四周 40dp
 *
 * @author venshine
 */
class MainActivity : AppCompatActivity() {

    private var clicksA = 0
    private var clicksB = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val host = findViewById<ViewGroup>(R.id.host)
        val dotA = findViewById<View>(R.id.dot_a)
        val dotB = findViewById<View>(R.id.dot_b)
        val counter = findViewById<TextView>(R.id.counter)
        val margin = (40 * resources.displayMetrics.density).toInt()

        fun refresh() {
            counter.text = getString(R.string.clicks, clicksA, clicksB)
        }
        refresh()
        dotA.setOnClickListener { clicksA++; refresh() }
        dotB.setOnClickListener { clicksB++; refresh() }

        val touchRegion = TouchRegion(host)
        findViewById<SwitchCompat>(R.id.switch_expand).setOnCheckedChangeListener { _, checked ->
            if (checked) {
                touchRegion.expandViewTouchRegion(dotA, margin)
                touchRegion.expandViewTouchRegion(dotB, margin)
            } else {
                touchRegion.restoreViewTouchRegion(dotA)
                touchRegion.restoreViewTouchRegion(dotB)
            }
        }
        // 开关默认打开，XML 里的初始状态不会触发监听，手动应用一次
        touchRegion.expandViewTouchRegion(dotA, margin)
        touchRegion.expandViewTouchRegion(dotB, margin)

        // 隐藏后扩大的区域不再响应点击
        findViewById<Button>(R.id.button_hide_b).apply {
            setOnClickListener {
                val hide = dotB.visibility == View.VISIBLE
                dotB.visibility = if (hide) View.INVISIBLE else View.VISIBLE
                setText(if (hide) R.string.show_b else R.string.hide_b)
            }
        }
    }
}
