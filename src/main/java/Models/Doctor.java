package Models;

public class Doctor {
    private int id;
    private String email;
    private String givenName;
    private String familyName;
    private String passwordHash;
    private boolean verified;
    private String verificationToken;
    private String resetToken;

    public Doctor() {}

    public Doctor(int id, String email, String givenName, String familyName,
                  String passwordHash, boolean verified) {
        this.id = id;
        this.email = email;
        this.givenName = givenName;
        this.familyName = familyName;
        this.passwordHash = passwordHash;
        this.verified = verified;
    }

    // Getters
    public int getId() {return id;}
    public String getEmail() {return email;}
    public String getGivenName() {return givenName;}
    public String getFamilyName() {return familyName;}
    public String getPasswordHash() {return passwordHash;}
    public boolean isVerified() {return verified;}
    public String getVerificationToken() {return verificationToken;}
    public String getResetToken() {return resetToken;}

    // Setters
    public void setId(int id) {this.id = id;}
    public void setEmail(String email) {this.email = email;}
    public void setGivenName(String givenName) {this.givenName = givenName;}
    public void setFamilyName(String familyName) {this.familyName = familyName;}
    public void setPasswordHash(String passwordHash) {this.passwordHash = passwordHash;}
    public void setVerified(boolean verified) {this.verified = verified;}
    public void setVerificationToken(String verificationToken) {this.verificationToken = verificationToken;}
    public void setResetToken(String resetToken) {this.resetToken = resetToken;}
}
