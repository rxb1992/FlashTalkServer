package com.example.rxb.util;

//对server sdk返回的封装
public class SdkHttpResult {

	private int code;
	private String result;

	public SdkHttpResult(int code, String result) {
		this.code = code;
		this.result = result;
	}

	public int getHttpCode() {
		return code;
	}

	public String getResult() {
		return result;
	}

	@Override
	//返回了一个json格式的串
	public String toString() {
		return String.format("{\"code\":\"%s\",\"result\":%s}", code,
				result);
	}
}
