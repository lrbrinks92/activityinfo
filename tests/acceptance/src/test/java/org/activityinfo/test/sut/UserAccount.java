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

    /**
     *
     * @return the user portion of the email address. 
     */
    public String nameFromEmail() {
        int at = email.indexOf('@');
        if(at == -1) {
            throw new IllegalStateException("Invalid email address: " + email);
        }
        return email.substring(0, at);
    }
    
    public String domainFromEmail() {
        int at = email.indexOf('@');
        if(at == -1) {
            throw new IllegalStateException("Invalid email address: " + email);
        }
        return email.substring(at+1);
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return email;
    }
}
