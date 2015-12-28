package com.example.rxb.util;

import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;

public class CodeUtil {
	
	public static String hexSHA1(String value) {
		try {
			//创建具有指定算法名称的MessageDigest 实例对象。
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			//计算数据的摘要的第二步是向已初始化的MessageDigest对象提供传送要计算的数据。
			//这将通过一次或多次调用以下某个 update（更新）方法来完成：
			md.update(value.getBytes("utf-8"));
			
			//计算消息摘要
			byte[] digest = md.digest();
			return byteToHexString(digest);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static String byteToHexString(byte[] bytes) {
		return String.valueOf(Hex.encodeHex(bytes));
	}
}
