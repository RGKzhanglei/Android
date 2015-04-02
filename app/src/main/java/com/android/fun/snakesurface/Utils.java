package com.android.fun.snakesurface;

import java.util.EnumSet;
import java.util.Iterator;

/**
 * Created by zhang.la on 2015/3/30.
 */
public class Utils {

    /**
     * 根据枚举value 获取具体的枚举对象
     * @param value
     * @param enumType
     * @return
     */
    public static  IEnum getEnumByValue(int value,Class<?> enumType ){
        if(!enumType.isEnum()){
            return null;
        }
        try {
            EnumSet enumSet = EnumSet.allOf(enumType.asSubclass(Enum.class));
            Iterator iterator = enumSet.iterator();
            IEnum temp = null;
            while (iterator.hasNext())
            {
                temp = (IEnum)iterator.next();
                if(temp.getValue() == value) return temp;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
