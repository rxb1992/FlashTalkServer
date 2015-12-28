package com.example.rxb.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class HttpUtil {

	private static final String APPKEY = "RC-App-Key";
	private static final String NONCE = "RC-Nonce";
	private static final String TIMESTAMP = "RC-Timestamp";
	private static final String SIGNATURE = "RC-Signature";

	/**
	 * 设置body体
	 * 也就是传递接口的参数
	 * */ 
	public static void setBodyParameter(StringBuilder sb, HttpURLConnection conn)
			throws IOException {
		DataOutputStream out = new DataOutputStream(conn.getOutputStream());
		out.writeBytes(sb.toString());
		out.flush();
		out.close();
	}

	/**
	 * 添加签名header
	 * 获取Http连接
	 * */ 
	public static HttpURLConnection CreatePostHttpConnection(String appKey,
			String appSecret, String uri) throws MalformedURLException,
			IOException, ProtocolException {
		String nonce = String.valueOf(Math.random() * 1000000);
		String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
		StringBuilder toSign = new StringBuilder(appSecret).append(nonce)
				.append(timestamp);
		String sign = CodeUtil.hexSHA1(toSign.toString());

		URL url = new URL(uri);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setUseCaches(false);//禁止使用文档缓存
		conn.setDoInput(true);//使用 URL 连接进行输入
		conn.setDoOutput(true);//使用URL连接进行输出(因为这个是post请求，参数要放在http正文内，因此需要设为true)
		conn.setRequestMethod("POST");//请求方式为post
		conn.setInstanceFollowRedirects(true);//设置此 HttpURLConnection 实例是否应该自动执行 HTTP 重定向
		/**
		 * 设置一个指定的超时值（以毫秒为单位），该值将在打开到此 URLConnection 引用的资源的通信链接时使用。如果在建立连接之前超时期满，则会引发一个 java.net.SocketTimeoutException。超时时间为零表示无穷大超时。
		 * 此方法的一些非标准实现可能忽略指定的超时。要查看连接超时设置，请调用 getConnectTimeout()。
		 * */
		conn.setConnectTimeout(30000);
		/**
		 * 将读超时设置为指定的超时值，以毫秒为单位。用一个非零值指定在建立到资源的连接后从 Input 流读入时的超时时间。如果在数据可读取之前超时期满，则会引发一个 java.net.SocketTimeoutException。超时时间为零表示无穷大超时。
		 * 此方法的一些非标准实现会忽略指定的超时。要查看读入超时设置，请调用 getReadTimeout()。
		 * */
		conn.setReadTimeout(30000);

		//设置一般请求属性。如果已存在具有该关键字的属性，则用新值改写其值。
		//这些参数是融云的签名规则校验的
		conn.setRequestProperty(APPKEY, appKey);
		conn.setRequestProperty(NONCE, nonce);
		conn.setRequestProperty(TIMESTAMP, timestamp);
		conn.setRequestProperty(SIGNATURE, sign);
		conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");

		return conn;
	}

	/**
	 * 返回Http请求响应的数据
	 * */
	public static SdkHttpResult returnResult(HttpURLConnection conn)
			throws Exception, IOException {
		String result;
		InputStream input = null;
		/**
		 * 从 HTTP 响应消息获取状态码。例如，就以下状态行来说：
		 * HTTP/1.0 200 OK
		 * HTTP/1.0 401 Unauthorized
		 * 将分别返回 200 和 401。如果无法从响应中识别任何代码（即响应不是有效的 HTTP），则返回 -1
		 * */
		if (conn.getResponseCode() == 200) {
			//获取响应的输入流
			input = conn.getInputStream();
			
		} else {
			//如果连接失败但服务器仍然发送了有用数据，则返回错误流
			input = conn.getErrorStream();
		}
		result = new String(readInputStream(input));
		return new SdkHttpResult(conn.getResponseCode(), result);
	}
	
	/**
	 * 解析返回请求的输入流
	 * */
	public static byte[] readInputStream(InputStream inStream) throws Exception {
		
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		
		byte[] buffer = new byte[1024];
		int len = 0;
		//从输入流中读取一定数量的字节，并将其存储在缓冲区数组 b 中
		//-1代表读到文件尾
		while ((len = inStream.read(buffer)) != -1) {
			outStream.write(buffer, 0, len);
		}
		byte[] data = outStream.toByteArray();
		outStream.close();
		inStream.close();
		return data;
	}
}
