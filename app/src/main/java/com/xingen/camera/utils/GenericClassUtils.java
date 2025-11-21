package com.xingen.camera.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * author：HeXinGen
 * date：on 2023/7/1
 * describe:
 * <p>
 * 获取范型类
 */
public class GenericClassUtils {
    /**
     * 获取抽象类中的泛型类
     *
     * @param mClass
     * @return
     */
    public static Class<?>[] queryGenericClass(Class<?> mClass) {
        try {
            // 获取泛型类
            Type intefaceType = mClass.getGenericSuperclass();
            if (intefaceType == null) {
                return null;
            }
            if (!(intefaceType instanceof ParameterizedType)) {
                return null;
            }
            //获取到实际的泛型中参数类型
            Type[] parameterizedType = ((ParameterizedType) intefaceType).getActualTypeArguments();
            Class<?>[] genericClassArray = new Class[parameterizedType.length];
            int i = 0;
            for (Type type1 : parameterizedType) {
                if (type1 instanceof Class) {
                    genericClassArray[i] = (Class<?>) type1;
                } else {
                    genericClassArray[i] = TypeUtils.getClass(type1);
                }
                i++;
            }
            return genericClassArray;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取接口中的泛型类
     *
     * @param mClass
     * @return
     */
    public static Class<?>[] queryGenericInterfaceClass(Class<?> mClass) {
        try {
            //获取泛型接口
            Type[] interfaceTypeArray = mClass.getGenericInterfaces();
            if (interfaceTypeArray == null && interfaceTypeArray.length == 0) {
                return null;
            }
            //获取实现的第一个接口类型
            Type type = interfaceTypeArray[0];
            if (!(type instanceof ParameterizedType)) {
                //这是Object类型
                return null;
            }
            //获取到实际的泛型中参数类型
            Type[] parameterizedType = ((ParameterizedType) type).getActualTypeArguments();
            Class<?>[] genericClassArray = new Class[parameterizedType.length];
            int i = 0;
            for (Type type1 : parameterizedType) {
                if (type1 instanceof Class) {
                    genericClassArray[i] = (Class<?>) type1;
                } else {
                    genericClassArray[i] = TypeUtils.getClass(type1);
                }
                i++;
            }
            return genericClassArray;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 用于Type和Class之间的转换
     */
    public static class TypeUtils {
        private static final String TYPE_NAME_PREFIX = "class ";

        public static String getClassName(Type type) {
            if (type == null) {
                return "";
            }
            String className = type.toString();
            if (className.startsWith(TYPE_NAME_PREFIX)) {
                className = className.substring(TYPE_NAME_PREFIX.length());
            }
            return className;
        }

        public static Class<?> getClass(Type type)
                throws ClassNotFoundException {
            String className = getClassName(type);
            if (className == null || className.isEmpty()) {
                return null;
            }
            return Class.forName(className);
        }
    }
}
