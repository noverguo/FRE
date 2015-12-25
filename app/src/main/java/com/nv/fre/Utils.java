package com.nv.fre;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import de.robv.android.xposed.XposedBridge;

public class Utils {
	public static View getChild(MatchView[] matchViews, View view) {
		if(matchViews == null || matchViews.length == 0) {
			return null;
		}
		View curView = view;
		ViewGroup curGroup = null;
		for(int i=0;i<matchViews.length;++i) {
			if(matchViews[i].viewClasses == null || matchViews[i].viewClasses.length == 0) {
				return null;
			}
			if(!(curView instanceof ViewGroup)) {
				return null;
			}
			curGroup = (ViewGroup) curView;
			if(curGroup.getChildCount() != matchViews[i].viewClasses.length) {
				return null;
			}
			for(int j=0;j<matchViews[i].viewClasses.length;++j) {
				if(!instanceOf(curGroup.getChildAt(j), matchViews[i].viewClasses[j])) {
					return null;
				}
			}
			curView = curGroup.getChildAt(matchViews[i].idx);
		}
		
		return curView;
	}
	
	public static boolean instanceOf(Object obj, Class<?> clazz) {
		return instanceOf(obj, clazz, false);
	}
	public static boolean instanceOf(Object obj, Class<?> clazz, boolean debug) {
		if(obj == null) {
			return false;
		}
		if(clazz == TextView.class) {
			if(debug) {
				XposedBridge.log(obj.getClass().getName() + " --> TextView: " + (obj instanceof TextView));
			}
			return obj instanceof TextView;
		} else if(clazz == LinearLayout.class) {
			if(debug) {
				XposedBridge.log(obj.getClass().getName() + " --> LinearLayout: " + (obj instanceof LinearLayout));
			}
			return obj instanceof LinearLayout;
		} else if(clazz == RelativeLayout.class) {
			if(debug) {
				XposedBridge.log(obj.getClass().getName() + " --> RelativeLayout: " + (obj instanceof RelativeLayout));
			}
			return obj instanceof RelativeLayout;
		} else if(clazz == ImageView.class) {
			if(debug) {
				XposedBridge.log(obj.getClass().getName() + " --> ImageView: " + (obj instanceof ImageView));
			}
			return obj instanceof ImageView;
		} else if(clazz == View.class) {
			if(debug) {
				XposedBridge.log(obj.getClass().getName() + " --> View: " + (obj instanceof View));
			}
			return obj instanceof View;
		}
		return obj.getClass() == clazz;
	}
	
	public static void printViewAndSubView(View curView) {
		printViewAndSubView(curView, "  ");
	}

	public static void printViewAndSubView(View curView, String space) {
		if(curView == null) {
			return;
		}
		if (curView instanceof TextView) {
			String info = ": " + ((TextView) curView).getText().toString();
			XposedBridge.log(space + curView.getClass().getName() + info);
			return;
		}
		XposedBridge.log(space + curView.getClass().getName());
		if (curView instanceof ViewGroup) {
			space += "  ";
			ViewGroup group = (ViewGroup) curView;
			for (int i = 0; i < group.getChildCount(); ++i) {
				printViewAndSubView(group.getChildAt(i), space);
			}
		}
	}
	public static String getPrintInfos(Object[] objs) {
		StringBuilder buf = new StringBuilder();
		if(objs != null) {
			for(Object obj : objs) {
				if(obj != null) {
					buf.append(obj.getClass() + ": ");
				}
				buf.append(obj).append(", ");
			}
		}
		return buf.toString();
	}
}
