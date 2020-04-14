package com.baidu.ai.edge.ui.view.model;

import android.graphics.Rect;

/**
 * Created by ruanshimin on 2018/5/13.
 */

public class SegmentResultModel extends BaseRectBoundResultModel {

    public SegmentResultModel() {
        super();
    }

    public SegmentResultModel(int index, String name, float confidence, Rect bounds, byte[] mask) {
        super(index, name, confidence, bounds);
        this.setMask(mask);
    }

    public SegmentResultModel(int index, String name, float confidence, Rect bounds) {
        super(index, name, confidence, bounds);
    }


}
