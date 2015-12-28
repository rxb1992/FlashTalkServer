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
	public static final int TCP_PORT = 25033;//��������TCP���Ӷ˿ں�
	public static final int UDP_PORT = 6666;//��������UDP���Ӷ˿ں�
	
	//�������ҵ�app��key��secret
	private static String APP_KEY = "x18ywvqf80cmc";
	private static String APP_SECRET = "4Xo34sHwXvHPrR";
	private static int pageSize = 10;
	private static DatagramSocket ds = null;

	private Vector<Client> clients = new Vector<Client>();//�ͻ��˵ļ���
	private String userIP;//�����û���ip
	private int userUDP_Port;
	private int msgType;
	private byte[] buffer = new byte[1024];//��Ϣ������
	
	public ServerListener() {

		try {
			//����һ��TCP���ӣ����ڴ����½
			ss = new ServerSocket(TCP_PORT);		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * ��������
	 * �÷���������������
	 * һ��TCP���ӣ�������½
	 * һ��UDP���ӣ��������ո�������
	 * */
	public void connect() {

		//ÿ����һ���µĿͻ��˶�����һ��UDP���߳��ý���������Ϣ
		new Thread(new UDPThread()).start();
		
		//����Ķ���TCP�����ӣ�ֻ�����������ã���ȡIP��port,�û�id,�û���
		Socket s = null;
		while (true) {
			if (ss == null || ss.isClosed()) {
				//System.exit(0);
				return;
			}
			try {
				
				String userId = null;//���󷽵�id
				String userName = null;//���ӵ��û���
				String userToken = null;//�����û���Token
				
				//����һ�����Կͻ��˵�TCP���󣨵�½������
				s = ss.accept();				
				userIP = s.getInetAddress().getHostAddress();//��ȡ�ͻ���ip
						
				//��ȡ�ͻ���TCP����ʱ�������Ĳ���	
				DataInputStream dis = new DataInputStream(s.getInputStream());
				userId = dis.readUTF();//��ȡ�ͻ��˵��û���
				userName = dis.readUTF();//��ȡ�ͻ����û���
				userToken = dis.readUTF();//��ȡ�ͻ��˵�token
							
				//����ͻ���û�д���userId��˵����һ�����ӣ�������Ӧ���ݴ��ؿͻ��˼�¼
				if(userId==null || userId.length()==0)
				{			
					//��ȡ�û������id������token	
					userId = new SimpleDateFormat("yyyyMMddHHmmssSSS") .format(new Date()).toString();
					userToken = getToken(userId,userName);
					
					//���û�id���û���token���ص��ͻ���		
					DataOutputStream dos = new DataOutputStream(s.getOutputStream());
					dos.writeInt(1);//1���������ȡ��id��token����
					dos.writeUTF(userId);
					dos.writeUTF(userToken);
					System.out.println("һ���û�����IP="+ s.getInetAddress().getHostAddress() + "userToken="+ userToken);
				}
				else {
					//���û�id���û���token���ص��ͻ���		
					DataOutputStream dos = new DataOutputStream(s.getOutputStream());
					dos.writeInt(0);//�ͻ�������id��token,����Ҫ1
				}

			} catch (IOException e) {
				e.printStackTrace();				
			} 
		}
	}

	//�Ͽ�����
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

	//�û�����
	private void clientOn(String id,String name,String token){
		if(findClient(id)==null){
			//ÿ����һ���ͻ��˾�ά��һ���ͻ���ʵ������������ӵ��ͻ����б���
			Client cOn = new Client(userIP,userUDP_Port,id,name,token,"");
			clients.add(cOn);
			//for(int i=0;i<13;i++){
				//Client cOn = new Client(userIP,userUDP_Port,UUID.randomUUID().toString(),UUID.randomUUID()+userName,userToken);
				//clients.add(cOn);
			//}
			System.out.println("һ���û����߳ɹ�����ǰ����������"+clients.size());	
		}	
	}
	
	//�û�����
	private void clientOff(String userId){
		Client cOff = findClient(userId);
		if(cOff!=null){
			ServerListener.this.clients.remove(cOff);
			System.out.println("һ���û����߳ɹ�����ǰ����������"+clients.size());
		}
	}
	
	/**
	 * ��������������Ϣ
	 * һ�μ���10��
	 * */
	private void askClientsCount(DatagramPacket dp,DatagramSocket ds,ByteArrayOutputStream baos,DataOutputStream dos,int page){	
		//һ�����ݵı�ʶ
		String identify = UUID.randomUUID().toString();		
		if(ServerListener.this.clients.size()>4){
			//�����ݷ�������
			askClientByPager(dp, ds, baos, dos,identify,page);
		}
		else {
			askClientAll(dp, ds, baos, dos,identify);
		} 	
	}
	//С����һ��ȫ������
	private void askClientAll(DatagramPacket dp,DatagramSocket ds,ByteArrayOutputStream baos,DataOutputStream dos,String identify){

		//��������д��������
		try {

			dos.writeInt(msgType);//��Ϣ����
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
				dos.writeUTF(name);//�û���
				dos.writeUTF(token);//�û�token
				dos.writeUTF(pic);//�û�pic					
			}
			dos.writeInt(Msg.MsgEnd);//һ����������ݷ�����
			buffer = baos.toByteArray();
			
			//�ͷ�DatagramPacket���Ѵӿͻ��˶�ȡ����Ϣ��գ���������������Ϣ���뵽DatagramPacket��
			dp = null;
			dp = new DatagramPacket(buffer, buffer.length);
			baos.reset();
						
			if (dp != null) {
				
				//ѭ���Ľ���Ϣ���͵�ÿ�����ߵĿͻ�����
				for (int i = 0; i < ServerListener.this.clients.size(); i++) {
					Client c = ServerListener.this.clients.get(i);
					//����Ҫ�������ݱ�������Զ�������� SocketAddress��ͨ��Ϊ IP ��ַ + �˿ںţ�
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
	//������̫������������
	private void askClientByPager(DatagramPacket dp,DatagramSocket ds,ByteArrayOutputStream baos,DataOutputStream dos,String identify,int page){
		
		//һ�μ���10������
		int start = (page-1)*pageSize;//�����������ݵ���ʼλ��
		int end = page*pageSize-1;//�����������ݵĽ���λ��

		//��������������Ϣ���뵽һ��btye�Ļ�����������
		try {
			//��������д��������
			int clientCount = ServerListener.this.clients.size()>end?end:ServerListener.this.clients.size()-1;		
			int clientCount2 = 0;
			int j = 0;
			int index = start;
			boolean check = true;

			while(check){
				dos.writeInt(msgType);//��Ϣ����
				dos.writeUTF(identify);//һ�����ݵı�ʶ,��һ��UUID����ʶ
				clientCount2 = (clientCount-index+1)>4?4:clientCount-index+1;
				dos.writeInt(clientCount2);
				
				if(clientCount==ServerListener.this.clients.size()-1){
					dos.writeInt(Msg.PagerEnd);//�����Ƿ�ȫ����ҳ������ɱ�ǣ�1���ǣ�0����
					
				}
				else{
					dos.writeInt(Msg.PagerNotEnd);//�����Ƿ�ȫ����ҳ������ɱ�ǣ�1���ǣ�0����
					
				}
			
				for(int i=index; i<=clientCount&&j<4; i++,j++) {
					String id = ServerListener.this.clients.get(i).userId;
					String name = ServerListener.this.clients.get(i).userName;
					String token = ServerListener.this.clients.get(i).userToken;
					String pic = ServerListener.this.clients.get(i).userPic;				
				
					dos.writeUTF(id);
					dos.writeUTF(name);//�û���
					dos.writeUTF(token);//�û�token
					dos.writeUTF(pic);//�û�pic
					index = i;

				}
				j=0;

				buffer = baos.toByteArray();
				
				//�ͷ�DatagramPacket���Ѵӿͻ��˶�ȡ����Ϣ��գ���������������Ϣ���뵽DatagramPacket��
				dp = null;
				dp = new DatagramPacket(buffer, buffer.length);
				baos.reset();
			
				if (dp != null) {
					
					//ѭ���Ľ���Ϣ���͵�ÿ�����ߵĿͻ�����
					for (int i = 0; i < ServerListener.this.clients.size(); i++) {
						Client c = ServerListener.this.clients.get(i);
						//����Ҫ�������ݱ�������Զ�������� SocketAddress��ͨ��Ϊ IP ��ַ + �˿ںţ�
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
					dos.writeInt(1);//һ����������ݷ�����
					check = false;
				}
				else{
					dos.writeInt(0);//һ�����������û����
					index++;
				}
	
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	/**
	 * �ÿͻ������°󶨱��û���token
	 * ����ط���һ��һ���û���tokenʧЧ���»�ȡ�󣬲����ߵ��û�û��֪�����ò������û�����֮����ʱ�Ͳ����ˣ���
	 * Ҫ���������⣬ֻ�ܰ��û���Ϣά�����Լ������������ݿ��У�ÿ�����û���½�����û���������ȡ���µ�token���û���������tokenһ��Ҫ�����Ƶı���һ�£�
	 * */
	private void askClientToken(String id,String token,DatagramPacket dp,DatagramSocket ds,ByteArrayOutputStream baos,DataOutputStream dos){
		Client c0 = findClient(id);
		if(c0!=null){
			c0.userToken = token;
		}
		//��Ϣ����д�������
		try {

			dos.writeInt(msgType);//��Ϣ����
			dos.writeUTF(id);//�û�id
			dos.writeUTF(token);//�µ�token		
			
			//��������������Ϣ���뵽һ��btye�Ļ�����������
			buffer = baos.toByteArray();
			
			//�ͷ�DatagramPacket���Ѵӿͻ��˶�ȡ����Ϣ��գ���������������Ϣ���뵽DatagramPacket��
			dp = null;
			dp = new DatagramPacket(buffer, buffer.length);
			baos.reset();
			
			if (dp != null) {		
				//ѭ���Ľ���Ϣ���͵�ÿ�����ߵĿͻ�����
				for (int i = 0; i < ServerListener.this.clients.size(); i++) {
					Client c = ServerListener.this.clients.get(i);
					//����Ҫ�������ݱ�������Զ�������� SocketAddress��ͨ��Ϊ IP ��ַ + �˿ںţ�
					dp.setSocketAddress(new InetSocketAddress(c.userIP,c.UDPPort));
					ds.send(dp);
				}
			}
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 		
		
	}

	 //������Ҫ�����߿ͻ���
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
	
	//�����ƻ�ȡtoken
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

	//�����û�
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
	
	//��Ӻ���
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
					dos.writeUTF(name);//�û���
					dos.writeUTF(token);//�û�token
					dos.writeUTF(pic==null?"":pic);//�û�pic	
					
					buffer = baos.toByteArray();
					
					//�ͷ�DatagramPacket���Ѵӿͻ��˶�ȡ����Ϣ��գ���������������Ϣ���뵽DatagramPacket��
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
	
	//ɾ������
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

	//��ȡ����
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
		//һ�����ݵı�ʶ
		if(clients.size()>0){
			String identify = UUID.randomUUID().toString();		
			if(ServerListener.this.clients.size()>4){
				//�����ݷ�������
				getFriendByPager(clients,myid,dp, ds, baos, dos,identify);
			}
			else {
				getFriendAll(clients,myid,dp, ds, baos, dos,identify);
			} 	
		}
	}	
	//С����һ��ȫ������
	private void getFriendAll(List<Client> friList,String myid,DatagramPacket dp,DatagramSocket ds,ByteArrayOutputStream baos,DataOutputStream dos,String identify){
		//��������д��������
		try {

			dos.writeInt(msgType);//��Ϣ����
			dos.writeUTF(identify);
			int clientCount = friList.size();		
			dos.writeInt(clientCount);
			for(int i=0; i<friList.size(); i++) {
				String id = friList.get(i).userId;
				String name = friList.get(i).userName;
				String token = friList.get(i).userToken;
				String pic = friList.get(i).userPic==null?"":friList.get(i).userPic;				
			
				dos.writeUTF(id);
				dos.writeUTF(name);//�û���
				dos.writeUTF(token);//�û�token
				dos.writeUTF(pic);//�û�pic					
			}
			dos.writeInt(Msg.MsgEnd);//һ����������ݷ�����
			buffer = baos.toByteArray();
			
			//�ͷ�DatagramPacket���Ѵӿͻ��˶�ȡ����Ϣ��գ���������������Ϣ���뵽DatagramPacket��
			dp = null;
			dp = new DatagramPacket(buffer, buffer.length);
			baos.reset();
						
			if (dp != null) {

				Client c = findClient(myid);
				//����Ҫ�������ݱ�������Զ�������� SocketAddress��ͨ��Ϊ IP ��ַ + �˿ںţ�
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
	//������̫������������
	private void getFriendByPager(List<Client> friList,String myid,DatagramPacket dp,DatagramSocket ds,ByteArrayOutputStream baos,DataOutputStream dos,String identify){
		//��������������Ϣ���뵽һ��btye�Ļ�����������
		try {
			//��������д��������
			int friendCount = friList.size()-1;		
			int friendCount2 = 0;
			int j = 0;
			int index = 0;
			boolean check = true;

			while(check){
				dos.writeInt(msgType);//��Ϣ����
				dos.writeUTF(identify);//һ�����ݵı�ʶ,��һ��UUID����ʶ
				friendCount2 = (friendCount-index+1)>4?4:friendCount-index+1;
				dos.writeInt(friendCount2);
			
				for(int i=index; i<=friendCount&&j<4; i++,j++) {
					String id = ServerListener.this.clients.get(i).userId;
					String name = ServerListener.this.clients.get(i).userName;
					String token = ServerListener.this.clients.get(i).userToken;
					String pic = friList.get(i).userPic==null?"":friList.get(i).userPic;				
				
					dos.writeUTF(id);
					dos.writeUTF(name);//�û���
					dos.writeUTF(token);//�û�token
					dos.writeUTF(pic);//�û�pic
					index = i;

				}
				j=0;
				buffer = baos.toByteArray();
				
				//�ͷ�DatagramPacket���Ѵӿͻ��˶�ȡ����Ϣ��գ���������������Ϣ���뵽DatagramPacket��
				dp = null;
				dp = new DatagramPacket(buffer, buffer.length);
				baos.reset();
			
				if (dp != null) {

					Client c = findClient(myid);
					//����Ҫ�������ݱ�������Զ�������� SocketAddress��ͨ��Ϊ IP ��ַ + �˿ںţ�
					if(c!=null){
						dp.setSocketAddress(new InetSocketAddress(c.userIP,c.UDPPort));
					}
				}
				
				if(index == friendCount){
					dos.writeInt(Msg.MsgEnd);//һ����������ݷ�����
					check = false;
				}
				else{
					dos.writeInt(Msg.MsgNotEnd);//һ�����������û����
					index++;
				}
	
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	//������һ��UDP�����ӵ��߳�
	private class UDPThread implements Runnable {	
		@Override
		public void run() {
			try {
				//���ڽ��պͷ���UDP��Socketʵ��
				ds = new DatagramSocket(UDP_PORT);
				
			} catch (SocketException e1) {
				e1.printStackTrace();
			}

			while (ds != null) {
				try {
				
					if(ds.isClosed()) {
						System.exit(0);
					}
					
					//DatagramPacket���ڴ����ģ�����Byte���顢Ŀ���ַ��Ŀ��˿ڵ����ݰ�װ�ɱ��Ļ��߽����Ĳ�ж��Byte����
					//���͸��ͻ��˵ı��İ�
					//�����ݰ���buffer.length��������װ��buffer���飬һ���������տͻ��˷��͵����ݡ�
					DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
					
					//�����ͻ��˵������
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(baos);		
									
					//�������ݱ��ķŵ�dp�С�
					//receive��������һ����������������������һ��רҵ���ʣ��������һ���ڲ�ѭ����ʹ������ͣ������ط���ֱ��һ������������
					//�÷����������������ո��Կͻ��˴�������Ϣ
					ds.receive(dp);
					userUDP_Port = dp.getPort();//��ȡ�ͻ���udp��Ϣ�Ķ˿�
				
					//���յ��ͻ����������
					ByteArrayInputStream bais = new ByteArrayInputStream(dp.getData());
					DataInputStream dis = new DataInputStream(bais);		
					
					msgType = dis.readInt();//��ȡ��Ϣ����
					String userid = dis.readUTF();//���󷽵�id
					
					switch (msgType) {		
					case Msg.ClientOnMsg://�û�����			
						String name = dis.readUTF();		
						String token = dis.readUTF();
						clientOn(userid, name,token);	
						//askCientsCount(dp, ds, baos, dos);
						break;
						
					case Msg.ClientOffMsg://�û�����
						clientOff(userid);
						//askCientsCount(dp, ds, baos, dos);
						break;						
						
					case Msg.AskClientsMsg://��������������Ϣ				
						//��������������Ϣ
						int page = dis.readInt();//��ǰ����ҳ
						askClientsCount(dp,ds,baos,dos,page);
						break;						
						
					case Msg.GetTokenMsg://��ȡtoken	
						//���»�ȡtoken,����
						String name2 = dis.readUTF();		
						String token2 = getToken(userid,name2);
						askClientToken(userid,token2,dp, ds,baos,dos);
						break;
						
					case Msg.CreateUserMsg://�����û�	
						String name3 = dis.readUTF();		
						String token3 = dis.readUTF();
						createUser(userid, name3, token3);
						break;
						
					case Msg.AddFriendMsg://��Ӻ���
						String friendid = dis.readUTF();
						addFriend(userid,friendid,dp, ds,baos,dos);
						break;
						
					case Msg.DeleteFriendMsg://ɾ������	
						String friendid2 = dis.readUTF();
						deleteFriend(userid, friendid2);
						break;
						
					case Msg.GetFriendMsg://��ȡ�û�	
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
