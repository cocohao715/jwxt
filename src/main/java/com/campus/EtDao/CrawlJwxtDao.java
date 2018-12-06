package com.campus.EtDao;

import com.campus.model.ClassWeek;
import com.campus.model.CurrentTime;
import com.campus.model.PersonInfo;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

public interface CrawlJwxtDao {
    public HashMap login(String account,String password);
    public List getEmptyClassroom(HashMap headers, String emptyDate, String emptyTime)throws ParseException;
    public List getClassSchedule(HashMap headers, String account, String termRange, String weekNumber);
    public ClassWeek getClassWeek(HashMap headers, String currDate) throws ParseException;
    public PersonInfo getPersonInfo(HashMap headers, String account);
    public List getScoreData(HashMap headers, String account);
    public List getTermRange(HashMap headers);
    public CurrentTime getCurrenTime(HashMap headers);
    }
