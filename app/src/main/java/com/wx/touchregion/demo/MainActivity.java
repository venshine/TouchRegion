package com.wx.touchregion.demo;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.wx.touchregion.TouchRegion;

public class MainActivity extends AppCompatActivity {

    private LinearLayout mLayout;

    private Button mButton1, mButton2;

    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLayout = (LinearLayout) findViewById(R.id.layout);

        mButton1 = (Button) findViewById(R.id.button1);
        mButton2 = (Button) findViewById(R.id.button2);

        mListView = (ListView) findViewById(R.id.listview);
        mListView.setAdapter(new MyAdapter(this));

        TouchRegion touchRegion = new TouchRegion(mLayout);
        touchRegion.expandViewTouchRegion(mButton1, 300);
        touchRegion.expandViewTouchRegion(mButton2, 500, 100, 500, 100);

    }

    class MyAdapter extends BaseAdapter {

        LayoutInflater inflate;

        public MyAdapter(Context context) {
            inflate = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return 10;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = inflate.inflate(R.layout.item, null);
                holder.tv = (TextView) convertView.findViewById(R.id.textview);
                holder.btn = (ImageView) convertView.findViewById(R.id.button);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.tv.setText("text" + position);

            TouchRegion touchRegion = new TouchRegion(holder.tv);
            touchRegion.expandViewTouchRegion(holder.tv, 100, 50, 100, 20);
            touchRegion.expandViewTouchRegion(holder.btn, 200, 50, 200, 20);

            return convertView;
        }

        class ViewHolder {
            TextView tv;
            ImageView btn;
        }

    }

}
