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

/**
 * 把一个父 View 的触摸分发给多个 [TouchDelegate]：`ACTION_DOWN` 时按添加顺序询问，
 * 第一个处理的接管该次触摸的后续事件。
 *
 * @author venshine
 */
open class TouchDelegateGroup(delegateView: View) : TouchDelegate(Rect(), delegateView) {

    private val touchDelegates = ArrayList<TouchDelegate>()
    private var currentTouchDelegate: TouchDelegate? = null

    fun addTouchDelegate(touchDelegate: TouchDelegate) {
        touchDelegates.add(touchDelegate)
    }

    fun removeTouchDelegate(touchDelegate: TouchDelegate) {
        touchDelegates.remove(touchDelegate)
        if (currentTouchDelegate === touchDelegate) {
            currentTouchDelegate = null
        }
    }

    fun clearTouchDelegates() {
        touchDelegates.clear()
        currentTouchDelegate = null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val delegate: TouchDelegate?
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 拷贝一份再遍历，委托在处理中增删自己也不会出错
                for (touchDelegate in touchDelegates.toList()) {
                    if (touchDelegate.onTouchEvent(event)) {
                        currentTouchDelegate = touchDelegate
                        return true
                    }
                }
                currentTouchDelegate = null
                delegate = null
            }
            MotionEvent.ACTION_MOVE -> delegate = currentTouchDelegate
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                delegate = currentTouchDelegate
                currentTouchDelegate = null
            }
            else -> delegate = null
        }
        return delegate?.onTouchEvent(event) ?: false
    }
}
