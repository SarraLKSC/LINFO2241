import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ThreadLocalRandom;


public class Main {

    /**
     * This function hashes a string with the SHA-1 algorithm
     * @param data The string to hash
     * @return An array of 20 bytes which is the hash of the string
     */
    public static byte[] hashSHA1(String data) throws NoSuchAlgorithmException{
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            return md.digest(data.getBytes());
    }

    /**
     * This function is used by a client to send the information needed by the server to process the file
     * @param out Socket stream connected to the server where the data are written
     * @param hashPwd SHA-1 hash of the password used to derive the key of the encryption
     * @param pwdLength Length of the clear password
     * @param fileLength Length of the encrypted file
     */
    public static void sendRequest(DataOutputStream out, byte[] hashPwd, int pwdLength,
                       long fileLength) throws IOException {
        out.write(hashPwd,0, 20);
        out.writeInt(pwdLength);
        out.writeLong(fileLength);
    }

    public static String randomPWD() throws IOException {
        String path="D:/SINF2M/LINFO-2241/Project_Part1/10k-most-common_filered.txt";
        //Get number of lines in the file
        long lines = Files.lines(Path.of(path)).count();
        System.out.println("number of lines in password file= "+lines);
        //Generate random number of a line in the file
        int randomNum = ThreadLocalRandom.current().nextInt(0, (int) lines+1);
        System.out.println("random number is= "+randomNum);
        //Get password at that random line
        String randompwd = Files.readAllLines(Paths.get(path)).get(randomNum);
        System.out.println("password of that line is= "+randompwd);


        return randompwd;
    }
    public static String randomFilePath(){
        String[] paths=new String[5];
        paths[0]="D:/SINF2M/LINFO-2241/Project_Part1/Files-5MB/PineTools.com_files";
        paths[1]="D:/SINF2M/LINFO-2241/Project_Part1/Files-20KB/PineTools.com_files";
        paths[2]="D:/SINF2M/LINFO-2241/Project_Part1/Files-50KB/PineTools.com_files";
        paths[3]="D:/SINF2M/LINFO-2241/Project_Part1/Files-50MB/PineTools.com_files";
        paths[4]="D:/SINF2M/LINFO-2241/Project_Part1/Files-100KB/PineTools.com_files";
        int[] lengths= new int[5];
        lengths[0]=5; lengths[1]=5; lengths[2]=5; lengths[3]=2; lengths[4]=5;
        int randomDir = ThreadLocalRandom.current().nextInt(0, (int) 4);
        int randomFile= ThreadLocalRandom.current().nextInt(1, (int) lengths[randomDir]+1);
        String randomFilepath=paths[randomDir]+ "/file-" +randomFile+ ".bin";
        return randomFilepath;
    }

    public static void main(String[] args) {
        try{
            String password = randomPWD();
            SecretKey keyGenerated = CryptoUtils.getKeyFromPassword(password);
            File inputFile = new File(randomFilePath());
            System.out.println(password);
            System.out.println(randomFilePath());
            File encryptedFile = new File("test_file-encrypted-client.pdf");
            File decryptedClient = new File("test_file-decrypted-client.pdf");

            // set file for results
            FileWriter writer;
            File resultsFile = new File("measurements_results.txt");
            if(resultsFile.createNewFile()){
                 writer= new FileWriter("measurements_results.txt");
                writer.write("File_size,Password_length,Execution_time");}
            else {
                 writer= new FileWriter("measurements_results.txt",true);}


            // This is an example to help you create your request
            CryptoUtils.encryptFile(keyGenerated, inputFile, encryptedFile);
            System.out.println("Encrypted file length: " + encryptedFile.length());



            // Creating socket to connect to server (in this example it runs on the localhost on port 3333)
            Socket socket = new Socket("localhost", 3333);
            //Socket socket = new Socket("192.168.0.104", 3333);
            // For any I/O operations, a stream is needed where the data are read from or written to. Depending on
            // where the data must be sent to or received from, different kind of stream are used.
            OutputStream outSocket = socket.getOutputStream();
            DataOutputStream out = new DataOutputStream(outSocket);
            InputStream inFile = new FileInputStream(encryptedFile);
            DataInputStream inSocket = new DataInputStream(socket.getInputStream());


            // SEND THE PROCESSING INFORMATION AND FILE
            byte[] hashPwd = hashSHA1(password);
            int pwdLength = password.length();
            long fileLength = encryptedFile.length();
            long time_before_sending_request = System.currentTimeMillis();
            sendRequest(out, hashPwd, pwdLength, fileLength);
            out.flush();

            FileManagement.sendFile(inFile, out);

            // GET THE RESPONSE FROM THE SERVER
            OutputStream outFile = new FileOutputStream(decryptedClient);
            long fileLengthServer = inSocket.readLong();
            System.out.println("Length from the server: "+ fileLengthServer);
            FileManagement.receiveFile(inSocket, outFile, fileLengthServer);
            long time_after_receiving_file = System.currentTimeMillis();
            long exe_time=(time_after_receiving_file-time_before_sending_request);
            writer.write("\n");
            writer.write(Files.size(Path.of(inputFile.getPath())) + "," + pwdLength + "," + exe_time);



            //System.out.println((inputFile.getName()+','+ Files.size(Path.of(inputFile.getPath()))+','+exe_time));


            out.close();
            outSocket.close();
            outFile.close();
            writer.close();
            inFile.close();
            inSocket.close();
            socket.close();

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException |
                NoSuchPaddingException | IllegalBlockSizeException | IOException | BadPaddingException |
                InvalidKeyException e) {
            e.printStackTrace();
        }

    }
}
