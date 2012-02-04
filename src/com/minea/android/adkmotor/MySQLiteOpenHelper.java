package com.minea.android.adkmotor;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class MySQLiteOpenHelper extends SQLiteOpenHelper {
		static final String DB_NAME = null;
		// DB名
		static final int DB_VERSION = 1;
		// DBのVersion
		// SQL文をStringに保持しておく
		static String CREATE_TABLE = null;
		static final String DROP_TABLE = "drop table mytable;";

		// コンストラクタ
		// CREATE用のSQLを取得する
		public MySQLiteOpenHelper(Context mContext, String sql) {
			super(mContext, DB_NAME, null, DB_VERSION);
			CREATE_TABLE = sql;
		}

		public MySQLiteOpenHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		// DBが存在しない状態でOpenすると、onCreateがコールされる
		// 新規作成されたDBのインスタンスが付与されるので、テーブルを作成する。
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TABLE);
		}

		// コンストラクタで指定したバージョンと、参照先のDBのバージョンに差異があるときにコールされる
		// 今回バージョンは１固定のため、処理は行わない。
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(DROP_TABLE);
			onCreate(db);
		}
	}