package org.activityinfo.test.pageobject.api;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class FluentElements implements Iterable<FluentElement> {

    private FluentIterable<FluentElement> list;

    public FluentElements(List<FluentElement> list) {
        this.list = FluentIterable.from(list);
    }


    @Override
    public Iterator<FluentElement> iterator() {
        return list.iterator();
    }

    public FluentElements topToBottom() {
        List<FluentElement> ordered = Lists.newArrayList(list);
        Collections.sort(ordered, Ordering.natural().onResultOf(new Function<FluentElement, Integer>() {
            @Override
            public Integer apply(FluentElement input) {
                return input.location().getY();
            }
        }));
        return new FluentElements(ordered);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public int size() {
        return list.size();
    }

    public List<FluentElement> list() {
        return Lists.newArrayList(list);
    }

    public FluentElement get(int index) {
        return list.get(index);
    }

    public Optional<FluentElement> first() {
        return list.first();
    }
    
    public Iterable<String> text() {
        return list.transform(new Function<FluentElement, String>() {
            @Override
            public String apply(FluentElement input) {
                return input.text();
            }
        });
    }
    
    public <T> FluentIterable<T> as(final Class<T> clazz) {
        return list.transform(new Function<FluentElement, T>() {
            @Override
            public T apply(FluentElement input) {
                try {
                    return clazz.getConstructor(FluentElement.class).newInstance(input);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(clazz.getName() +
                            " requires a constructor which accepts FluentElement as the single parameter");
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Exception while creating component %s", clazz.getName()), e);
                }
            }
        });
    }

}
