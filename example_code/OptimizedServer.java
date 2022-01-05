import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class OptimizedServer {

    // Use of contiguous array storage instead of a list to benefit from burst access and increase blocks caching hit rate
    static String[] allStrings;
    static int idx=0;
    // Hashmap used for DP to store the password length received before as well as their lookup data
    static HashMap<Integer,HashMap> seenBefore=new HashMap<Integer,HashMap>();

    public static byte[] hashSHA1(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return md.digest(data.getBytes());
    }

    /**
     * @param in Stream from which to read the request
     * @return Request with information required by the server to process encrypted file
     */
    public static Request readRequest(DataInputStream in) throws IOException {
        byte[] hashPwd = new byte[20];
        int count = in.read(hashPwd, 0, 20);
        if (count < 0) {
            throw new IOException("Server could not read from the stream");
        }
        int pwdLength = in.readInt();
        long fileLength = in.readLong();

        return new Request(hashPwd, pwdLength, fileLength);
    }
    /**
     * This function generates of strings of length k in a recursive way, the strings are stored in the allStrings structure
     * @param k size of the string we want to generate
     */
    static void allKLengthStrings( int k)
    {
        char[] set = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'};
        int n = set.length;
        allKLengthRec(set, "", n, k);
    }
    /**
     * This function is the recursive body of previous function
     * @param set the alphabet from which we generate the string
     * @param prefix  result of previous recurcive call
     * @param n length of the alphabet
     * @param k number of remaining characters to fill in the string
     */
    static void allKLengthRec(char[] set,String prefix,int n, int k)
    {   // Base case: k is 0, it means we have reached the string length of k
        if (k == 0)    { allStrings[idx]=prefix; idx++;   return;  }
        for (int i = 0; i < n; ++i)
        {   String newPrefix = prefix + set[i];
            allKLengthRec(set, newPrefix,
                    n, k - 1);
        }
    }
    /**
     * this function extracts all strings from 10k-most-common file with a length of k
     * @param k length of targeted strings
     */
    static void getKLength(int k) throws IOException {
        File file =new File("D:/SINF2M/LINFO-2241/Project_Part1/10k-most-common_filered.txt");
        System.out.println("wesh zin !");
        BufferedReader br = new BufferedReader(new FileReader(file));

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            if (line.length()==k){ allStrings[idx]=line; idx++;}
        }
    }
    /**
     * This function brute forces the SHA-1 hash using a look-up attack to extract the password
     * @param hashPwd SHA-1 hash of the password
     * @param pwdlength length of original password before hash
     * @return cracked password
     */
    public static String bruteForce(byte [] hashPwd,int pwdlength) throws NoSuchAlgorithmException, IOException {

        if (!existInSeenBefore(pwdlength)){

            System.out.println(pwdlength+" has NOT been seen before");
            allStrings=new String[8200];
            getKLength(pwdlength);
            HashMap<String,String> lookUp=new HashMap<String,String>();
            String real_pass=Arrays.toString(hashPwd);
            String result="";

            for (int i=0;i< idx-1;i++){
                //System.out.println( i+"  goal  "+idx);
                String hash=Arrays.toString(hashSHA1(allStrings[i]));
                lookUp.put(hash,allStrings[i]); // add hash,pass entry to the lookup structure
                if( real_pass.equals(hash)){
                    System.out.println("aya bien"); result= allStrings[i];  }
            }
            System.out.println("last part");
            seenBefore.put(pwdlength,lookUp); //add new length hashmap to the DP structure
            return result;
        }
        else {
            System.out.println(pwdlength+" has been seen before");
            String real_pass=Arrays.toString(hashPwd);

            HashMap<String,String> lookup = seenBefore.get(pwdlength);
            System.out.println(lookup.get(real_pass));
            return lookup.get(real_pass);
        }
    }
    /**
     * This function checks the existence of a value in the keys of the seenBefore hashmap
     * @param length integer value to look for in the structure
     * @return true if the value is found, false otherwise
     */
    public static boolean existInSeenBefore(int length){
        for (int i : seenBefore.keySet()) {
            if(i==length) {  return true;}
        }
        return false;
    }

    public static void process_file(Socket socket) throws IOException, NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        File decryptedFile = new File("test_file-decrypted-server.pdf");
        File networkFile = new File("temp-server.pdf");

        // ServerSocket ss = new ServerSocket(3333);
        System.out.println("Waiting connection");
        // Socket socket = ss.accept();
        // System.out.println("Connection from: " + socket);

        // Stream to read request from socket
        InputStream inputStream = socket.getInputStream();
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        // Stream to write response to socket
        DataOutputStream outSocket = new DataOutputStream(socket.getOutputStream());


        // Stream to write the file to decrypt
        OutputStream outFile = new FileOutputStream(networkFile);

        Request request = readRequest(dataInputStream);
        long fileLength = request.getLengthFile();

        FileManagement.receiveFile(inputStream, outFile, fileLength);
        /*
        int readFromFile = 0;
        int bytesRead = 0;
        byte[] readBuffer = new byte[64];

        System.out.println("[Server] File length: "+ fileLength);
        while((readFromFile < fileLength)){
            bytesRead = inputStream.read(readBuffer);
            readFromFile += bytesRead;
            outFile.write(readBuffer, 0, bytesRead);
        }*/

        System.out.println("File length: " + networkFile.length());

        // HERE THE PASSWORD IS REPLACED WITH THE BRUTEFORCE PROCESS
        String password = bruteForce(request.getHashPassword(), request.getLengthPwd());
        System.out.println(" found password from brute force :" + password);
        SecretKey serverKey = CryptoUtils.getKeyFromPassword(password);
        CryptoUtils.decryptFile(serverKey, networkFile, decryptedFile);

        // Send the decryptedFile
        InputStream inDecrypted = new FileInputStream(decryptedFile);
        outSocket.writeLong(decryptedFile.length());
        outSocket.flush();
        FileManagement.sendFile(inDecrypted, outSocket);


        dataInputStream.close();
        inputStream.close();
        inDecrypted.close();
        outFile.close();
        socket.close();

    }

    public static void main(String[] args){
        int number_of_threads =5;

        int port = 3333;

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(number_of_threads);
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            Socket un_client;
            System.out.println("Waiting connection");
            int id = 0;
            while ((un_client = serverSocket.accept()) != null ){
                Socket actuel_client = un_client;

                executor.submit(()-> {
                    try {
                        process_file(actuel_client);
                    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
                        e.printStackTrace();
                    }
                });
                id++;
            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            executor.shutdown();
        }
    }
}