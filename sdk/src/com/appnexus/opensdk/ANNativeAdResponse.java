/*
 *    Copyright 2015 APPNEXUS INC
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.appnexus.opensdk;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.MutableContextWrapper;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.appnexus.opensdk.utils.Clog;
import com.appnexus.opensdk.utils.JsonUtil;
import com.appnexus.opensdk.utils.Settings;
import com.appnexus.opensdk.utils.ViewUtil;
import com.appnexus.opensdk.utils.WebviewUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ANNativeAdResponse implements NativeAdResponse {
    private String title;
    private String description;
    private String imageUrl;
    private String iconUrl;
    private Bitmap image;
    private Bitmap icon;
    private String clickUrl;
    private String clickFallBackUrl;
    private String callToAction;
    private String socialContext;
    private Rating rating;
    private HashMap<String, Object> nativeElements;
    private boolean expired = false;
    private ArrayList<String> imp_trackers;
    private ArrayList<String> click_trackers;
    private String fullText;
    private String sponsoredBy;
    private Handler anNativeExpireHandler;
    private String creativeId = "";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_CONTEXT = "context";
    private static final String KEY_MAIN_MEDIA = "main_media";
    private static final String KEY_IMAGE_LABEL = "label";
    private static final String VALUE_DEFAULT_IMAGE = "default";
    private static final String KEY_IMAGE_URL = "url";
    private static final String KEY_FULL_TEXT = "full_text";
    private static final String KEY_ICON = "icon_img_url";
    private static final String KEY_CTA = "cta";
    private static final String KEY_CLICK_TRACK = "click_trackers";
    private static final String KEY_IMP_TRACK = "impression_trackers";
    private static final String KEY_CLICK_URL = "click_url";
    private static final String KEY_CLICK_FALLBACK_URL = "click_fallback_url";
    private static final String KEY_RATING = "rating";
    private static final String KEY_RATING_VALUE = "value";
    private static final String KEY_RATING_SCALE = "scale";
    private static final String KEY_CUSTOM = "custom";
    private static final String KEY_SPONSORED_BY = "sponsored_by";


    private Runnable expireRunnable = new Runnable() {
        @Override
        public void run() {
            expired = true;
            registeredView = null;
            clickables = null;
            if (visibilityDetector != null) {
                visibilityDetector.destroy();
                visibilityDetector = null;
            }
            impressionTrackers = null;
            listener = null;
            // free assets
            if (icon != null) {
                icon.recycle();
                icon = null;
            }
            if (image != null) {
                image.recycle();
                image = null;
            }
        }
    };

    private View registeredView;
    private List<View> clickables;
    private NativeAdEventListener listener;
    private View.OnClickListener clickListener;
    private VisibilityDetector visibilityDetector;
    private ArrayList<ImpressionTracker> impressionTrackers;
    private ProgressDialog progressDialog;

    /**
     * Process the metadata of native response from ad server
     *
     * @param metaData JsonObject that contains info of native ad
     * @return ANNativeResponse if no issue happened during processing
     */
    public static ANNativeAdResponse create(JSONObject metaData) {
        if (metaData == null) {
            return null;
        }
        JSONArray impTrackerJson = JsonUtil.getJSONArray(metaData, KEY_IMP_TRACK);
        ArrayList<String> imp_trackers = JsonUtil.getStringArrayList(impTrackerJson);
        if (imp_trackers == null) {
            return null;
        }
        ANNativeAdResponse response = new ANNativeAdResponse();
        response.imp_trackers = imp_trackers;
        response.title = JsonUtil.getJSONString(metaData, KEY_TITLE);
        response.description = JsonUtil.getJSONString(metaData, KEY_DESCRIPTION);
        JSONArray main_media = JsonUtil.getJSONArray(metaData, KEY_MAIN_MEDIA);
        if (main_media != null) {
            int l = main_media.length();
            for (int i = 0; i < l; i++) {
                JSONObject media = JsonUtil.getJSONObjectFromArray(main_media, i);
                if (media != null) {
                    String label = JsonUtil.getJSONString(media, KEY_IMAGE_LABEL);
                    if (label != null && label.equals(VALUE_DEFAULT_IMAGE)) {
                        response.imageUrl = JsonUtil.getJSONString(media, KEY_IMAGE_URL);
                        break;
                    }
                }
            }
        };
        response.iconUrl = JsonUtil.getJSONString(metaData, KEY_ICON);
        response.socialContext = JsonUtil.getJSONString(metaData, KEY_CONTEXT);
        response.callToAction = JsonUtil.getJSONString(metaData, KEY_CTA);
        response.clickUrl = JsonUtil.getJSONString(metaData, KEY_CLICK_URL);
        response.clickFallBackUrl = JsonUtil.getJSONString(metaData, KEY_CLICK_FALLBACK_URL);

        response.sponsoredBy = JsonUtil.getJSONString(metaData, KEY_SPONSORED_BY);
        response.fullText = JsonUtil.getJSONString(metaData, KEY_FULL_TEXT);


        JSONObject rating = JsonUtil.getJSONObject(metaData, KEY_RATING);
        response.rating = new Rating(
                JsonUtil.getJSONDouble(rating, KEY_RATING_VALUE),
                JsonUtil.getJSONDouble(rating, KEY_RATING_SCALE)
        );
        JSONArray clickTrackerJson = JsonUtil.getJSONArray(metaData, KEY_CLICK_TRACK);
        response.click_trackers = JsonUtil.getStringArrayList(clickTrackerJson);
        JSONObject custom = JsonUtil.getJSONObject(metaData, KEY_CUSTOM);
        response.nativeElements = JsonUtil.getStringObjectHashMap(custom);
        response.anNativeExpireHandler = new Handler(Looper.getMainLooper());
        response.anNativeExpireHandler.postDelayed(response.expireRunnable, Settings.NATIVE_AD_RESPONSE_EXPIRATION_TIME);
        return response;
    }

    private ANNativeAdResponse() {
    }

    @Override
    public Network getNetworkIdentifier() {
        return Network.APPNEXUS;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public Bitmap getImage() {
        return image;
    }

    @Override
    public void setImage(Bitmap bitmap) {
        this.image = bitmap;
    }

    @Override
    public String getIconUrl() {
        return iconUrl;
    }

    @Override
    public Bitmap getIcon() {
        return icon;
    }

    @Override
    public void setIcon(Bitmap bitmap) {
        this.icon = bitmap;
    }

    @Override
    public String getCallToAction() {
        return callToAction;
    }

    @Override
    public HashMap<String, Object> getNativeElements() {
        return nativeElements;
    }

    @Override
    public String getSocialContext() {
        return socialContext;
    }

    @Override
    public String getFullText() { return fullText; }

    @Override
    public String getSponsoredBy() { return sponsoredBy; }

    @Override
    public Rating getAdStarRating() {
        return rating;
    }

    @Override
    public boolean hasExpired() {
        return expired;
    }

    @Override
    public String getCreativeId() {
        return creativeId;
    }

    @Override
    public void setCreativeId(String creativeId) {
        this.creativeId  = creativeId;
    }

    @Override
    public boolean registerView(final View view, final NativeAdEventListener listener) {
        if (!expired && view != null) {
            this.listener = listener;
            visibilityDetector = VisibilityDetector.create(view);
            if (visibilityDetector == null) {
                return false;
            }

            impressionTrackers = new ArrayList<ImpressionTracker>(imp_trackers.size());
            for (String url : imp_trackers) {
                ImpressionTracker impressionTracker = ImpressionTracker.create(url, visibilityDetector, view.getContext());
                impressionTrackers.add(impressionTracker);
            }
            this.registeredView = view;
            setClickListener();
            view.setOnClickListener(clickListener);
            if(anNativeExpireHandler!=null) {
                anNativeExpireHandler.removeCallbacks(expireRunnable);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean registerViewList(final View view, final List<View> clickables, NativeAdEventListener listener) {
        if (registerView(view, listener)) {
            view.setOnClickListener(null); // unset the click listener in registerView()
            for (View clickable : clickables) {
                clickable.setOnClickListener(clickListener);
            }
            this.clickables = clickables;
            return true;
        }
        return false;
    }

    @Override
    public void unregisterViews() {
        if (registeredView != null) {
            registeredView.setOnClickListener(null);
        }
        if (clickables != null && !clickables.isEmpty()) {
            for (View clickable : clickables) {
                clickable.setOnClickListener(null);
            }
        }
        destroy();
    }

    @Override
    public void destroy() {
        if(anNativeExpireHandler!=null) {
            anNativeExpireHandler.removeCallbacks(expireRunnable);
            anNativeExpireHandler.post(expireRunnable);
        }
    }



    private boolean openNativeBrowser = false;

    public boolean isOpenNativeBrowser() {
        return openNativeBrowser;
    }

    void openNativeBrowser(boolean openNativeBrowser) {
        this.openNativeBrowser = openNativeBrowser;
    }

    private boolean doesLoadingInBackground = true;

    public boolean getLoadsInBackground() {
        return doesLoadingInBackground;
    }

    void setLoadsInBackground(boolean doesLoadingInBackground) {
        this.doesLoadingInBackground = doesLoadingInBackground;
    }

    void setClickListener() {
        clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // fire click tracker first
                if (click_trackers != null) {
                    for (String url : click_trackers) {
                        new ClickTracker(url).execute();
                    }
                }
                if (listener != null) {
                    listener.onAdWasClicked();
                }
                if (!handleClick(clickUrl, v.getContext())) {
                    if (!handleClick(clickFallBackUrl, v.getContext())) {
                        Clog.d(Clog.nativeLogTag, "Unable to handle click.");
                    }
                }

            }
        };
    }

    private class RedirectWebView extends WebView {

        @SuppressLint("SetJavaScriptEnabled")
        public RedirectWebView(final Context context) {
            super(new MutableContextWrapper(context));

            WebviewUtil.setWebViewSettings(this);
            this.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageFinished(WebView view, String url) {
                    Clog.v(Clog.browserLogTag, "Opening URL: " + url);
                    ViewUtil.removeChildFromParent(RedirectWebView.this);
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }

                    startBrowserActivity(context);
                }
            });
        }
    }

    private void startBrowserActivity(Context context) {
        Class<?> activity_clz = AdActivity.getActivityClass();
        try {
            Intent intent = new Intent(context, activity_clz);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(AdActivity.INTENT_KEY_ACTIVITY_TYPE, AdActivity.ACTIVITY_TYPE_BROWSER);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Clog.w(Clog.baseLogTag, Clog.getString(R.string.adactivity_missing, activity_clz.getName()));
            BrowserAdActivity.BROWSER_QUEUE.remove();
        }
    }


    boolean handleClick(String clickUrl, Context context) {
        if (clickUrl == null || clickUrl.isEmpty()) {
            return false;
        }
        // if install, open store
        if (clickUrl.contains("://play.google.com") || clickUrl.contains("market://")) {
            Clog.d(Clog.nativeLogTag,
                    Clog.getString(R.string.opening_app_store));
            return openNativeIntent(clickUrl, context);
        }
        // open browser
        if (openNativeBrowser) {
            // if set to use native browser, open intent
            if (openNativeIntent(clickUrl, context)) {
                if (listener != null) {
                    listener.onAdWillLeaveApplication();
                }
                return true;
            }
            return false;
        } else {
            try {
                if (getLoadsInBackground()) {
                    final WebView out = new RedirectWebView(new MutableContextWrapper(context));
                    WebviewUtil.setWebViewSettings(out);
                    out.loadUrl(clickUrl);
                    BrowserAdActivity.BROWSER_QUEUE.add(out);
                    // Otherwise, create an invisible 1x1 webview to load the landing
                    // page and detect if we're redirecting to a market url
                    //Show a dialog box
                    progressDialog = new ProgressDialog(context);
                    progressDialog.setCancelable(true);
                    progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            out.stopLoading();
                        }
                    });
                    progressDialog.setMessage(context.getResources().getString(R.string.loading));
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progressDialog.show();
                } else {
                    WebView out = new WebView(new MutableContextWrapper(context));
                    WebviewUtil.setWebViewSettings(out);
                    out.loadUrl(clickUrl);
                    BrowserAdActivity.BROWSER_QUEUE.add(out);
                    startBrowserActivity(context);
                }


                return true;
            } catch (Exception e) {
                // Catches PackageManager$NameNotFoundException for webview
                Clog.e(Clog.baseLogTag, "Exception initializing the redirect webview: " + e.getMessage());
                return false;
            }
        }
    }

    private boolean openNativeIntent(String url, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Clog.w(Clog.baseLogTag,
                    Clog.getString(R.string.opening_url_failed, url));
            return false;
        }
    }
}
