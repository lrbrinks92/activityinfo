package org.activityinfo.test.pageobject.api;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.Locatable;
import org.openqa.selenium.internal.WrapsElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.FieldDecorator;
import org.openqa.selenium.support.pagefactory.internal.LocatingElementHandler;
import org.openqa.selenium.support.pagefactory.internal.LocatingElementListHandler;

import java.lang.reflect.*;
import java.util.List;

public class MyFieldDecorator implements FieldDecorator {

    protected ElementLocatorFactory factory;

    public MyFieldDecorator(ElementLocatorFactory factory) {
        this.factory = factory;
    }

    public Object decorate(ClassLoader loader, Field field) {

        if(field.getAnnotation(FindBy.class) == null) {
            return null;
        }

        ElementLocator locator = factory.createLocator(field);
        Preconditions.checkState(locator != null);

        if (List.class.isAssignableFrom(field.getType())) {

            List<WebElement> elements = proxyForListLocator(loader, locator);
            Class itemClass = listItemClass(field);

            System.out.println("item class = " + itemClass.getName());

            if(WebElement.class.isAssignableFrom(itemClass)) {
                return elements;
            } else {
                return Lists.transform(elements, itemConstructor(itemClass));
            }

        } else {
            WebElement element = proxyForLocator(loader, locator);

            if(WebElement.class.isAssignableFrom(field.getType())) {
                return element;
            } else {
                return itemConstructor(field.getType()).apply(element);
            }
        }
    }


    private Function<WebElement, Object> itemConstructor(final Class itemClass) {
        final Constructor constructor;
        try {
            constructor = itemClass.getConstructor(WebElement.class);
        } catch(NoSuchMethodException e) {
            throw new RuntimeException(itemClass.getName() + " needs to have a constructor that" +
                    " accepts a single parameter of type WebElement");
        }

        return new Function<WebElement, Object>() {
            @Override
            public Object apply(WebElement webElement) {
                try {
                    return constructor.newInstance(webElement);
                } catch (Exception e) {
                    return new RuntimeException("Exception creating new instance of " + itemClass.getName());
                }
            }
        };
    }

    private Class listItemClass(Field field) {

        // Type erasure in Java isn't complete. Attempt to discover the generic
        // type of the list.
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            throw new UnsupportedOperationException();
        }

        Type listType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        return (Class)listType;
    }

    protected WebElement proxyForLocator(ClassLoader loader, ElementLocator locator) {
        InvocationHandler handler = new LocatingElementHandler(locator);

        WebElement proxy;
        proxy = (WebElement) Proxy.newProxyInstance(
                loader, new Class[]{WebElement.class, WrapsElement.class, Locatable.class}, handler);
        return proxy;
    }

    @SuppressWarnings("unchecked")
    protected List<WebElement> proxyForListLocator(ClassLoader loader, ElementLocator locator) {
        InvocationHandler handler = new LocatingElementListHandler(locator);

        List<WebElement> proxy;
        proxy = (List<WebElement>) Proxy.newProxyInstance(
                loader, new Class[] {List.class}, handler);
        return proxy;
    }
}
