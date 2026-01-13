package Models;

public class Patient {

    private int id;
    private String givenName;
    private String familyName;
    private String gender;
    private int age;
    private String bloodPressure;

    private boolean sticky = false; // sticky on top

    public Patient() {}  // GSON requires a no-arg constructor

    public Patient(int id, String givenName, String familyName,
                   String gender, int age,
                   String bloodPressure) {

        this.id = id;
        this.givenName = givenName;
        this.familyName = familyName;
        this.gender = gender;
        this.age = age;
        this.bloodPressure = bloodPressure;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return givenName + " " + familyName; }
    public String getGivenName() { return givenName; }
    public String getFamilyName() { return familyName; }
    public String getGender() { return gender; }
    public int getAge() { return age; }
    public String getBloodPressure() { return bloodPressure; }
    public boolean isSticky() { return sticky; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setGivenName(String givenName) { this.givenName = givenName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }
    public void setGender(String gender) { this.gender = gender; }
    public void setAge(int age) { this.age = age; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }
    public void setSticky(boolean sticky) { this.sticky = sticky; }

    @Override
    public String toString() {
        return givenName + " " + familyName +
                " (Gender: " + gender +
                ", Age: " + age +
                ", BP: " + bloodPressure + ")";
    }
}