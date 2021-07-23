/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dsphase2.ws;

import dsphase2.entities.Config;
import dsphase2.node.Node;
import java.util.Observable;
import javax.jws.WebService;


 
//Service Implementation
@WebService(endpointInterface = "dsphase2.ReceiverWebService")
public class ReceiverWebServiceImpl extends Observable implements ReceiverWebService {

    public ReceiverWebServiceImpl() {
        addObserver(Node.getInstance(Config.MY_IP, Config.MY_PORT, Config.MY_NAME));
        addObserver(Config.CONFIG_WINDOW);
    }
    
        
 
	@Override
	public String getHelloWorldAsString(String name) {
		return "Hello World JAX-WS " + name;
	}
        
        @Override
        public void receiveMessage(String message){
            System.out.println("Message received: "+message);
            
                    setChanged();
                    notifyObservers(message);
                    clearChanged();
            
        }
 
}
