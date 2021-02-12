package za.co.entelect.challenge.command;

import za.co.entelect.challenge.enums.Direction;

public class BananaCommand implements Command {

    private int x;
    private int y;

    public BananaCommand(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String render() {
        return String.format("banana %d %d", x, y);
    }
}