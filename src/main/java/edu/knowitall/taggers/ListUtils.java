package edu.knowitall.taggers;

import java.util.ArrayList;

public class ListUtils {
    public static <E> void removeNulls(ArrayList<E> list) {
        int pos = 0;
        for (int i = 0; i < list.size(); i++) {
            E current = list.get(i);
            if (current != null) {
                if (i != pos) {
                    list.set(pos, current);
                }

                pos++;
            }
        }

        for (int i = list.size() - 1; i >= pos; i--) {
            list.remove(i);
        }
    }

    public static <E> E swapRemove(ArrayList<E> list, int index) {
    	if (list.size() > 1) {
	    	list.set(index, list.get(list.size() - 1));
    	}
    	return list.remove(list.size() - 1);
    }
}
