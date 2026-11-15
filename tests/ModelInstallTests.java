package com.yeezi.classlive;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Arrays;
public final class ModelInstallTests {
    static int checks;
    static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
    static String hash(byte[] bytes)throws Exception{StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))s.append(String.format("%02x",b&255));return s.toString();}
    public static void main(String[] args)throws Exception{
        File dir=Files.createTempDirectory("model-install-test").toFile();File target=new File(dir,"model.gguf");
        byte[] bytes="GGUFtest payload".getBytes("UTF-8");ModelCatalog.Spec spec=new ModelCatalog.Spec("test",bytes.length,hash(bytes),"test");
        try{
            OfflineModel.install(spec,target,new ByteArrayInputStream(bytes),p->{});check(Arrays.equals(bytes,Files.readAllBytes(target.toPath())),"valid model installs");
            byte[][] bad={"NOTGGUF".getBytes("UTF-8"),Arrays.copyOf(bytes,bytes.length-1),Arrays.copyOf(bytes,bytes.length+1),"GGUFwrong bytes!".getBytes("UTF-8")};
            for(byte[] input:bad){boolean failed=false;try{OfflineModel.install(spec,target,new ByteArrayInputStream(input),p->{});}catch(IOException expected){failed=true;}check(failed,"reject invalid model");check(Arrays.equals(bytes,Files.readAllBytes(target.toPath())),"preserve previous model");check(!new File(target+".part").exists(),"clean partial file");}
            Thread.currentThread().interrupt();boolean cancelled=false;try{OfflineModel.install(spec,target,new ByteArrayInputStream(bytes),p->{});}catch(InterruptedIOException e){cancelled=true;}finally{Thread.interrupted();}check(cancelled,"cancel install");check(!new File(target+".part").exists(),"cancel cleans partial");
            check(ModelCatalog.ALL.length==4,"four supported variants");check(ModelCatalog.find("invalid")==ModelCatalog.ALL[0],"unknown id defaults Q4");for(ModelCatalog.Spec m:ModelCatalog.ALL){check(m.sha256.matches("[0-9a-f]{64}"),"sha256 present");check(m.url().contains(ModelCatalog.REVISION),"pinned model revision");}
            System.out.println("ModelInstallTests: "+checks+" checks passed");
        }finally{target.delete();new File(target+".part").delete();dir.delete();}
    }
}
