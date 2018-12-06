package com.campus.result;

public class CodeMsg {
    private  int code;
    private String msg;


    //通用的错误码
    public static CodeMsg SUCCESS = new CodeMsg(0, "success");
    public static CodeMsg SERVER_ERROR = new CodeMsg(500100, "服务端异常");
    public static CodeMsg BIND_ERROR = new CodeMsg(500101, "绑定失败");
    public static CodeMsg Format_ERROR = new CodeMsg(500102, "格式错误");
    public static CodeMsg Invalid_ERROR = new CodeMsg(500103, "无效openid");
    //登录模块 5002XX
    public static CodeMsg PASSWORD_ERROR = new CodeMsg(500201, "密码错误");
    public static CodeMsg RepeatBind_ERROR = new CodeMsg(500202, "重复绑定");
    //绑定模块 5003XX
    public static CodeMsg Not_Bind = new CodeMsg(500301, "用户未绑定");
    private CodeMsg( ) {
    }

    private CodeMsg(int code,String msg) {
        this.code = code;
        this.msg = msg;
    }



    @Override
    public String toString() {
        return "CodeMsg [code=" + code + ", msg=" + msg + "]";
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
