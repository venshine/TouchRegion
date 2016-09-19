# TouchRegion

[![API](https://img.shields.io/badge/API-14%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=14)


Scene
--
1. View整体尺寸小，点击区域小，需要点击很多次才能选中；
2. 通过设置View的padding属性增大View的点击区域；
3. 通过设置View的父类点击事件实现点击效果；
4. 通过给View增加父类，实现点击效果。  

如果你有遇到过以上几点，那么[TouchRegion](https://github.com/venshine/TouchRegion)项目将会方便地帮助你实现扩大View的触摸和点击区域。

ScreenShot
--
![](https://github.com/venshine/TouchRegion/blob/master/screenshot/screenshot.gif)

Usage
--
##### Gradle:
```groovy
compile 'com.wx.touchregion:touchregion:1.0.0'
```

Demo
--
Use the TouchRegion as a Utils, Java are supported.

##### Java:
```Java
    public class MainActivity extends Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main)

            TouchRegion touchRegion = new TouchRegion(mLayout); // 传入父类引用
            touchRegion.expandViewTouchRegion(mButton1, 300);   // 设置button1扩大300px点击区域
            touchRegion.expandViewTouchRegion(mButton2, 500, 100, 500, 100);    // 设置button2扩大(ltrt:500,100,500,100)点击区域

        }
    }
```

##### Methods:
| method 方法          | description 描述 |
|:---				 |:---|
| void **expandViewTouchRegion**(View view, int margin)  	     | 扩大View的触摸和点击范围，最大不超过其父View范围 |
| void **expandViewTouchRegion**(final View view, final int left, final int top, final int right, final int bottom) | 扩大View的触摸和点击范围，最大不超过其父View范围 |
| void **restoreViewTouchRegion**(final View view) 	     | 恢复View的触摸和点击范围，最小不小于View自身范围 |


About
--
* Email：venshine.cn@gmail.com


License
--
    Copyright (C) 2016 venshine.cn@gmail.com

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

