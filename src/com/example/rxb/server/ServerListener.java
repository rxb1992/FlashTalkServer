package com.example.rxb.server;

import java.awt.TexturePaint;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Vector;

import javax.swing.plaf.synth.SynthEditorPaneUI;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.UUID;
import com.example.rxb.message.*;
import com.example.rxb.models.Client;
import com.example.rxb.models.TokenResult;
import com.example.rxb.util.*;

public class ServerListener {
	
	//public static int ID = 100;	
	public ServerSocket ss = null;
	public static final int TCP_PORT = 25033;//服务器端TCP连接端口号
	public static final int UDP_PORT = 6666;//服务器端UDP连接端口号
	
	//融云上我的app的key和secret
	private static String APP_KEY = "x18ywvqf80cmc";
	private static String APP_SECRET = "4Xo34sHwXvHPrR";
	private static int pageSize = 10;
	private static DatagramSocket ds = null;

	private Vector<Client> clients = new Vector<Client>();//客户端的集合
	private String userIP;//连接用户的ip
	private int userUDP_Port;
	private int msgType;
	private byte[] buffer = new byte[1024];//消息缓冲区
	
	public ServerListener() {

		try {
			//开启一个TCP连接，用于处理登陆
			ss = new ServerSocket(TCP_PORT);		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 建立连接
	 * 该方法创建两个链接
	 * 一个TCP连接：用来登陆
	 * 一个UDP连接：用来接收各种请求
	 * */
	public void connect() {

		//每连接一个新的客户端都开启一个UDP的线程用接收聊天消息
		new Thread(new UDPThread()).start();
		
		//下面的都是TCP的连接，只是用来连接用，获取IP，port,用户id,用户名
		Socket s = null;
		while (true) {
			if (ss == null || ss.isClosed()) {
				//System.exit(0);
				return;
			}
			try {
				
				String userId = null;//请求方的id
				String userName = null;//连接的用户名
				String userToken = null;//连接用户的Token
				
				//接收一个来自客户端的TCP请求（登陆的请求）
				s = ss.accept();				
				userIP = s.getInetAddress().getHostAddress();//获取客户机ip
						
				//读取客户端TCP连接时传过来的参数	
				DataInputStream dis = new DataInputStream(s.getInputStream());
				userId = dis.readUTF();//读取客户端的用户名
				userName = dis.readUTF();//读取客户端用户名
				userToken = dis.readUTF();//读取客户端的token
							
				//如果客户端没有传递userId，说明第一次连接，生成相应数据传回客户端记录
				if(userId==null || userId.length()==0)
				{			
					//获取用户的随机id和融云token	
					userId = new SimpleDateFormat("yyyyMMddHHmmssSSS") .format(new Date()).toString();
					userToken = getToken(userId,userName);
					
					//将用户id和用户的token返回到客户端		
					DataOutputStream dos = new DataOutputStream(s.getOutputStream());
					dos.writeInt(1);//1代表服务器取得id和token传回
					dos.writeUTF(userId);
					dos.writeUTF(userToken);
					System.out.println("一个用户上线IP="+ s.getInetAddress().getHostAddress() + "userToken="+ userToken);
				}
				else {
					//将用户id和用户的token返回到客户端		
					DataOutputStream dos = new DataOutputStream(s.getOutputStream());
					dos.writeInt(0);//客户端已有id和token,不需要1
				}

			} catch (IOException e) {
				e.printStackTrace();				
			} 
		}
	}

	//断开连接
	public void disConnect() {
		if (ss != null) {
			try {
				ss.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			ss = null;
		}
	}	

	//用户上线
	private void clientOn(String id,String name,String token){
		if(findClient(id)==null){
			//每连接一个客户端就维护一个客户端实例，并将其添加到客户端列表中
			Client cOn = new Client(userIP,userUDP_Port,id,name,token,"");
			clients.add(cOn);
			//for(int i=0;i<13;i++){
				//Client cOn = new Client(userIP,userUDP_Port,UUID.randomUUID().toString(),UUID.randomUUID()+userName,userToken);
				//clients.add(cOn);
			//}
			System.out.println("一个用户上线成功，当前在线人数："+clients.size());	
		}	
	}
	
	//用户下线
	private void clientOff(String userId){
		Client cOff = findClient(userId);
		if(cOff!=null){
			ServerListener.this.clients.remove(cOff);
			System.out.println("一个用户下线成功，当前在线人数："+clients.size());
		}
	}
	
	/**
	 * 返回在线人数消息
	 * 一次加载10个
	 * */
	private void askClientsCount(DatagramPacket dp,DatagramSocket ds,ByteArrayOutputStream baos,DataOutputStream dos,int page){	
		//一批数据的标识
		String identify = UUID.randomUUID().toString();		
		if(ServerListener.this.clients.size()>4){
			//大数据分批发送
			askClientByPager(dp, ds, baos, dos,identify,page);
		}
		else {
			askClientAll(dp, ds, baos, dos,identify);
		} 	
	}
	//小数据一次全部发送
	private void askClientAll(DatagramPacket dp,DatagramSocket ds,ByteArrayOutputStream baos,DataOutputStream dos,String identify){

		//在线人数写入的输出流
		try {

			dos.writeInt(msgType);//消息类型
			dos.writeUTF(identify);
			int clientCount = ServerListener.this.clients.size();		
			dos.writeInt(clientCount);
			dos.writeInt(Msg.PagerNotEnd);
			for(int i=0; i<ServerListener.this.clients.size(); i++) {
				String id = ServerListener.this.clients.get(i).userId;
				String name = ServerListener.this.clients.get(i).userName;
				String token = ServerListener.this.clients.get(i).userToken;
				String pic = ServerListener.this.clients.get(i).userPic;				
			
				dos.writeUTF(id);
				dos.writeUTF(name);//用户名
				dos.writeUTF(token);//用户token
				dos.writeUTF(pic);//用户pic					
			}
			dos.writeInt(Msg.MsgEnd);//一批请求的数据发完了
			buffer = baos.toByteArray();
			
			//释放DatagramPacket，把从客户端读取的消息清空，并把在线人数消息放入到DatagramPacket中
			dp = null;
			dp = new DatagramPacket(buffer, buffer.length);
			baos.reset();
						
			if (dp != null) {
				
				//循环的将消息发送到每个在线的客户端上
				for (int i = 0; i < ServerListener.this.clients.size(); i++) {
					Client c = ServerListener.this.clients.get(i);
					//设置要将此数据报发往的远程主机的 SocketAddress（通常为 IP 地址 + 端口号）
					dp.setSocketAddress(new InetSocketAddress(c.userIP,c.UDPPort));
					try {
						ds.send(dp);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	}
	//数据量太大拆包分批发送
	private void askClientByPager(DatagramPacket dp,DatagramSocket ds,ByteArrayOutputStream baos,DataOutputStream dos,String identify,int page){
		
		//一次加载10条数据
		int start = (page-1)*pageSize;//分批加载数据的起始位置
		int end = page*pageSize-1;//分批加载数据的截至位置

		//把在线人数的消息放入到一个btye的缓冲区数组中
		try {
			//在线人数写入的输出流
			int clientCount = ServerListener.this.clients.size()>end?end:ServerListener.this.clients.size()-1;		
			int clientCount2 = 0;
			int j = 0;
			int index = start;
			boolean check = true;

			while(check){
				dos.writeInt(msgType);//消息类型
				dos.writeUTF(identify);//一批数据的标识,用一个UUID来标识
				clientCount2 = (clientCount-index+1)>4?4:clientCount-index+1;
				dos.writeInt(clientCount2);
				
				if(clientCount==ServerListener.this.clients.size()-1){
					dos.writeInt(Msg.PagerEnd);//数据是否全部分页加载完成标记（1：是，0：否）
					
				}
				else{
					dos.writeInt(Msg.PagerNotEnd);//数据是否全部分页加载完成标记（1：是，0：否）
					
				}
			
				for(int i=index; i<=clientCount&&j<4; i++,j++) {
					String id = ServerListener.this.clients.get(i).userId;
					String name = ServerListener.this.clients.get(i).userName;
					String token = ServerListener.this.clients.get(i).userToken;
					String pic = ServerListener.this.clients.get(i).userPic;				
				
					dos.writeUTF(id);
					dos.writeUTF(name);//用户名
					dos.writeUTF(token);//用户token
					dos.writeUTF(pic);//用户pic
					index = i;

				}
				j=0;

				buffer = baos.toByteArray();
				
				//释放DatagramPacket，把从客户端读取的消息清空，并把在线人数消息放入到DatagramPacket中
				dp = null;
				dp = new DatagramPacket(buffer, buffer.length);
				baos.reset();
			
				if (dp != null) {
					
					//循环的将消息发送到每个在线的客户端上
					for (int i = 0; i < ServerListener.this.clients.size(); i++) {
						Client c = ServerListener.this.clients.get(i);
						//设置要将此数据报发往的远程主机的 SocketAddress（通常为 IP 地址 + 端口号）
						dp.setSocketAddress(new InetSocketAddress(userIP,userUDP_Port));
						try {
							ds.send(dp);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
				}
				
				if(index == clientCount){
					dos.writeInt(1);//一批请求的数据发完了
					check = false;
				}
				else{
					dos.writeInt(0);//一批请求的数据没发完
					index++;
				}
	
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	/**
	 * 让客户端重新绑定本用户的token
	 * 这个地方，一但一个用户额token失效重新获取后，不在线的用户没法知晓（该不在线用户在与之聊天时就不对了）。
	 * 要解决这个问题，只能把用户信息维护在自己服务器的数据库中，每次新用户登陆都从用户服务器获取最新的token（用户服务器的token一定要和融云的保持一致）
	 * */
	private void askClientToken(String id,String token,DatagramPacket dp,DatagramSocket ds,ByteArrayOutputStream baos,DataOutputStream dos){
		Client c0 = findClient(id);
		if(c0!=null){
			c0.userToken = token;
		}
		//消息类型写入输出流
		try {

			dos.writeInt(msgType);//消息类型
			dos.writeUTF(id);//用户id
			dos.writeUTF(token);//新的token		
			
			//把在线人数的消息放入到一个btye的缓冲区数组中
			buffer = baos.toByteArray();
			
			//释放DatagramPacket，把从客户端读取的消息清空，并把在线人数消息放入到DatagramPacket中
			dp = null;
			dp = new DatagramPacket(buffer, buffer.length);
			baos.reset();
			
			if (dp != null) {		
				//循环的将消息发送到每个在线的客户端上
				for (int i = 0; i < ServerListener.this.clients.size(); i++) {
					Client c = ServerListener.this.clients.get(i);
					//设置要将此数据报发往的远程主机的 SocketAddress（通常为 IP 地址 + 端口号）
					dp.setSocketAddress(new InetSocketAddress(c.userIP,c.UDPPort));
					ds.send(dp);
				}
			}
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 		
		
	}

	 //查找需要的在线客户端
	private Client findClient(String clientId) {
		Client c = null;
		for (int i = 0; i < clients.size(); i++) {
			c = clients.get(i);
			if(c.userId.equals(clientId)==false){
				c = null;
				continue;
			}
			else {
				break;
			}
		}
		return c;
	}
	
	//从融云获取token
	private String getToken(String id,String name){
		SdkHttpResult httpResult = null;
		try {
			httpResult = RongCloudHttp.getToken(
					APP_KEY, 
					APP_SECRET, 
				    id, 
				    name,
					"http://aa.com/a.png", 
					FormatType.json);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TokenResult tokenResult = (TokenResult) GsonUtil.fromJson(httpResult.toString(), TokenResult.class);
		return tokenResult.getResult().getToken()==null?"0":tokenResult.getResult().getToken();
	}

	//创建用户
	private void createUser(String id,String name,String token){
		Connection conn = DBUtil.getConn();
		Statement stat = DBUtil.getStat(conn);
		boolean result = false;
		StringBuilder sb = new StringBuilder();
		//sb.append("begin ");
		sb.append("delete from users where userID='"+id+"';");
		sb.append("insert into users(userID,userName,userToken,userIP,userPort) values('"+id+"','"+name+"','"+token+"','"+userIP+"',"+userUDP_Port+");");
		//sb.append(" end;");
		System.out.println(sb.toString());
		try {
			//stat.addBatch(sb.toString());
			stat.executeUpdate(sb.toString());
				
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		} finally {
			try {
				stat.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	//添加好友
	private void addFriend(String id,String friendid,DatagramPacket dp,DatagramSocket ds,ByteArrayOutputStream baos,DataOutputStream dos){
		Connection conn = DBUtil.getConn();
		Statement stat = DBUtil.getStat(conn);
		ResultSet rs = null;
		boolean result = false;
		StringBuilder sb1 = new StringBuilder();
		//sb1.append("begin ");
		sb1.append("delete from friends where userID='"+id+"' and friendID='"+friendid+"';");
		sb1.append("insert into friends(userID,friendID) values('"+id+"','"+friendid+"');");
		//sb1.append(" end;");
		
		StringBuilder sb2 = new StringBuilder();
		Client friClient = null;
		sb2.append("select userID,userName,userToken,userPic from users where userID='"+friendid+"'");
		try {
			stat.executeUpdate(sb1.toString());
			
			rs = stat.executeQuery(sb2.toString());
			while (rs.next()) {
				String name = rs.getString("userName");
				String token = rs.getString("userToken");
				String pic = rs.getString("userPic");		
				Client c = findClient(id);
				if(c!=null){
					dos.writeInt(msgType);
					dos.writeUTF(friendid);
					dos.writeUTF(name);//用户名
					dos.writeUTF(token);//用户token
					dos.writeUTF(pic==null?"":pic);//用户pic	
					
					buffer = baos.toByteArray();
					
					//释放DatagramPacket，把从客户端读取的消息清空，并把在线人数消息放入到DatagramPacket中
					dp = null;
					dp = new DatagramPacket(buffer, buffer.length);
					baos.reset();							
					if (dp != null) {						
						dp.setSocketAddress(new InetSocketAddress(c.userIP,c.UDPPort));		
					}
				}
				
			}
				
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				stat.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	//删除好友
	private void deleteFriend(String id,String friendid){
		Connection conn = DBUtil.getConn();
		Statement stat = DBUtil.getStat(conn);
		boolean result = false;
		String sql = "delete from friends where userID='"+id+"' and friendID='"+friendid+"'";
		try {
			stat.executeUpdate(sql);
				
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				stat.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	//获取好友
	private void getFriend(String myid,DatagramPacket dp,DatagramSocket ds,ByteArrayOutputStream baos,DataOutputStream dos){
		Connection conn = DBUtil.getConn();
		ResultSet rs = null;
		Statement stat = DBUtil.getStat(conn);
		boolean result = false;
		StringBuilder sb = new StringBuilder();
		sb.append("select userID,userName,userToken,userPic from users ");
		sb.append("where (userID in(select friendID FROM friends where userID = '"+myid+"') or userID='"+myid+"')");
		List<Client> clients = new ArrayList<Client>();
		try {
			rs = stat.executeQuery(sb.toString());
			String id = "";
			String name = "";
			String token = "";
			String pic = "";
			while(rs.next()){
				id = rs.getString("userID");
				name = rs.getString("userName");
				token = rs.getString("userToken");		
				pic = rs.getString("userPic");
				Client client = new Client(id,name,token,pic);
				clients.add(client);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close();
				stat.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		//一批数据的标识
		if(clients.size()>0){
			String identify = UUID.randomUUID().toString();		
			if(ServerListener.this.clients.size()>4){
				//大数据分批发送
				getFriendByPager(clients,myid,dp, ds, baos, dos,identify);
			}
			else {
				getFriendAll(clients,myid,dp, ds, baos, dos,identify);
			} 	
		}
	}	
	//小数据一次全部发送
	private void getFriendAll(List<Client> friList,String myid,DatagramPacket dp,DatagramSocket ds,ByteArrayOutputStream baos,DataOutputStream dos,String identify){
		//在线人数写入的输出流
		try {

			dos.writeInt(msgType);//消息类型
			dos.writeUTF(identify);
			int clientCount = friList.size();		
			dos.writeInt(clientCount);
			for(int i=0; i<friList.size(); i++) {
				String id = friList.get(i).userId;
				String name = friList.get(i).userName;
				String token = friList.get(i).userToken;
				String pic = friList.get(i).userPic==null?"":friList.get(i).userPic;				
			
				dos.writeUTF(id);
				dos.writeUTF(name);//用户名
				dos.writeUTF(token);//用户token
				dos.writeUTF(pic);//用户pic					
			}
			dos.writeInt(Msg.MsgEnd);//一批请求的数据发完了
			buffer = baos.toByteArray();
			
			//释放DatagramPacket，把从客户端读取的消息清空，并把在线人数消息放入到DatagramPacket中
			dp = null;
			dp = new DatagramPacket(buffer, buffer.length);
			baos.reset();
						
			if (dp != null) {

				Client c = findClient(myid);
				//设置要将此数据报发往的远程主机的 SocketAddress（通常为 IP 地址 + 端口号）
				if(c!=null){
					dp.setSocketAddress(new InetSocketAddress(c.userIP,c.UDPPort));
				}
				try {
					ds.send(dp);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	}
	//数据量太大拆包分批发送
	private void getFriendByPager(List<Client> friList,String myid,DatagramPacket dp,DatagramSocket ds,ByteArrayOutputStream baos,DataOutputStream dos,String identify){
		//把在线人数的消息放入到一个btye的缓冲区数组中
		try {
			//在线人数写入的输出流
			int friendCount = friList.size()-1;		
			int friendCount2 = 0;
			int j = 0;
			int index = 0;
			boolean check = true;

			while(check){
				dos.writeInt(msgType);//消息类型
				dos.writeUTF(identify);//一批数据的标识,用一个UUID来标识
				friendCount2 = (friendCount-index+1)>4?4:friendCount-index+1;
				dos.writeInt(friendCount2);
			
				for(int i=index; i<=friendCount&&j<4; i++,j++) {
					String id = ServerListener.this.clients.get(i).userId;
					String name = ServerListener.this.clients.get(i).userName;
					String token = ServerListener.this.clients.get(i).userToken;
					String pic = friList.get(i).userPic==null?"":friList.get(i).userPic;				
				
					dos.writeUTF(id);
					dos.writeUTF(name);//用户名
					dos.writeUTF(token);//用户token
					dos.writeUTF(pic);//用户pic
					index = i;

				}
				j=0;
				buffer = baos.toByteArray();
				
				//释放DatagramPacket，把从客户端读取的消息清空，并把在线人数消息放入到DatagramPacket中
				dp = null;
				dp = new DatagramPacket(buffer, buffer.length);
				baos.reset();
			
				if (dp != null) {

					Client c = findClient(myid);
					//设置要将此数据报发往的远程主机的 SocketAddress（通常为 IP 地址 + 端口号）
					if(c!=null){
						dp.setSocketAddress(new InetSocketAddress(c.userIP,c.UDPPort));
					}
				}
				
				if(index == friendCount){
					dos.writeInt(Msg.MsgEnd);//一批请求的数据发完了
					check = false;
				}
				else{
					dos.writeInt(Msg.MsgNotEnd);//一批请求的数据没发完
					index++;
				}
	
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	//开启了一个UDP的连接的线程
	private class UDPThread implements Runnable {	
		@Override
		public void run() {
			try {
				//用于接收和发送UDP的Socket实例
				ds = new DatagramSocket(UDP_PORT);
				
			} catch (SocketException e1) {
				e1.printStackTrace();
			}

			while (ds != null) {
				try {
				
					if(ds.isClosed()) {
						System.exit(0);
					}
					
					//DatagramPacket用于处理报文，它将Byte数组、目标地址、目标端口等数据包装成报文或者将报文拆卸成Byte数组
					//发送给客户端的报文包
					//将数据包中buffer.length长的数据装进buffer数组，一般用来接收客户端发送的数据。
					DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
					
					//发给客户端的输出流
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(baos);		
									
					//接收数据报文放到dp中。
					//receive方法产生一个“阻塞”。“阻塞”是一个专业名词，它会产生一个内部循环，使程序暂停在这个地方，直到一个条件触发。
					//该方法会阻塞用来接收各自客户端传来的消息
					ds.receive(dp);
					userUDP_Port = dp.getPort();//获取客户端udp消息的端口
				
					//接收到客户端输入的流
					ByteArrayInputStream bais = new ByteArrayInputStream(dp.getData());
					DataInputStream dis = new DataInputStream(bais);		
					
					msgType = dis.readInt();//获取消息类型
					String userid = dis.readUTF();//请求方的id
					
					switch (msgType) {		
					case Msg.ClientOnMsg://用户上线			
						String name = dis.readUTF();		
						String token = dis.readUTF();
						clientOn(userid, name,token);	
						//askCientsCount(dp, ds, baos, dos);
						break;
						
					case Msg.ClientOffMsg://用户下线
						clientOff(userid);
						//askCientsCount(dp, ds, baos, dos);
						break;						
						
					case Msg.AskClientsMsg://返回在线人数信息				
						//返回在线人数信息
						int page = dis.readInt();//当前请求页
						askClientsCount(dp,ds,baos,dos,page);
						break;						
						
					case Msg.GetTokenMsg://获取token	
						//重新获取token,并绑定
						String name2 = dis.readUTF();		
						String token2 = getToken(userid,name2);
						askClientToken(userid,token2,dp, ds,baos,dos);
						break;
						
					case Msg.CreateUserMsg://创建用户	
						String name3 = dis.readUTF();		
						String token3 = dis.readUTF();
						createUser(userid, name3, token3);
						break;
						
					case Msg.AddFriendMsg://添加好友
						String friendid = dis.readUTF();
						addFriend(userid,friendid,dp, ds,baos,dos);
						break;
						
					case Msg.DeleteFriendMsg://删除好友	
						String friendid2 = dis.readUTF();
						deleteFriend(userid, friendid2);
						break;
						
					case Msg.GetFriendMsg://获取用户	
						getFriend(userid,dp, ds,baos,dos);
						break;
						
					default:
						break;
						
					}
									
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
