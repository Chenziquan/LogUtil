/*
 * ===========================================================================================
 *    COPYRIGHT
 *             PAX Computer Technology(Shenzhen) CO., LTD PROPRIETARY INFORMATION
 *      This software is supplied under the terms of a license agreement or nondisclosure
 *      agreement with PAX Computer Technology(Shenzhen) CO., LTD and may not be copied or
 *      disclosed except in accordance with the terms in that agreement.
 *        Copyright (C) 2018 - ? PAX Computer Technology(Shenzhen) CO., LTD All rights reserved.
 *    Description: // Detail description about the function of this module,
 *                // interfaces with the other modules, and dependencies.
 *    Revision History:
 *    Date	                 Author	                Action
 *    2019-05-05   	         JQChen           	Create/Add/Modify/Delete
 *   ===========================================================================================
 */

package com.jqchen.log;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * @author JQChen.
 * @date on 2019/5/5.
 */
public class LogUtil {
    private static final int V = Log.VERBOSE;
    private static final int D = Log.DEBUG;
    private static final int I = Log.INFO;
    private static final int W = Log.WARN;
    private static final int E = Log.ERROR;
    private static final int A = Log.ASSERT;
    private static final String NULL = "null";
    private static final String ARGS = "args";
    private static final String LINE_SEP = System.getProperty("line.separator");
    private static final String TOP_CORNER = "┌";
    private static final String MIDDLE_CORNER = "├";
    private static final String LEFT_BORDER = "│ ";
    private static final String BOTTOM_CORNER = "└";
    private static final String SIDE_DIVIDER =
            "────────────────────────────────────────────────────────";
    private static final String MIDDLE_DIVIDER =
            "┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄";
    private static final String TOP_BORDER = TOP_CORNER + SIDE_DIVIDER + SIDE_DIVIDER;
    private static final String MIDDLE_BORDER = MIDDLE_CORNER + MIDDLE_DIVIDER + MIDDLE_DIVIDER;
    private static final String BOTTOM_BORDER = BOTTOM_CORNER + SIDE_DIVIDER + SIDE_DIVIDER;
    private static final int MAX_LEN = 3000;
    private static final String NOTHING = "log nothing";
    private static final SimpleArrayMap<Class, BaseFormatter> I_FORMATTER_MAP = new SimpleArrayMap<>();
    private static final LogUtil.Config CONFIG = new LogUtil.Config();

    public LogUtil() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static Config getConfig() {
        return CONFIG;
    }

    abstract static class BaseFormatter<T> {
        abstract String format(T t);
    }

    public static void v(final Object... contents) {
        log(V, CONFIG.getGlobalTag(), contents);
    }

    public static void vTag(final String tag, final Object... contents) {
        log(V, tag, contents);
    }

    public static void d(final Object... contents) {
        log(D, CONFIG.getGlobalTag(), contents);
    }

    public static void dTag(final String tag, final Object... contents) {
        log(D, tag, contents);
    }

    public static void i(final Object... contents) {
        log(I, CONFIG.getGlobalTag(), contents);
    }

    public static void iTag(final String tag, final Object... contents) {
        log(I, tag, contents);
    }

    public static void w(final Object... contents) {
        log(W, CONFIG.getGlobalTag(), contents);
    }

    public static void wTag(final String tag, final Object... contents) {
        log(W, tag, contents);
    }

    public static void e(final Object... contents) {
        log(E, CONFIG.getGlobalTag(), contents);
    }

    public static void eTag(final String tag, final Object... contents) {
        log(E, tag, contents);
    }

    public static void a(final Object... contents) {
        log(A, CONFIG.getGlobalTag(), contents);
    }

    public static void aTag(final String tag, final Object... contents) {
        log(A, tag, contents);
    }

    private static void log(final int type, final String tag, final Object... contents) {
        if (!CONFIG.isLogSwitch()) {
            return;
        }
        int typeLow = type & 0x0f, typeHigh = type & 0xf0;
        final TagHead tagHead = processTagAndHead(tag);
        final String body = processBody(typeHigh, contents);
        print2Console(typeLow, tagHead.tag, tagHead.consoleHead, body);
    }

    private static void print2Console(final int type,
                                      final String tag,
                                      final String[] head,
                                      final String msg) {
        printBorder(type, tag, true);
        printHead(type, tag, head);
        printMsg(type, tag, msg);
        printBorder(type, tag, false);
    }

    private static void printBorder(final int type, final String tag, boolean isTop) {
        Log.println(type, tag, isTop ? TOP_BORDER : BOTTOM_BORDER);
    }

    private static void printHead(final int type, final String tag, final String[] head) {
        if (head != null) {
            for (String aHead : head) {
                Log.println(type, tag, LEFT_BORDER + aHead);
            }
            Log.println(type, tag, MIDDLE_BORDER);
        }
    }

    private static void printMsg(final int type, final String tag, final String msg) {
        int len = msg.length();
        int countOfSub = len / MAX_LEN;
        if (countOfSub > 0) {
            int index = 0;
            for (int i = 0; i < countOfSub; i++) {
                printSubMsg(type, tag, msg.substring(index, index + MAX_LEN));
                index += MAX_LEN;
            }
            if (index != len) {
                printSubMsg(type, tag, msg.substring(index, len));
            }
        } else {
            printSubMsg(type, tag, msg);
        }
    }

    private static void printSubMsg(final int type, final String tag, final String msg) {
        StringBuilder sb = new StringBuilder();
        String[] lines = msg.split(LINE_SEP);
        for (String line : lines) {
            Log.println(type, tag, LEFT_BORDER + line);
        }
    }

    private static String processBody(final int type, final Object... contents) {
        String body = NULL;
        if (contents != null) {
            if (contents.length == 1) {
                body = formatObject(type, contents[0]);
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0, len = contents.length; i < len; ++i) {
                    Object content = contents[i];
                    sb.append(ARGS)
                            .append("[")
                            .append(i)
                            .append("]")
                            .append(" = ")
                            .append(formatObject(content))
                            .append(LINE_SEP);
                }
                body = sb.toString();
            }
        }
        return body.length() == 0 ? NOTHING : body;
    }

    private static String formatObject(int type, Object object) {
        if (object == null) {
            return NULL;
        }
        return formatObject(object);
    }

    private static String formatObject(Object object) {
        if (object == null) {
            return NULL;
        }
        if (!I_FORMATTER_MAP.isEmpty()) {
            BaseFormatter iFormatter = I_FORMATTER_MAP.get(getClassFromObject(object));
            if (iFormatter != null) {
                //noinspection unchecked
                return iFormatter.format(object);
            }
        }
        return LogFormatter.object2String(object);
    }

    private static Class getClassFromObject(final Object obj) {
        Class objClass = obj.getClass();
        if (objClass.isAnonymousClass() || objClass.isSynthetic()) {
            Type[] genericInterfaces = objClass.getGenericInterfaces();
            String className;
            if (genericInterfaces.length == 1) {// interface
                Type type = genericInterfaces[0];
                while (type instanceof ParameterizedType) {
                    type = ((ParameterizedType) type).getRawType();
                }
                className = type.toString();
            } else {// abstract class or lambda
                Type type = objClass.getGenericSuperclass();
                while (type instanceof ParameterizedType) {
                    type = ((ParameterizedType) type).getRawType();
                }
                assert type != null;
                className = type.toString();
            }

            if (className.startsWith("class ")) {
                className = className.substring(6);
            } else if (className.startsWith("interface ")) {
                className = className.substring(10);
            }
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return objClass;
    }

    private static TagHead processTagAndHead(String tag) {
        if (!CONFIG.mTagIsSpace) {
            tag = CONFIG.getGlobalTag();
        } else {
            final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
            final int stackIndex = 3;
            if (stackIndex >= stackTrace.length) {
                StackTraceElement targetElement = stackTrace[3];
                final String fileName = getFileName(targetElement);
                if (CONFIG.mTagIsSpace && isSpace(tag)) {
                    int index = fileName.indexOf('.');// Use proguard may not find '.'.
                    tag = index == -1 ? fileName : fileName.substring(0, index);
                }
                return new TagHead(tag, null, ": ");
            }
            StackTraceElement targetElement = stackTrace[stackIndex];
            final String fileName = getFileName(targetElement);
            if (CONFIG.mTagIsSpace && isSpace(tag)) {
                int index = fileName.indexOf('.');// Use proguard may not find '.'.
                tag = index == -1 ? fileName : fileName.substring(0, index);
            }

            String tName = Thread.currentThread().getName();
            final String head = new Formatter()
                    .format("%s, %s.%s(%s:%d)",
                            tName,
                            targetElement.getClassName(),
                            targetElement.getMethodName(),
                            fileName,
                            targetElement.getLineNumber())
                    .toString();
            final String fileHead = " [" + head + "]: ";
            return new TagHead(tag, new String[]{head}, fileHead);

        }
        return new TagHead(tag, null, ": ");
    }

    private static String getFileName(final StackTraceElement targetElement) {
        String fileName = targetElement.getFileName();
        if (fileName != null) {
            return fileName;
        }
        // If name of file is null, should add
        // "-keepattributes SourceFile,LineNumberTable" in proguard file.
        String className = targetElement.getClassName();
        String[] classNameInfo = className.split("\\.");
        if (classNameInfo.length > 0) {
            className = classNameInfo[classNameInfo.length - 1];
        }
        int index = className.indexOf('$');
        if (index != -1) {
            className = className.substring(0, index);
        }
        return className + ".java";
    }

    private static boolean isSpace(final String s) {
        if (s == null) {
            return true;
        }
        for (int i = 0, len = s.length(); i < len; ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static class TagHead {
        String tag;
        String[] consoleHead;
        String fileHead;

        TagHead(String tag, String[] consoleHead, String fileHead) {
            this.tag = tag;
            this.consoleHead = consoleHead;
            this.fileHead = fileHead;
        }
    }

    private static class LogFormatter {

        static String object2String(Object object) {
            if (object.getClass().isArray()) {
                return array2String(object);
            }
            if (object instanceof Throwable) {
                return throwable2String((Throwable) object);
            }
            if (object instanceof Bundle) {
                return bundle2String((Bundle) object);
            }
            if (object instanceof Intent) {
                return intent2String((Intent) object);
            }
            return object.toString();
        }

        static String object2Json(Object object) {
            if (object instanceof CharSequence) {
                return formatJson(object.toString());
            }
            try {
                return "";//GSON.toJson(object);
            } catch (Throwable t) {
                return object.toString();
            }
        }

        static String formatXml(String xml) {
            try {
                Source xmlInput = new StreamSource(new StringReader(xml));
                StreamResult xmlOutput = new StreamResult(new StringWriter());
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transformer.transform(xmlInput, xmlOutput);
                xml = xmlOutput.getWriter().toString().replaceFirst(">", ">" + LINE_SEP);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return xml;
        }

        private static String throwable2String(final Throwable e) {
            return "";//ThrowableUtils.getFullStackTrace(e);
        }

        private static String bundle2String(Bundle bundle) {
            Iterator<String> iterator = bundle.keySet().iterator();
            if (!iterator.hasNext()) {
                return "Bundle {}";
            }
            StringBuilder sb = new StringBuilder(128);
            sb.append("Bundle { ");
            for (; ; ) {
                String key = iterator.next();
                Object value = bundle.get(key);
                sb.append(key).append('=');
                if (value instanceof Bundle) {
                    sb.append(value == bundle ? "(this Bundle)" : bundle2String((Bundle) value));
                } else {
                    sb.append(formatObject(value));
                }
                if (!iterator.hasNext()) {
                    return sb.append(" }").toString();
                }
                sb.append(',').append(' ');
            }
        }

        private static String intent2String(Intent intent) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Intent { ");
            boolean first = true;
            String mAction = intent.getAction();
            if (mAction != null) {
                sb.append("act=").append(mAction);
                first = false;
            }
            Set<String> mCategories = intent.getCategories();
            if (mCategories != null) {
                if (!first) {
                    sb.append(' ');
                }
                first = false;
                sb.append("cat=[");
                boolean firstCategory = true;
                for (String c : mCategories) {
                    if (!firstCategory) {
                        sb.append(',');
                    }
                    sb.append(c);
                    firstCategory = false;
                }
                sb.append("]");
            }
            Uri mData = intent.getData();
            if (mData != null) {
                if (!first) {
                    sb.append(' ');
                }
                first = false;
                sb.append("dat=").append(mData);
            }
            String mType = intent.getType();
            if (mType != null) {
                if (!first) {
                    sb.append(' ');
                }
                first = false;
                sb.append("typ=").append(mType);
            }
            int mFlags = intent.getFlags();
            if (mFlags != 0) {
                if (!first) {
                    sb.append(' ');
                }
                first = false;
                sb.append("flg=0x").append(Integer.toHexString(mFlags));
            }
            String mPackage = intent.getPackage();
            if (mPackage != null) {
                if (!first) {
                    sb.append(' ');
                }
                first = false;
                sb.append("pkg=").append(mPackage);
            }
            ComponentName mComponent = intent.getComponent();
            if (mComponent != null) {
                if (!first) {
                    sb.append(' ');
                }
                first = false;
                sb.append("cmp=").append(mComponent.flattenToShortString());
            }
            Rect mSourceBounds = intent.getSourceBounds();
            if (mSourceBounds != null) {
                if (!first) {
                    sb.append(' ');
                }
                first = false;
                sb.append("bnds=").append(mSourceBounds.toShortString());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ClipData mClipData = intent.getClipData();
                if (mClipData != null) {
                    if (!first) {
                        sb.append(' ');
                    }
                    first = false;
                    clipData2String(mClipData, sb);
                }
            }
            Bundle mExtras = intent.getExtras();
            if (mExtras != null) {
                if (!first) {
                    sb.append(' ');
                }
                first = false;
                sb.append("extras={");
                sb.append(bundle2String(mExtras));
                sb.append('}');
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                Intent mSelector = intent.getSelector();
                if (mSelector != null) {
                    if (!first) {
                        sb.append(' ');
                    }
                    first = false;
                    sb.append("sel={");
                    sb.append(mSelector == intent ? "(this Intent)" : intent2String(mSelector));
                    sb.append("}");
                }
            }
            sb.append(" }");
            return sb.toString();
        }

        private static String formatJson(String json) {
            try {
                for (int i = 0, len = json.length(); i < len; i++) {
                    char c = json.charAt(i);
                    if (c == '{') {
                        return new JSONObject(json).toString(2);
                    } else if (c == '[') {
                        return new JSONArray(json).toString(2);
                    } else if (!Character.isWhitespace(c)) {
                        return json;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        private static void clipData2String(ClipData clipData, StringBuilder sb) {
            ClipData.Item item = clipData.getItemAt(0);
            if (item == null) {
                sb.append("ClipData.Item {}");
                return;
            }
            sb.append("ClipData.Item { ");
            String mHtmlText = item.getHtmlText();
            if (mHtmlText != null) {
                sb.append("H:");
                sb.append(mHtmlText);
                sb.append("}");
                return;
            }
            CharSequence mText = item.getText();
            if (mText != null) {
                sb.append("T:");
                sb.append(mText);
                sb.append("}");
                return;
            }
            Uri uri = item.getUri();
            if (uri != null) {
                sb.append("U:").append(uri);
                sb.append("}");
                return;
            }
            Intent intent = item.getIntent();
            if (intent != null) {
                sb.append("I:");
                sb.append(intent2String(intent));
                sb.append("}");
                return;
            }
            sb.append("NULL");
            sb.append("}");
        }

        private static String array2String(Object object) {
            if (object instanceof Object[]) {
                return Arrays.deepToString((Object[]) object);
            } else if (object instanceof boolean[]) {
                return Arrays.toString((boolean[]) object);
            } else if (object instanceof byte[]) {
                return Arrays.toString((byte[]) object);
            } else if (object instanceof char[]) {
                return Arrays.toString((char[]) object);
            } else if (object instanceof double[]) {
                return Arrays.toString((double[]) object);
            } else if (object instanceof float[]) {
                return Arrays.toString((float[]) object);
            } else if (object instanceof int[]) {
                return Arrays.toString((int[]) object);
            } else if (object instanceof long[]) {
                return Arrays.toString((long[]) object);
            } else if (object instanceof short[]) {
                return Arrays.toString((short[]) object);
            }
            throw new IllegalArgumentException("Array has incompatible type: " + object.getClass());
        }
    }

    public static class Config {
        private boolean mLogSwitch = true;
        private String mGlobalTag = "";
        private boolean mTagIsSpace = true;

        private Config() {
        }

        public boolean isLogSwitch() {
            return mLogSwitch;
        }

        public Config setLogSwitch(boolean logSwitch) {
            this.mLogSwitch = logSwitch;
            return this;
        }

        private String getGlobalTag() {
            return mGlobalTag;
        }

        public Config setGlobalTag(String tag) {
            if (isSpace(tag)) {
                mGlobalTag = "";
                mTagIsSpace = true;
            } else {
                mGlobalTag = tag;
                mTagIsSpace = false;
            }
            return this;
        }
    }
}
