package ftpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A very simple FTP Server class. On receiving a new connection it creates a
 * new worker thread.
 * 
 * @author Moritz Stueckler
 *
 */
public class Server {
  private int controlPort = 1025;
  private ServerSocket welcomeSocket;
  boolean serverRunning = true;

  public static void main(String[] args) {
    new Server();
  }

  public Server() {
    try {
      welcomeSocket = new ServerSocket(controlPort);
    } catch (IOException e) {
      System.out.println("Could not create server socket");
      System.exit(-1);
    }

    System.out.println("FTP Server started listening on port " + controlPort);

    int noOfThreads = 0;

    while (serverRunning) {

      try {

        Socket client = welcomeSocket.accept();

        // Port for incoming dataConnection (for passive mode) is the controlPort +
        // number of created threads + 1
        int dataPort = controlPort + noOfThreads + 1;

        // Create new worker thread for new connection
        Worker w = new Worker(client, dataPort);

        System.out.println("New connection received. Worker was created.");
        noOfThreads++;
        w.start();
      } catch (IOException e) {
        System.out.println("Exception encountered on accept");
        e.printStackTrace();
      }

    }
    
    try {
      welcomeSocket.close();
      System.out.println("Server was stopped");

    } catch (IOException e) {
      System.out.println("Problem stopping server");
      System.exit(-1);
    }

  }

}
