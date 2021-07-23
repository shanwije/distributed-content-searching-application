/*
 * CS4262 Distributed Systems Mini Project
 */

package dsphase2.entities;

import dsphase2.gui.ConfigWindow;
import java.util.ArrayList;


public class Config {
    
    //Whe  configured as normal node
//    static final String MY_IP = "127.0.0.1";
//    static final int MY_PORT = 5001;
//    static final String MY_NAME = "Devni";
//    static final boolean isSuper = true;
    
    //When configured as peer
    
    
    //static final String MY_IP = "127.0.0.2";
    //static final int MY_PORT = 5002;
    //static final String MY_NAME = "Sasikala";
    //static final boolean isSuper = false;
    
    public static ConfigWindow CONFIG_WINDOW; 
    
    public static  String MY_IP = "127.0.0.1";

    public static  int MY_PORT = 500;

    public static  String MY_NAME = "S";
    public static  boolean isSuper = false;
    public static boolean isWebService=false;
    

    public static  String BOOTSTRAP_IP = "127.0.0.1";

    public static int noOfNodes = 1;
    public static int myNodeNumber = 0;
    
    public static  int BOOTSTRAP_PORT =  9876;
    static ArrayList<String> availableFiles = new ArrayList<>(); 
   
    public static int TTL = 10;
    public static int noOfPeersPreset = 8;
    
    public void addNewFile(String fileName){
        availableFiles.add(fileName); 
    }
    
    
     
    
    
}
