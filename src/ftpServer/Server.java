package ftpServer;

import java.net.ServerSocket;
import java.net.Socket;

public class Server
{

    public Server()
    {
        // TODO Auto-generated constructor stub
    }

    public static void main(String[] args)
    {
        try
        {
            int nPort = 1025;
            ServerSocket welcomeSocket = new ServerSocket(nPort);

            System.out.println("FTP Server started on port " + nPort);


            while (true)
            {
                //
                // Accept connection and start a new FTPSession object for 
                // each one.
                //
                
                Socket client = welcomeSocket.accept();
                client.setSoTimeout(60*1000);
                Worker w = new Worker(client);
                w.start();
                System.out.println("Connection received, worker started");
                w.join();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

}
