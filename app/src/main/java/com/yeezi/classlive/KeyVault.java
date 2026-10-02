package com.yeezi.classlive;
import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
final class KeyVault {
    private final Context context;
    KeyVault(Context c) { context=c; }
    private SecretKey key() throws Exception {
        KeyStore s=KeyStore.getInstance("AndroidKeyStore"); s.load(null);
        if (!s.containsAlias("classlive-api")) {
            KeyGenerator g=KeyGenerator.getInstance("AES", "AndroidKeyStore");
            g.init(new KeyGenParameterSpec.Builder("classlive-api", KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            g.generateKey();
        }
        return (SecretKey)s.getKey("classlive-api",null);
    }
    void put(String name,String text) throws Exception {
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key());
        String value=Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(text.getBytes("UTF-8")),Base64.NO_WRAP);
        if(!context.getSharedPreferences("keys",0).edit().putString(name,value).commit())throw new java.io.IOException("key save failed");
    }
    String get(String name) throws Exception {
        String value=context.getSharedPreferences("keys",0).getString(name,""); if(value.isEmpty())return "";
        String[] v=value.split(":"); Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(v[0],Base64.NO_WRAP)));
        return new String(c.doFinal(Base64.decode(v[1],Base64.NO_WRAP)),"UTF-8");
    }
}
