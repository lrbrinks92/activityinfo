package org.activityinfo.test.webdriver;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.io.IOException;
import java.util.*;

/**
 * Creates a set of Browser profiles to test against
 */
public class BrowserProfileSetBuilder {

    public static final int LATEST = 1;
    public static final int LAST_TWO_VERSIONS = 2;


    private Iterable<BrowserProfile> supported = Lists.newArrayList();
    private List<BrowserProfile> selected = Lists.newArrayList();


    public BrowserProfileSetBuilder(Iterable<BrowserProfile> supported) {
        this.supported = supported;
    }


    public static Predicate<BrowserProfile> withVendor(final BrowserVendor vendor) {
        return new Predicate<BrowserProfile>() {
            @Override
            public boolean apply(BrowserProfile input) {
                return input.getType() == vendor;
            }
        };
    }

    public static Predicate<BrowserProfile> withOsType(final OperatingSystemType type) {
        return new Predicate<BrowserProfile>() {
            @Override
            public boolean apply(BrowserProfile input) {
                return input.getOS().getType() == type;
            }
        };
    }
    
    public static Predicate<BrowserProfile> withOs(final Optional<OperatingSystem> os) {
        if(os.isPresent()) {
            return new Predicate<BrowserProfile>() {
                @Override
                public boolean apply(BrowserProfile input) {
                    return input.getOS().equals(os.get());
                }
            };
        } else {
            return Predicates.alwaysFalse();
        }
    }
    
    public static Predicate<BrowserProfile> withBrowser(final BrowserVendor vendor, final int versionNumber) {
        final Version version = new Version(versionNumber);
        return new Predicate<BrowserProfile>() {
            @Override
            public boolean apply(BrowserProfile input) {
                return input.getType().equals(vendor) && input.getVersion().equals(version);
            }
        };
    }

    public Iterable<BrowserProfile> select(Predicate<BrowserProfile>... criteria) {
        return Iterables.filter(supported, Predicates.and(criteria));
    }
    
    public List<BrowserProfile> byBrowserVersion(Iterable<BrowserProfile> profiles) {
        List<BrowserProfile> selected = Lists.newArrayList(profiles);
        Collections.sort(selected, Ordering.natural().onResultOf(new Function<BrowserProfile, Comparable>() {
            @Override
            public Comparable apply(BrowserProfile input) {
                return input.getVersion();
            }
        }).reverse());
        return selected;
    }
    
    public Optional<OperatingSystem> latestOsVersion(OperatingSystemType type) {
        Set<Version> set = new HashSet<>();
        for(BrowserProfile profile : select(withOsType(type))) {
            set.add(profile.getOS().getVersion());
        }
        if(set.isEmpty()) {
            return Optional.absent();
        } else {
            List<Version> sorted = Lists.newArrayList(set);
            Collections.sort(sorted, Ordering.natural().reverse());
        
            return Optional.of(new OperatingSystem(type, sorted.get(0)));
        }
    }
    
    public void addLatestPerOS(BrowserVendor vendor, OperatingSystemType... osTypes) {
        for(OperatingSystemType type : osTypes) {
            addLatest(vendor, latestOsVersion(type));
        }
    }


    private void addIEVersionFromLatestOS(int version) {
        Iterable<BrowserProfile> browsers = select(withBrowser(BrowserVendor.IE, version));
        addAll(Iterables.limit(browsers, 1));

    }

    private void addAll(Iterable<BrowserProfile> browsers) {
        Iterables.addAll(selected, browsers);
    }

    private void addLatest(BrowserVendor vendor, Optional<OperatingSystem> type) {
        List<BrowserProfile> latest = byBrowserVersion(select(withVendor(vendor), withOs(type), stable()));
        Iterables.addAll(selected, Iterables.limit(latest, 1));
    }
    
    
    
    public static Predicate<BrowserProfile> stable() {
        return new Predicate<BrowserProfile>() {
            @Override
            public boolean apply(BrowserProfile input) {
                String version = input.getVersion().toString();
                return !version.equals("beta") && !version.equals("dev");
            }
        };
    }
    
    public void addLatest() {
        addLatestPerOS(BrowserVendor.CHROME, OperatingSystemType.values());
        addLatestPerOS(BrowserVendor.FIREFOX, OperatingSystemType.values());
        addIEVersionFromLatestOS(8);
        addIEVersionFromLatestOS(9);
        addIEVersionFromLatestOS(10);
        addIEVersionFromLatestOS(11);
    }
    
    public void addLight() {
        addLatest(BrowserVendor.CHROME, Optional.of(OperatingSystemType.WINDOWS.version("7")));
//        addLatest(BrowserVendor.FIREFOX, Optional.of(OperatingSystemType.WINDOWS.version("7")));
//        addIEVersionFromLatestOS(9);
    }


    public static void main(String[] args) throws IOException {
        BrowserProfileSetBuilder builder = new BrowserProfileSetBuilder(SaucePlatforms.fetchBrowsers());
        builder.addLatest();
        builder.dump();
    }


    public void dump() {
        for(BrowserProfile profile : selected) {
            System.out.println(profile.toString());
        }
    }

    public Collection<? extends DeviceProfile> selected() {
        return selected;
    }
}
