package JokeServer;
/*--------------------------------------------------------
Brandon Markwalder / 01/21/2018
CSC 435 Winter 2018, Clark Elliott
Java 8 Update 111 (build 1.8.0_111-b14)

File is: JokeServer.java
To compile: javac JokeServer.java
To run: In separate shell windows
    >java JokeClient
    >java JokeClientAdmin
    >java JokeServer

Files needed to run the program:
    JockClient.java
    JokeClientAdmin.java
    JokeServer.java

----------------------------------------------------------*/
//Import the required libraries
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.Random;


public class JokeServer {

    //Global mode variable for the JokeServer. Initially set to joke mode.
    public static String mode = "joke";


    public static void main(String a[]) throws IOException {
        int q_len = 6; // Not interesting but living dangerously
        int clientPort = 4545;

        //Declare a Socket and assign it to a variable
        Socket sock;

        //Instantiate a new ServerSocket instances, servsock and adminSock
        ServerSocket servsock = new ServerSocket(clientPort, q_len);

        //Instantiate a new ConnectToAdminServer instance.
        //Create a thread and assign the new ConnectToAdminServer.
        //Finally we start the thread
        ConnectToAdminServer connect = new ConnectToAdminServer();
        Thread thread = new Thread(connect);
        thread.start();

        //Print identity of the server
        //Loop until user inputs stop word / kill command
        //For every new connection create a sock and give it a worker thread
        System.out.println("Brandon Markwalder's Joke Server 1.0 starting up, listening on port 4545.\n");
        while(true) {
            sock = servsock.accept();
            new ServerWorker(sock).start();
        }
    }

    //ConnectToAdminServer creates a new Server Socket which we have
    //set to run in its own thread. This class connects to the JokeClientAdmin
    //and runs our AdminWorker which communicates with the JokeClientAdmin on
    //a separate port.
    private static class ConnectToAdminServer implements Runnable {
        public void run(){
            //Declare a Socket and assign it to a variable
            Socket adminSock;
            //Set the port, hard coded for now
            int clientAdminPort = 5050;
            //Set the max number of simultaneous requests to queue up before tossing them out
            int adminQ_len = 7;
            ServerSocket adminServerSock = null;
            try {
                //Here we instantiate our adminserver Socket
                adminServerSock = new ServerSocket(clientAdminPort, adminQ_len);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while(true) {
                try {
                    //Wait for a connection
                    adminSock = adminServerSock.accept();
                    //Start the socket
                    new AdminServerWorker(adminSock).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Creates a ServerWorker thread for each connecting client
    private static class ServerWorker extends Thread {
        //Declare a Socket and assign it to a variable
        Socket serverSock = new Socket();

        //To keep track of the state of each client we will build state strings
        //The state strings can be modified easily with the handy methods in StringBuilder
        //Instantiate new builders and assign them to variables
        StringBuilder jokesSent = new StringBuilder();
        StringBuilder proverbsSent = new StringBuilder();

        //Assign the passed in socket
        public ServerWorker(Socket sockIn) {
            serverSock = sockIn;
        }
        public void run() {

            PrintStream out = null;
            BufferedReader in = null;
            try{
                //Variable to read and store the input stream
                in = new BufferedReader(new InputStreamReader(serverSock.getInputStream()));
                //Variable to store and print the output stream
                out = new PrintStream(serverSock.getOutputStream());
                try{
                    //Declare our local variables to keep track of user inputs and the joke and
                    //proverb state for each client
                    String userName, userInput, jokesIn, proverbsIn, jokesOut, proverbsOut;
                    userName = in.readLine();
                    userInput = in.readLine();
                    jokesIn = in.readLine();
                    proverbsIn = in.readLine();

                    //Check the state, which is modified as below after a joke or proverb is sent
                    //A state of "1111" indicates that all four jokes or proverbs have been sent
                    //so we reset the state to "0000"
                    if (jokesIn.equals("1111")){
                        jokesIn = "0000";
                    }

                    if (proverbsIn.equals("1111")){
                        proverbsIn = "0000";
                    }

                    //Here we use the append method of string builder to set the current state of the client
                    jokesSent.append(jokesIn);
                    proverbsSent.append(proverbsIn);
                    //Basic logic to check that the input is valid and to check what state
                    //The JokeServer is currently set to
                    if (userInput.equals("")){
                        if (JokeServer.mode.equals("joke")){
                            System.out.println("Sending a joke to " + userName + "\n");
                            //Call the send joke method passing in the userName and our output stream
                            sendJoke(userName, out);
                        }
                        else{
                            System.out.println("Sending a proverb to " + userName + "\n");
                            //Call the send proverb method passing in the userNAme and output stream
                            sendProverb(userName, out);
                        }

                        //Update the joke and proverb state and send them back to the client
                        jokesOut = String.valueOf(jokesSent);
                        proverbsOut = String.valueOf(proverbsSent);
                        out.println(jokesOut);
                        out.println(proverbsOut);
                    }

                }
                //Catch the bad stuff
                catch (IOException x) {
                    System.out.println("Server read error");
                    x.printStackTrace();
                }
                //Close the socket
                serverSock.close();
            }
            //Catch the bad setuff
            catch (IOException ioe) {
                System.out.println(ioe);
            }

        }

        //These two methods, sendProverb and send Joke take as input the userName and
        //The output stream. This could be cleaner if I wrapped them into one method that took a joke or proverb
        //argument but I ran out of time. The jokes are randomized using Random, and then using a hack (charAt)
        //to get the state from the state string. This is not an efficient solution, whereas using a hashmap or
        //similar data structure would scale a lot better.
        private void sendProverb(String userName, PrintStream out) {
            Random random = new Random();
                String [] proverbs = {
                        "PA " + userName + ": Don't look a gift horse in the mouth",
                        "PB " + userName + ": Who needs napkins when we have jeans?",
                        "PC " + userName + ": A bird in the hand is a boring proverb",
                        "PD " + userName + ": The one to rise early and run, catches all the spider webs in the face",};
            while (true){
                //Create a random number 0-4
                int randomProverb = random.nextInt(4);
                //Grab the state flag at index of the random number
                char proverb = proverbsSent.charAt(randomProverb);
                if (proverb == '0'){
                    //Send the proverb
                    out.println(proverbs[randomProverb]);
                    //Update the state for the currently flagged proverb to indicate that it has been sent
                    proverbsSent.setCharAt(randomProverb, '1');
                    return;
                }
            }
        }

        //This is an almost identical method to sendProverb
        //Please See the comments on the send proverb method as the two
        //are execute with the same logic
        private void sendJoke(String userName, PrintStream out) {
            Random random = new Random();
            String [] jokes = {
                    "JA " + userName + ": What did the acorn say after it fell from the tree? Geometry",
                    "JB " + userName + ": Why did the chicken cross the road? To Show the squirrels it can be done",
                    "JC " + userName + ": What is a bird's favorite type of math? Owl-gebra",
                    "JD " + userName + ": Why do plants hate math? Because it gives them square roots",};
            while (true){
                int randomJoke = random.nextInt(4);
                char joke = jokesSent.charAt(randomJoke);
                if (joke == '0'){
                    out.println(jokes[randomJoke]);
                    jokesSent.setCharAt(randomJoke, '1');
                    return;
                }
            }
        }

    }
    }

    //Here is our AdminServer Worker which will run in a separate thread.
    //This worker is responsible for switching the JokeServer between modes
    class AdminServerWorker extends Thread {
        Socket adminSock = new Socket();
        public AdminServerWorker(Socket sockIn) {
            adminSock = sockIn;

        }
        public void run(){
            BufferedReader in = null;
            try{
                //Variable to read and store the input stream
                in = new BufferedReader(new InputStreamReader(adminSock.getInputStream()));
                try{
                    String userInput;
                    userInput = in.readLine();
                    if (userInput.equals("")){
                        //The mode is first declared globally and first set to joke mode
                        //We update the mode by simply reassignging the mode which is
                        //not the tidyest way of doing things

                        //Switch to proverb mode
                        if (JokeServer.mode.equals("joke")) {
                            JokeServer.mode = "proverb";
                            System.out.println("Switching to proverb mode");
                        }
                        //Switch to joke mode
                        else if (JokeServer.mode.equals("proverb")){
                            JokeServer.mode = "joke";
                            System.out.println("Switching to joke mode");
                        }
                        else{
                            System.out.println("Invalid switching input");
                        }
                    }
                }
                //Catch the bad sdtuff
                catch (IOException x) {
                    System.out.println("Server read error");
                    x.printStackTrace();
            }

            adminSock.close();
        }
        catch (IOException ioe) {
                System.out.println(ioe);
            }
    }
}
