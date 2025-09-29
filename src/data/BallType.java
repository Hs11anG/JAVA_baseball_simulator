package data;

public class BallType {
    private int bid;
    private String bname;

    public BallType(int bid, String bname) {
        this.bid = bid;
        this.bname = bname;
    }

    public int getBid() { return bid; }
    public String getBname() { return bname; }
}