package com.example.rxb.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
/**
 * 链接数据库的dao
 * @author rxb-pc
 *
 */
public class DBUtil {
	
	public static Connection getConn() {
		Connection conn = null;
		try {		
			Class.forName("com.mysql.jdbc.Driver");//加载MySql的驱动类，成功加载后，会将Driver类的实例注册到DriverManager类中。      
			//Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		String url = "jdbc:mysql://localhost:3306/talk?allowMultiQueries=true";
		try {
			conn = DriverManager.getConnection(url,"root","root");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return conn;
	}

	public static Statement getStat(Connection conn) {
		Statement stat = null;
		try {
			stat = conn.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return stat;
	}
}
