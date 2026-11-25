package farmbot.control;

import farmbot.navigation.FollowLineStrategy;

public class LineFollowerBot extends Robot {
    public LineFollowerBot(String id, Controller controller) {
        super(id, controller);
    }

    @Override
    public void start() {
        super.start();
        controller.setStrategy(new FollowLineStrategy());
    }
}
