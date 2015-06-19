package org.activityinfo.test.capacity.model;

import com.google.common.collect.Lists;
import org.activityinfo.test.capacity.TestContext;
import org.activityinfo.test.driver.AliasTable;
import org.activityinfo.test.sut.DevServerAccounts;
import org.activityinfo.test.sut.Server;

import java.util.List;

/**
 * Represents an independent test scenario which might involve multiple simulated users.
 * Each scenario has its own alias table so it can run completely independently of another scenario
 */
public class ScenarioContext {

    private TestContext testContext;
    private DevServerAccounts accounts;
    private AliasTable aliasTable;

    private List<User> users = Lists.newArrayList();

    public ScenarioContext(TestContext context) {
        this.testContext = context;
        this.accounts = new DevServerAccounts();
        this.aliasTable = new AliasTable();
    }

    public DevServerAccounts getAccounts() {
        return accounts;
    }

    public Server getServer() {
        return testContext.getServer();
    }

    public AliasTable getAliasTable() {
        return aliasTable;
    }

    public User user(UserRole role) {
        User user = new User(this, role);
        users.add(user);
        return user;
    }
    
}
