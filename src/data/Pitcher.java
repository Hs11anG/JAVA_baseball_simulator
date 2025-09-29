package data;

public class Pitcher {
    private int pid;
    private int tid;
    private String pname;
    private int year;
    private String lr;
    private int stuff;
    private int velocity;
    private int ptype;

    public Pitcher(int pid, int tid, String pname, int year, String lr, int stuff, int velocity, int ptype) {
        this.pid = pid;
        this.tid = tid;
        this.pname = pname;
        this.year = year;
        this.lr = lr;
        this.stuff = stuff;
        this.velocity = velocity;
        this.ptype = ptype;
    }

    // Getter methods
    public int getPid() { return pid; }
    public int getTid() { return tid; }
    public String getPname() { return pname; }
    public int getYear() { return year; }
    public String getLr() { return lr; }
    public int getStuff() { return stuff; }
    public int getVelocity() { return velocity; }
    public int getPtype() { return ptype; }

    @Override
    public String toString() {
        return pname; // Convenient for displaying pitcher's name in a list
    }
}