/*
 * CS4262 Distributed Systems Mini Project
 */
package dsphase2.node;

import dsphase2.util.Utils;
import dsphase2.entities.MessageType;
import dsphase2.entities.Message;
import dsphase2.entities.Config;
import dsphase2.entities.RegisterResponse;
import dsphase2.entities.UDPResponse;
import dsphase2.ws.WSPublisher;
import dsphase2.ws.SenderWebServiceClient;
import dsphase2.sockets.Reciever;
import dsphase2.sockets.Sender;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Node extends Observable implements Observer {

    private final String myIp;
    private final int myPort;
    private final String myName;
    private static Node instance = null;
    private boolean isSuper;
    // only available if this is a normal node
    private String mySuperNode;  // supernode = "peer_IP:port_no"
    //only available if this is a super node
    private Set<String> superPeers = new HashSet<>();
    private Set<String> childNodes = new HashSet<>();
    private HashSet<String> innerSet = new HashSet<>();
    private HashMap<String, HashSet<String>> routingTable = new HashMap<>();
    private int inquireResponses;
    private Set<String> leaveSentNodes = new HashSet<>();
    //my files, stored as an invereted index, format term:set of files
    private HashMap<String, ArrayList<String>> myFiles = new HashMap<>();
    //Store the files children have in key,peers format
    private HashMap<String, ArrayList<String>> chilrensFiles = new HashMap<>();
    private long searchStartTime;
    private long searchEndTime; 
    private int noOfReceivedSearchMsgs =0;
     private int noOfAnsweredSearchMsgs =0;
      private int noOfForwardedSearchMsgs =0;

    // private HashMap<String, Integer> joinRequestSentPeers;
    public static Node getInstance(String ip, int port, String name) {
        if (instance == null) {
            instance = new Node(ip, port, name);
        }
        return instance;
    }

    public Node(String ip, int port, String name) {
        this.myIp = ip;
        this.myPort = port;
        this.myName = name;
        isSuper = Config.isSuper;
        // joinRequestSentPeers = new HashMap<>();
        //addMyFiles(4);
        this.addObserver(Config.CONFIG_WINDOW);
    }

    public String getIp() {
        return myIp;
    }

    public int getPort() {
        return myPort;
    }

    private void sendMessage(String message, String peerIp, int peerPort) {
        //System.out.println("sending message: " + message + " from:" + Config.MY_IP + ":" + Config.MY_PORT + " to:" + peerIp + ":" + peerPort);
        if (Config.isWebService) {
            SenderWebServiceClient.getInstance().sendMessage(message, peerIp, peerPort);
        } else {
            Sender.getInstance().sendUDPMessage(message, peerIp, peerPort);
        }
    }

    public void start() {

        RegisterResponse response = register();
        if (response.isSucess()) {
            if (Config.isWebService) {
                UpdateTheLog("Publishing my web service");
                WSPublisher.getInstance().publishWebService(myIp, myPort);
            } else {
                UpdateTheLog("Starting my msg receiver thread");
                Thread reciever = new Thread(Reciever.getInstance());
                reciever.start();
            }

            //now join the network
            String[] peerIPs = response.getPeerIps();
            UpdateTheLog("***Available Peer IPs***");
            System.out.println("Peer IPs");
            if (peerIPs != null) {
                for (String i : peerIPs) {
                    UpdateTheLog(i); 
                    System.out.println(i);
                }
            }
            UpdateTheLog("***********************");
            int[] peerPorts = response.getpeerPorts();
            if (peerIPs != null) {
                if (isSuper) {
                    UpdateTheLog("I am a Super node");
                    System.out.println("I am a Super node");
                    int[] randomPeers;
                    if (peerIPs.length >= 2) {
                        //get random 2 peers to connect and check for super peer
                        randomPeers = Utils.getRandomTwo(peerIPs.length);
                        inquireResponses = 2;
                        for (int peer : randomPeers) {
                            UpdateTheLog("Sending INQUIRE to"+peerIPs[peer]+":"+peerPorts[peer]);
                            System.out.println("random peer: " + peerIPs[peer]);
                            String outGoingMessage = (new Message(MessageType.INQUIRE, myIp, myPort, "")).getMessage();
                            sendMessage(outGoingMessage, peerIPs[peer], peerPorts[peer]);
                        }
                    } else {
                        if (peerIPs.length == 1) {
                            String outGoingMessage = (new Message(MessageType.INQUIRE, myIp, myPort, "")).getMessage();
                            UpdateTheLog("Sending INQUIRE to"+peerIPs[0]+":"+peerPorts[0]);
                            sendMessage(outGoingMessage, peerIPs[0], peerPorts[0]);
                        }
                    }
                } else {
                    System.out.println("I am a Normal node");
                    // get a peer to connect and check for super peer
                    if (peerIPs.length > 0) {
                        int peer;
                        if (peerIPs.length == 1) {
                            peer = 0;
                        } else {
                            //peer = getRandomNo(peerIPs.length);
                            int peers = peerIPs.length;
                            if (peers == 1) {
                                peer = 0;
                            } else if (peers == 2) {
                                peer = 2;
                            } else {
                                peer = peers - 3;
                            }
                        }
                        inquireResponses = 1;
                        System.out.println("random peer: " + peerIPs[peer]);
                        UpdateTheLog("Sending INQUIRE to"+peerIPs[peer]+":"+peerPorts[peer]);
                        String outGoingMessage = (new Message(MessageType.INQUIRE, myIp, myPort, "")).getMessage();
                        sendMessage(outGoingMessage, peerIPs[peer], peerPorts[peer]);
                    }
                }
            }
        }
        String message = "Peer " + myName + " joined the network...";
        System.out.println(message);
        //UpdateTheLog(message);
    }

    // Register node in super node
    public RegisterResponse register() {

        String message = (new Message(MessageType.REG, myIp, myPort, myName)).getMessage();
        UpdateTheLog("sending REG to BS");
        String response = Sender.getInstance().sendTCPMessage(message);
        UpdateTheLog("Received" + response + " << from BS");
        System.out.println("Response:" + response);

        if ((response.trim()).equals("-1")) {
            return new RegisterResponse(MessageType.REG_FAILURE, null, null);
        } else if ((response.trim()).equals("-2")) {
            unreg();
            return new RegisterResponse(MessageType.REG_FAILURE, null, null);
        } else {
            String[] splitted = response.split(" ");

            String noOfNodes = splitted[2];
            Config.currentNodeId = Integer.parseInt(noOfNodes.trim());
            Config.nodeCount = Config.currentNodeId + 1;

            String[] peerIps;
            int[] peerPorts;

            // System.out.println(noOfNodes);
            switch (noOfNodes.trim()) {
                case "0":
                    isSuper = true;
                    addMyFiles(Config.noOfPeersPreset);
                    return new RegisterResponse(MessageType.REG_SUCCESS, null, null);
                // break;
                case "1":
                    isSuper = true;
                    peerIps = new String[1];
                    peerPorts = new int[1];
                    peerIps[0] = splitted[3];
                    peerPorts[0] = Integer.parseInt(splitted[4]);
                    addMyFiles(Config.noOfPeersPreset);
                    //  System.out.println(joinNetwork(peerIps[0], peerPorts[0]));
                    return new RegisterResponse(MessageType.REG_SUCCESS, peerIps, peerPorts);
                //  break;
                case "9996":
                    System.out.println("Failed, can’t register. BS full.");
                    UpdateTheLog("Failed, can’t register. BS full.");
                    return new RegisterResponse(MessageType.REG_FAILURE, null, null);
                //     break;
                case "9997":
                    System.out.println("Failed, registered to another user, try a different IP and port");
                    UpdateTheLog("Failed, registered to another user, try a different IP and port");
                    return new RegisterResponse(MessageType.REG_FAILURE, null, null);
                //  break;
                case "9998":
                    System.out.println("Failed, already registered to you, unregister first");
                    UpdateTheLog("Failed, already registered to you, unregister first");
                    return new RegisterResponse(MessageType.REG_FAILURE, null, null);
                // break;
                case "9999":
                    System.out.println("Failed, there is some error in the command");
                    UpdateTheLog("Failed, there is some error in the command");
                    return new RegisterResponse(MessageType.REG_FAILURE, null, null);
                //  break;

                default:
                    int number = Integer.parseInt(noOfNodes.trim());
                    peerIps = new String[number];
                    peerPorts = new int[number];
                    System.out.println("number:" + number);
                    for (int i = 1; i < number + 1; i++) {
                        peerIps[i - 1] = splitted[3 * i];
                        peerPorts[i - 1] = Integer.parseInt(splitted[3 * i + 1]);
                        //System.out.println(peerIps[i - 1] + "," + peerPorts[i - 1]);
                    }
                    addMyFiles(Config.noOfPeersPreset);
                    return new RegisterResponse(MessageType.REG_SUCCESS, peerIps, peerPorts);
            }
        }
    }

    private void addMyFiles(int numberOfNodes) {

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File("src/resources/FileNames")));
            String readLine = null;
            int lineNumber = 0;
            int ipRemainder = Config.currentNodeId;
            int[] twoRandomFiles = Utils.getRandomTwo(20);
            while ((readLine = reader.readLine()) != null) {
                if (lineNumber % numberOfNodes == ipRemainder) {
                    addFile(readLine);
                } else if (ipRemainder >= numberOfNodes) {
                    if (lineNumber == twoRandomFiles[0] || lineNumber == twoRandomFiles[1]) {
                        addFile(readLine);
                    }
                }
                lineNumber++;
            }
            UpdateTheLog("Adding my files: "+myFiles);
            //myFiles.put("Windows",new String[]{"Windows XP","Windows 8"});
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
            }
      
            System.out.println(myFiles);
        }
    }

    private void addFile(String readLine) {
        String[] terms = readLine.toLowerCase().split(" ");
        String fileName = readLine.replace(" ", "_");
        UpdateTheLog(fileName);
        String previousTerm;
        int count = 0;
        int fileNameSize = terms.length;
        if (fileNameSize > 2) {
            addTermFile(readLine.toLowerCase().replace(" ", "_"), fileName);
        }
        for (String term : terms) {
            addTermFile(term.replace(" ", "_"), fileName);
            if (count > 0) {
                addTermFile(terms[count - 1]+ "_" + term, fileName);
            }
            count++;
        }
    }

    private void addTermFile(String term, String fileName) {
        if (myFiles.containsKey(term)) {
            (myFiles.get(term)).add(fileName);
        } else {
            ArrayList<String> files = new ArrayList<>();
            files.add(fileName);
            myFiles.put(term, files);
        }
    }

    private void addChildrensFiles(String termsString, String childIp, int childPort) {
        termsString = termsString.substring(26);
        System.out.println("Adding child terms:" + termsString);
        Set<String> childTerms = chilrensFiles.keySet();
        String[] terms = termsString.split(",");
        for (String term : terms) {
            if (childTerms.contains(term)) {
                ArrayList<String> childrenHavingTerm = chilrensFiles.get(term);
                childrenHavingTerm.add(childIp + ":" + String.valueOf(childPort));
            } else {
                ArrayList<String> ipAddress = new ArrayList<>();
                ipAddress.add(childIp + ":" + String.valueOf(childPort));
                chilrensFiles.put(term, ipAddress);
            }
        }
    }

    private int getIndex(ArrayList<String> list, String string) {
        for (int i = 0; i < list.size(); i++) {
            if (string.equals(list.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public HashMap getMyFiles() {
        return myFiles;
    }

    @Override
    public void update(Observable o, Object arg) {
        //Process incoming message

        //UDPResponse receivedMessage = (UDPResponse) arg;
        String incoming;
        if (Config.isWebService) {
            incoming = (String) arg;
        } else {
            UDPResponse receivedMessage = (UDPResponse) arg;
            incoming = receivedMessage.getData().trim();
        }

        //System.out.println("incoming message:" + incoming);
        String[] msg = incoming.split(" ");
        MessageType msgType = MessageType.valueOf(msg[1]);
        String requesterIp = null;
        int requesterPort = 0;

        switch (msgType) {
            case SEROK:
                String success = msg[2].trim();
                if (!success.equals("0")) {
                    requesterIp = msg[3].trim();
                    requesterPort = Integer.parseInt(msg[4].trim());
                }
                break;

            default:
                requesterIp = msg[2].trim();
                requesterPort = Integer.parseInt(msg[3].trim());
        }
        String info = requesterIp + ":" + requesterPort;
        String outGoingMessage;
        switch (msgType) {
            // for inquire msg : <length INQUIRE IP_address port_no is_super>
            case INQUIRE:
                //System.out.println("Received INQUIRE message");
                UpdateTheLog("Received INQUIRE msg from "+requesterIp+":"+requesterPort);
                if (isSuper) {
                    UpdateTheLog("Sending my IP:Port with INQUIREOK");
                    outGoingMessage = (new Message(MessageType.INQUIREOK, myIp, myPort, "")).getMessage();
                    sendMessage(outGoingMessage, requesterIp, requesterPort);
                } else {
                     UpdateTheLog("Sending my super nodes' IP:Port with INQUIREOK");
                    String[] superNodeInfo = mySuperNode.split(":");
                    String message = (new Message(MessageType.INQUIREOK, superNodeInfo[0], Integer.parseInt(superNodeInfo[1]), "")).getMessage();
                    sendMessage(message, requesterIp, requesterPort);
                }
                break;
            // for inquire reply: <length INQUIREOK IP_address port_no> 
            case INQUIREOK:
                inquireResponses--;
                UpdateTheLog("Received INQUIREOK msg from "+requesterIp+":"+requesterPort);
                 UpdateTheLog("Sending JOIN request to "+requesterIp+":"+requesterPort);
                outGoingMessage = (new Message(MessageType.JOIN, myIp, myPort, getMyPrefixedName())).getMessage();

                sendMessage(outGoingMessage, requesterIp, requesterPort);

                //joinRequestSentPeers.put(requesterIp, requesterPort);
                break;
            // for join req : <length JOIN IP_address port_no>
            case JOIN:
                UpdateTheLog("Received JOIN req from "+requesterIp+":"+requesterPort);
                if ((msg[4]).startsWith("SUPER")) {
                    UpdateTheLog("Added as a super peer");
                    System.out.println("Added peer super node:" + (msg[4]).substring(5));
                    superPeers.add(info);
                } else if ((msg[4]).startsWith("NORMAL")) {
                    UpdateTheLog("Added as a child node");
                    System.out.println("Added child node:" + (msg[4]).substring(6));
                    childNodes.add(info);
                }
                  UpdateTheLog("Sending JOINOK msg to "+requesterIp+":"+requesterPort);
                outGoingMessage = (new Message(MessageType.JOINOK, myIp, myPort, myName)).getMessage();
                sendMessage(outGoingMessage, requesterIp, requesterPort);
                break;
            //for join resp length JOINOK value
            case JOINOK:
                 UpdateTheLog("Received JOINOK from "+requesterIp+":"+requesterPort);
                info = requesterIp + ":" + requesterPort;

                if (isSuper) {
                    String superPeer = info;
                    superPeers.add(superPeer);
                    UpdateTheLog("Added as a super peer");
                    System.out.println("Added peer super node: " + info);
                } else {
                    mySuperNode = info;
                     UpdateTheLog("Added as my super node");
                    System.out.println("Added my super node: " + info);

                    //Send the indexed terms for the files I have, to my super peer
                    Iterator iterator = myFiles.keySet().iterator();
                    String myTerms = "";
                    String key;
                    while (iterator.hasNext()) {
                        key = (String) iterator.next();
                        myTerms += "," + key;
                    }
                     UpdateTheLog("Sending my files");
                    outGoingMessage = (new Message(MessageType.FILES, myIp, myPort, myTerms.substring(1))).getMessage();
                    sendMessage(outGoingMessage, requesterIp, requesterPort);
                }
                break;
            case FILES:
                UpdateTheLog("Received FILES from "+requesterIp+":"+requesterPort);
                if (!isSuper) {
                    System.out.println("Unexpected request");
                } else {
                    addChildrensFiles(incoming, requesterIp, requesterPort);
                }

                break;
            case SER:
                noOfReceivedSearchMsgs++; 
                String[] messageComponents = incoming.split("\"");
                String[] searcherIpPort = messageComponents[0].split(" ");
                String searcherIp = searcherIpPort[2];
                int searcherPort = Integer.parseInt(searcherIpPort[3]);
                String fileKey = messageComponents[1];
                int hopCount;
                //System.out.println("Hop count length:" + messageComponents[2].length());
                
                if (messageComponents.length > 2) {
                    hopCount = 1 + Integer.parseInt(messageComponents[2].trim());
                } else {
                    hopCount = 1;
                }
                UpdateTheLog("Received SER request from "+requesterIp+":"+requesterPort+" for "+fileKey);
                UpdateTheLog("I am hop no: "+hopCount+"for this SER"); 
                //System.out.println("Search message received for key:" + fileKey);
                //check if I have the file
                if (hopCount < Config.TTL) {
                    fileKey = fileKey.toLowerCase();
                    boolean locatable = false;
                    if (myFiles.containsKey(fileKey)) {
                        UpdateTheLog("I have this kind of file");
                        locatable = true;
                        ArrayList<String> files = myFiles.get(fileKey);
                        int noOfFiles = files.size();
                        //first send the list of files to the searcher
                        // String response = (new Message(MessageType.SEROK, noOfFiles, Config.MY_IP, Config.MY_PORT, hopCount, files, fileKey)).getMessage();
                       UpdateTheLog("Sending SEROK to"+searcherIp+":"+searcherPort);
                        String response = (new Message(MessageType.SEROK, noOfFiles, Config.MY_IP, Config.MY_PORT, hopCount, files, fileKey, myIp, myPort)).getMessage();
                        //System.out.println("Created response:" + response);
                        noOfAnsweredSearchMsgs++;  
                        sendMessage(response, searcherIp, searcherPort);
                    }

                    //if I am a super peer, forward the search message to respective peers
                    boolean isSearcherAChild = false;
                    
                    if (childNodes.contains(searcherIp + ":" + searcherPort)) {
                        UpdateTheLog("The searcher is a child of mine");
                        isSearcherAChild = true;
                    }else{
                        UpdateTheLog("The searcher is not a child of mine");
                    }
                    if (isSuper) {
                        //forward the search query to a random peer
                        ArrayList<String> superPeersList = new ArrayList<>(superPeers);

                        //int randomPeerNumer = getRandomNo(superPeers.size(), superPeersList.indexOf(searcherIp + ":" + searcherPort), isSearcherAChild);
                        int randomPeerNumer = Utils.getRandomNo(superPeers.size(), getIndex(superPeersList, searcherIp + ":" + searcherPort), isSearcherAChild);
                        System.out.println("random peer number:" + randomPeerNumer);
                        String[] ipPort;
                        if (randomPeerNumer != -1) {
                            locatable = true;
                            noOfForwardedSearchMsgs++;
                            UpdateTheLog("Forwarding the SER to random super peer as random walk");
                            ipPort = (superPeersList.get(randomPeerNumer)).split(":");

                            ////search(fileKey, searcherIp, searcherPort, ipPort[0], Integer.parseInt(ipPort[1]), hopCount);
                            System.out.println("adding to routing table,key:" + ipPort[0] + ipPort[1] + fileKey + "   value:" + searcherIp + ":" + searcherPort);

                            if (routingTable.containsKey(ipPort[0] + ipPort[1] + fileKey)) {
                                innerSet = routingTable.get(ipPort[0] + ipPort[1] + fileKey);
                            } else {
                                innerSet = new HashSet<>();
                            }
                            innerSet.add(searcherIp + ":" + searcherPort);

                            routingTable.put(ipPort[0] + ipPort[1] + fileKey, innerSet);

                            search(fileKey, myIp, myPort, ipPort[0], Integer.parseInt(ipPort[1]), hopCount);
                        }

                        //next forward the search query to children having the file
                        Iterator it = chilrensFiles.keySet().iterator();
                        while (it.hasNext()) {
                            System.out.println("my child has:" + it.next());
                            UpdateTheLog("Seems like some children have the file too");
                        }
                        if (chilrensFiles.containsKey(fileKey)) {
                            locatable = true;
                            ArrayList<String> peersWithFile = chilrensFiles.get(fileKey);
                            for (String peer : peersWithFile) {
                                ipPort = peer.split(":");
                                // System.out.println("my child has:"+peer);
                                if (routingTable.containsKey(ipPort[0] + ipPort[1] + fileKey)) {
                                    innerSet = routingTable.get(ipPort[0] + ipPort[1] + fileKey);
                                } else {
                                    innerSet = new HashSet<>();
                                }
                                innerSet.add(searcherIp + ":" + searcherPort);

                                routingTable.put(ipPort[0] + ipPort[1] + fileKey, innerSet);
                                //routingTable.put(ipPort[0] + fileKey, searcherIp + ":" + searcherPort);
                                noOfForwardedSearchMsgs++; 
                                UpdateTheLog("Forwarding to my child"+ipPort[0]+":"+ipPort[1]);
                                search(fileKey, myIp, myPort, ipPort[0], Integer.parseInt(ipPort[1]), hopCount);

                            }
                        } else {
                            UpdateTheLog("No children of mine got the file");
                            System.out.println("children's files do not contain the key:" + fileKey);
                        }

                        if (!locatable) {
                            //String response = (new Message(MessageType.SEROK, 0)).getMessage();
                            //String response = (new Message(MessageType.SEROK, fileKey)).getMessage();
                            String response = (new Message(MessageType.SEROK, fileKey, myIp, myPort)).getMessage();
                            // System.out.println("Created response:" + response);
                            sendMessage(response, searcherIp, searcherPort);
                        }
                    }
                }
                break;
            case SEROK:
                UpdateTheLog("Received SEROK from "+requesterIp+":"+requesterPort);
                String[] parts = incoming.split(" ");
                int noOfFiles = Integer.parseInt(parts[2].trim());
                switch (noOfFiles) {
                    case 0:
                        UpdateTheLog("No files found for the query");
                        System.out.println("Files not found!");
                        //forwardSEROKToImmediateRequester(incoming, receivedMessage.getIpAddress(),receivedMessage.getPort(),false);
                        System.out.println("incoming:" + incoming);
                        forwardSEROKToImmediateRequester(incoming, parts[4], Integer.parseInt(parts[5].trim()), false);
                        break;
                    case 1:
                        // System.out.println("Files found:");
                        // System.out.println(incoming);
                        //forwardSEROKToImmediateRequester(incoming, receivedMessage.getIpAddress(),receivedMessage.getPort(),true);
                        forwardSEROKToImmediateRequester(incoming, parts[parts.length - 2], Integer.parseInt(parts[parts.length - 1].trim()), true);
                        break;
                    case 9999:
                        System.out.println("Node unreachable");
                        break;
                    case 9998:
                        System.out.println("Unknown error occured...");
                        break;
                    default:
                        forwardSEROKToImmediateRequester(incoming, parts[parts.length - 2], Integer.parseInt(parts[parts.length - 1].trim()), true);

                }
                
                
                break;
            case LEAVE:
                
                
                int length = msg.length;
                //if its just a child asking to leave, remove him from the childNodes list 
                //and remove all file names from the super node which were in the leaving node but not in any other children
                if ("CHILD-LEAVING".equals(msg[length - 1].trim())) {
                    //TO-DO: need to remove all file names from the super node which were in the leaving node but not in any other children
                    String leavingChild = msg[2].trim() + ":" + msg[3].trim();
                    Set<String> terms = chilrensFiles.keySet();
                    Iterator<String> iterator = terms.iterator();
                    ArrayList<String> termsToBeRemoved = new ArrayList<>();
                    while (iterator.hasNext()) {
                        String currentTerm = iterator.next();
                        ArrayList<String> children = chilrensFiles.get(currentTerm);
                        if (children.contains(leavingChild)) {
                            if (children.size() == 1) {
                                termsToBeRemoved.add(currentTerm);
                            } else {
                                children.remove(leavingChild);
                            }
                        }
                    }
                    for (String term : termsToBeRemoved) {
                        chilrensFiles.remove(term);
                    }
                    childNodes.remove(leavingChild);
                    outGoingMessage = (new Message(MessageType.LEAVEOK, myIp, myPort)).getMessage();
                    sendMessage(outGoingMessage, requesterIp, requesterPort);
                } // if is is a super node that is leaving and if it doesn't have two peers
                //this will only be sent to children
                else if (length == 4) {
                    if (isSuper) {
                        outGoingMessage = (new Message(MessageType.LEAVEOK, myIp, myPort)).getMessage();
                        sendMessage(outGoingMessage, requesterIp, requesterPort);
                    } //I have to become a superchild
                    else {
                        Config.isSuper = true;
                        isSuper = true;
                        System.out.println(myName + " got promoted to super");
                        mySuperNode = null;
                        outGoingMessage = (new Message(MessageType.LEAVEOK, myIp, myPort)).getMessage();
                        sendMessage(outGoingMessage, requesterIp, requesterPort);
                    }
                } //if it is a super node that is leaving, take the ip and port it sends and send a JOIN message to it asking to connect
                else {
                    if (isSuper) {
                        superPeers.remove(requesterIp + ":" + requesterPort);
                    }
                    outGoingMessage = (new Message(MessageType.LEAVEOK, myIp, myPort)).getMessage();
                    sendMessage(outGoingMessage, requesterIp, requesterPort);
                    String[] ipPort = msg[length - 1].split(":");
                    if (!superPeers.contains(msg[length - 1])) {
                        outGoingMessage = (new Message(MessageType.JOIN, myIp, myPort, getMyPrefixedName())).getMessage();
                        sendMessage(outGoingMessage, ipPort[0], Integer.parseInt(ipPort[1]));
                    }
                }
                break;
            case LEAVEOK:
               
                leaveSentNodes.remove(requesterIp + ":" + requesterPort);
                if (leaveSentNodes.isEmpty()) {
                    //unreg from bootstrap
                    System.out.println("Peer " + myName + " is leaving...");
                    unreg();
                    System.exit(0);
                }
                break;
        }
        printStat();

    }

    private String getMyPrefixedName() {
        String name = myName;
        if (isSuper) {
            name = "SUPER" + name;
        } else {
            name = "NORMAL" + name;
        }
        return name;
    }

    private boolean searchStarted = false; 
    
    public void search(String fileName) {
        UpdateTheLog("I am Searching for "+fileName);
        searchStarted = true; 
        searchStartTime = System.nanoTime();
        fileName = fileName.replace(" ", "_");
        String[] ipPort;
        if (isSuper) {
            int noOfSuperPeers = superPeers.size();
            UpdateTheLog("Getting a random super peer");
            int randomSuperPeer = Utils.getRandomNo(noOfSuperPeers);
            ipPort = (((new ArrayList<>(superPeers))).get(randomSuperPeer)).split(":");
        } else {
            UpdateTheLog("Asking from my super node");
            ipPort = mySuperNode.split(":");
        }
        search(fileName, ipPort[0], Integer.parseInt(ipPort[1]));
    }

    public void search(String fileName, String peerIp, int peerPort, int hopCount) {
        search(fileName, Config.MY_IP, Config.MY_PORT, peerIp, peerPort, hopCount);
    }

    public void search(String fileName, String searcherIp, int searcherPort, String peerIp, int peerPort, int hopCount) {
        String fileNameString = "\"" + fileName + "\"";
        String message = (new Message(MessageType.SER, searcherIp, searcherPort, fileNameString, hopCount)).getMessage();
        //System.out.println("created message" + message);
        sendMessage(message, peerIp, peerPort);
        //System.out.println("Message sent:" + message);
    }

    public void search(String fileName, String searcherIp, int searcherPort, String peerIp, int peerPort) {
        String fileNameString = "\"" + fileName + "\"";
        UpdateTheLog("Sending SER request to"+peerIp+":"+peerPort);
        String message = (new Message(MessageType.SER, searcherIp, searcherPort, fileNameString)).getMessage();
        //System.out.println("created message" + message);
        sendMessage(message, peerIp, peerPort);
        //System.out.println("Message Sent:" + message);
    }

    public void search(String fileName, String peerIp, int peerPort) {
        search(fileName, myIp, myPort, peerIp, peerPort);
    }

    public void leave() {
        String[] ipPort;
        String message;
        String leaveSentNode;
        ArrayList<String> superPeerList = new ArrayList<>(superPeers);
        ArrayList<String> childList = new ArrayList<>(childNodes);
        //if I am a superNode 
        if (isSuper) {
            //send messages to all peers saying I am leaving and give them the ip and port of another super peer to connect with
            int noOfPeers = superPeers.size();
            if (noOfPeers == 0) {
                unreg();
                System.exit(0);
            } else if (noOfPeers == 1) {
                //select one of my children to be a super peer
                int noOfchildren = childNodes.size();
                if (noOfchildren > 0) {
                    int randomChildNo = Utils.getRandomNo(noOfchildren - 1);
                    message = (new Message(MessageType.LEAVE, myIp, myPort)).getMessage();
                    leaveSentNode = childList.get(randomChildNo);
                    String[] childIpPort = leaveSentNode.split(":");
                    String newSuperChild = leaveSentNode;
                    sendMessage(message, childIpPort[0], Integer.parseInt(childIpPort[1]));
                    leaveSentNodes.add(leaveSentNode);

                    for (int i = 0; i < randomChildNo; i++) {
                        message = (new Message(MessageType.LEAVE, myIp, myPort, newSuperChild)).getMessage();
                        leaveSentNode = childList.get(i);
                        ipPort = leaveSentNode.split(":");
                        sendMessage(message, ipPort[0], Integer.parseInt(ipPort[1]));
                        leaveSentNodes.add(leaveSentNode);
                    }
                    for (int i = randomChildNo + 1; i < noOfchildren; i++) {
                        message = (new Message(MessageType.LEAVE, myIp, myPort, newSuperChild)).getMessage();
                        leaveSentNode = childList.get(i);
                        ipPort = leaveSentNode.split(":");
                        sendMessage(message, ipPort[0], Integer.parseInt(ipPort[1]));
                        leaveSentNodes.add(leaveSentNode);
                    }

                    //inform the onlt peer about the newly promoted child
                    message = (new Message(MessageType.LEAVE, myIp, myPort, newSuperChild)).getMessage();
                    leaveSentNode = superPeerList.get(0);
                    ipPort = leaveSentNode.split(":");
                    sendMessage(message, ipPort[0], Integer.parseInt(ipPort[1]));
                    leaveSentNodes.add(leaveSentNode);
                } else {
                    //inform the onlt peer that I'm leaving
                    message = (new Message(MessageType.LEAVE, myIp, myPort)).getMessage();
                    leaveSentNode = superPeerList.get(0);
                    ipPort = leaveSentNode.split(":");
                    sendMessage(message, ipPort[0], Integer.parseInt(ipPort[1]));
                    leaveSentNodes.add(leaveSentNode);
                }
            } else {
                for (int i = 0; i < noOfPeers; i++) {

                    leaveSentNode = superPeerList.get(i);
                    ipPort = leaveSentNode.split(":");
                    if (i < noOfPeers / 2) {
                        //if the index of the super peer in the super peer array list is < length/2 then direct him to join (i+1)th super peer
                        message = (new Message(MessageType.LEAVE, myIp, myPort, superPeerList.get(i + 1))).getMessage();
                    } else {
                        message = (new Message(MessageType.LEAVE, myIp, myPort, superPeerList.get(i - 1))).getMessage();
                    }
                    leaveSentNodes.add(leaveSentNode);
                    sendMessage(message, ipPort[0], Integer.parseInt(ipPort[1]));
                }

                //send messages to all children saying I am leaving and give them the ip and port of a super peer to connect with
                int noOfChildren = childNodes.size();
                for (int i = 0; i < noOfChildren; i++) {
                    leaveSentNode = childList.get(i);
                    ipPort = leaveSentNode.split(":");
                    String randomSuperNode = superPeerList.get(Utils.getRandomNo(noOfPeers));
                    message = (new Message(MessageType.LEAVE, myIp, myPort, randomSuperNode)).getMessage();
                    leaveSentNodes.add(leaveSentNode);
                    sendMessage(message, ipPort[0], Integer.parseInt(ipPort[1]));
                }
            }
        } //if I am not a super Node then just tell the super peer that I am leaving
        else {
            ipPort = mySuperNode.split(":");
            //just pass null to show that I am a normal node
            message = (new Message(MessageType.LEAVE, myIp, myPort, null)).getMessage();
            leaveSentNodes.add(mySuperNode);
            sendMessage(message, ipPort[0], Integer.parseInt(ipPort[1]));
        }
        System.out.println("Leave message sent to " + leaveSentNodes.size() + " peers");
    }

    private void unreg() {
        String message = (new Message(MessageType.UNREG, myIp, myPort, myName)).getMessage();
        Sender.getInstance().sendTCPMessage(message);
    }

    private void forwardSEROKToImmediateRequester(String incoming, String senderIp, int senderPort, boolean filesFound) {

        String[] parts = incoming.split(" ");

        String routingTableKey, key, immediateRequesterIpPort;

        Iterator iter;
        String fileString = "", message;
        if (filesFound) {
            key = parts[6];
//            fileString = parts[7];
//            for (int i = 8; i < parts.length - 2; i++) {
            fileString = key;
            for (int i = 7; i < parts.length - 2; i++) {
                fileString += " " + parts[i];
            }
        } else {
            key = parts[3];
        }

        //routingTableKey = senderIp + ":" + key;
        routingTableKey = senderIp + senderPort + key;
        System.out.println("checking routing table for the key:" + routingTableKey);
        if (routingTable.containsKey(routingTableKey)) {
            System.out.println("key is inside routing table");
            iter = (routingTable.get(routingTableKey)).iterator();

            if (filesFound) {
                while (iter.hasNext()) {
                    //immediateRequesterIpPort = (String[]) iter.next();
                    immediateRequesterIpPort = (String) iter.next();
                    System.out.println("parts[1]:" + parts[1] + " " + "parts[2]:" + parts[2] + " " + "parts[3]:" + parts[3] + "parts[4]:" + parts[4]);
                    System.out.println("filestring:" + fileString);
                    message = (new Message(MessageType.SEROK, Integer.parseInt(parts[2]), parts[3], Integer.parseInt(parts[4].trim()), Integer.parseInt(parts[5].trim()), fileString, myIp, myPort)).getMessage();
                    sendMessage(message, immediateRequesterIpPort.split(":")[0], Integer.parseInt(immediateRequesterIpPort.split(":")[1]));
                }
            } else {
                while (iter.hasNext()) {
                    immediateRequesterIpPort = (String) iter.next();
                    message = (new Message(MessageType.SEROK, key, myIp, myPort)).getMessage();
                    sendMessage(message, immediateRequesterIpPort.split(":")[0], Integer.parseInt(immediateRequesterIpPort.split(":")[1]));
                }
            }
            routingTable.remove(routingTableKey);

            //sendMessage(incoming, immediateRequesterIpPort[0], Integer.parseInt(immediateRequesterIpPort[1]));
        } else {
            searchEndTime = System.nanoTime(); 
            System.out.println("key is not inside routing table");
            UpdateTheLog("Received a SEROK for my search");
            UpdateTheLog(incoming);
            System.out.println(incoming);
            String[] info = incoming.split(" ");
            int hops = Integer.parseInt(info[5]); 
            if(searchStarted){
                UpdateTheLog("No of App level hops for query: "+hops);
                searchStarted = false; 
                UpdateTheLog("Query latency: "+(searchEndTime - searchStartTime)/1e6); 
            }
            
            // System.out.println("*Files found");
        }

    }

    public void UpdateTheLog(String msg) {
        setChanged();
        notifyObservers(msg);
        clearChanged();
    }
    
    public void printStat(){
        int nodeDegree; 
        if(isSuper){
                    nodeDegree = childNodes.size()+superPeers.size();
                }else{
                    nodeDegree = 1; 
                }
                    
                UpdateTheLog("Noden degree = "+nodeDegree); 
                UpdateTheLog("************Statistics**********");
                UpdateTheLog("No of received search msgs = "+noOfReceivedSearchMsgs);
                UpdateTheLog("No of answered search msgs = "+noOfAnsweredSearchMsgs);
                UpdateTheLog("No of forwarded search msgs = "+noOfForwardedSearchMsgs);
                UpdateTheLog("Routing table size = "+routingTable.size());
                UpdateTheLog("********************************");
                System.out.println("Noden degree = "+nodeDegree); 
                System.out.println("************Statistics**********");
                System.out.println("No of received search msgs = "+noOfReceivedSearchMsgs);
                System.out.println("No of answered search msgs = "+noOfAnsweredSearchMsgs);
                System.out.println("No of forwarded search msgs = "+noOfForwardedSearchMsgs);
                System.out.println("Routing table size = "+routingTable.size());
                System.out.println("********************************");
    }
}
