
public class OptimizedServer {
/*
*
    static void printAllKLength(int k) {
        char[] set = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
        int n = set.length;
        printAllKLengthRec(set, "", n, k);
    }

    static void printAllKLengthRec(char[] set, String prefix, int n, int k) {   // Base case: k is 0, print prefix
        if (k == 0) {
            allStrings.add(prefix);
            return;
        }
        for (int i = 0; i < n; ++i) {
            String newPrefix = prefix + set[i];
            printAllKLengthRec(set, newPrefix,
                    n, k - 1);
        }
    }

    public static String bruteForce(byte [] hashPwd,int pwdlength) throws NoSuchAlgorithmException {
        printAllKLength(pwdlength);
        String real_pass=Arrays.toString(hashPwd);
        for (int i=0;i< allStrings.size();i++){
            if( real_pass.equals(Arrays.toString(hashSHA1(allStrings.get(i))))){
                return allStrings.get(i);
            }
        }
        return "";
    }*/

}