package za.co.entelect.challenge.entities;

import com.google.gson.annotations.SerializedName;

public class Position {
    @SerializedName("x")
    public int x;

    @SerializedName("y")
    public int y;

    public Position(int x0, int y0) {
        x = x0;
        y = y0;
    }
}
