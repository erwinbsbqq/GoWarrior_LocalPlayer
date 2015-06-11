package com.gowarrior.myplayer.common;

/**
 * Created by jerry.xiong on 2015/6/11.
 */
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogTag {

    /**
     * Output Format: [MethodName:LineNumber]
     * @return
     */
    public static String getMethodLine() {
        StackTraceElement traceElement = ((new Exception()).getStackTrace())[1];
        StringBuffer toStringBuffer = new StringBuffer("[")
                .append(traceElement.getMethodName()).append(":")
                .append(traceElement.getLineNumber()).append("]");
        return toStringBuffer.toString();
    }

    /**
     * Output Format: [FileName:LineNumber:MethodName]
     *
     * @return
     */
    public static String getFileMethodLine() {
        StackTraceElement traceElement = ((new Exception()).getStackTrace())[1];
        StringBuffer toStringBuffer = new StringBuffer("[")
                .append(traceElement.getFileName()).append(":")
                .append(traceElement.getMethodName()).append(":")
                .append(traceElement.getLineNumber()).append("]");
        return toStringBuffer.toString();
    }

    public static String _FILE_() {
        StackTraceElement traceElement = ((new Exception()).getStackTrace())[1];
        return traceElement.getFileName();
    }

    public static String _FUNC_() {
        StackTraceElement traceElement = ((new Exception()).getStackTrace())[1];
        return traceElement.getMethodName();
    }

    public static int _LINE_() {
        StackTraceElement traceElement = ((new Exception()).getStackTrace())[1];
        return traceElement.getLineNumber();
    }

    public static String _TIME_() {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return sdf.format(now);
    }

}

