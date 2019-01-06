package com.campus.service;

import com.campus.exception.PasswordErrorException;
import com.campus.exception.RepeatBindException;
import com.campus.model.CurrentTime;
import com.campus.model.EmptyClassroom;
import com.campus.model.JwxtInfo;
import com.campus.model.PersonInfo;

import java.text.ParseException;
import java.util.List;

public interface JwxtService {

    public void jwxtBind(String account,String password,String openid) throws PasswordErrorException, RepeatBindException;
    public void obtainJwxt(String openid) throws ParseException ,PasswordErrorException;
    public List getClassSchedule(String openid,String termRange,String weekNumber);
    public List getEmptyClassroom(String openid,EmptyClassroom emptyClassroom) throws Exception;
    public PersonInfo getPersonInfo(String openid);
    public List getScoreData(String openid,String termRange);
    public CurrentTime getCurrentTime(String openid) throws PasswordErrorException;
    public List getTermRange(String openid)throws PasswordErrorException;
    public JwxtInfo checkBind(String openid);
    public int deleteJwxt(String openid);
}
