import utils.Database;
import utils.RSA;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Random;
import java.util.Scanner;

public class Create {
    public static void main(String[] args) {
        try {
            String remark = "";
            while (remark.equals("")) {
                System.out.println("请输入应用名称：");
                Scanner scan = new Scanner(System.in);
                remark = scan.nextLine();
            }
            System.out.println("正在创建...");

            // 生成应用ID
            String appid = generateRandomStr();
            appid = appid + System.currentTimeMillis();
            appid = md5(appid);
            // 生成RSA公钥私钥对
            String[] keys = RSA.generateKeys(2048);
            // 存入数据库
            Database.getInstance().createCertificate(appid, keys[0], keys[1], remark);

            System.out.println("已成功创建应用 " + appid);
            System.out.println("你可以使用以下SQL在 certificate 表里查找对应的记录：");
            System.out.println("SELECT * FROM certificate WHERE appid = '"+appid+"';");
        } catch (NoSuchAlgorithmException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateRandomStr() {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public static String md5(String str) throws NoSuchAlgorithmException {
        StringBuilder sb = new StringBuilder();
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] bs = digest.digest(str.getBytes());
        for (byte b : bs) {
            int x = b & 255;
            String s = Integer.toHexString(x);
            if (x > 0 && x < 16) {
                sb.append("0");
                sb.append(s);
            } else {
                sb.append(s);
            }
        }
        return sb.toString();
    }
}
