package com.nv.fre;

public class TalkSel {
	public String talkName;
	public String showName;
	public boolean check;

	public TalkSel(String value) {
		if(value == null) {
			return;
		}
		String[] arr = value.split(":");
		if(arr.length > 1) {
			check = Boolean.valueOf(arr[1]);
		}
		String[] strings = arr[0].split(",");
		talkName = strings[0];
		if(strings.length > 1) {
			showName = strings[1];
		}
	}

	@Override
	public String toString() {
		return talkName + (showName == null ? "" : ("," + showName)) + ":" + check;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((showName == null) ? 0 : showName.hashCode());
		result = prime * result + ((talkName == null) ? 0 : talkName.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TalkSel other = (TalkSel) obj;
		if (talkName == null) {
			if (other.talkName != null)
				return false;
		} else if (!talkName.equals(other.talkName))
			return false;
		if (showName == null) {
			if (other.showName != null)
				return false;
		} else if (!showName.equals(other.showName))
			return false;
		return true;
	}
	
	
}