package com.example.rxb.server;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ServerManager extends JFrame implements ActionListener {
	
	private JButton jbStart = new JButton("����������");
	private JButton jbEnd = new JButton("�رշ�����");
	private boolean isStarted = false;
	private ServerListener serverListener = null;
	
	//��������
	public static void main(String[] args) {
		new ServerManager();
	}
	
	//�������ļ���
	public ServerManager() {
		this.setTitle("����˹�����");
		this.setVisible(true);
		this.setLocation(300, 200);
		this.add(jbStart);
		this.add(jbEnd);
		jbStart.addActionListener(this);
		jbEnd.addActionListener(this);
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.out.println("shut down the server");
				jbEnd.doClick();
				System.exit(0);
			}
		});
		this.setLayout(new FlowLayout());
		this.pack();
		this.setResizable(false);
	}

	//��ť�¼� 
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == jbStart) {
			if(!isStarted) {
				new Thread(new ServerThread()).start();
				JOptionPane.showMessageDialog(this, "OK���������Ѿ�������");
				this.jbStart.setEnabled(false);
				isStarted = true;
			}
		}
		if(e.getSource() == jbEnd) {
			if(serverListener != null) {
				Socket s = null;
				try {
					s = new Socket(serverListener.ss.getInetAddress(),ServerListener.TCP_PORT);
					DataOutputStream dos = new DataOutputStream(s.getOutputStream());
					dos.writeInt(0);
				} catch (IOException e1) {
					e1.printStackTrace();
				} finally {
					if(s != null) {
						try {
							//s.close();
							serverListener.ss.close();
							this.jbStart.setEnabled(true);
							isStarted = false;
							//System.exit(0);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	//����һ�������������߳�
	private class ServerThread implements Runnable {

		public void run() {
			serverListener = new ServerListener();
			serverListener.connect();
			
		}
		
	}

}

