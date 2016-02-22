package com.qwm.qwmlockdemo.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.qwm.qwmlockdemo.MainActivity;
import com.qwm.qwmlockdemo.OpenLockActivity;
import com.qwm.qwmlockdemo.bean.SerialPortSendData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;

/**
 * @author qiwenming
 * @creation 2015-7-20 上午10:06:12
 * @instruction 串口操作工具类
 */
public class DevicesUtils {
	protected OutputStream mOutputStream;
	private InputStream mInputStream;
	private ReadThread mReadThread;
	public SerialPortFinder mSerialPortFinder = new SerialPortFinder();
	private SerialPort mSerialPort;
	private Context context;

	/**
	 * @author qiwenming
	 * @creation 2015-6-18 下午4:38:54
	 * @instruction 读取类
	 */
	private class ReadThread extends Thread {
		private ReciverListener listener;
		private SerialPortSendData sendData;
		public boolean isReadData = false;
		public boolean isOK = true;

		public ReadThread(SerialPortSendData sendData, ReciverListener listener) {
			this.listener = listener;
			this.sendData = sendData;
		}

		@Override
		public void run() {
			StringBuffer sb = new StringBuffer();
			StringBuffer sb2 = new StringBuffer();
			//线程安全的集合
			// List<Byte> synchArrayList = Collections.synchronizedList(new ArrayList<Byte>());
			super.run();
			while (!isInterrupted()) {
				int size;
				try {
					byte[] buffer = new byte[1024];
					if (mInputStream == null)
						return;
					size = mInputStream.read(buffer);
					Log.i("--------------", "---------------mInputStream---------------" + mInputStream.available());
					if (size > 0) { // 读取数据 数据c
						String str = StringUtils.bytesToHexString(buffer, size).trim().toLowerCase();
						sb2.append(str);
						// /onDataReceived(buffer, size,sendData,listener);
						if (sendData.isOnlyLenght) {//这里我们只按照长度读取
							sb.append(str);
							if(sb2.toString().matches("\\w+"+sendData.failStr+"\\w+")){
								closeDevice();
								if (null == context)
									return;
								((OpenLockActivity) context)
										.runOnUiThread(new Runnable() {
											@Override
											public void run() {
												listener.onFail(sendData.failStr);
											}
										});
							}
							else if(sb.toString().length()>=(2*sendData.readByteCount)){
								final String data = sb.toString();
								closeDevice();
								if (null == context)
									return;
								((OpenLockActivity) context)
										.runOnUiThread(new Runnable() {
											@Override
											public void run() {
												listener.onReceived(data);
											}
										});
							}
						} else {//下面的处理是不安长度读取的
							Log.i("onDataReceived", str);
							if (sendData.isFlag) {// 需要标志的
								if (sb2.toString().contains(sendData.stopStr)) {
									// 根据结束标志获取字符消息
									String[] strs = str.split(sendData.stopStr);
									if (strs.length > 1)
										for (int i = 0; i < strs.length - 1; i++)
											sb.append(strs[i]);

									final String data = sb.toString();
									sb = new StringBuffer();
									Log.i("onDataReceived_stop", data);
									Log.i("onDataReceived_stop_ascii",StringUtils.convertHexToString(data));
									isReadData = false;
									closeDevice();
									if (null == context)
										return;
									((OpenLockActivity) context)
											.runOnUiThread(new Runnable() {
												@Override
												public void run() {
													if (isOK)
														listener.onReceived(data);
													else
														listener.onFail(sendData.failStr);
												}
											});
								}
								if (isReadData) {
									sb.append(str);
								}
								if (sb2.toString().contains(sendData.okStr)) {
									isReadData = true;
									isOK = true;
									String[] datas = str.split(sendData.okStr);
									for (int i = 1; i < datas.length; i++) {
										sb.append(str);
									}
								}
//							   if (str.contains(sendData.failStr)) {
//								if (Pattern.matches(sendData.failStr,sb2.toString())) {
//								if (Pattern.compile(sendData.failStr).matcher(sb2.toString()).find()) {
								if (sb2.toString().matches("\\w+"+sendData.failStr+"\\w+")) {
//									sb = new StringBuffer();
									isReadData = false;
									isOK = false;
									closeDevice();
									// String[] datas =
									// str.split(sendData.failStr);
									// for(int i=1;i<datas.length;i++){
									// sb.append(str);
									// }
									if (null == context)
										return;
									((OpenLockActivity) context)
											.runOnUiThread(new Runnable() {
												@Override
												public void run() {
													listener.onFail(sendData.failStr);
												}
											});
								}
								// if(str.equals(sendData.okStr)){
								// isReadData = true;
								// isOK = true;
								// }
								// if(str.equals(sendData.failStr))
								// {
								// isReadData = false;
								// isOK = false;
								// }
							} else {
								// sb.append(str);
								// final String data = sb.toString();
								// sb = new StringBuilder();
								// ((HomeActivity) context).runOnUiThread(new
								// Runnable() {
								// @Override
								// public void run() {
								// listener.onReceived(data);
								// closeDevice();
								// }});

								sb.append(str);
//								if (str.contains(sendData.stopStr)
//										|| str.contains(sendData.stopStr1)) {
								if (sb.toString().contains(sendData.stopStr)
										|| sb.toString().contains(sendData.stopStr1)) {
									final String msg = sb.toString();
									Log.i("run__msg", msg);
									sb = new StringBuffer();
									closeDevice();
									if (null == context)
										return;
									((OpenLockActivity) context)
											.runOnUiThread(new Runnable() {
												@Override
												public void run() {
													listener.onReceived(msg);
												}
											});
								}
							}
						}

					}
				} catch (Exception e) {
					listener.onErr(e);
					return;
				}
			}
		}
	}

	/**
	 * 发送数据
	 * @param context
	 * @param sendData
	 * @param listener
	 */
	public void toSend(Context context, SerialPortSendData sendData,
			ReciverListener listener) {
		this.context = context;
		if ("".equals(sendData.path) || "/dev/tty".equals(sendData.path)) {
			Toast.makeText(context, "设备地址不能为空", Toast.LENGTH_SHORT).show();
			return;
			// devStr = "/dev/ttyS1";
		}
		if ("".equals(sendData.commandStr)) {
			Toast.makeText(context, "指令不能为空", Toast.LENGTH_SHORT).show();
			return;
		}
		try {
			mSerialPort = getSerialPort(sendData.path, sendData.baudRate);
			mOutputStream = mSerialPort.getOutputStream();
			mInputStream = mSerialPort.getInputStream();

			/* Create a receiving thread */
			mReadThread = new ReadThread(sendData, listener);
			mReadThread.start();
		} catch (SecurityException e) {
			// DisplayError(R.string.error_security);
		} catch (IOException e) {
			// DisplayError(R.string.error_unknown);
		} catch (InvalidParameterException e) {
			// DisplayError(R.string.error_configuration);
		}

		// 上面是获取设置而已 下面这个才是发送指令
		byte[] text = StringUtils.hexStringToBytes(sendData.commandStr);
		try {
			mOutputStream.write(text);
			//mOutputStream.write('\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * 获取到串口通信的一个是咧
	 *
	 * @param path
	 * @param baudrate
	 * @return
	 * @throws SecurityException
	 * @throws IOException
	 * @throws InvalidParameterException
	 */
	public SerialPort getSerialPort(String path, int baudrate)
			throws SecurityException, IOException, InvalidParameterException {
		// if (mSerialPort == null) {
		/* Check parameters */
		if ((path.length() == 0) || (baudrate == -1)) {
			throw new InvalidParameterException();
		}
		/* Open the serial port */
		mSerialPort = new SerialPort(new File(path), baudrate, 0);// 打开这个串口
		// }

		return mSerialPort;
	}

	public void closeDevice() {
		if (mReadThread != null)
			mReadThread.interrupt();
		// mApplication.closeSerialPort();
		closeSerialPort();
		// mSerialPort = null;
	}

	public void closeSerialPort() {// 关闭窗口
		if (mSerialPort != null) {
			mSerialPort.close();
			mSerialPort = null;
		}
	}

	/**
	 * @author qiwenming
	 * @creation 2015-7-20 上午10:16:54
	 * @instruction 接受回调类
	 */
	public interface ReciverListener {

		/**
		 * 接受以后的处理方法
		 *
		 * @param receviceStr
		 */
		public abstract void onReceived(String receviceStr);

		/**
		 * 出错
		 *
		 * @param fialStr
		 */
		public abstract void onFail(String fialStr);

		/**
		 * 出现异常
		 *
		 * @param e
		 */
		public abstract void onErr(Exception e);

	}

	/**
	 * @author qiwenming
	 * @creation 2015-7-20 下午2:34:28
	 * @instruction 这个是我们用于存储读取的数据
	 */
	public class RecevedData {
		public ReturnType returnType;
		/**
		 * 数据
		 */
		public String receviedData;
	}

	/**
	 * @author qiwenming
	 * @creation 2015-7-20 下午2:36:21
	 * @instruction 使用辨识返回的数据的
	 */
	public enum ReturnType {
		ERR, // 错误
		OK, // OK
		Exception
	}

}
