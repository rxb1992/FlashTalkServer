package com.example.rxb.models;

public class TokenResult {

	private String code;
	private Result result;
	

	
	public String getCode() {
		return code;
	}



	public void setCode(String code) {
		this.code = code;
	}



	public Result getResult() {
		return result;
	}



	public void setResult(Result result) {
		this.result = result;
	}



	public class Result{
		private String code;
		private String userId;
		private String token;
		public String getCode() {
			return code;
		}
		public void setCode(String code) {
			this.code = code;
		}
		public String getUserId() {
			return userId;
		}
		public void setUserId(String userId) {
			this.userId = userId;
		}
		public String getToken() {
			return token;
		}
		public void setToken(String token) {
			this.token = token;
		}
		
	}
}
