public class ParkingSpot {
    private int id;
    private int spotNumber;
    private boolean isReserved;
    private Integer reservedBy;

    // Constructor
    public ParkingSpot(int spotNumber) {
        this.spotNumber = spotNumber;
        this.isReserved = false;
        this.reservedBy = null;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSpotNumber() { return spotNumber; }
    public void setSpotNumber(int spotNumber) { this.spotNumber = spotNumber; }
    public boolean isReserved() { return isReserved; }
    public void setReserved(boolean reserved) { isReserved = reserved; }
    public Integer getReservedBy() { return reservedBy; }
    public void setReservedBy(Integer reservedBy) { this.reservedBy = reservedBy; }
}
