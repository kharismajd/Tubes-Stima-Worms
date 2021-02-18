package za.co.entelect.challenge.command;

public class SelectCommand implements Command {

    private int x;
    private Command command;

    public SelectCommand(int id, Command command) {
        this.x = id;
        this.command = command;
    }

    @Override
    public String render() {
            return String.format("select %d;%s", this.x, this.command.render());
    }
}