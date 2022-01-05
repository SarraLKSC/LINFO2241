import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class Client_Multithreaded {





    public static byte[] hashSHA1(String data) throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance("SHA-1");

        return md.digest(data.getBytes());
    }

    public static void sendRequest(DataOutputStream out, byte[] hashPwd, int pwdLength,
                                   long fileLength) throws IOException {
        out.write(hashPwd,0, 20);
        out.writeInt(pwdLength);
        out.writeLong(fileLength);
    }


    public static void main(String[] args)
    {
        // establish a connection by providing host and port
        // number
        try  {

            String password = "test";
            SecretKey keyGenerated = CryptoUtils.getKeyFromPassword(password);
            File inputFile = new File("test_file.pdf");
            File encryptedFile = new File("test_file-encrypted-client.pdf");
            File decryptedClient = new File("test_file-decrypted-client.pdf");

            // set file for results
            File resultsFile = new File("measurements_results.txt");
            FileWriter writer;
            if(resultsFile.createNewFile()){
                writer= new FileWriter("measurements_results.txt");
                writer.write("File_name,File_size,Execution_time");}
            else {
                writer= new FileWriter("measurements_results.txt",true);}



            CryptoUtils.encryptFile(keyGenerated, inputFile, encryptedFile);
            System.out.println(new StringBuilder().append("Encrypted file length: ").append(encryptedFile.length()).toString());


            Socket socket = new Socket("localhost", 3333);





            /** writing to server
            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);**/

            //TEACHER VERSION !!
            OutputStream outSocket = socket.getOutputStream();
            DataOutputStream out = new DataOutputStream(outSocket);

           /** // reading from server
            BufferedReader in
                    = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));**/

            InputStream inFile = new FileInputStream(encryptedFile);
            DataInputStream inSocket = new DataInputStream(socket.getInputStream());

            // object of scanner class
            Scanner sc = new Scanner(System.in);
            String line = null;

            while (!"exit".equalsIgnoreCase(line)) {

                // reading from user
                line = sc.nextLine();

                // sending the user input to server
              //  out.println(line);
                out.flush();

                // displaying server reply
                System.out.println("Server replied ");

            }

            // closing the scanner object
            sc.close();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException |
                NoSuchPaddingException | IllegalBlockSizeException | IOException | BadPaddingException |
                InvalidKeyException e) {
            e.printStackTrace();
        }
    }
}

