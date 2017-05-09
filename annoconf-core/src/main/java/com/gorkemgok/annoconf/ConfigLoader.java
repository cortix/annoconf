package com.gorkemgok.annoconf;

import com.gorkemgok.annoconf.annotation.ConfigBean;
import com.gorkemgok.annoconf.annotation.ConfigParam;
import com.gorkemgok.annoconf.source.ConfigSource;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Created by gorkem on 04.04.2017.
 */
public class ConfigLoader {

    private final ConfigOptions configOptions;

    private Map<String, Object> configMap;

    public ConfigLoader(ConfigOptions configOptions) {
        this.configOptions = configOptions;
    }

    public Config load() {
        Map<Class, Object> configPojoSet = new HashMap<>();
        for (Class clazz : findConfigClasses()) {
            System.out.println("ConfigBean found : "+clazz.getName());
            try {
                Object confPojo = instantiate(clazz.getConstructors()[0]);
                setFields(confPojo);
                configPojoSet.put(clazz, confPojo);
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
        return new Config(configPojoSet, configMap);
    }

    private Object setFields(Object object) {
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields){
            ConfigParam configParam = field.getAnnotation(ConfigParam.class);
            if (configParam != null){
                if (field.getType().equals(String.class)){
                    String value = getStringFromSourceList(configParam);
                    field.setAccessible(true);
                    try {field.set(object, value);} catch (IllegalAccessException e) {e.printStackTrace();}
                    field.setAccessible(false);
                    configMap.put(configParam.key(), value);
                    configMap.put(configParam.env(), value);
                }else if (field.getType().getName().equals("int")){
                    Integer value = getIntegerFromSourceList(configParam);
                    field.setAccessible(true);
                    try {field.set(object, value);} catch (IllegalAccessException e) {e.printStackTrace();}
                    field.setAccessible(false);
                    configMap.put(configParam.key(), value);
                    configMap.put(configParam.env(), value);
                }
            }
        }
        return object;
    }

    private Object instantiate(Constructor constructor) throws InstantiationException {
        configMap = new HashMap<>();
        List<Object> newParameters = new ArrayList<>();
        Parameter[] constructorParameters = constructor.getParameters();
        for (Parameter constructorParam : constructorParameters) {
            ConfigParam configParam = constructorParam.getAnnotation(ConfigParam.class);
            if (configParam != null) {
                if (constructorParam.getType().equals(String.class)) {
                    String value = getStringFromSourceList(configParam);
                    newParameters.add(value);
                    configMap.put(configParam.key(), value);
                    configMap.put(configParam.env(), value);
                } else if (constructorParam.getType().getName().equals("int")) {
                    Integer value = getIntegerFromSourceList(configParam);
                    newParameters.add(value);
                    configMap.put(configParam.key(), value);
                    configMap.put(configParam.env(), value);
                }
            }else{
                throw new InstantiationException("All constructor parameters must have ConfigParam annotation");
            }
        }

        try {
            Object confPojo = constructor.newInstance(newParameters.toArray());
            return confPojo;
        } catch (IllegalAccessException e) {
            System.out.println(e);
            throw new InstantiationException();
        } catch (InvocationTargetException e) {
            System.out.println(e);
            throw new InstantiationException();
        }
    }

    private String getStringFromSourceList(ConfigParam configParam){
        for (ConfigSource configSource : configOptions.getSourceList()){
            if (configSource.hasValue(configParam)){
                return configSource.getString(configParam);
            }
        }
        return configParam.defaultValue();
    }

    private int getIntegerFromSourceList(ConfigParam configParam){
        for (ConfigSource configSource : configOptions.getSourceList()){
            if (configSource.hasValue(configParam)){
                return configSource.getInt(configParam);
            }
        }
        return Integer.valueOf(configParam.defaultValue());
    }

    public static Set<Class<?>> findConfigClasses(){
        Reflections reflections = new Reflections();

        System.out.println("Scanning configurations...");
        Set<Class<?>> clazzSet = reflections.getTypesAnnotatedWith(ConfigBean.class);
        return clazzSet;
    }
}