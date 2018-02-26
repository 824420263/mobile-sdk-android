package com.appnexus.opensdk;

import com.appnexus.opensdk.shadows.ShadowSettings;
import com.appnexus.opensdk.ut.UTAdResponse;
import com.appnexus.opensdk.ut.adresponse.BaseAdResponse;
import com.appnexus.opensdk.ut.adresponse.CSMSDKAdResponse;
import com.appnexus.opensdk.ut.adresponse.RTBVASTAdResponse;
import com.appnexus.opensdk.ut.adresponse.SSMHTMLAdResponse;
import com.appnexus.opensdk.util.RoboelectricTestRunnerWithResources;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.util.HashMap;
import java.util.LinkedList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RoboelectricTestRunnerWithResources.class)
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ShadowSettings.class,ShadowLog.class})
public class UTAdResponseTest extends BaseRoboTest{

    UTAdResponse utAdResponse;

    @Override
    public void setup() {
        super.setup();

    }



    /**
     * Tests no ad response
     *
     * @throws Exception
     */
    @Test
    public void testNOResponse() throws Exception {

        String bannerString = TestResponsesUT.noResponse();
        utAdResponse = new UTAdResponse(bannerString,null,MediaType.BANNER, "v");

        assertNotNull(utAdResponse);
        LinkedList<BaseAdResponse> list = utAdResponse.getAdList();
        assertEquals(MediaType.BANNER,utAdResponse.getMediaType());
        assertNull(utAdResponse.getNoAdUrl());
        assertNull(list);
    }

    /**
     * Tests no rtb banner response
     *
     * @throws Exception
     */
    @Test
    public void testRTBNOAdResponse() throws Exception {

        String bannerString = TestResponsesUT.blankBanner();
        utAdResponse = new UTAdResponse(bannerString,null,MediaType.BANNER, "v");

        assertNotNull(utAdResponse);
        LinkedList<BaseAdResponse> list = utAdResponse.getAdList();
        assertEquals(MediaType.BANNER,utAdResponse.getMediaType());
        assertNotNull(utAdResponse.getNoAdUrl());
        assertTrue(list.isEmpty());
    }

    /**
     * Tests rtb banner response
     *
     * @throws Exception
     */
    @Test
    public void testBannerResponse() throws Exception {

        String bannerString = TestResponsesUT.banner();
        utAdResponse = new UTAdResponse(bannerString,null,MediaType.BANNER,"v");

        assertNotNull(utAdResponse);
        LinkedList<BaseAdResponse> list = utAdResponse.getAdList();
        assertNotNull(utAdResponse.getAdList());
        while(!list.isEmpty()){
            BaseAdResponse baseAdResponse = (BaseAdResponse) list.removeFirst();
            assertEquals("rtb",baseAdResponse.getContentSource());
            assertEquals("6332753",baseAdResponse.getCreativeId());
        }
    }


    /**
     * Tests rtb banner Video response
     *
     * @throws Exception
     */
    @Test
    public void testBannerVideoResponse() throws Exception {

        String bannerString = TestResponsesUT.rtbVASTVideo();
        utAdResponse = new UTAdResponse(bannerString,null,MediaType.BANNER,"v");

        assertNotNull(utAdResponse);
        LinkedList<BaseAdResponse> list = utAdResponse.getAdList();
        assertNotNull(utAdResponse.getAdList());
        while(!list.isEmpty()){
            RTBVASTAdResponse vastAdResponse = (RTBVASTAdResponse) list.removeFirst();
            assertEquals("rtb",vastAdResponse.getContentSource());
            assertEquals("video",vastAdResponse.getAdType());
            assertTrue(vastAdResponse.getAdContent().contains("<VAST version=\"2.0\">"));
            HashMap<String, Object> extras = vastAdResponse.getExtras();
            assertTrue(extras.containsKey("MRAID"));
            assertTrue(extras.containsValue(true));
        }
    }

    /**
     * Tests csm & rtb banner response
     *
     * @throws Exception
     */
    @Test
    public void testBannerCSMResponse() throws Exception {
        String bannerCSMString = TestResponsesUT.noFillCSM_RTBBanner();

        utAdResponse = new UTAdResponse(bannerCSMString,null,MediaType.BANNER, "v");

        assertNotNull(utAdResponse);
        LinkedList<BaseAdResponse> list = utAdResponse.getAdList();
        assertNotNull(utAdResponse.getAdList());
        System.out.println("Printing first");
        CSMSDKAdResponse baseCSMSDKAdResponse = (CSMSDKAdResponse) list.getFirst();
        assertEquals("csm", baseCSMSDKAdResponse.getContentSource());
        assertEquals("44863345",baseCSMSDKAdResponse.getCreativeId());
        System.out.println("Printing second");
        BaseAdResponse baseAdResponse = (BaseAdResponse) list.getLast();
        assertEquals("rtb",baseAdResponse.getContentSource());
        assertEquals("6332753",baseAdResponse.getCreativeId());

    }

    /**
     * Tests no content csm banner response
     *
     * @throws Exception
     */
    @Test
    public void testNoBannerCSMResponse() throws Exception {
        String bannerCSMString = TestResponsesUT.mediatedNoFillBanner();
        utAdResponse = new UTAdResponse(bannerCSMString,null,MediaType.BANNER, "v");

        assertNotNull(utAdResponse);
        LinkedList<BaseAdResponse> list = utAdResponse.getAdList();
        assertNotNull(utAdResponse.getAdList());
        System.out.println("Printing first");
        CSMSDKAdResponse baseCSMSDKAdResponse = (CSMSDKAdResponse) list.getFirst();
        assertEquals("csm", baseCSMSDKAdResponse.getContentSource());
        assertNull(baseCSMSDKAdResponse.getAdContent());
        assertEquals("44863345", baseCSMSDKAdResponse.getCreativeId());


    }

    /**
     * Tests ssm banner response
     *
     * @throws Exception
     */
    @Test
    public void testBannerSSMResponse() throws Exception {
        String bannerSSMString = TestResponsesUT.mediatedSSMBanner();
        utAdResponse = new UTAdResponse(bannerSSMString,null,MediaType.BANNER, "v");

        assertNotNull(utAdResponse);
        LinkedList<BaseAdResponse> list = utAdResponse.getAdList();
        assertNotNull(utAdResponse.getAdList());
        System.out.println("Printing first");
        SSMHTMLAdResponse baseSSMHTMLAdResponse = (SSMHTMLAdResponse) list.getFirst();
        assertEquals("ssm", baseSSMHTMLAdResponse.getContentSource());
        assertEquals((TestResponsesUT.SSM_URL), baseSSMHTMLAdResponse.getAdUrl());
        assertEquals("44863345",baseSSMHTMLAdResponse.getCreativeId());
    }

    /**
     * Tests no ssm banner response
     *
     * @throws Exception
     */
    @Test
    public void testBannerSSMNoURLResponse() throws Exception {
        String bannerSSMString = TestResponsesUT.mediatedNoSSMBanner();
        utAdResponse = new UTAdResponse(bannerSSMString,null,MediaType.BANNER, "v");

        assertNotNull(utAdResponse);
        @SuppressWarnings("UnusedAssignment") LinkedList<BaseAdResponse> list = utAdResponse.getAdList();
    }



    @Override
    public void tearDown() {
        super.tearDown();
    }
}
