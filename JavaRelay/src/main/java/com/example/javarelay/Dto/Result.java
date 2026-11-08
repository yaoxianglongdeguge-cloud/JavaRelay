package com.example.javarelay.Dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result implements Serializable {

    private Integer code = 200;

    private String message = null;

    private Object data = null;

    private Result(){}

    public Result success(){
        return success(null,null);
    }

    public Result success(Object data){
        return success(null,data);
    }

    public Result success(String msg,Object data){
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

    public Result fail(String msg){

        Result result = new Result();
        result.setCode(500);
        result.setMessage(msg);
        result.setData(data);

        return result;
    }
}
