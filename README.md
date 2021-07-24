# Distributed Content Searching

A simple overlay-based solution that allows a set of nodes to share contents among each other. A set of nodes connected via some overlay topology. Each of the nodes has a set of files that it is willing to share with other nodes. Node x is interested in a file f. x issues a  search query to the overlay to locate a at least one node y containing that particular file. Once the node is identified, the file f can be exchanged between X and y.

### How to run bootstrap server (from project root directory)
```
cd BootstrapServer
javac *.java
java BootstrapServer
```

### How to run the node
This project can be opened from netbeans and built and run.

Or else, you can download Apache and from
https://ant.apache.org/manual/install.html and then run ant command from node source code directory
```
cd P2PFileSharing
ant -f build.xml
java -jar dist/DSPhase2.jar

```