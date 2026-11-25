package farmbot.hardware;

import farmbot.model.Command;

public class SprayPump extends Actuator {
    public SprayPump(String id) {
        super(id, "Spray Pump");
    }

    @Override
    public void actuate(Command command) {
        throw new UnsupportedOperationException("spray system not yet implemented");
    }

    @Override
    public String getDescription() {
        return "Future work: chemical/water spray actuator";
    }
}
