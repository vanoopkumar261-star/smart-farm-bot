package farmbot.hardware;

public abstract class Component {
    protected String id;
    protected String name;
    protected String status;

    public Component(String id, String name) {
        this.id = id;
        this.name = name;
        this.status = "OK";
    }

    public void initialize() {
        status = "OK";
    }

    public void shutdown() {
        status = "INACTIVE";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public abstract String getDescription();
}
