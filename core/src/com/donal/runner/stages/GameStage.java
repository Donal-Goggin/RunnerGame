package com.donal.runner.stages;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.donal.runner.actors.Background;
import com.donal.runner.actors.Enemy;
import com.donal.runner.actors.Ground;
import com.donal.runner.actors.Runner;
import com.donal.runner.utils.BodyUtils;
import com.donal.runner.utils.Constants;
import com.donal.runner.utils.WorldUtils;

import static com.donal.runner.utils.Constants.RUNNER_DODGE_DELAY;

public class GameStage extends Stage implements ContactListener {

    // This will be our viewport measurements while working with the debug renderer
//    private static final int VIEWPORT_WIDTH = 20;
//    private static final int VIEWPORT_HEIGHT = 13;

    private static final int VIEWPORT_WIDTH = Constants.APP_WIDTH;
    private static final int VIEWPORT_HEIGHT = Constants.APP_HEIGHT;

    private World world;
    private Ground ground;
    private Runner runner;

    private final float TIME_STEP = 1 / 300f;
    private float accumulator = 0f;

    private OrthographicCamera camera;
    private Box2DDebugRenderer renderer;

    private Rectangle screenLeftSide;
    private Rectangle screenRightSide;

    private Vector3 touchPoint;
    private Vector2 lastTouch = new Vector2();



    public GameStage() {
        super(new ScalingViewport(Scaling.stretch, VIEWPORT_WIDTH, VIEWPORT_HEIGHT,
                new OrthographicCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)));
        setUpWorld();
        setupCamera();
        setupTouchControlAreas();
    }

    private void setUpWorld() {
        world = WorldUtils.createWorld();
        // Let the world know you are handling contacts
        world.setContactListener(this);
        setUpBackground();
        setUpGround();
        setUpRunner();
        createEnemy();
    }

    private void setUpGround() {
        ground = new Ground(WorldUtils.createGround(world));
        addActor(ground);
    }

    private void setUpRunner() {
        runner = new Runner(WorldUtils.createRunner(world));
        addActor(runner);
    }

    private void setupCamera() {
        camera = new OrthographicCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0f);
        camera.update();
    }

    private void setupTouchControlAreas() {
//        touchPoint = new Vector3();
//        screenLeftSide = new Rectangle(0, 0, getCamera().viewportWidth / 2, getCamera().viewportHeight);
//        screenRightSide = new Rectangle(getCamera().viewportWidth / 2, 0, getCamera().viewportWidth / 2,
//                getCamera().viewportHeight);
        Gdx.input.setInputProcessor(this);
    }

    private void setUpBackground() {
        addActor(new Background());
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        Array<Body> bodies = new Array<Body>(world.getBodyCount());
        world.getBodies(bodies);

        for (Body body : bodies) {
            update(body);
        }

        // Fixed timestep
        accumulator += delta;

        while (accumulator >= delta) {
            world.step(TIME_STEP, 6, 2);
            accumulator -= TIME_STEP;
        }

        //TODO: Implement interpolation

    }

    private void update(Body body) {
        if (!BodyUtils.bodyInBounds(body)) {
            if (BodyUtils.bodyIsEnemy(body) && !runner.isHit()) {
                createEnemy();
            }
            world.destroyBody(body);
        }
    }

    private void createEnemy() {
        Enemy enemy = new Enemy(WorldUtils.createEnemy(world));
        addActor(enemy);
    }

//    @Override
//    public void draw() {
//        super.draw();
//        renderer.render(world, camera.combined);
//    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        Vector2 newTouch = new Vector2(screenX, screenY);
        // delta will now hold the difference between the last and the current touch positions
        // delta.x > 0 means the touch moved to the right, delta.x < 0 means a move to the left
        Vector2 delta = newTouch.cpy().sub(lastTouch);

        //Swipe DOWN
        if(delta.y > 0) runner.dodge();

        //Swipe UP
        if(delta.y < 0) runner.jump();

        lastTouch = newTouch;
        return super.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {

        // Need to get the actual coordinates
       // translateScreenToWorldCoordinates(x, y);

        lastTouch.set(x, y);

//        if (rightSideTouched(touchPoint.x, touchPoint.y)) {
//            runner.jump();
//        }else if (leftSideTouched(touchPoint.x, touchPoint.y)) {
//            runner.dodge();
//        }

        return super.touchDown(x, y, pointer, button);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {


        if (runner.isDodging()) {
            Timer.schedule(new Timer.Task(){
                @Override
                public void run() {
                        runner.stopDodge();
                }
            }, RUNNER_DODGE_DELAY);
            }

        return super.touchUp(screenX, screenY, pointer, button);
    }

//    private boolean leftSideTouched(float x, float y) {
//        return screenLeftSide.contains(x, y);
//    }
//
//    private boolean rightSideTouched(float x, float y) {
//        return screenRightSide.contains(x, y);
//    }

//    /**
//     * Helper function to get the actual coordinates in my world
//     * @param x
//     * @param y
//     */
//    private void translateScreenToWorldCoordinates(int x, int y) {
//        getCamera().unproject(touchPoint.set(x, y, 0));
//    }

    @Override
    public void beginContact(Contact contact) {

        Body a = contact.getFixtureA().getBody();
        Body b = contact.getFixtureB().getBody();

        if ((BodyUtils.bodyIsRunner(a) && BodyUtils.bodyIsEnemy(b)) ||
                (BodyUtils.bodyIsEnemy(a) && BodyUtils.bodyIsRunner(b))) {
            runner.hit();
        } else if ((BodyUtils.bodyIsRunner(a) && BodyUtils.bodyIsGround(b)) ||
                (BodyUtils.bodyIsGround(a) && BodyUtils.bodyIsRunner(b))) {
            runner.landed();
        }

    }

    @Override
    public void endContact(Contact contact) {

    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }

}
