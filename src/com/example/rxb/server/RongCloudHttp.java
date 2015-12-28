package com.example.rxb.server;

import java.net.HttpURLConnection;
import java.net.URLEncoder;

import com.example.rxb.message.*;
import com.example.rxb.util.*;

public class RongCloudHttp {
	
	private static final String RONGCLOUDURI = "http://api.cn.ronghub.com";
	private static final String UTF8 = "UTF-8";
	
	/**
	 * 获取token
	 * */ 
	public static SdkHttpResult getToken(
			String appKey, 
			String appSecret,
			String userId, 
			String userName, 
			String portraitUri,
			FormatType format) throws Exception {

		HttpURLConnection conn = HttpUtil.CreatePostHttpConnection(
				appKey, 
				appSecret, 
				RONGCLOUDURI+ "/user/getToken." + format.toString());

		StringBuilder sb = new StringBuilder();
		sb.append("userId=").append(URLEncoder.encode(userId, UTF8));
		sb.append("&name=").append(URLEncoder.encode(userName==null?"":userName, UTF8));
		sb.append("&portraitUri=").append(URLEncoder.encode(portraitUri==null?"":portraitUri, UTF8));
		HttpUtil.setBodyParameter(sb, conn);

		return HttpUtil.returnResult(conn);
	}
	
	/**
	 * 检查用户在线状态
	 * */ 
	public static SdkHttpResult checkOnline(
			String appKey, 
			String appSecret,
			String userId, 
			FormatType format) throws Exception {

		HttpURLConnection conn = HttpUtil.CreatePostHttpConnection(appKey,
				appSecret,
				RONGCLOUDURI + "/user/checkOnline." + format.toString());

		StringBuilder sb = new StringBuilder();
		sb.append("userId=").append(URLEncoder.encode(userId, UTF8));
		HttpUtil.setBodyParameter(sb, conn);

		return HttpUtil.returnResult(conn);
	}

	/**
	 * 刷新用户信息
	 * */ 
	public static SdkHttpResult refreshUser(
			String appKey, 
			String appSecret,
			String userId, 
			String userName, 
			String portraitUri,
			FormatType format) throws Exception {

		HttpURLConnection conn = HttpUtil.CreatePostHttpConnection(appKey,
				appSecret, RONGCLOUDURI + "/user/refresh." + format.toString());

		StringBuilder sb = new StringBuilder();
		sb.append("userId=").append(URLEncoder.encode(userId, UTF8));
		if (userName != null) {
			sb.append("&name=").append(URLEncoder.encode(userName, UTF8));
		}
		if (portraitUri != null) {
			sb.append("&portraitUri=").append(
					URLEncoder.encode(portraitUri, UTF8));
		}

		HttpUtil.setBodyParameter(sb, conn);

		return HttpUtil.returnResult(conn);
	}	
}
