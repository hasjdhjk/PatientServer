package Models;

public class Patient {

    private int id;
    private String givenName;
    private String familyName;
    private int heartRate;
    private double temperature;
    private String bloodPressure;

    public Patient() {}  // GSON requires a no-arg constructor

    public Patient(int id, String givenName, String familyName,
                   int heartRate, double temperature,
                   String bloodPressure) {

        this.id = id;
        this.givenName = givenName;
        this.familyName = familyName;
        this.heartRate = heartRate;
        this.temperature = temperature;
        this.bloodPressure = bloodPressure;
    }

    // Getters
    public int getId() { return id; }
    public String getGivenName() { return givenName; }
    public String getFamilyName() { return familyName; }
    public int getHeartRate() { return heartRate; }
    public double getTemperature() { return temperature; }
    public String getBloodPressure() { return bloodPressure; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setGivenName(String givenName) { this.givenName = givenName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }
    public void setHeartRate(int heartRate) { this.heartRate = heartRate; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }

    @Override
    public String toString() {
        return givenName + " " + familyName +
                " (HR: " + heartRate +
                ", Temp: " + temperature +
                ", BP: " + bloodPressure + ")";
    }
}
