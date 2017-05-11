package com.example.ngfngf.zoomimageview;

import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

import com.example.ngfngf.zoomimageview.view.ZoomImageView;

public class MainActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    private int[] mImgs = new int[]{R.mipmap.bg, R.mipmap.ic_launcher};
    private ZoomImageView[] mImageViews = new ZoomImageView[mImgs.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mViewPager = (ViewPager) findViewById(R.id.viewpage);
        mViewPager.setAdapter(new PagerAdapter() {
            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                ZoomImageView imageView = new ZoomImageView(getApplicationContext());
                imageView.setImageResource(mImgs[position]);
                container.addView(imageView);
                mImageViews[position] = imageView;
                return imageView;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView(mImageViews[position]);
            }

            @Override
            public int getCount() {
                return mImageViews.length;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }
        });
    }
}
