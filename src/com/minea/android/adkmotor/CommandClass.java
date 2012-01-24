package com.minea.android.adkmotor;

import java.util.HashMap;

public class CommandClass {
	// メンバ変数
	HashMap<String, String> commandHm;
	CommandClass primaryConnection, secondaryConnection;

	// コンストラクタ
	public CommandClass(){
		commandHm = new HashMap<String,String>();
	}
	
	public void setAttribute(String name, String attri){
		// name:ユニークな変数名 attri:ローレベルコマンド
		commandHm.put(name,attri);
	}
	
	public void setPrimaryConnection(CommandClass c){
		primaryConnection = c;
	}
	
	public CommandClass getPrimaryConnection(){
		return primaryConnection;
	}
	
	public void setsecondaryConnection(CommandClass c){
		secondaryConnection = c;
	}
	
	public CommandClass getsecondaryConnection(){
		return secondaryConnection;
	}
	
	public HashMap<String, String> getAttribute(){
		return commandHm;
	}
}