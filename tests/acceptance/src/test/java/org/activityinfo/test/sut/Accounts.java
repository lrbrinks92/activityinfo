package org.activityinfo.test.sut;

/**
 * Strategy for creating or accessing accounts on the server under test
 */
public interface Accounts {


    UserAccount ensureAccountExists(String email);


}
