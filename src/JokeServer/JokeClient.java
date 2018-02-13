package JokeServer;
/*--------------------------------------------------------
Brandon Markwalder / 01/21/2018
CSC 435 Winter 2018, Clark Elliott
Java 8 Update 111 (build 1.8.0_111-b14)

File is: JokeClient.java
To compile: javac JokeClient.java
To run: In separate shell windows
    >java JokeClient
    >java JokeClientAdmin
    >java JokeServer

Files needed to run the program:
    JockClient.java
    JokeClientAdmin.java
    JokeServer.java

----------------------------------------------------------*/
//Import required input/output and networking libraries
import java.io.*;
import java.net.*;

public class JokeClient {

    //Global variables to keep track of the current joke and proverb state
    //Each character can be thought of as a flag with 0 indicating not seen
    //and 1 indicating seen. These strings are sent to the JokeServer and updated as
    //jokes and proverbs are sent back. The client keeps track of what it has seen and sends
    //the state with each request for a new joke or proverb.
    public static String jokesRecieved = "0000";
    public static String proverbsRecieved = "0000";

    //Main method where we declare the server name variable and
    //we default to localhost if there are no args supplied.
    public static void main (String args[]) {
        String serverName;
        if (args.length < 1) serverName = "localhost";
        else serverName = args[0];

        System.out.println("Brandon Markwalder's Joke Client 1.0.\n");
        System.out.println("Using server: " + serverName + ", Port: 4545");

        //Instantiate a new Input Stream Reader wrapped in a Buffer Reader
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        //Hard coded stop word = "quit". While stop word not entered, loop forever
        //Each loop assigns user input to String name.
        try {
            String userName, userInput;
            System.out.print("Enter your username, (quit) to end: ");
            System.out.flush();
            userName = in.readLine();  //Assign user input to userName
            //Logic to exit the client on stop word
            if (userName.equals("quit")){
                System.out.println("Cancelled by user request");
                System.exit(0);
            }
            else{

                do {
                    System.out.print("Press Enter to get a joke or proverb from the JokeServer: ");
                    System.out.flush();
                    userInput = in.readLine();  //Assign user input to userName

                    if (userInput.equals("")){
                        //Call the getJokeorProverb method passing in the userName,
                        //userInput, and serverName variables as arguments
                        getJokeOrProverb(userName, userInput, serverName);

                    }
                    //Logic to exit the client on stop word
                    else if(userInput.equals("quit")){
                        System.out.println("Cancelled by user request");
                    }
                    else{
                        System.out.println("Invalid input, Press Enter to get a joke or proverb from the JokeServer");
                    }
                }
                while(!userInput.equals("quit"));
            }
        }
        //Catch the bad stuff
        catch (IOException x) {
            x.printStackTrace();
        }
    }

    //Take as input the client userName, userInput, and serverName
    private static void getJokeOrProverb(String userName, String userInput, String serverName) {

        //Delcare our variables for the socket, reader and sender
        Socket sock;
        BufferedReader fromServer;
        PrintStream toServer;
        String textFromServer;

        try {

            //Instantiate a new Socket instance and assign it to a port
            sock = new Socket(serverName, 4545);

            //Instantiate and assign are instances for reading from and sending to the server
            fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            toServer = new PrintStream(sock.getOutputStream());

            //Send the user input to the server
            toServer.println(userName);
            toServer.println(userInput);

            //Send the current seen joke and proverb states
            toServer.println(jokesRecieved);
            toServer.println(proverbsRecieved);

            //Read and store JokeServers response
            textFromServer = fromServer.readLine();

            if (textFromServer != null){
                //Print the joke or proverb
                System.out.println(textFromServer);
            }

            //Update the returned state that the JokeServer sent
            jokesRecieved = fromServer.readLine();
            proverbsRecieved = fromServer.readLine();
            toServer.flush();
            sock.close();

        }
        //Catch the bad stuff
        catch (IOException x) {
            System.out.println("Socket error.");
            x.printStackTrace();
        }
    }
}
