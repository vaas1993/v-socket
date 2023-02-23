package utils;

import exceptions.ProcessException;
import com.alibaba.fastjson.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

public class RSA {
    /**
     * 将字符串形式的私钥转成 PrivateKey 实例
     */
    private PrivateKey stringToPrivateKey(String key) throws ProcessException {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            ArrayList<String> keys = new ArrayList<>(Arrays.asList(key.split("\n")));
            // 掐头去尾
            keys.remove(0);
            keys.remove(keys.size() - 1);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(String.join("", keys)));
            return factory.generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ProcessException("私钥内容异常：" + e.getMessage());
        }
    }

    /**
     * 解密
     */
    public String decrypt(String str, String appid) throws ProcessException {
        int maxLength = 256;
        ArrayList<String> origin = new ArrayList<>();

        JSONObject data;
        try {
            data = Database.getInstance().getCertificate(appid);
        } catch (ProcessException e) {
            throw new ProcessException(e.getMessage());
        }

        // 私钥在数据库以字符串的形式保存，这里转成 PrivateKey 实例
        PrivateKey key = this.stringToPrivateKey(data.getString("private"));

        // RSA加密的数据长度不能超过密钥长度，所以密文是经过多段加密后拼接的
        // 加密后的字符串会经过base64处理，所以可以直接用 == 切割，取出多段密文
        ArrayList<String> ciphertext = new ArrayList<>(Arrays.asList(str.split("==")));
        for (String s : ciphertext) {
            // 将密文末尾加上 == ，得到完成的 base64 密文
            byte[] temp = Base64.getDecoder().decode(s + "==");
            int strLength = temp.length;
            Cipher cipher;
            try {
                cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.DECRYPT_MODE, key);
                int offset = 0;
                do {
                    origin.add(new String(cipher.doFinal(temp, offset, maxLength)));
                    offset += maxLength;
                } while (strLength - offset > 0);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                     IllegalBlockSizeException | BadPaddingException e) {
                throw new ProcessException("消息体解密出错：" + e.getMessage());
            }
        }
        return String.join("", origin);
    }

    /**
     * 生成密钥对
     */
    public static String[] generateKeys(int size) throws NoSuchAlgorithmException {
        // KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        // 初始化密钥对生成器
        keyPairGen.initialize(size,new SecureRandom());
        // 生成一个密钥对，保存在keyPair中
        KeyPair keyPair = keyPairGen.generateKeyPair();
        // 得到私钥
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        // 得到公钥
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        // 得到公钥字符串
        String publicKeyString = new String(Base64.getEncoder().encode(publicKey.getEncoded()));
        // 得到私钥字符串
        String privateKeyString = new String(Base64.getEncoder().encode((privateKey.getEncoded())));

        String key1 = "-----BEGIN PUBLIC KEY-----\n" +
                publicKeyString.replaceAll("(.{64})", "$1\n") +
                "\n-----END PUBLIC KEY-----";
        String key2 = "-----BEGIN PRIVATE KEY-----\n" +
                privateKeyString.replaceAll("(.{64})", "$1\n") +
                "\n-----END PRIVATE KEY-----";
        return new String[]{key1, key2};
    }
}
