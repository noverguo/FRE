package com.nv.fre.utils;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nv.fre.MatchView;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

public class Utils {
	public static boolean matchParent(View view, String... classNames) {
		ViewParent parent = view.getParent();
		for(String className : classNames) {
			if(parent == null) {
				return false;
			}
			XposedBridge.log("matchParent: " + parent.getClass().getSimpleName() + " <--> " + className);
			if(!parent.getClass().getSimpleName().equals(className)) {
				return false;
			}
			parent = parent.getParent();
		}
		return true;
	}
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
//				XposedBridge.log(obj.getClass().getName() + " --> TextView: " + (obj instanceof TextView));
			}
			return obj instanceof TextView;
		} else if(clazz == LinearLayout.class) {
			if(debug) {
//				XposedBridge.log(obj.getClass().getName() + " --> LinearLayout: " + (obj instanceof LinearLayout));
			}
			return obj instanceof LinearLayout;
		} else if(clazz == RelativeLayout.class) {
			if(debug) {
//				XposedBridge.log(obj.getClass().getName() + " --> RelativeLayout: " + (obj instanceof RelativeLayout));
			}
			return obj instanceof RelativeLayout;
		} else if(clazz == ImageView.class) {
			if(debug) {
//				XposedBridge.log(obj.getClass().getName() + " --> ImageView: " + (obj instanceof ImageView));
			}
			return obj instanceof ImageView;
		} else if(clazz == View.class) {
			if(debug) {
//				XposedBridge.log(obj.getClass().getName() + " --> View: " + (obj instanceof View));
			}
			return obj instanceof View;
		}
		return obj.getClass() == clazz;
	}

	public static void printViewHierarchy(View parent) {
		while(parent.getParent() != null) {
			parent = (View) parent.getParent();
		}
		Utils.printViewAndSubView(parent);
	}
	
	public static void printViewAndSubView(View curView) {
		printViewAndSubView(curView, "  ");
	}

	public static void printViewAndSubView(View curView, String space) {
		if(curView == null) {
			return;
		}
		XposedBridge.log(space + curView.getClass().getName() + ": " + getPrintString(curView));
		if (curView instanceof ViewGroup) {
			space += "  ";
			ViewGroup group = (ViewGroup) curView;
			for (int i = 0; i < group.getChildCount(); ++i) {
				printViewAndSubView(group.getChildAt(i), space);
			}
		}
	}
	public static void printParent(View view) {
		List<String> parents = new ArrayList<>();
		while(view != null) {
			parents.add(view.getClass().getName() + ": " + getPrintString(view));
			view = (View) view.getParent();
		}
		String space = "";
		for(int i=parents.size()-1;i>=0;--i) {
			XposedBridge.log(space + parents.get(i));
			space += "  ";
		}
	}
	public static String getPrintString(View view) {
		if(view == null) {
			return null;
		}
		String res = view.toString();
		if (view instanceof TextView) {
			res += ": " + ((TextView) view).getText().toString();
		}
		res += " --> visiable: " + (view.getVisibility() == View.VISIBLE) + " enable: " + view.isEnabled() + " clickable: " + view.isClickable() + " active: " + view.isActivated();
		return res;
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
