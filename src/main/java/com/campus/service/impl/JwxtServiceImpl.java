package com.campus.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.campus.EtDao.Impl.CrawlJwxtDaoImpl;
import com.campus.exception.InvalidException;
import com.campus.exception.PasswordErrorException;
import com.campus.exception.RepeatBindException;
import com.campus.exception.ServeErrorException;
import com.campus.mapper.*;
import com.campus.model.*;
import com.campus.result.RedisResult;
import com.campus.service.JwxtService;
import com.campus.util.DateUtils;
import com.campus.util.SerializeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.text.ParseException;
import java.util.*;

@Service("jwxtService")
public class JwxtServiceImpl implements JwxtService {

    @Autowired
    JwxtInfoMapper jwxtInfoMapper;
    @Autowired
    CrawlJwxtDaoImpl crawlJwxtDao;
    @Autowired
    PersonInfoMapper personInfoMapper;
    @Autowired
    ClassScheduleMapper classScheduleMapper;
    @Autowired
    ClassroomUpdateMapper classroomUpdateMapper;
    @Autowired
    EmptyClassroomMapper emptyClassroomMapper;
    @Autowired
    ScoreDataMapper scoreDataMapper;
    @Autowired
    ClassUpdateMapper classUpdateMapper;

    @Override
    public void jwxtBind(String account, String password, String openid) throws PasswordErrorException, RepeatBindException, ServeErrorException {

        JwxtInfo jwxtInfo = new JwxtInfo();
        jwxtInfo.setOpenid(openid);
        jwxtInfo = jwxtInfoMapper.selectByPrimaryKey(openid);
        if (jwxtInfo == null) {
            HashMap headers = crawlJwxtDao.login(account, password);
            JwxtInfo jwxtInfo1 = new JwxtInfo();
            jwxtInfo1.setAccount(account);
            jwxtInfo1.setOpenid(openid);
            jwxtInfo1.setPassword(password);
            jwxtInfoMapper.insertSelective(jwxtInfo1);
        } else if (!StringUtils.equals(jwxtInfo.getAccount(), account)
                || !StringUtils.equals(jwxtInfo.getPassword(), password)) {
            HashMap headers = crawlJwxtDao.login(account, password);
            JwxtInfo jwxtInfo1 = new JwxtInfo();
            jwxtInfo1.setAccount(account);
            jwxtInfo1.setOpenid(openid);
            jwxtInfo1.setPassword(password);
            jwxtInfoMapper.updateByPrimaryKeySelective(jwxtInfo1);
        } else if (StringUtils.equals(jwxtInfo.getAccount(), account)
        ) {
            throw new RepeatBindException("重复绑定");
        }
    }

    @Override
    public void obtainJwxt(String openid) throws ParseException, PasswordErrorException, ServeErrorException {
        JwxtInfo jwxtInfo = jwxtInfoMapper.selectByPrimaryKey(openid);

        if (jwxtInfo != null) {
            final HashMap headers = crawlJwxtDao.login(jwxtInfo.getAccount(), jwxtInfo.getPassword());
            //个人信息
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PersonInfo personInfo = crawlJwxtDao.getPersonInfo(headers, jwxtInfo.getAccount());
                    if (personInfoMapper.selectByPrimaryKey(jwxtInfo.getAccount()) == null) {
                        personInfoMapper.insertSelective(personInfo);
                    } else {
                        personInfoMapper.updateByPrimaryKeySelective(personInfo);
                    }
                }
            }).start();
            //课程表

            final ClassUpdate classUpdate = classUpdateMapper.selectFirst(jwxtInfo.getAccount());
            if (classUpdate == null || !DateUtils.isNow(classUpdate.getUpdateTime())) {
                //获取学期范围
                List list = crawlJwxtDao.getTermRange(headers);
                Iterator iterator = list.iterator();
                List listSchedule = new LinkedList();

                //得到各学期的课表
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (iterator.hasNext()) {
                            Term term = (Term) iterator.next();

                            for (int i = 1; i < 22; i++) {
                                try {
                                    List list1 = crawlJwxtDao.getClassSchedule(headers, jwxtInfo.getAccount(), term.getTermRange(), i + "");
                                    if (!list1.isEmpty()) {
                                        listSchedule.addAll(list1);
                                    }
                                } catch (NullPointerException e) {
                                    break;
                                }
                            }
                        }
                            if (!listSchedule.isEmpty()) {

                                classScheduleMapper.deleteAccount(jwxtInfo.getAccount());
                                classScheduleMapper.insertList(listSchedule);
                            }

                    }
                }).start();
                if (classUpdate == null) {
                    ClassUpdate classUpdate2 = new ClassUpdate();
                    classUpdate2.setUpdateTime(new Date());
                    classUpdate2.setAccount(jwxtInfo.getAccount());
                    classUpdateMapper.insertSelective(classUpdate2);
                } else {
                    classUpdate.setUpdateTime(new Date());
                    classUpdateMapper.updateByPrimaryKeySelective(classUpdate);
                }
            }


            //空教室
            ClassroomUpdateKey classroomUpdateKey = classroomUpdateMapper.selectFirst();
            List emptyClassList = new LinkedList();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (classroomUpdateKey == null || !DateUtils.isNow(classroomUpdateKey.getCreationTime())) {
                        String today = DateUtils.getNowDateString("yyyy-MM-dd");
                        List list1 = null;
                        List list2 = null;
                        List list3 = null;
                        List list4 = null;
                        List list5 = null;
                        List list6 = null;
                        List list7 = null;
                        List list8 = null;
                        try {
                            list1 = crawlJwxtDao.getEmptyClassroom(headers, today, "allday");
                            list2 = crawlJwxtDao.getEmptyClassroom(headers, today, "am");
                            list3 = crawlJwxtDao.getEmptyClassroom(headers, today, "pm");
                            list4 = crawlJwxtDao.getEmptyClassroom(headers, today, "0102");
                            list5 = crawlJwxtDao.getEmptyClassroom(headers, today, "0304");
                            list6 = crawlJwxtDao.getEmptyClassroom(headers, today, "0506");
                            list7 = crawlJwxtDao.getEmptyClassroom(headers, today, "0708");
                            list8 = crawlJwxtDao.getEmptyClassroom(headers, today, "night");
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        emptyClassList.addAll(list1);
                        emptyClassList.addAll(list2);
                        emptyClassList.addAll(list3);
                        emptyClassList.addAll(list4);
                        emptyClassList.addAll(list5);
                        emptyClassList.addAll(list6);
                        emptyClassList.addAll(list7);
                        emptyClassList.addAll(list8);
                        emptyClassroomMapper.deleteDefiniteDay(DateUtils.getNowDate());
                        if (!emptyClassList.isEmpty()) {
                            emptyClassroomMapper.insertList(emptyClassList);
                        }
                        ClassroomUpdateKey classroomUpdateKey2 = new ClassroomUpdateKey();
                        classroomUpdateKey2.setCreationTime(new Date());
                        classroomUpdateKey2.setOpenid(openid);
                        classroomUpdateMapper.insertSelective(classroomUpdateKey2);
                    }

                }
            }).start();

            //成绩
            new Thread(new Runnable() {
                @Override
                public void run() {
                    List scoreDataList = crawlJwxtDao.getScoreData(headers, jwxtInfo.getAccount());
                    if (!scoreDataList.isEmpty()) {
                        scoreDataMapper.insertList(scoreDataList);
                    }
                }
            }).start();

        }else
        {
            throw new InvalidException("无效openid");
        }
    }


    @Override
    public List getClassSchedule(String openid, String termRange, String weekNumber) {
        JwxtInfo jwxtInfo = jwxtInfoMapper.selectByPrimaryKey(openid);
        if (jwxtInfo != null) {
            ClassSchedule classSchedule = new ClassSchedule();
            classSchedule.setWeekNumber(weekNumber);
            classSchedule.setAccount(jwxtInfo.getAccount());
            classSchedule.setTermRange(termRange);
            List list = classScheduleMapper.selectClassSchedule(classSchedule);
            return list;
        }
        return null;
    }

    @Override
    public List getEmptyClassroom(EmptyClassroom emptyClassroom)  {

        List list = emptyClassroomMapper.selectEmptyClassroom(emptyClassroom);
        return list;
    }

    @Override
    public PersonInfo getPersonInfo(String openid) {
        JwxtInfo jwxtInfo = jwxtInfoMapper.selectByPrimaryKey(openid);
        if (jwxtInfo != null) {
            PersonInfo personInfo=personInfoMapper.selectByPrimaryKey(jwxtInfo.getAccount());
            return personInfo;
        }

        return null;
    }

    @Override
    public List getScoreData(String openid, String termRange) {
        JwxtInfo jwxtInfo = jwxtInfoMapper.selectByPrimaryKey(openid);
        if (jwxtInfo != null) {
            List list = scoreDataMapper.selectScoreData(jwxtInfo.getAccount(), termRange);
            return list;
        }

        return null;
    }

    @Override
    public CurrentTime getCurrentTime(String openid)throws PasswordErrorException {
        JwxtInfo jwxtInfo = jwxtInfoMapper.selectByPrimaryKey(openid);
        if (jwxtInfo != null) {
            HashMap headers = crawlJwxtDao.login(jwxtInfo.getAccount(), jwxtInfo.getPassword());
            CurrentTime currentTime=crawlJwxtDao.getCurrenTime(headers);
            return currentTime;
        }
        return null;
    }

    @Override
    public List getTermRange(String openid) throws PasswordErrorException{
        JwxtInfo jwxtInfo = jwxtInfoMapper.selectByPrimaryKey(openid);
        if (jwxtInfo!= null) {
            HashMap headers = crawlJwxtDao.login(jwxtInfo.getAccount(), jwxtInfo.getPassword());
           List list=crawlJwxtDao.getTermRange(headers);
           return list;
        }
        return null;
    }

    @Override
    public JwxtInfo checkBind(String openid)throws PasswordErrorException,ServeErrorException {
        JwxtInfo jwxtInfo=jwxtInfoMapper.selectByPrimaryKey(openid);
        if(jwxtInfo!=null)
        {
            crawlJwxtDao.login(jwxtInfo.getAccount(),jwxtInfo.getPassword());
        }
        return jwxtInfo;
    }
}
