package com.p6spy.spring.boot.ext;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.FormattedLogger;
import com.p6spy.engine.spy.appender.P6Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
  
public class Slf4jLogger extends FormattedLogger implements P6Logger{
	
    private static final Logger logger = LoggerFactory.getLogger("p6spy");
  
    public String getLastEntry() {
        return lastEntry;
    }
  
    public void setLastEntry(String lastEntry) {
        this.lastEntry = lastEntry;
    }
  
    protected String lastEntry;
  
    @Override
    public void logSQL(int connectionId, String s, long l, Category category, String s1,String sql, String url) {
        if (!"resultset".equals(category.getName())) {
            logger.info(trim(sql));
        }
    }
  
    @Override
    public void logException(Exception e) {
        logger.error(e.getMessage(),e);
    }
  
    @Override public void logText(String s) {
        logger.info(s);
        this.setLastEntry(s);
    }
  
    @Override public boolean isCategoryEnabled(Category category) {
        return true;
    }
  
    private String trim(String sql){
        StringBuilder sb = new StringBuilder("\r\n");
        sb.append(sql.replaceAll("\n|\r|\t|'  '"," "));
        return sb.toString();
    }
  
} 