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

import com.appnexus.opensdk.shadows.ShadowAsyncTaskNoExecutor;
import com.appnexus.opensdk.shadows.ShadowSettings;
import com.appnexus.opensdk.shadows.ShadowWebSettings;
import com.appnexus.opensdk.util.TestUtil;
import com.appnexus.opensdk.utils.Clog;
import com.appnexus.opensdk.viewability.ANOmidAdSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowWebView;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21)
public class BaseNativeTest extends BaseRoboTest implements NativeAdRequestListener,NativeAdEventListener {
    protected NativeAdRequest adRequest;
    protected NativeAdResponse     response;

    protected boolean adLoaded, adFailed;


    @Override
    public void setup() {
        super.setup();

        adLoaded = false;
        adFailed = false;

        adRequest = new NativeAdRequest(activity, "0");
        adRequest.setListener(this);
    }

    @Override
    public void tearDown() {
        super.tearDown();
        if (response != null) {
            response.destroy();
        }
    }

    public void assertCallbacks(boolean success) {
        assertEquals(success, adLoaded);
        assertEquals(!success, adFailed);
    }

    @Override
    public void onAdLoaded(NativeAdResponse response) {
        adLoaded = true;
        this.response = response;
        Clog.w(TestUtil.testLogTag, "BaseNativeTest onAdLoaded");
    }

    @Override
    public void onAdFailed(ResultCode errorcode) {
        adFailed = true;
        Clog.w(TestUtil.testLogTag, "BaseNativeTest onAdFailed");

    }

    @Test
    public void testDummy() {
        assertTrue(true);
    }

    @Override
    public void onAdWasClicked() {

    }

    @Override
    public void onAdWillLeaveApplication() {

    }

    @Override
    public void onAdWasClicked(String clickUrl, String fallbackURL) {

    }

    public ANOmidAdSession getOMIDAdSession() {
        return ((BaseNativeAdResponse)response).anOmidAdSession;
    }
}
