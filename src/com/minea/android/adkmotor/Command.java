package com.minea.android.adkmotor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import android.util.Log;

interface Command {
	public enum ConnectionTarget {
		NEXT, IF_TRUE, IF_FALSE
	};

	public Command run(FileInputStream istream, FileOutputStream ostream,
			HashMap<String, Integer> hm);

	public boolean isEnd();

	public void setNext(ConnectionTarget target, Command c);

	public void removeConnection(Command c);
}

// �������Ȃ��R�}���h
class NOP implements Command {
	Command command;

	NOP() {
		// �f�t�H���g�̎��̃R�}���h�͏I��
		command = new END();
	}

	// ���̃R�}���h��ݒ肷��
	@Override
	public void setNext(ConnectionTarget target, Command c) {
		Log.d("Command", "NOP.setNext");
		if (target == ConnectionTarget.NEXT)
			command = c;
	}

	public void removeConnection(Command c) {
		if (command == c)
			command = new END();
	}

	// �R�}���h�����s����
	@Override
	public Command run(FileInputStream istream, FileOutputStream ostream,
			HashMap<String, Integer> hm) {
		Log.d("Command", "NOP.run");
		return command;
	}

	@Override
	public boolean isEnd() {
		Log.d("Command", "NOP.isEnd");
		return false;
	}
}

// �������Ȃ��R�}���h
class WAIT implements Command {
	Command command;
	int millis = 0;

	// ���̃R�}���h��ݒ肷��
	@Override
	public void setNext(ConnectionTarget target, Command c) {
		Log.d("Command", "NOP.setNext");
		if (target == ConnectionTarget.NEXT)
			command = c;
	}

	public void removeConnection(Command c) {
		if (command == c)
			command = new END();
	}

	public void setTime(int times) {
		millis = times;
	}

	// �R�}���h�����s����
	@Override
	public Command run(FileInputStream istream, FileOutputStream ostream,
			HashMap<String, Integer> hm) {
		Log.d("Command", "WAIT.run");
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
		return command;
	}

	@Override
	public boolean isEnd() {
		Log.d("Command", "WAIT.isEnd");
		return false;
	}
}

// END�R�}���h�͉������Ȃ� ���̃R�}���h���Ă΂Ȃ�=���̃R�}���h���Ō�ɏ������I������
class END implements Command {
	@Override
	public void setNext(ConnectionTarget target, Command c) {
		Log.d("Command", "END.setNext");
	}

	public void removeConnection(Command c) {
	}

	void sendCommand(FileOutputStream ostream, byte bin_command, byte value) {
		byte[] buffer = new byte[2];
		// if (value != 0x0 && value != 0x1 && value != 0x2)
		// value = 0x0;
		// 2byte �̃I���I���v���g�R��
		// 0x1 0x0 �� 0x1 0x1
		buffer[0] = bin_command;
		buffer[1] = value;
		if (ostream != null) {
			try {
				ostream.write(buffer);
			} catch (IOException e) {
				Log.e("SEND", "write failed", e);
			}
		}
	}

	@Override
	public Command run(FileInputStream istream, FileOutputStream ostream,
			HashMap<String, Integer> hm) {
		Log.d("SEND", "CANCEL");
		byte bin_command = (byte) 0x1;
		byte value = (byte) 0x0;
		sendCommand(ostream, bin_command, value);
		Log.d("Command", "END.run: The program reached to the end.");
		return this;
	}

	@Override
	public boolean isEnd() {
		Log.d("Command", "END.isEnd");
		return true;
	}
}

// SEND�͎w�肳�ꂽ���߂�
class SEND implements Command {
	Command command;
	int id;
	String arduino_command;
	private static final byte ADVANCE_COMMAND = 0x1;
	private static final byte BACK_COMMAND = 0x2;
	private static final byte RROTATE_COMMAND = 0x3;
	private static final byte LROTATE_COMMAND = 0x4;

	SEND() {
		// �f�t�H���g�̎��̃R�}���h�͏I��
		command = new END();
		// �f�t�H���g�̖��߂�Advance
		arduino_command = "Advance";
	}

	// ���̃R�}���h��ݒ肷��
	public void setNext(ConnectionTarget target, Command c) {
		Log.d("Command", "SEND.setNext");
		if (target == ConnectionTarget.NEXT)
			command = c;
	}

	public void removeConnection(Command c) {
		if (command == c)
			command = new END();
	}

	// ���̃R�}���h�ɓ��B��������Arduino�ɑ��閽�߂��w�肷��
	void setOperation(String _arduino_command) {
		Log.d("Command", "SEND.setOperation");
		arduino_command = _arduino_command;
	}

	void sendCommand(FileOutputStream ostream, byte bin_command, byte value) {
		byte[] buffer = new byte[2];
		// 2byte �̃I���I���v���g�R��
		// 0x1 0x0 �� 0x1 0x1
		buffer[0] = bin_command;
		buffer[1] = value;
		if (ostream != null) {
			try {
				ostream.write(buffer);
			} catch (IOException e) {
				Log.e("SEND", "write failed", e);
			}
		}
	}

	// �R�}���h�����s����
	@Override
	public Command run(FileInputStream istream, FileOutputStream ostream,
			HashMap<String, Integer> hm) {
		Log.d("Command", "SEND.run");
		// ������Arduino�֖��߂𑗂�R�[�h��ǉ�����
		byte bin_command = (byte) 0x1;
		if (arduino_command.equals("ADVANCE")) {
			Log.i("SEND", "HAS ADVANCE");
			byte value = (byte) ADVANCE_COMMAND;
			sendCommand(ostream, bin_command, value);
		} else if (arduino_command.equals("BACK")) {
			Log.i("SEND", "HAS Back");
			byte value = (byte) BACK_COMMAND;
			sendCommand(ostream, bin_command, value);
		} else if (arduino_command.equals("RROTATE")) {
			Log.i("SEND", "HAS RROTATE");
			byte value = (byte) RROTATE_COMMAND;
			sendCommand(ostream, bin_command, value);
		} else if (arduino_command.equals("LROTATE")) {
			Log.i("SEND", "HAS LROTATE");
			byte value = (byte) LROTATE_COMMAND;
			sendCommand(ostream, bin_command, value);
		} else if (arduino_command.equals("STOP")) {
			Log.i("SEND", "STOP");
			byte value = (byte) 0x0;
			sendCommand(ostream, bin_command, value);
		} else if (arduino_command.equals("End")) {
			Log.i("SEND", "CANCEL");
			byte value = (byte) 0x0;
			sendCommand(ostream, bin_command, value);
		}
		return command;
	}

	@Override
	public boolean isEnd() {
		Log.d("Command", "SEND.isEnd");
		return false;
	}
}

// GET�̓Z���T�[����ǂݎ�����l��ϐ��ɑ���
class GET implements Command {
	Command command;
	String storage;
	String sensor_name;

	GET() {
		// �f�t�H���g�̎��̃R�}���h�͏I��
		command = new END();
		// �f�t�H���g�̃Z���T�[��hoge
		sensor_name = "hoge";
		// �f�t�H���g�̕ϐ�����sensor_value;
		storage = "sensor_value";
	}

	// ���̃R�}���h��ݒ肷��
	public void setNext(ConnectionTarget target, Command c) {
		Log.d("Command", "GET.setNext");
		if (target == ConnectionTarget.NEXT)
			command = c;
	}

	public void removeConnection(Command c) {
		if (command == c)
			command = new END();
	}

	// ���̃R�}���h�ɓ��B�������ɓǂރZ���T�[�̖��O�Ƒ���̕ϐ��̖��O���w�肷��
	void setOperation(String _storage, String _sensor_name) {
		Log.d("Command", "GET.setOperation");
		storage = _storage;
		sensor_name = _sensor_name;
	}

	// �R�}���h�����s����
	@Override
	public Command run(FileInputStream istream, FileOutputStream ostream,
			HashMap<String, Integer> hm) {
		Log.d("Command", "GET.run");
		int value = 0;
		// �����ɃZ���T�[����value�ɒl���擾����R�[�h��ǉ�����

		// �l��ϐ����X�g�ɕۑ�����
		Integer result = new Integer(value);
		hm.put(storage, result);
		return command;
	}

	public boolean isEnd() {
		Log.d("Command", "GET.isEnd");
		return false;
	}
}

// IF�͕ϐ��̒l�ɂ���Ď��Ɏ��s����R�}���h�𕪊򂷂�
class IF implements Command {
	// ��r���Z�̎�ވꗗ
	public enum CompOperation {
		EQUAL, NOT_EQUAL, LESS_THAN, MORE_THAN, LESS_EQUAL, MORE_EQUAL
	};

	Command ifTrue;
	Command ifFalse;
	CompOperation operation;
	String left;
	String right;

	IF() {
		// �f�t�H���g�̎��̃R�}���h�͏I��
		ifTrue = new END();
		ifFalse = new END();
		// �f�t�H���g�̔�r�� ��ɐ^
		operation = CompOperation.EQUAL;
		left = "0";
		right = "0";
	}

	// ��r���Z�̎�ނƔ�r����ϐ����w�肷��
	void setOperation(String _left, CompOperation _operation, String _right) {
		Log.d("Command", "IF.setOperation");
		left = _left;
		operation = _operation;
		right = _right;
	}

	// ��r���ʂ��^�������ꍇ�̎��̃R�}���h���w�肷��
	@Override
	public void setNext(ConnectionTarget target, Command c) {
		Log.d("Command", "IF.setNext");
		if (target == ConnectionTarget.IF_TRUE)
			ifTrue = c;
		else if (target == ConnectionTarget.IF_FALSE)
			ifFalse = c;
	}

	public void removeConnection(Command c) {
		if (ifTrue == c)
			ifTrue = new END();
		if (ifFalse == c)
			ifFalse = new END();
	}

	// �R�}���h�����s����
	@Override
	public Command run(FileInputStream istream, FileOutputStream ostream,
			HashMap<String, Integer> hm) {
		Log.d("Command", "IF.run");

		// 変数だった場合
		int left_value = 0;
		if (hm.containsKey(left))
			left_value = hm.get(left).intValue();
		else {
			try {
				Integer temp = new Integer(left_value);
				left_value = temp.intValue();
			} catch (NumberFormatException oops) {
				Log.d("IF", "Unrecognized variable at left: " + left);
			}
		}

		int right_value = 0;
		if (hm.containsKey(right))
			right_value = hm.get(right).intValue();
		else {
			try {
				Integer temp = new Integer(right_value);
				right_value = temp.intValue();
			} catch (NumberFormatException oops) {
				Log.d("IF", "Unrecognized variable at right: " + right);
			}
		}

		if (left_value == 0) {
			try {
				left_value = Integer.parseInt(left);
			} catch (NumberFormatException oops) {
				Log.d("IF", "Unrecognized variable at left: " + left);
			}
		} else if (right_value == 0) {
			try {
				right_value = Integer.parseInt(right);
			} catch (NumberFormatException oops) {
				Log.d("IF", "Unrecognized variable at right: " + right);
			}
		}

		Log.d("IF", "right: " + right_value + ", left: " + left_value);

		if (operation.equals(CompOperation.EQUAL)
				&& (left_value == right_value)) {
			Log.d("IF", "EQUAL");
			return ifTrue;
		} else if (operation.equals(CompOperation.NOT_EQUAL)
				&& (left_value != right_value)) {
			Log.d("IF", "NOT_EQUAL");
			return ifTrue;
		} else if (operation.equals(CompOperation.LESS_THAN)
				&& (left_value < right_value)) {
			Log.d("IF", "LESS_THAN");
			return ifTrue;
		} else if (operation.equals(CompOperation.MORE_THAN)
				&& (left_value > right_value)) {
			Log.d("IF", "MORE_THAN");
			return ifTrue;
		} else if (operation.equals(CompOperation.LESS_EQUAL)
				&& (left_value <= right_value)) {
			Log.d("IF", "LESS_EQUAL");
			return ifTrue;
		} else if (operation.equals(CompOperation.MORE_EQUAL)
				&& (left_value >= right_value)) {
			Log.d("IF", "MORE_EQUAL");
			return ifTrue;
			// �U��������ifFalse��Ԃ�
		} else {
			Log.d("IF", "IfFalse");
			return ifFalse;
		}
	}

	@Override
	public boolean isEnd() {
		Log.d("Command", "IF.isEnd");
		return false;
	}
}

class EXPR implements Command {
	// �Z�p���Z�̎�ވꗗ
	public enum ArithOperation {
		ADD, SUB, MUL, DIV, MOD, LSHIFT, RSHIFT, AND, OR, XOR
	}

	Command command;
	ArithOperation operation;
	String storage;
	String left;
	String right;

	EXPR() {
		// �f�t�H���g�̎��̃R�}���h�͏I��
		command = new END();
		// �f�t�H���g�̉��Z�͕ϐ�null��0�����
		operation = ArithOperation.OR;
		storage = "null";
		left = "0";
		right = "0";
	}

	// ���̃R�}���h��ݒ肷��
	@Override
	public void setNext(ConnectionTarget target, Command c) {
		Log.d("Command", "EXPR.setNext");
		if (target == ConnectionTarget.NEXT)
			command = c;
	}

	public void removeConnection(Command c) {
		if (command == c)
			command = new END();
	}

	// �Z�p���Z�̎�ނƌv�Z�Ɏg���ϐ��Ƒ���̕ϐ����w�肷��
	void setOperation(String _storage, String _left, ArithOperation _operation,
			String _right) {
		Log.d("Command", "EXPR.setOperation");
		storage = _storage;
		left = _left;
		operation = _operation;
		right = _right;
	}

	@Override
	public Command run(FileInputStream istream, FileOutputStream ostream,
			HashMap<String, Integer> hm) {
		Log.d("Command", "EXPR.run");
		// ���Ӓl�𓾂� �^����ꂽ���Ӓl�̖��O���ϐ��Ɋ�ɂ������ꍇ�͂��̒l��ǂݏo��
		// �Ȃ������ꍇ�͂��̕�����𐮐��l�Ƃ��Ďg�����Ƃ����݂�
		int left_value = 0;
		if (hm.containsKey(left))
			left_value = hm.get(left).intValue();
		else {
			try {
				Integer temp = new Integer(left_value);
				left_value = temp.intValue();
			} catch (NumberFormatException oops) {
				Log.d("EXPR", "Unrecognized variable at left: " + left);
			}
		}
		// �E�Ӓl�𓾂� �^����ꂽ�E�Ӓl�̖��O���ϐ��Ɋ�ɂ������ꍇ�͂��̒l��ǂݏo��
		// �Ȃ������ꍇ�͂��̕�����𐮐��l�Ƃ��Ďg�����Ƃ����݂�
		int right_value = 0;
		if (hm.containsKey(right))
			right_value = hm.get(right).intValue();
		else {
			try {
				Integer temp = new Integer(right_value);
				right_value = temp.intValue();
			} catch (NumberFormatException oops) {
				Log.d("EXPR", "Unrecognized variable at right: " + right);
			}
		}
		// �w�肳�ꂽ�Z�p���Z�ɑΉ����鉉�Z���s��
		int value = 0;
		if (operation.equals(ArithOperation.ADD))
			value = left_value + right_value;
		else if (operation.equals(ArithOperation.SUB))
			value = left_value - right_value;
		else if (operation.equals(ArithOperation.MUL))
			value = left_value * right_value;
		else if (operation.equals(ArithOperation.DIV))
			value = left_value / right_value;
		else if (operation.equals(ArithOperation.MOD))
			value = left_value % right_value;
		else if (operation.equals(ArithOperation.AND))
			value = left_value & right_value;
		else if (operation.equals(ArithOperation.OR))
			value = left_value | right_value;
		else if (operation.equals(ArithOperation.XOR))
			value = left_value ^ right_value;
		else
			Log.d("EXPR", "Unrecognized operation was requested");
		// �v�Z���ʂ�ϐ����X�g�ɑ���
		Integer result = new Integer(value);
		hm.put(storage, result);
		return command;
	}

	@Override
	public boolean isEnd() {
		Log.d("Command", "EXPR.isEnd");
		return false;
	}
}
