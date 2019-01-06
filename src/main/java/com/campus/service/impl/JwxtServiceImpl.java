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
import com.campus.util.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    @Autowired
    private RedisClient redisClinet;

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
        if (jwxtInfo != null) {
            redisClinet.delKey(RedisResult.jwxtInfo + openid);
        }
       JwxtServiceImpl jwxtService=this;
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

    }

    @Override
    public void obtainJwxt(String openid) throws ParseException, PasswordErrorException, ServeErrorException {
        JwxtInfo jwxtInfo = (JwxtInfo) redisClinet.get(RedisResult.jwxtInfo + openid);
        if (jwxtInfo == null) {
            jwxtInfo = jwxtInfoMapper.selectByPrimaryKey(openid);
            if (jwxtInfo != null) {
                redisClinet.setEx(RedisResult.jwxtInfo + openid, 86400, jwxtInfo);
            }
        }


        if (jwxtInfo != null) {
            final HashMap headers = crawlJwxtDao.login(jwxtInfo.getAccount(), jwxtInfo.getPassword());
            final String account=jwxtInfo.getAccount();
            //个人信息
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PersonInfo personInfo = crawlJwxtDao.getPersonInfo(headers, account);
                    if (personInfoMapper.selectByPrimaryKey(account) == null) {
                        personInfoMapper.insertSelective(personInfo);
                    } else {
                        personInfoMapper.updateByPrimaryKeySelective(personInfo);
                    }
                    redisClinet.delKey(RedisResult.personInfo+account);
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
                                    List list1 = crawlJwxtDao.getClassSchedule(headers,account, term.getTermRange(), i + "");
                                    if (!list1.isEmpty()) {
                                        listSchedule.addAll(list1);
                                    }
                                } catch (NullPointerException e) {
                                    break;
                                }
                            }
                        }
                        if (!listSchedule.isEmpty()) {

                            classScheduleMapper.deleteAccount(account);
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
                    List scoreDataList = crawlJwxtDao.getScoreData(headers, account);
                    if (!scoreDataList.isEmpty()) {
                        scoreDataMapper.insertList(scoreDataList);
                    }
                }
            }).start();

        } else {
            throw new InvalidException("无效openid");
        }
    }


    @Override
    public List getClassSchedule(String openid, String termRange, String weekNumber) {
        JwxtInfo jwxtInfo = (JwxtInfo) redisClinet.get(RedisResult.jwxtInfo + openid);
        if (jwxtInfo == null) {
            jwxtInfo = jwxtInfoMapper.selectByPrimaryKey(openid);
            if (jwxtInfo != null) {
                redisClinet.setEx(RedisResult.jwxtInfo + openid, 86400, jwxtInfo);
            }
        }

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
    public List getEmptyClassroom(String openid, EmptyClassroom emptyClassroom) throws ParseException {
        JwxtInfo jwxtInfo = (JwxtInfo) redisClinet.get(RedisResult.jwxtInfo + openid);
        if (jwxtInfo == null) {
            jwxtInfo = jwxtInfoMapper.selectByPrimaryKey(openid);
            if (jwxtInfo != null) {
                redisClinet.setEx(RedisResult.jwxtInfo + openid, 86400, jwxtInfo);
            }
        }

        String today = DateUtils.getDateString(emptyClassroom.getEmptyDate(), "yyyy-MM-dd");
        List<EmptyClassroom> list = null;
        if (jwxtInfo != null) {

            if (emptyClassroom.getEmptyTime() != null) {
                List<EmptyClassroom> list2 = (List<EmptyClassroom>) redisClinet.get(RedisResult.emptyRoomUpdate + today + ":" + emptyClassroom.getEmptyTime());

            } else {
                Set keys = redisClinet.Keys(RedisResult.emptyRoomUpdate + today + "*");
                Iterator it = keys.iterator();
                List list2 = new LinkedList();
                while (it.hasNext()) {
                    String key = it.next().toString();
                    List<EmptyClassroom> list3 = (List<EmptyClassroom>) redisClinet.get(key);
                    if (!list3.isEmpty())
                        list2.add((List) redisClinet.get(key));
                }
                if (!list2.isEmpty()) {
                    list = list2;
                }
            }

            //缓存不存在
            if (list == null) {
                //数据库查找
                list = emptyClassroomMapper.selectEmptyClassroom(emptyClassroom);
                if (!list.isEmpty()) {
                    List<EmptyClassroom> finalList = list;

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (!finalList.isEmpty()) {
                                Iterator it = finalList.iterator();
                                List list1 = new LinkedList();
                                List list2 = new LinkedList();
                                List list3 = new LinkedList();
                                List list4 = new LinkedList();
                                List list5 = new LinkedList();
                                List list6 = new LinkedList();
                                List list7 = new LinkedList();
                                List list8 = new LinkedList();
                                while (it.hasNext()) {
                                    EmptyClassroom emptyClassroom1 = (EmptyClassroom) it.next();

                                    if (StringUtils.equals(emptyClassroom1.getEmptyTime(), "allday")) {
                                        list1.add(emptyClassroom1);
                                    } else if (StringUtils.equals(emptyClassroom1.getEmptyTime(), "am")) {
                                        list2.add(emptyClassroom1);
                                    } else if (StringUtils.equals(emptyClassroom1.getEmptyTime(), "pm")) {
                                        list3.add(emptyClassroom1);
                                    } else if (StringUtils.equals(emptyClassroom1.getEmptyTime(), "0102")) {
                                        list4.add(emptyClassroom1);
                                    } else if (StringUtils.equals(emptyClassroom1.getEmptyTime(), "0304")) {
                                        list5.add(emptyClassroom1);
                                    } else if (StringUtils.equals(emptyClassroom1.getEmptyTime(), "0506")) {
                                        list6.add(emptyClassroom1);
                                    } else if (StringUtils.equals(emptyClassroom1.getEmptyTime(), "0708")) {
                                        list7.add(emptyClassroom1);
                                    } else if (StringUtils.equals(emptyClassroom1.getEmptyTime(), "night")) {
                                        list8.add(emptyClassroom1);
                                    }
                                }

                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":allday", 43200, list1);
                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":am", 43200, list2);
                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":pm", 43200, list3);
                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":0102", 43200, list4);
                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":0304", 43200, list5);
                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":0506", 43200, list6);
                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":0708", 43200, list7);
                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":night", 43200, list8);
                            }
                        }
                    }).start();

                }
                //数据库不存在
                else {

                    HashMap headers = crawlJwxtDao.login(jwxtInfo.getAccount(), jwxtInfo.getPassword());

                    List emptyClassList = new LinkedList();
                    if (emptyClassroom.getEmptyTime() == null) {

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
                        //存入缓存
                        List finalList1 = list1;
                        List finalList2 = list2;
                        List finalList3 = list3;
                        List finalList4 = list4;
                        List finalList5 = list5;
                        List finalList6 = list6;
                        List finalList7 = list7;
                        List finalList8 = list8;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":allday", 43200, finalList1);
                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":am", 43200, finalList2);
                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":pm", 43200, finalList3);
                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":0102", 43200, finalList4);
                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":0304", 43200, finalList5);
                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":0506", 43200, finalList6);
                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":0708", 43200, finalList7);
                                redisClinet.setEx(RedisResult.emptyRoomUpdate + today + ":night", 43200, finalList8);

                            }
                        }).start();

                        List finalEmptyClassList = emptyClassList;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                emptyClassroomMapper.insertList(finalEmptyClassList);
                            }
                        }).start();

                    } else {
                        emptyClassList = crawlJwxtDao.getEmptyClassroom(headers, today, emptyClassroom.getEmptyTime());
                        List finalEmptyClassList1 = emptyClassList;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                emptyClassroomMapper.insertList(finalEmptyClassList1);
                            }
                        }).start();
                    }
                    list = emptyClassList;
                }
            }


        }

        return list;
    }

    @Override
    public PersonInfo getPersonInfo(String openid) {

        JwxtInfo jwxtInfo = (JwxtInfo) redisClinet.get(RedisResult.jwxtInfo + openid);
        if (jwxtInfo == null) {
            jwxtInfo = jwxtInfoMapper.selectByPrimaryKey(openid);
            if (jwxtInfo != null) {
                redisClinet.setEx(RedisResult.jwxtInfo + openid, 86400, jwxtInfo);
            }
        }
        if (jwxtInfo != null) {

            PersonInfo personInfo = (PersonInfo) redisClinet.get(RedisResult.personInfo + jwxtInfo.getAccount());
            if (personInfo == null) {
                personInfo = personInfoMapper.selectByPrimaryKey(jwxtInfo.getAccount());
                if (personInfo != null) {
                    redisClinet.setEx(RedisResult.personInfo + jwxtInfo.getAccount(), 86400, personInfo);
                }
            }

            return personInfo;
        }

        return null;
    }

    @Override
    public List getScoreData(String openid, String termRange) {
        JwxtInfo jwxtInfo = (JwxtInfo) redisClinet.get(RedisResult.jwxtInfo + openid);
        if (jwxtInfo == null) {
            jwxtInfo = jwxtInfoMapper.selectByPrimaryKey(openid);
            if (jwxtInfo != null) {
                redisClinet.setEx(RedisResult.jwxtInfo + openid, 86400, jwxtInfo);
            }
        }

        if (jwxtInfo != null) {
            String account = jwxtInfo.getAccount();
            List<ScoreData> list = (List<ScoreData>) redisClinet.get(RedisResult.scoreData + account + ":" + termRange);
            if (list==null) {
                list = scoreDataMapper.selectScoreData(jwxtInfo.getAccount(), termRange);
                redisClinet.setEx(RedisResult.scoreData + account + ":" + termRange, 7200, list);
            }
            return list;
        }

        return null;
    }

    @Override
    public CurrentTime getCurrentTime(String openid) throws PasswordErrorException {
        JwxtInfo jwxtInfo = (JwxtInfo) redisClinet.get(RedisResult.jwxtInfo + openid);
        if (jwxtInfo == null) {
            jwxtInfo = jwxtInfoMapper.selectByPrimaryKey(openid);
            if (jwxtInfo != null) {
                redisClinet.setEx(RedisResult.jwxtInfo + openid, 86400, jwxtInfo);
            }
        }
        if (jwxtInfo != null) {
            HashMap headers = crawlJwxtDao.login(jwxtInfo.getAccount(), jwxtInfo.getPassword());
            CurrentTime currentTime = crawlJwxtDao.getCurrenTime(headers);
            return currentTime;
        }
        return null;
    }

    @Override
    public List getTermRange(String openid) throws PasswordErrorException {
        JwxtInfo jwxtInfo = (JwxtInfo) redisClinet.get(RedisResult.jwxtInfo + openid);
        if (jwxtInfo == null) {
            jwxtInfo = jwxtInfoMapper.selectByPrimaryKey(openid);
            if (jwxtInfo != null) {
                redisClinet.setEx(RedisResult.jwxtInfo + openid, 86400, jwxtInfo);
            }
        }

        if (jwxtInfo != null) {
            HashMap headers = crawlJwxtDao.login(jwxtInfo.getAccount(), jwxtInfo.getPassword());
            List list = crawlJwxtDao.getTermRange(headers);
            return list;
        }
        return null;
    }

    @Override
    public JwxtInfo checkBind(String openid) throws PasswordErrorException, ServeErrorException {
        JwxtInfo jwxtInfo = (JwxtInfo) redisClinet.get(RedisResult.jwxtInfo + openid);
        if (jwxtInfo == null) {
            jwxtInfo = jwxtInfoMapper.selectByPrimaryKey(openid);
            if (jwxtInfo != null) {
                redisClinet.setEx(RedisResult.jwxtInfo + openid, 86400, jwxtInfo);
            }
        }
        if (jwxtInfo != null) {
            crawlJwxtDao.login(jwxtInfo.getAccount(), jwxtInfo.getPassword());
        }
        return jwxtInfo;
    }

    @Override
    public int deleteJwxt(String openid) {
        return jwxtInfoMapper.deleteByPrimaryKey(openid);
    }
}
