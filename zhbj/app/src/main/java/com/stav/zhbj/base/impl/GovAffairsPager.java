package com.stav.zhbj.base.impl;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.stav.zhbj.base.BasePager;

/**
 * 政务
 * Created by Administrator on 2017/7/21.
 */
public class GovAffairsPager extends BasePager {

    public GovAffairsPager(Activity activity) {
        super(activity);
    }

    @Override
    public void initData() {
        System.out.println("政务设置完成");
        //给帧布局填充布局对象
        TextView view = new TextView(mActivity);
        view.setText("政务");
        view.setTextColor(Color.RED);
        view.setTextSize(22);
        view.setGravity(Gravity.CENTER);
        //添加布局
        fl_content.addView(view);

        //修改标题
        tv_title.setText("人口管理");

        //显示菜单按钮
        ib_menu.setVisibility(View.VISIBLE);
    }
}
