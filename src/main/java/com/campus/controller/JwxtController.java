package com.campus.controller;


import com.alibaba.druid.util.StringUtils;
import com.campus.exception.InvalidException;
import com.campus.exception.PasswordErrorException;
import com.campus.exception.RepeatBindException;
import com.campus.exception.ServeErrorException;
import com.campus.model.CurrentTime;
import com.campus.model.EmptyClassroom;
import com.campus.model.JwxtInfo;
import com.campus.model.PersonInfo;
import com.campus.result.CodeMsg;
import com.campus.result.Result;
import com.campus.service.JwxtService;
import com.campus.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;

@RestController
public class JwxtController {

    @Autowired
    JwxtService jwxtService;
    private Logger logger = LoggerFactory.getLogger(JwxtController.class);
    /**
     * 绑定
     *
     * @param openid
     * @param account
     * @param password
     * @return
     */
    @RequestMapping(value = "jwxtbind", method = RequestMethod.POST)
    @ResponseBody
    public Object Bind(@RequestParam(value = "openid") String openid,
                       @RequestParam(value = "account") String account,
                       @RequestParam(value = "password") String password) {
        try {
            jwxtService.jwxtBind(account, password, openid);
        } catch (PasswordErrorException e) {
            return CodeMsg.PASSWORD_ERROR;
        } catch (ServeErrorException e) {
            return CodeMsg.SERVER_ERROR;
        } catch (RepeatBindException e) {
            return CodeMsg.RepeatBind_ERROR;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    jwxtService.obtainJwxt(openid);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return CodeMsg.SUCCESS;
    }

    /**
     * 更新信息
     *
     * @param openid
     * @return
     */
    @RequestMapping(value = "jwxtobtain", method = RequestMethod.POST)
    @ResponseBody
    public Object obtain(@RequestParam(value = "openid") String openid) {
        try {
            jwxtService.obtainJwxt(openid);
        } catch (PasswordErrorException e) {
            return CodeMsg.PASSWORD_ERROR;
        } catch (ServeErrorException e) {
            return CodeMsg.SERVER_ERROR;
        } catch (ParseException e) {
            return CodeMsg.Format_ERROR;
        } catch (InvalidException e) {
            return CodeMsg.Invalid_ERROR;
        }
        return CodeMsg.SUCCESS;
    }

    /**
     * 空教室
     *
     * @param openid
     * @return
     */
    @RequestMapping(value = "jwxtempty", method = RequestMethod.POST)
    @ResponseBody
    public Object getEmptyClassroom(@RequestParam(value = "openid") String openid, HttpServletRequest request) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String emptyTime = request.getParameter("emptytime")==null? null : request.getParameter("emptytime");
        String emptyDate = request.getParameter("emptydate")==null ? DateUtils.getNowDateString("yyyy-MM-dd") : request.getParameter("emptydate");
        System.out.println(request.getParameter("emptydate"));
        String classroomBuilding = request.getParameter("building");
        try {
            EmptyClassroom emptyClassroom = new EmptyClassroom();
            emptyClassroom.setClassroomBuilding(classroomBuilding);
            emptyClassroom.setEmptyTime(emptyTime);
            emptyClassroom.setEmptyDate(DateUtils.getDate(emptyDate, "yyyy-MM-dd"));
            List list = jwxtService.getEmptyClassroom(openid,emptyClassroom);
            return Result.success(list);
        } catch (ParseException e) {
            return CodeMsg.Format_ERROR;
        }
        catch (Exception e)
        {
            return CodeMsg.SERVER_ERROR;
        }

    }

    /**
     * 课程表
     *
     * @param openid
     * @return
     */
    @RequestMapping(value = "jwxtclassSchedule", method = RequestMethod.POST)
    @ResponseBody
    public Object getClassSchedule(@RequestParam(value = "openid") String openid,
                                   HttpServletRequest request) {
        CurrentTime currentTime;
        String termRange = request.getParameter("termrange");
        String weekNumber = request.getParameter("weeknumber");
        if (StringUtils.isEmpty(termRange) || StringUtils.isEmpty(weekNumber)) {
            currentTime = jwxtService.getCurrentTime(openid);
            termRange = StringUtils.isEmpty(termRange) ? currentTime.getXnxqh() : termRange;
            weekNumber = StringUtils.isEmpty(weekNumber) ? currentTime.getZc() : weekNumber;
        }
        List list = jwxtService.getClassSchedule(openid, termRange, weekNumber);
        return Result.success(list);
    }

    /**
     * 成绩查询
     *
     * @param openid
     * @param termRange
     * @return
     */
    @RequestMapping(value = "jwxtscoredata", method = RequestMethod.POST)
    @ResponseBody
    public Object getScoreData(@RequestParam(value = "openid") String openid, @RequestParam(value = "termrange") String termRange) {
        List list = jwxtService.getScoreData(openid, termRange);
        return Result.success(list);
    }

    /**
     * 个人信息
     *
     * @param openid
     * @return
     */
    @RequestMapping(value = "jwxtpersoninfo", method = RequestMethod.POST)
    @ResponseBody
    public Object getPersonInfo(@RequestParam(value = "openid") String openid) {
        PersonInfo personInfo = jwxtService.getPersonInfo(openid);
        return Result.success(personInfo);
    }


    /**
     * 获取当前时间所在的学期与教学周
     *
     * @param openid
     * @return
     */
    @RequestMapping(value = "jwxtcurrentime", method = RequestMethod.POST)
    @ResponseBody
    public Object getCurrentTime(@RequestParam(value = "openid") String openid) {
        try {
            CurrentTime currentTime = jwxtService.getCurrentTime(openid);
            if (currentTime != null) {
                HashMap json = new HashMap();
                json.put("startTime", currentTime.getS_time());
                json.put("endTime", currentTime.getE_time());
                json.put("weekNumber", currentTime.getZc());
                json.put("termRange", currentTime.getXnxqh());
                return Result.success(json);
            }
        } catch (PasswordErrorException e) {
            return CodeMsg.PASSWORD_ERROR;
        }
        return null;
    }


    /**
     * 获取所有学期范围
     *
     * @param openid
     * @return
     */
    @RequestMapping(value = "jwxtterm", method = RequestMethod.POST)
    @ResponseBody
    public Object getTermRange(@RequestParam(value = "openid") String openid) {
        try {
            List list = jwxtService.getTermRange(openid);
            return Result.success(list);
        } catch (PasswordErrorException e) {
            return CodeMsg.PASSWORD_ERROR;
        }
    }

    /**
     * 检查是否绑定
     *
     * @param openid
     * @return
     */
    @RequestMapping(value = "jwxtcheck", method = RequestMethod.POST)
    @ResponseBody
    public Object checkBind(@RequestParam(value = "openid") String openid) {

        try {
            JwxtInfo jwxtInfo = jwxtService.checkBind(openid);
            if (jwxtInfo != null) {
                return Result.success(jwxtInfo);
            } else {
                return CodeMsg.Not_Bind;
            }
        } catch (PasswordErrorException e) {
            return CodeMsg.PASSWORD_ERROR;
        } catch (ServeErrorException e) {
            return CodeMsg.SERVER_ERROR;
        }

    }

    /**
     * 删除绑定
     *
     * @param openid
     * @return
     */
    @RequestMapping(value = "jwxtdelete", method = RequestMethod.POST)
    @ResponseBody
    public Object delete(@RequestParam(value = "openid") String openid) {

        try {
            int i=jwxtService.deleteJwxt(openid);
            if(i>0)
            {
                return CodeMsg.SUCCESS;
            }
            else
            {
                return CodeMsg.SERVER_ERROR;
            }
        } catch (PasswordErrorException e) {
            return CodeMsg.PASSWORD_ERROR;
        } catch (ServeErrorException e) {
            return CodeMsg.SERVER_ERROR;
        }

    }


}
