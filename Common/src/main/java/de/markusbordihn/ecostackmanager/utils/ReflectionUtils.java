/*
 * Copyright 2024 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.ecostackmanager.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionUtils {

  private ReflectionUtils() {}

  public static boolean changeIntValueField(Object object, String[] fieldNames, int value) {
    for (String fieldName : fieldNames) {
      if (changeIntValueField(object, fieldName, value)) {
        return true;
      }
    }
    return false;
  }

  public static boolean changeIntValueField(Object object, String fieldName, int value) {
    try {
      Field field = object.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.setInt(object, value);
      return true;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      return false;
    }
  }

  public static boolean invokeIntMethod(Object object, String[] methodNames, int value) {
    for (String methodName : methodNames) {
      if (invokeIntMethod(object, methodName, value)) {
        return true;
      }
    }
    return false;
  }

  public static boolean invokeIntMethod(Object object, String methodName, int value) {
    try {
      Method method = object.getClass().getDeclaredMethod(methodName, int.class);
      method.setAccessible(true);
      method.invoke(object, value);
      return true;
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      return false;
    }
  }
}
