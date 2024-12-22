public class User {
    private int id;
    private String fullName;
    private String userType;
    private String phoneNumber;
    private String carPlate;
    private String passwordHash;

    // Constructor
    public User(String fullName, String userType, String phoneNumber, String carPlate, String passwordHash) {
        this.fullName = fullName;
        this.userType = userType;
        this.phoneNumber = phoneNumber;
        this.carPlate = carPlate;
        this.passwordHash = passwordHash;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getCarPlate() { return carPlate; }
    public void setCarPlate(String carPlate) { this.carPlate = carPlate; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
