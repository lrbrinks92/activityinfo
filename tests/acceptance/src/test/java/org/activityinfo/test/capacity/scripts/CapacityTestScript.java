package org.activityinfo.test.capacity.scripts;

import com.google.common.base.Supplier;
import org.activityinfo.test.capacity.model.Scenario;

import java.util.Collection;

/**
 * Provides a specific configuration to run
 */
public interface CapacityTestScript extends Supplier<Collection<Scenario>> {
}
