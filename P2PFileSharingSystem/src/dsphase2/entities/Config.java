package dsphase2.entities;

import dsphase2.gui.ConfigGUI;
import java.util.ArrayList;


public class Config {
    
    //normal node config
//    static final String MY_IP = "127.0.0.1";
//    static final int MY_PORT = 5001;
//    static final String MY_NAME = "node1";
//    static final boolean isSuper = true;
    
    //Peer config
    //static final String MY_IP = "127.0.0.2";
    //static final int MY_PORT = 5002;
    //static final String MY_NAME = "peer1";
    //static final boolean isSuper = false;
    
    public static ConfigGUI CONFIG_WINDOW; 
//    public static ConfigWindow CONFIG_WINDOW; 
    
    public static  String MY_IP = "127.0.0.1";

    public static  int MY_PORT = 500;

    public static  String MY_NAME = "S";
    public static  boolean isSuper = false;
    public static boolean isWebService=false;
    

    public static  String BOOTSTRAP_IP = "127.0.0.1";

    public static int nodeCount = 1;
    public static int currentNodeId = 0;
    
    public static  int BOOTSTRAP_PORT =  9876;
    static ArrayList<String> availableFiles = new ArrayList<>(); 
   
    public static int TTL = 10;
    public static int noOfPeersPreset = 8;
    
    public void addNewFile(String fileName){
        availableFiles.add(fileName); 
    }
}
