package com.yeezi.classlive;

final class ModelCatalog {
    static final String REVISION="843b1b175ace04f5a89d379c79e2518c0d065b25";
    static final class Spec {
        final String id,name,sha256,description; final long size;
        Spec(String id,long size,String sha,String description){this.id=id;this.name="parakeet-unified-en-0.6b-"+id+".gguf";this.size=size;this.sha256=sha;this.description=description;}
        String url(){return "https://huggingface.co/handy-computer/parakeet-unified-en-0.6b-gguf/resolve/"+REVISION+"/"+name;}
        String sizeLabel(){return Math.round(size/1000000d)+" MB";}
        String label(){return "Parakeet English · "+id.replace("_K_M", "").replace("_K", "").replace("_0", "");}
    }
    static final Spec[] ALL={
        new Spec("Q4_K_M", 477274496L, "a8bf3de2b393bd14ead5a858c3748d5e3b07a20fdeabdd3b498fba4f463fa929", "推荐日常课堂；占用最小，延续测试版配置。"),
        new Spec("Q5_K_M", 540795264L, "f9def6f9b4e83ab7d006df3e1b676dfa1f973a3b6da232a9c99fcaa66bcd2836", "均衡档；量化损失更小，内存占用略增。"),
        new Spec("Q6_K", 602191232L, "912c42ec3fd1afa31b7eda45684577feadf413249ca767fa912a89c4810e38d0", "高精度档；保留更多权重精度，运行负担更高。"),
        new Spec("Q8_0", 731357568L, "4b50b6dd862bf6e346929aaf4f5eaacec003bfa3f56462d6c874b41ef2f38795", "低量化损失档；占用最高，适合对比测试。")
    };
    static Spec find(String id){for(Spec s:ALL)if(s.id.equals(id))return s;return ALL[0];}
}
