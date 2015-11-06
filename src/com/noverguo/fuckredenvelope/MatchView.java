package com.noverguo.fuckredenvelope;
public class MatchView {
		int idx;
		Class<?>[] viewClasses;

		public MatchView(int idx, Class<?>[] viewClasses) {
			this.idx = idx;
			this.viewClasses = viewClasses;
		}
	}