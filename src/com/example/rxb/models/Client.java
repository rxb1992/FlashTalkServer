package com.example.rxb.models;

public class Client {
	public String userIP;
	public int UDPPort;
	public String userId;
	public String userName;
	public String userToken;
	public String userPic;

	public Client(String IP, int UDPPort,String id, String name,String token,String pic) {
		this.userIP = IP;
		this.UDPPort = UDPPort;
		this.userId = id;
		this.userName = name;
		this.userToken = token;
		this.userPic = pic;
	}
	
	public Client(String id, String name,String token,String pic) {

		this.userId = id;
		this.userName = name;
		this.userToken = token;
		this.userPic = pic;
	}
}
