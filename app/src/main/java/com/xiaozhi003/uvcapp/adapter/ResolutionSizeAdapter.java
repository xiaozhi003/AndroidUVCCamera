package com.xiaozhi003.uvcapp.adapter;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.xiaozhi003.uvc.usb.Size;

public class ResolutionSizeAdapter implements SpinnerAdapter {

    private Context mContext;
    private List<Size> mPreviewSizeList;
    private LayoutInflater mLayoutInflater;

    public ResolutionSizeAdapter(Context context, List<Size> previewSizeList) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
        mPreviewSizeList = previewSizeList == null ? new ArrayList<>() : previewSizeList;
    }

    @Override
    public View getDropDownView(int position, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = mLayoutInflater.inflate(android.R.layout.simple_spinner_dropdown_item, null);
        }
        Size size = mPreviewSizeList.get(position);
        ((TextView) view).setText(size.width + "x" + size.height);

        return view;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {

    }

    @Override
    public int getCount() {
        return mPreviewSizeList.size();
    }

    @Override
    public Size getItem(int i) {
        return mPreviewSizeList.get(i);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        TextView textView = (TextView) View.inflate(mContext, android.R.layout.simple_spinner_item, null);
        Size size = mPreviewSizeList.get(position);
        textView.setText(size.width + "x" + size.height);
        return textView;
    }

    @Override
    public int getItemViewType(int i) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
