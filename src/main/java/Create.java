import exceptions.ProcessException;
import utils.Database;
import utils.RSA;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Random;
import java.util.Scanner;

/**
 * 该类用来创建应用ID和对应的RSA密钥
 * 编译后使用 "java -cp xxx.jar Create" 调用
 */
public class Create {
    public static void main(String[] args) {
        try {
            String remark = "";
            // 确保操作者一定输入应用名称，后面会将应用名称保存在数据库的remark字段里
            while (remark.equals("")) {
                System.out.println("请输入应用名称：");
                Scanner scan = new Scanner(System.in);
                remark = scan.nextLine();
            }
            System.out.println("正在创建应用...");

            // 生成应用ID，应用ID就是生成随机字符串 + 时间戳拼接然后取md5值
            String appid = generateRandomStr();
            appid = appid + System.currentTimeMillis();
            appid = md5(appid);
            // 生成RSA公钥私钥对
            String[] keys = RSA.generateKeys(2048);
            // 存入数据库
            Database.getInstance().createCertificate(appid, keys[0], keys[1], remark);

            System.out.println("\033[32m已成功创建应用"+appid+"\033[0m");
            // 在执行目录下保存密钥文件
            try {
                System.out.println("");
                System.out.println("\033[33m是否需要创建本地密钥副本（yes/no）？\033[0m");
                String putToDisk = null;
                while (putToDisk == null) {
                    Scanner scan = new Scanner(System.in);
                    putToDisk = scan.nextLine();
                    if( !putToDisk.equalsIgnoreCase("yes") && !putToDisk.equalsIgnoreCase("no") ) {
                        putToDisk = null;
                        System.out.println("\033[33m请输入 yes / no：\033[0m");
                    }
                }

                if( putToDisk.equalsIgnoreCase("yes") ) {
                    System.out.println("正在创建密钥副本...");
                    String path = put2disk(appid, remark, keys);
                    System.out.println("\033[32m密钥副本保存在 "+path+" 下\033[0m");
                }
            } catch (ProcessException e) {
                System.out.println("\033[31m密钥副本创建失败："+ e.getMessage() +"\033[0m");
            }
        } catch (NoSuchAlgorithmException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String put2disk(String appid, String remark, String[] keys) throws ProcessException {
        String parentDirName = "./certificates/";
        String dirName = appid + "-" + remark;
        String path = parentDirName + dirName + "/";

        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new ProcessException("文件夹 " + path + " 创建失败，请确认是否拥有写权限");
            }
        }

        String[][] outputs = {
                {
                        path + "appid.txt",
                        appid,
                },
                {
                        path + "public.txt",
                        keys[0],
                },
                {
                        path + "private.txt",
                        keys[1],
                },
        };
        FileWriter fw;
        File file;
        for (String[] output : outputs) {
            file = new File(output[0]);
            try {
                file.createNewFile();
                fw = new FileWriter(file);
                fw.write(output[1]);
                fw.close();
            } catch (IOException e) {
                throw new ProcessException(output[0] + " 操作失败，" + e.getMessage());
            }
        }
        return path;
    }

    /**
     * 生成随机数
     */
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

    /**
     * 计算 md5
     */
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
