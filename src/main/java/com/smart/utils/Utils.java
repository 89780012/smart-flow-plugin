package com.smart.utils;

public class Utils {

    public static boolean isEmpty(Object obj){
        if(obj == null){
            return true;
        }
        if("".equals((String)obj)){
            return true;
        }
        return false;
    }

}
