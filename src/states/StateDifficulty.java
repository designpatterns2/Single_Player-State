package states;

import com.jme.input.InputHandler;
import com.jme.renderer.Camera;
import com.jme.scene.Node;
import com.jme.scene.state.LightState;
import main.DisplayDifficulty;
import main.Main;

public class StateDifficulty implements State {
    @Override
    public void display(StateContext context, Node rootNode, LightState lightState, Camera camera, InputHandler input) {
        int state = DisplayDifficulty.display(rootNode, lightState, camera, input);
        switch (state) {
            case Main.LOADING:
                context.changeState(new StateLoading());
                break;
            case Main.INTRO:
                context.changeState(new StateIntro());
                break;
        }
    }
}
