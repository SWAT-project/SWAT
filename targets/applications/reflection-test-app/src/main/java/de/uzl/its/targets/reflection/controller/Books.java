package de.uzl.its.targets.reflection.controller;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import de.uzl.its.targets.reflection.util.Number;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/books")
public class Books {

    @GetMapping("/libraryCounts")
    @ResponseBody
    List<String> invokeMethodTestCase(@RequestParam Integer type)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
                    InstantiationException, IllegalAccessException {

        Class<?> cls = Class.forName("de.uzl.its.targets.reflection.model.BookLibrary");
        Constructor<?> constructor = cls.getDeclaredConstructor();
        Object obj = constructor.newInstance();
        Method method =
                cls.getDeclaredMethod("getLibraryCount", Class.forName("java.lang.Integer"));
        @SuppressWarnings("unchecked")
        List<String> books = (List<String>) method.invoke(obj, type);
        return books;
    }

    @GetMapping("/dvds")
    @ResponseBody
    List<String> invokeMethodTestCase2(@RequestParam Integer type, @RequestParam Integer number)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
                    InstantiationException, IllegalAccessException {

        Class<?> cls = Class.forName("de.uzl.its.targets.reflection.model.DvdLibrary");
        Constructor<?> constructor = cls.getDeclaredConstructor(int.class);
        Object obj = constructor.newInstance(number);
        Method method =
                cls.getDeclaredMethod("getLibraryCount", Class.forName("java.lang.Integer"));
        @SuppressWarnings("unchecked")
        List<String> books = (List<String>) method.invoke(obj, type);
        return books;
    }

    @GetMapping("/random")
    @ResponseBody
    List<String> invokeMethodTestCase3(@RequestParam String name, @RequestParam Integer type,
                                       @RequestParam Integer number)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {

        Class<?> cls = Class.forName("de.uzl.its.targets.reflection.model.DvdLibrary");
        Constructor<?> constructor = cls.getDeclaredConstructor(int.class);
        Object obj = constructor.newInstance(number);

        Field f;

        try {
            f = cls.getDeclaredField("name");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        f.set(obj, name);

        Method method =
                cls.getDeclaredMethod("getLibraryCountConsideringName",
                        Class.forName("java.lang.Integer"));
        @SuppressWarnings("unchecked")
        List<String> books = (List<String>) method.invoke(obj, type);
        return books;
    }

    @GetMapping("/random2")
    @ResponseBody
    List<String> invokeMethodTestCase4(@RequestParam String name, @RequestParam String type,
                                       @RequestParam Integer number)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {

        Class<?> cls = Class.forName("de.uzl.its.targets.reflection.model.DvdLibrary");
        Constructor<?> constructor = cls.getDeclaredConstructor(int.class);
        Object obj = constructor.newInstance(number);

        Field f;

        try {
            f = cls.getDeclaredField("name");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        f.set(obj, name);

        Number n = new Number();
        n.setNumber(type);
        Field numberField;
        Class<?> numberCls = Class.forName("de.uzl.its.targets.reflection.util.Number");
        try {
            numberField = numberCls.getDeclaredField("number");
            numberField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        String getNumber = (String) numberField.get(n);
        Method method =
                cls.getDeclaredMethod("getLibraryCountConsideringName",
                        Class.forName("java.lang.String"));
        @SuppressWarnings("unchecked")
        List<String> books = (List<String>) method.invoke(obj, getNumber);
        return books;
    }

    @GetMapping("/staticgetset")
    @ResponseBody
    String invokeMethodTestCase5(@RequestParam int numberOfCustomers) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        Class<?> cls = Class.forName("de.uzl.its.targets.reflection.model.DvdLibrary");
        Constructor<?> constructor = cls.getDeclaredConstructor(int.class);
        Object obj = constructor.newInstance(42);

        Field f;

        try {
            f = cls.getDeclaredField("numberOfCustomers");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        f.set(obj, numberOfCustomers);

        int nCustomers = (int) f. get(obj);
        if (nCustomers > 10) {
            return "Yes";
        } else {
            return "No";
        }
    }

    @GetMapping("/staticmethod")
    @ResponseBody
    String invokeMethodTestCase6(@RequestParam int numberOfCustomers) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        Class<?> cls = Class.forName("de.uzl.its.targets.reflection.model.DvdLibrary");
        Constructor<?> constructor = cls.getDeclaredConstructor(int.class);
        Object obj = constructor.newInstance(42);

        Field f;

        try {
            f = cls.getDeclaredField("numberOfCustomers");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        f.set(obj, numberOfCustomers);

        Method method =
                cls.getDeclaredMethod("isNbOfCustomersLarger10");

        return (String) method.invoke(obj);
    }
}
