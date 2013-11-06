package ve.ucv.ciens.ccg.nxtcam.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

import ve.ucv.ciens.ccg.nxtcam.camera.CameraImageMonitor;
import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import ve.ucv.ciens.ccg.nxtcam.utils.Logger.LOG_TYPES;
import ve.ucv.ciens.ccg.nxtcam.utils.ProjectConstants;

public class ImageTransferThread extends Thread{
	private final String TAG = "IM_THREAD";
	private final String CLASS_NAME = ImageTransferThread.class.getSimpleName();

	private boolean pause, done, connected;
	private Object threadPauseMonitor;
	private CameraImageMonitor camMonitor;
	private Socket socket;
	private BufferedWriter writer;
	private BufferedReader reader;
	private byte[] image;
	private String serverIp;

	public ImageTransferThread(String serverIp){
		this.serverIp = serverIp;
		pause = false;
		done = false;
		connected = false;
		threadPauseMonitor = new Object();
		socket = null;
		writer = null;
		reader = null;
		camMonitor = CameraImageMonitor.getInstance();
	}

	public void run(){
		connectToServer();
		if(socket.isConnected()){
			Logger.log(Logger.LOG_TYPES.ERROR, TAG, CLASS_NAME + ".run() :: Not connected to a server. Finishing thread.");
		}else{
			while(!done){
				checkPause();
				image = camMonitor.getImageData();
				// TODO: implement image transfer protocol.
			}
		}
	}

	private void connectToServer(){
		try{
			Logger.log(Logger.LOG_TYPES.INFO, TAG, CLASS_NAME + ".connectToServer() :: Connecting to the server at " + serverIp);
			socket = new Socket(InetAddress.getByName(serverIp), ProjectConstants.SERVER_TCP_PORT_1);
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			Logger.log(Logger.LOG_TYPES.INFO, TAG, CLASS_NAME + ".connectToServer() :: Connection successful.");
		}catch(IOException io){
			Logger.log(Logger.LOG_TYPES.ERROR, TAG, CLASS_NAME + ".connectToServer() :: Connection failed with message: " + io.getMessage());
		}
	}

	public void disconnect(){
		if(socket != null && socket.isConnected()){
			try{
				socket.close();
			}catch (IOException io) {
				Logger.log(Logger.LOG_TYPES.ERROR, TAG, CLASS_NAME + ".connectToServer() :: " + io.getMessage());
			}
		}
	}

	public synchronized void finish(){
		done = true;
		Logger.log(Logger.LOG_TYPES.INFO, TAG, CLASS_NAME + ".finish() :: Finishing thread.");
	}

	private void checkPause(){
		synchronized (threadPauseMonitor){
			while(pause){
				Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".checkPause() :: Pause requested.");
				try{ threadPauseMonitor.wait(); }catch(InterruptedException ie){}
			}
		}
	}

	public synchronized void pauseThread(){
		pause = true;
		Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".pauseThread() :: Pausing thread.");
	}

	public synchronized void resumeThread(){
		Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".resumeThread() :: Resuming thread.");
		synchronized (threadPauseMonitor) {
			pause = false;
			threadPauseMonitor.notifyAll();
		}
	}

	public boolean isConnected(){
		if(socket != null && socket.isConnected())
			return true;
		else
			return false;
	}
}
