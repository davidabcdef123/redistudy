package com;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.nio.charset.Charset;

/**
 * Created by sc on 2019/1/26.
 */
public class BloomFilterDemo {

    //Funnels 漏斗
    //expectedInsertions 预期插入元素总数

    public static void main(String[] args) {
        BloomFilter bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()),1000000,0.001);//!%
        bloomFilter.put("avc");
        System.out.println(bloomFilter.mightContain("qqq"));
    }
}
