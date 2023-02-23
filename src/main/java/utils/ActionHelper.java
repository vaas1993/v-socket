package utils;

import exceptions.ProcessException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ActionHelper {

    /**
     * 将路由名称转为类名
     *
     * @param route String 路由
     * @return String
     */
    public static String route2name(String route) {
        StringBuilder actionName = new StringBuilder();
        String[] names = route.split("/");
        for (String name : names) {
            if (!name.equals("")) {
                // 首字母大写
                actionName.append(name.substring(0, 1).toUpperCase()).append(name.substring(1));
            }
        }
        actionName.append("Action");
        return actionName.toString();
    }

    /**
     * 将名字转成类实例
     *
     * @param name String
     * @return Object
     */
    public static Object name2instance(String name) throws ProcessException {
        Object instance;
        try {
            Class<?> actionClass = Class.forName(name);
            Constructor<?> action = actionClass.getConstructor();
            instance = action.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 InstantiationException | IllegalAccessException e) {
            throw new ProcessException("接口不存在", 404);
        }
        return instance;
    }
}
