package com.campus.EtDao.Impl;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.campus.EtDao.CrawlJwxtDao;
import com.campus.exception.PasswordErrorException;
import com.campus.exception.ServeErrorException;
import com.campus.model.*;
import com.campus.util.DateUtils;
import com.campus.util.Request;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("crawlJWxtDao")
public class CrawlJwxtDaoImpl implements CrawlJwxtDao {
    String url="http://jwxt.gduf.edu.cn/app.do";
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    @Override
    public HashMap login(String account,String password) {
        HashMap params=new HashMap();
        HashMap headers=new HashMap();
        params.put("method","authUser");
        params.put("xh",account);
        params.put("pwd",password);
        headers.put("User-Agent","Mozilla/5.0 (Linux; U; Mobile; Android 6.0.1;DUK-AL20 Build/FRF91");
        headers.put("Accept","application/json");
        headers.put("Content-Type","application/x-www-form-urlencoded");
        CloseableHttpResponse response =Request.post(url,headers,params);
        Header[] headersCookie=null;
        try
        {
            headersCookie = response.getHeaders(
                    "Set-Cookie"
            );
        }
        catch (NullPointerException e)
        {
            throw new ServeErrorException("服务器错误");
        }
        String cookie=headersCookie[0].getValue();
        Pattern pattern = Pattern.compile("JSESSIONID=.*?;");
        Matcher m = pattern.matcher(cookie);
        if (m.find()) {
            cookie = m.group(0);
        }
        String content = Request.getContent(response);
        JSONObject json = JSONObject.parseObject(content);
        String token= json.getString("token");
        String flag=json.getString("flag");
        if(StringUtils.equals(flag,"0"))
        {
            throw new PasswordErrorException("账号或密码错误");
        }
        headers.put("token",token);
        headers.put("User-Agent"," Mozilla/5.0 (Linux; U; Mobile; Android 8.0.0;STF-AL00 Build/FRF91 )");
        return headers;
    }

    @Override
    public List getEmptyClassroom(HashMap headers, String emptyDate, String emptyTime) throws ParseException {
        HashMap params=new HashMap();
        params.put("method","getKxJscx");
        params.put("time",emptyDate);
        params.put("idleTime",emptyTime);
        CloseableHttpResponse response = Request.post(url, headers, params);
        List list=new LinkedList();
        String content=Request.getContent(response);
        JSONArray jsonArrays= JSONArray.parseArray(content);
        Iterator iterator=jsonArrays.iterator();
        while(iterator.hasNext())
        {
            String text=iterator.next().toString();
            JSONObject jsonObject=JSONObject.parseObject(text);
            text=jsonObject.getString("jsList");
            JSONArray jsonArray2=JSONArray.parseArray(text);
            Iterator iterator2=jsonArray2.iterator();
            while(iterator2.hasNext()&&jsonArray2.get(0)!=null)
            {
                text=iterator2.next().toString();
                jsonObject=JSONObject.parseObject(text);
                EmptyClassroom emptyClassroom=new EmptyClassroom();
                emptyClassroom.setClassroomCapacity(jsonObject.getString("yxzws"));
                emptyClassroom.setClassroomNumber(jsonObject.getString("jsid"));
                emptyClassroom.setClassroomBuilding(jsonObject.getString("jzwmc"));
                emptyClassroom.setEmptyDate(sdf.parse(emptyDate));
                emptyClassroom.setEmptyTime(emptyTime);
                emptyClassroom.setClassroomName(jsonObject.getString("jsmc"));
                emptyClassroom.setCampusType(jsonObject.getString("xqmc"));
                list.add(emptyClassroom);
            }
        }

        return list;
    }


    @Override
    public List getClassSchedule(HashMap headers, String account,String termRange,String weekNumber) {
        HashMap params=new HashMap();
        params.put("method","getKbcxAzc");
        params.put("xh",account);
        params.put("xnxqid",termRange);
        params.put("zc",weekNumber);
        CloseableHttpResponse response = Request.post(url, headers, params);
        List list=new LinkedList();
        String content=Request.getContent(response);
        JSONArray jsonArrays= JSONArray.parseArray(content);

        Iterator iterator=jsonArrays.iterator();
        //判断后续时间课表是否为空
        Boolean flag=true;
        while(iterator.hasNext()&&jsonArrays.get(0)!=null)
        {
            flag=false;
            String text= iterator.next().toString();
            JSONObject jsonObject=JSONObject.parseObject(text);
            ClassSchedule classSchedule=new ClassSchedule();
            classSchedule.setTeacher(jsonObject.getString("jsxm"));
            classSchedule.setWeekRange(jsonObject.getString("kkzc"));
            classSchedule.setAccount(account);
            classSchedule.setSessionStart(jsonObject.getString("kssj"));
            classSchedule.setSessionEnd(jsonObject.getString("jssj"));
            if(StringUtils.equals(jsonObject.getString("sjbz"),"0"))
            {
                classSchedule.setWeekType("单双周");
            }
            else if(StringUtils.equals(jsonObject.getString("sjbz"),"1"))
            {
                classSchedule.setWeekType("单周");
            }
            else{
            classSchedule.setWeekType("双周");
        }

            classSchedule.setCourseName(jsonObject.getString("kcmc"));
            classSchedule.setPlace(StringUtils.isEmpty(jsonObject.getString("jsmc"))?"未安排教室":jsonObject.getString("jsmc"));
            classSchedule.setCourseNumber(jsonObject.getString("kcsj"));
            classSchedule.setTermRange(termRange);
            classSchedule.setWeekNumber(weekNumber);
            list.add(classSchedule);
        }
        //后续课表都为空
        if(flag)
        {
            throw new NullPointerException();
        }
        return list;
    }

    @Override
    public ClassWeek getClassWeek(HashMap headers, String currDate) throws ParseException {
        HashMap params=new HashMap();
        params.put("method","getCurrentTime");
        params.put("currDate",currDate);
        CloseableHttpResponse response = Request.post(url, headers, params);
        String content=Request.getContent(response);
        JSONObject jsonObject=JSONObject.parseObject(content);
        ClassWeek classWeek=new ClassWeek();
        classWeek.setTermRange(jsonObject.getString("xnxqh"));
        classWeek.setClassStart(sdf.parse(jsonObject.getString("s_time")));
        classWeek.setClassEnd(sdf.parse(jsonObject.getString("e_time")));
        classWeek.setWeekNumber(jsonObject.getString("zc"));
        return classWeek;
    }

    @Override
    public PersonInfo getPersonInfo(HashMap headers, String account) {
        HashMap params=new HashMap();
        params.put("method","getUserInfo");
        params.put("xh",account);
        CloseableHttpResponse response = Request.post(url, headers, params);
        String content=Request.getContent(response);
        JSONObject jsonObject=JSONObject.parseObject(content);
        PersonInfo personInfo=new PersonInfo();
        personInfo.setAccount(account);
        personInfo.setName(jsonObject.getString("xm"));
        personInfo.setDepartment(jsonObject.getString("yxmc"));
        personInfo.setProfession(jsonObject.getString("zymc"));
        personInfo.setClbum(jsonObject.getString("bj"));
        personInfo.setEnrollmentDate(jsonObject.getString("rxnf"));
        personInfo.setSex(jsonObject.getString("xb"));
        personInfo.setGrade(jsonObject.getString("nj"));
        personInfo.setUpdateTime(new Date());
        return personInfo;
    }

    @Override
    public List getScoreData(HashMap headers, String account) {
        HashMap params=new HashMap();
        params.put("method","getCjcx");
        params.put("xh",account);
        CloseableHttpResponse response = Request.post(url, headers, params);
        String content=Request.getContent(response);
       JSONArray jsonArray=JSONArray.parseArray(content);
        List list =new LinkedList();
        Iterator iterator=jsonArray.iterator();
        while(iterator.hasNext())
        {
            String text=iterator.next().toString();
            JSONObject jsonObject=JSONObject.parseObject(text);
            ScoreData scoreData=new ScoreData();
            scoreData.setExamType(jsonObject.getString("ksxzmc"));
            scoreData.setHourCredit(jsonObject.getString("xf"));
            scoreData.setName(jsonObject.getString("xm"));
            scoreData.setUpdateTime(new Date());
            scoreData.setTermRange(jsonObject.getString("xqmc"));
            scoreData.setAccount(account);
            scoreData.setCourseName(jsonObject.getString("kcmc"));
            scoreData.setCourseType(jsonObject.getString("kcxzmc"));
            scoreData.setScore(jsonObject.getString("zcj"));
            list.add(scoreData);

        }
        return list;
    }

    @Override
    public List getTermRange(HashMap headers) {
        HashMap params=new HashMap();
        params.put("method","getXnxq");
        CloseableHttpResponse response = Request.post(url, headers, params);
        String content=Request.getContent(response);
        JSONArray jsonArray=JSONArray.parseArray(content);
        List list =new LinkedList();
        Iterator iterator=jsonArray.iterator();
        while(iterator.hasNext())
        {

            String text=iterator.next().toString();
            JSONObject jsonObject=JSONObject.parseObject(text);
            Term term=new Term();
            term.setTermRange(jsonObject.getString("xnxq01id"));
            if(jsonObject.getInteger("isdqxq")==1)
            {
                term.setTermType("第一学期");
            }
            else{
                term.setTermType("第二学期");
            }
            list.add(term);
        }
        return list;
    }

    @Override
    public CurrentTime getCurrenTime(HashMap headers) {
        HashMap params=new HashMap();
        params.put("method","getCurrentTime");
        params.put("currDate", DateUtils.getNowDateString("yyyy-MM-dd"));
        CloseableHttpResponse response=Request.post(url,headers,params);
        String content=Request.getContent(response);
        System.out.println(content);
        System.out.println(JSONObject.toJSONString(params));
        CurrentTime currentTime=JSONObject.parseObject(content,CurrentTime.class);
        return currentTime;
    }

}
