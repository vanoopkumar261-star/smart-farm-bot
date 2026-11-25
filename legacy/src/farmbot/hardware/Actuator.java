package farmbot.hardware;

import farmbot.exceptions.InvalidSpeedException;
import farmbot.exceptions.MotorStallException;
import farmbot.model.Command;

public abstract class Actuator extends Component {
    public Actuator(String id, String name) {
        super(id, name);
    }

    public abstract void actuate(Command command) throws MotorStallException, InvalidSpeedException;
}
