/**********COntributors Jeevitha Mahankali (800966168), Raghava Adarsh Mandarapu (800937296)**********/
import java.io.File;
import java.util.Scanner;
public class DVRServer {
		private static int[] ports;
		private static String nodes;
		public static void main(String[] args) throws Exception {
		if(args.length!=1) {
			System.out.println("Incorrect Usage.!\nTry Using: java DVRClient \"c:/users/jeevitha/datafiles/\"");
			return ;
		}
		File dir=new File(args[0]);
		if(!dir.isDirectory()) {
			System.out.println("Try providing Directory path of data files..!");
			return;
		}
		File[] data_files=dir.listFiles();
		ports=new int[data_files.length];
		nodes="";
		ProcessBuilder p=null;
		System.out.println("Initializing the Simulation of "+data_files.length+" Routers..!");
		Scanner read=new Scanner(System.in);
		for(int i=0;i<data_files.length;i++) {
			String temp=data_files[i].getName();
			String[] t=temp.split(".dat");
			System.out.println("Enter the UDP Port Number for Router: "+t[0]); 
			boolean flag=false;
			while(!flag) {
				try {
					ports[i]=Integer.parseInt(read.nextLine());
					if(ports[i]<=1024 || ports[i]>=65536)
						throw new NumberFormatException();
					if(exists(ports,i,((Integer)ports[i])))
						throw new Exception();
					flag=true;
				}
				catch(NumberFormatException e) {
					System.out.println("Enter a valid Port Number > 1024 && < 65536");
				}
				catch(Exception e) {
					System.out.println("Address is Already in Use:");
					flag=false;
				}
			}
			nodes+=" "+temp.substring(0,temp.indexOf("."))+":"+ports[i];
		}
		read.close();
		for(int i=0;i<data_files.length;i++) {
	    		p=new ProcessBuilder("cmd.exe","/c","start java DVRClient "+(i+1)+" \""+data_files[i].getParent().replace("\\","/")+"\" "+data_files.length+nodes);
			p.start();
		}
		System.out.println("Successfully Simulated all routers..!");
	}
	public static boolean exists(int[] a,int len,int b) {
		for(int i=0;i<len;i++)
			if(a[i]==b)
				return true;
		return false;
	}
}