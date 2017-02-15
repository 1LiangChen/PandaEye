package com.pandaq.pandaeye.ui.zhihu;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Slide;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.pandaq.pandaeye.R;
import com.pandaq.pandaeye.config.Constants;
import com.pandaq.pandaeye.entity.ZhiHu.ZhihuStoryContent;
import com.pandaq.pandaeye.presenter.zhihu.ZhihuStoryInfoPresenter;
import com.pandaq.pandaeye.ui.ImplView.IZhihuStoryInfoActivity;
import com.pandaq.pandaeye.utils.ColorUtils;
import com.pandaq.pandaeye.utils.DensityUtil;
import com.pandaq.pandaeye.utils.GlideUtils;
import com.pandaq.pandaeye.utils.ViewUtils;
import com.pandaq.pandaeye.utils.WebUtils;
import com.pandaq.pandaeye.widget.FiveThreeImageView;
import com.tencent.smtt.sdk.WebView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by PandaQ on 2016/9/8.
 * email : 767807368@qq.com
 * 知乎日报打开详情页面
 */
public class ZhihuStoryInfoActivity extends AppCompatActivity implements IZhihuStoryInfoActivity {

    private static final float SCRIM_ADJUSTMENT = 0.075f;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.toolbar_layout)
    CollapsingToolbarLayout mToolbarLayout;
    @BindView(R.id.app_bar)
    AppBarLayout mAppBar;
    @BindView(R.id.fab)
    FloatingActionButton mFab;
    @BindView(R.id.story_img)
    FiveThreeImageView mStoryImg;
    @BindView(R.id.zhihudaily_webview)
    WebView mZhihudailyWebview;
    private String story_id = "";
    private ZhihuStoryInfoPresenter mPresenter = new ZhihuStoryInfoPresenter(this);
    int[] mDeviceInfo;
    int width;
    int heigh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zhihu_story_info);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        mToolbarLayout.setTitle(getString(R.string.zhihustory));
        getWindow().setEnterTransition(new Fade().setDuration(800));
        getWindow().setExitTransition(new Fade().setDuration(800));
        initView();
        initData();
    }

    private void initView() {
        mDeviceInfo = DensityUtil.getDeviceInfo(this);
        width = mDeviceInfo[0];
        heigh = width * 3 / 5;
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAfterTransition();
            }
        });
    }

    private void initData() {
        Bundle bundle = getIntent().getExtras();
        story_id = String.valueOf(bundle.getInt(Constants.BUNDLE_KEY_ID));
        loadZhihuStory();
    }

    @Override
    public void showProgressBar() {

    }

    @Override
    public void hideProgressBar() {

    }

    @Override
    public void loadZhihuStory() {
        mPresenter.loadStory(story_id);
    }

    @Override
    public void loadFail(String errmsg) {

    }

    @Override
    public void loadSuccess(ZhihuStoryContent zhihuStory) {
        Glide.with(this)
                .load(zhihuStory.getImage())
                .listener(new GlideRequestListener())
                .override(width, heigh)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .crossFade()
                .into(mStoryImg);
        startPostponedEnterTransition();
        String url = zhihuStory.getShare_url();
        boolean isEmpty = TextUtils.isEmpty(zhihuStory.getBody());
        String mBody = zhihuStory.getBody();
        String[] scc = zhihuStory.getCss();
        //如果返回的html body为空则直接 load url
        if (isEmpty) {
            mZhihudailyWebview.loadUrl(url);
        } else {
            String data = WebUtils.buildHtmlWithCss(mBody, scc, false);
            mZhihudailyWebview.loadDataWithBaseURL(WebUtils.BASE_URL, data, WebUtils.MIME_TYPE, WebUtils.ENCODING, WebUtils.FAIL_URL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPresenter.unsubcription();
    }

    class GlideRequestListener implements RequestListener<String, GlideDrawable> {

        @Override
        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
            final Bitmap bitmap = GlideUtils.getBitmap(resource);
            final int twentyFourDip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    24, ZhihuStoryInfoActivity.this.getResources().getDisplayMetrics());
            assert bitmap != null;
            Palette.from(bitmap)
                    .maximumColorCount(16)
                    .clearFilters()
                    .setRegion(0, 0, bitmap.getWidth() - 1, twentyFourDip)
                    .generate(new Palette.PaletteAsyncListener() {
                        @TargetApi(Build.VERSION_CODES.M)
                        @Override
                        public void onGenerated(Palette palette) {
                            boolean isDark;
                            int lightness = ColorUtils.isDark(palette);
                            if (lightness == ColorUtils.LIGHTNESS_UNKNOWN) {
                                isDark = ColorUtils.isDark(bitmap, bitmap.getWidth() / 2, 0);
                            } else {
                                isDark = lightness == ColorUtils.IS_DARK;
                            }
                            // color the status bar. Set a complementary dark color on L,
                            // light or dark color on M (with matching status bar icons)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                int statusBarColor = getWindow().getStatusBarColor();
                                final Palette.Swatch topColor = ColorUtils.getMostPopulousSwatch(palette);
                                if (topColor != null && (isDark || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
                                    statusBarColor = ColorUtils.scrimify(topColor.getRgb(), isDark, SCRIM_ADJUSTMENT);
                                    // set a light status bar on M+
                                    if (!isDark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        ViewUtils.setLightStatusBar(mStoryImg);
                                    }
                                }
                                if (statusBarColor != getWindow().getStatusBarColor()) {
                                    mToolbarLayout.setContentScrimColor(statusBarColor);
                                    mToolbar.setBackgroundColor(getResources().getColor(R.color.trans_toolbar_7c424141, null));
                                    ValueAnimator statusBarColorAnim = ValueAnimator.ofArgb(
                                            getWindow().getStatusBarColor(), statusBarColor);
                                    statusBarColorAnim.addUpdateListener(new ValueAnimator
                                            .AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator animation) {
                                            getWindow().setStatusBarColor((int) animation.getAnimatedValue());
                                        }
                                    });
                                    statusBarColorAnim.setDuration(1000L);
                                    statusBarColorAnim.setInterpolator(
                                            new AccelerateInterpolator());
                                    statusBarColorAnim.start();
                                }
                            }
                        }
                    });
            return false;
        }
    }

}