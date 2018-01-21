/**********COntributors Jeevitha Mahankali (800966168), Raghava Adarsh Mandarapu (800937296)**********/
import java.io.*;
import java.util.*;
import java.net.*;
public class DVRClient {
	private static File file;
	private static int id;
	private static double MAXVALUE=Double.MAX_VALUE;
	private static int[] ports;
	private static String[] nodes;
	private static String[] neighbours;
	private static double[][] networkVectors;
	private static int display_count=1;
	private static double[] myVector;
	private static String[] myHopList;
	private static DatagramSocket sock;

	public static void setNetworkVectors(int len,String[] nodes_cmd) {
		networkVectors=new double[len][len];
		ports=new int[len];
		nodes=new String[len];
		for(int i=0;i<len;i++) {
			Arrays.fill(networkVectors[i],MAXVALUE);
			networkVectors[i][i]=0.0;
			String[] temp=nodes_cmd[i+3].split(":");
			nodes[i]=temp[0];
			ports[i]=Integer.parseInt(temp[1]);
		}
	}

	public static int indexOf(String a) {
		for(int i=0;i<nodes.length;i++)
			if(nodes[i].equals(a))
				return i;
		return -1;
	}

	public static void setLocalVectors(int id_cmd,String parent,int len) throws Exception {
		id=id_cmd;
		myVector=new double[len];
		myHopList=new String[len];
		Arrays.fill(myVector,MAXVALUE);
		myVector[id-1]=0.0;
		file=new File(parent+"/"+nodes[id-1]+".dat");
		sock=new DatagramSocket(ports[id-1]);
		System.out.println("Router "+nodes[id-1]+" is running");
	}

	public static void read() {
		try {
			Arrays.fill(networkVectors[id-1],MAXVALUE);
			networkVectors[id-1][id-1]=0.0;
			BufferedReader br=new BufferedReader(new FileReader(file));
			int len=Integer.parseInt(br.readLine());
			neighbours=new String[len];
			for(int i=0;i<len;i++) {
				String[] temp=br.readLine().split(" ");
				int ind=indexOf(temp[0]);
				neighbours[i]=temp[0];
				myHopList[ind]=(display_count!=1) ? myHopList[ind] : temp[0];
				myVector[ind]=(display_count!=1) ? myVector[ind] : Double.parseDouble(temp[1]);
				networkVectors[id-1][ind]=Double.parseDouble(temp[1]);
			}
			br.close();
		}
		catch(Exception e) { e.printStackTrace(); }
	}

	public static void displayRoutes() {
		System.out.println("> output number "+display_count++);
		String src=nodes[id-1];
		for(int i=0;i<myVector.length;i++)
			if(i!=id-1) {
				String dest=nodes[i];
				System.out.print("shortest path "+src+"-"+dest+": ");
				if(myVector[i]==MAXVALUE)
					System.out.println("no route found");
				else
					System.out.println("the next hop is "+myHopList[i]+" and the cost is "+myVector[i]);
			}
	}

	public synchronized static void updateNetworkVectors(String[] vector,int port) {
		int ind=0;
		for(;ind<ports.length;ind++)
			if(ports[ind]==port)
				break;
		if(ind==ports.length)
			return;
		for(int i=0;i<vector.length;i++)
			networkVectors[ind][i]=Double.parseDouble(vector[i]);
	}

	public static void computeDV() {
		for(int i=0;i<neighbours.length;i++) {
			int ind=indexOf(neighbours[i]);
			for(int j=0;j<myVector.length;j++) {
				if(j==id-1)
					continue;
				else if(i==0) {
					//System.out.println(nodes[i]+" : "+myVector[j]+" - "+networkVectors[id-1][ind]+networkVectors[ind][j]);
					myVector[j]=networkVectors[id-1][ind]+networkVectors[ind][j];
					myHopList[j]=neighbours[i];
				}
				else {
					//System.out.println(nodes[i]+" : "+myVector[j]+" - "+networkVectors[id-1][ind]+networkVectors[ind][j]);
					myHopList[j]=(myVector[j] <= networkVectors[id-1][ind]+networkVectors[ind][j]) ? myHopList[j] : neighbours[i];
					myVector[j]=(myVector[j] <= networkVectors[id-1][ind]+networkVectors[ind][j]) ? myVector[j] : networkVectors[id-1][ind]+networkVectors[ind][j];
				}		
			}
		}

	}

	public static void readNetworkVectors() {
		DatagramPacket packet=null;
		while(true) {
			try {
				byte[] data=new byte[1024];
				packet=new DatagramPacket(data,data.length);
				sock.receive(packet);
				new MyThread("update",new String(packet.getData(),0,packet.getLength()),packet.getPort()).start();
			}
			catch(Exception e) {
				System.out.println(e);
			 }
		}
	}

	public static void writeMyVector() {
		DatagramPacket packet=null;
		try {
			for(int i=0;i<neighbours.length;i++) {
				String data="";
				for(int j=0;j<myVector.length;j++)
					if(neighbours[i].equals(myHopList[j]))
						data+=MAXVALUE+":";
					else
						data+=myVector[j]+":";
				packet=new DatagramPacket(data.getBytes(),data.getBytes().length);
				packet.setAddress(InetAddress.getByName("localhost"));
				packet.setPort(ports[indexOf(neighbours[i])]);
				sock.send(packet);
				//System.out.println("Sent Vector: "+data);
			}
		}
		catch(Exception e) { 
			System.out.println(e);
		}
	}

	public static void main(String[] args) throws Exception {
		setNetworkVectors(Integer.parseInt(args[2]),args);
		setLocalVectors(Integer.parseInt(args[0]),args[1],Integer.parseInt(args[2]));
		MyThread mt1=new MyThread("read");
		mt1.start();
		MyThread mt2=new MyThread("write");
		mt2.start();
		while(true);
	}
}
class MyThread extends Thread {
	private String control;
	DVRClient vr;
	int port;
	String vector;
	public MyThread(String control) {
		this.control=control;
	}

	public MyThread(String control,String vector,int port) {
		this.control=control;
		this.vector=vector;
		this.port=port;
	}

	public void run() {
		if(control.equalsIgnoreCase("read"))
			DVRClient.readNetworkVectors();
		else if(control.equalsIgnoreCase("write")) {
			while(true) {
				try {
					DVRClient.read();
					DVRClient.displayRoutes();
					DVRClient.writeMyVector();
					this.sleep(5*1000);
					DVRClient.computeDV();
					this.sleep(10*1000);
				}
				catch(Exception e) { }
			}
		}
		else if(control.equalsIgnoreCase("update")) {
			//System.out.println("Port Number: "+vector);
			DVRClient.updateNetworkVectors(vector.split(":"),port);
	}}
}