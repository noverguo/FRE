package com.noverguo.fuckredenvelope;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XposedBridge;

public class ReflectUtil {
	
	private static Map<Class<?>, Set<Field>> fieldMap = new HashMap<Class<?>, Set<Field>>();
	
	public static void cacheFields(Class<?> clazz) {
		Field[] fields = clazz.getFields();
		Set<Field> fieldSet = new HashSet<Field>();
		if(fields != null) {
			fieldSet.addAll(Arrays.asList(fields));
		}
		fields = clazz.getDeclaredFields();
		if(fields != null) {
			fieldSet.addAll(Arrays.asList(fields));
		}
		for(Field field : fieldSet) {
			XposedBridge.log("cacheFields: " + clazz.getName() + "." + field.getName());
		}
		fieldMap.put(clazz, fieldSet);
	}
	
	public static String getFieldInfos(Object obj) throws Exception {
		Class<? extends Object> clazz = obj.getClass();
		if(!fieldMap.containsKey(clazz)) {
			return "";
		}
		Set<Field> fieldSet = fieldMap.get(clazz);
		StringBuilder buf = new StringBuilder();
		for(Field field : fieldSet) {
			buf.append(field.getName()).append(": ");
			field.setAccessible(true);
			Object res = field.get(obj);
			if(res != null) {
				buf.append(res.toString());
			}
			buf.append(", ");
		}
		return buf.toString();
	}
	
}
