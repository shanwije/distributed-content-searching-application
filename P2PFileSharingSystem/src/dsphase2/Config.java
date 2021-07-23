/*
 * CS4262 Distributed Systems Mini Project
 */

package dsphase2;

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
    
    static ConfigWindow CONFIG_WINDOW; 
    
    static  String MY_IP = "127.0.0.1";

    static  int MY_PORT = 500;

    static  String MY_NAME = "S";
    static  boolean isSuper = false;
    static boolean isWebService=false;
    

    static  String BOOTSTRAP_IP = "127.0.0.1";

    static int noOfNodes = 1;
    static int myNodeNumber = 0;
    
    static  int BOOTSTRAP_PORT =  9876;
    static ArrayList<String> availableFiles = new ArrayList<>(); 
   
    static int TTL = 10;
    static int noOfPeersPreset = 8;
    
    public void addNewFile(String fileName){
        availableFiles.add(fileName); 
    }
    
    
     
    
    
}
