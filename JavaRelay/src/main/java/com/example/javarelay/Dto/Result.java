package com.example.javarelay.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result implements Serializable {

    private int code = 200;

    private String message = null;

    private Object data = null;

    private Result(){}

    static public Result success(){
        return success(null,null);
    }

    static public Result success(Object data){
        return success(null,data);
    }

    static public Result success(String msg,Object data){
         Result result = new Result();
         result.setCode(200);
         if(msg == null){
             result.setMessage("操作成功");
         }else{
             result.setMessage(msg);
         }

         if(data != null){
             result.setData(data);
         }

         return result;
    }

    static public Result fail(String msg){

        Result result = new Result();
        result.setCode(500);
        result.setMessage(msg);
        result.setData(null);

        return result;
    }
}
