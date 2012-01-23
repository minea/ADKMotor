package com.minea.android.adkmotor;

import java.util.HashMap;

public class CommandClass {
	// メンバ変数
	HashMap<String, String> commandHm;
	CommandClass primaryConnection, secondaryConnection;

	// コンストラクタ
	public CommandClass(){
		
	}
	
	public void setAttribute(String name, String attri){
		// name:ユニークな変数名 attri:ローレベルコマンド
		commandHm.put(name,attri);
	}
	
	public HashMap<String,String> getAttribute(){
		return commandHm;
	}
}