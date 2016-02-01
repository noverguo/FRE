package com.nv.fre.mm;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by noverguo on 2016/1/29.
 */
public class HookClasses {
    public static final String KEY_MSG_MODEL_CLASS = "KEY_MSG_MODEL_CLASS";
    public static final String KEY_MSG_ADAPTER_CLASS = "KEY_MSG_ADAPTER_CLASS";
    public static final String KEY_RE_OPEN_BUTTON_FIELD = "KEY_RE_OPEN_BUTTON_FIELD";
    public static final String KEY_RE_INFO_TEXTVIEW_FIELD = "KEY_RE_INFO_TEXTVIEW_FIELD";
    public static final String KEY_RE_CLOSE_BUTTON_FIELD = "KEY_RE_CLOSE_BUTTON_FIELD";

    // 有默认值的
    public static final String KEY_MSG_ID_FIELD = "KEY_MSG_ID_FIELD";
    public static final String KEY_CONVERSATION_ADAPTER_CLASS = "KEY_CONVERSATION_ADAPTER_CLASS";
    public static final String KEY_CONVERSATION_ITEM_CLASS = "KEY_CONVERSATION_ITEM_CLASS";
    public static final String KEY_CONVERSATION_WINDOW_CLASS = "KEY_CONVERSATION_WINDOW_CLASS";
    public static final String KEY_LAUNCHER_UI_CLASS = "KEY_Launcher_UI_CLASS";
    public static final String KEY_CONVERSATION_LIST_VIEW_CLASS = "KEY_CONVERSATION_LIST_VIEW_CLASS";
    public static final String KEY_CHATTING_WINDOW_CLASS = "KEY_CHATTING_WINDOW_CLASS";
    public static final String KEY_CONVERSATION_TEXTVIEW_CLASS = "KEY_CONVERSATION_TEXTVIEW_CLASS";
    public static final String KEY_LUCKY_MONEY_DETAIL_UI_CLASS = "KEY_LUCKY_MONEY_DETAIL_UI_CLASS";
    public static final String KEY_LUCKY_MONEY_RECEIVE_UI_CLASS = "KEY_LUCKY_MONEY_RECEIVE_UI_CLASS";
    public static final String KEY_LUCKY_MONEY_RECEIVE_UI_RE_SET_ONCLICK_METHOD = "KEY_LUCKY_MONEY_RECEIVE_UI_RE_SET_ONCLICK_METHOD";
    public static final String KEY_LUCKY_MONEY_RECEIVE_UI_RE_SET_ONCLICK_METHOD_ARG3_CLASS = "KEY_LUCKY_MONEY_RECEIVE_UI_RE_SET_ONCLICK_METHOD_ARG3_CLASS";

    public static final String KEY_USERNAME_FIELD = "KEY_USERNAME_FIELD";

    private static Map<String, String> sDefaultValue;


    private static Map<String, String> sHookClassesMap;
    private static boolean sInit;
    public static void init(Map<String, String> map) {
        sDefaultValue = new HashMap<>();
        sDefaultValue.put(KEY_MSG_ID_FIELD, "field_msgId");
        sDefaultValue.put(KEY_CONVERSATION_ADAPTER_CLASS, "com.tencent.mm.ui.conversation.d");
        sDefaultValue.put(KEY_CONVERSATION_ITEM_CLASS, "com.tencent.mm.storage.r");
        sDefaultValue.put(KEY_CONVERSATION_WINDOW_CLASS, "com.tencent.mm.ui.conversation.e");
        sDefaultValue.put(KEY_LAUNCHER_UI_CLASS, "com.tencent.mm.ui.LauncherUI");
        sDefaultValue.put(KEY_CHATTING_WINDOW_CLASS, "com.tencent.mm.ui.chatting.ChattingUI$a");
        sDefaultValue.put(KEY_CONVERSATION_LIST_VIEW_CLASS, "com.tencent.mm.ui.conversation.ConversationOverscrollListView");
        sDefaultValue.put(KEY_CONVERSATION_TEXTVIEW_CLASS, "com.tencent.mm.ui.base.NoMeasuredTextView");
        sDefaultValue.put(KEY_LUCKY_MONEY_DETAIL_UI_CLASS, "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI");
        sDefaultValue.put(KEY_LUCKY_MONEY_RECEIVE_UI_CLASS, "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI");
        sDefaultValue.put(KEY_LUCKY_MONEY_RECEIVE_UI_RE_SET_ONCLICK_METHOD, "e");
        sDefaultValue.put(KEY_LUCKY_MONEY_RECEIVE_UI_RE_SET_ONCLICK_METHOD_ARG3_CLASS, "com.tencent.mm.r.j");

        sDefaultValue.put(KEY_USERNAME_FIELD, "field_username");

        sHookClassesMap = map;
        if(sHookClassesMap != null && !sHookClassesMap.isEmpty()) {
            sInit = true;
        } else {
            sInit = false;
        }
    }
    public static boolean isInit() {
        return sInit;
    }
    public static String getClassName(String key) {
        if(isInit()) {
            if(sHookClassesMap.containsKey(key)) {
                return sHookClassesMap.get(key);
            }
            if(sDefaultValue.containsKey(key)) {
                return sDefaultValue.get(key);
            }
        }
        return null;
    }
}
