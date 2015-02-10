package org.activityinfo.test.sut;

/**
 * A user account in ActivityInfo
 */
public class UserAccount {
    private String email;
    private String password;

    public UserAccount(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
