package a5;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioNode;
import com.jme3.audio.Environment;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.joints.HingeJoint;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class A5 extends SimpleApplication implements PhysicsCollisionListener {

    public static void main(String args[]) {
        A5 app = new A5();
        app.start();
    }
    /**
     * Prepare the Physics Application State (jBullet)
     */
    private BulletAppState bulletAppState;
    Material stone_mat;
    private Vector3f moveDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false, forward = false, back = false;
    // Prepare geometries and physical nodes for rigid bodies
    private RigidBodyControl ball_phy;
    private static final Sphere sphere;
    private Node fish;
    private static final float boxSize = 8f, boxWidth = 0.1f;
    private CopyOnWriteArrayList<Tray> trays = new CopyOnWriteArrayList();
    private float timeElapsed = 0f;
    private float traySpawnRate = 3f; // 1 second
    private AudioNode collisionBottomSound, collisionTraySound, bubbleSound, loseLifeSound;
    private Geometry score_geo;
    private int score = 0, trayNum = 0, lives = 3;
    private BitmapText scoreText;
    private CopyOnWriteArrayList<Node> livesList = new CopyOnWriteArrayList();
    private boolean isGameover = false;
    AnimChannel channel;

    static {
        /**
         * Initialize the sphere geometry
         */
        sphere = new Sphere(32, 32, 0.4f, true, false);
        sphere.setTextureMode(TextureMode.Projected);
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        //bulletAppState.setDebugEnabled(true);

        //flyCam.setMoveSpeed(20);
        flyCam.setEnabled(false);
        // Configure cam to look inside bounding box
        cam.setLocation(new Vector3f(0, boxSize * 2, 3 * boxSize));
        cam.lookAt(new Vector3f(0, boxSize, 0), Vector3f.UNIT_Y);
        /**
         * Add InputManager action: space spawns ball.
         */
        inputManager.addMapping("shoot",
                new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(actionListener, "shoot");

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f, -1f, -1f).normalizeLocal());
        rootNode.addLight(dl);

        /**
         * Initialize the scene, materials, and physics space
         */
        initMaterials();
        initFish();
        initAudio();
        initSkyBox();
        initScoreBox();
        initLives();

        bulletAppState.getPhysicsSpace().addCollisionListener(this);
    }
    
    /**
     * Initialize the materials used in this scene.
     */
    private void initMaterials() {
        stone_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key2 = new TextureKey("Textures/water.jpg");
        key2.setGenerateMips(true);
        Texture tex2 = assetManager.loadTexture(key2);
        stone_mat.setTexture("ColorMap", tex2);
    }
    
    private void initLives() {
        for (int i = 0; i < lives; i ++) {
            Node life = (Node) assetManager.loadModel("Models/blub3 - animated.j3o");
            rootNode.attachChild(life);
            life.setLocalScale(0.7f);
            life.setLocalTranslation(
                settings.getWidth()/130 + 2.5f*i,
                settings.getHeight()/37, 0);
            life.setLocalRotation(new Quaternion().fromAngleAxis((float) -Math.PI/2, Vector3f.UNIT_Y));
            livesList.add(life);
        }
        
    }
    
    private void loseLife() {
        lives --;
        rootNode.detachChild(livesList.get(0));
        livesList.remove(0);
        loseLifeSound.playInstance();
    }
    
    private void initScoreBox() {
        Material scoreBox_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        scoreBox_mat.setColor("Color", new ColorRGBA( 1f, 1f, 1f, 0.5f)); // White
        scoreBox_mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        
        Box scoreBox = new Box(2f, 1f, boxWidth);
        score_geo = new Geometry("TrayRight", scoreBox);
        score_geo.setMaterial(scoreBox_mat);
        score_geo.setQueueBucket(RenderQueue.Bucket.Transparent);
        score_geo.setLocalTranslation(0f, 2*boxSize, -boxSize);
        rootNode.attachChild(score_geo);
        
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        scoreText = new BitmapText(guiFont, false);
        scoreText.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        scoreText.setColor(ColorRGBA.Black);
        scoreText.setText(score+"");
        scoreText.setLocalTranslation(
          settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
          settings.getHeight() - 35f, 0);
        guiNode.attachChild(scoreText);
        
    }

    private void initAudio() {
        audioRenderer.setEnvironment(new Environment(Environment.Dungeon));

        collisionBottomSound = new AudioNode(assetManager, "Sounds/collide.wav");
        collisionBottomSound.setPositional(true);
        collisionBottomSound.setReverbEnabled(true);
        collisionBottomSound.setVolume(0.25f);

        collisionTraySound = new AudioNode(assetManager, "Sounds/splat.wav");
        collisionTraySound.setPositional(true);
        collisionTraySound.setReverbEnabled(true);
        collisionTraySound.setVolume(0.05f);

        bubbleSound = new AudioNode(assetManager, "Sounds/bubble.wav");
        bubbleSound.setVolume(0.15f);
        
        loseLifeSound = new AudioNode(assetManager, "Sounds/loseLife.wav");
        loseLifeSound.setVolume(2f);
    }

    private void initSkyBox() {
        TextureKey key = new TextureKey("Textures/SkyBox.dds", true);
        key.setGenerateMips(true);
        key.setTextureTypeHint(Texture.Type.CubeMap);
        final Texture tex = assetManager.loadTexture(key);
        rootNode.attachChild(SkyFactory.createSky(assetManager, tex, false));
    }

    private void trayTimer(float tpf) {
        timeElapsed += tpf;
        checkTrayLocations();

        // 1 second has passed, spawn a tray
        if (timeElapsed >= traySpawnRate) {
            timeElapsed = 0f;
            float x = -boxSize + ThreadLocalRandom.current().nextFloat() * 2 * boxSize;
            float y = -boxSize / 2 + ThreadLocalRandom.current().nextFloat() * 2 * boxSize;
            trays.add(new Tray(trayNum++,bulletAppState, rootNode, assetManager, x, y, fish.getLocalTranslation().getZ() - 20f));
        }
    }

    // Clean up old trays
    private synchronized void checkTrayLocations() {
        for (Tray t : trays) {
            
            // Target is behind fish, take away a life
            if (fish.getLocalTranslation().getZ() < t.getZ() && !t.isLifeLost()) {
                t.setLifeLost();
                loseLife();
                if (lives == 0) {
                    gameover();
                }
            }
            
            if (fish.getLocalTranslation().getZ() < t.getZ() - boxSize) { // if the tray is far behind the fish
                t.destroy(); // remove tray from rootNode
                trays.remove(t);
            }
        }
    }
    
    private void gameover() {
        isGameover = true;
        setDisplayStatView(false);
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText gameoverText = new BitmapText(guiFont, false);
        gameoverText.setSize(guiFont.getCharSet().getRenderedSize() * 3);
        gameoverText.setColor(ColorRGBA.Black);
        gameoverText.setText("Game Over");
        gameoverText.setLocalTranslation( // center
          settings.getWidth() / 2 - gameoverText.getLineWidth()/2, settings.getHeight() / 2 + gameoverText.getLineHeight()/2, 0);
        guiNode.attachChild(gameoverText);
        
        channel.setAnim("Stand", 0.50f);
        channel.setLoopMode(LoopMode.Loop);
    }
    
    /**
     * This method creates one individual physical bubble.
     */
    public void shootBubble() {
        /**
         * Create a cannon ball geometry and attach to scene graph.
         */
        Geometry ball_geo = new Geometry("bubble", sphere);
        ball_geo.setMaterial(stone_mat);
        rootNode.attachChild(ball_geo);
        /**
         * Position the cannon ball
         */
        ball_geo.setLocalTranslation(new Vector3f(fish.getLocalTranslation().x, fish.getLocalTranslation().y, fish.getLocalTranslation().z - 2f));
        /**
         * Make the ball physcial with a mass > 0.0f
         */
        ball_phy = new RigidBodyControl(1f);
        /**
         * Add physical ball to physics space.
         */
        ball_geo.addControl(ball_phy);
        bulletAppState.getPhysicsSpace().add(ball_phy);
        /**
         * Accelerate the physcial ball to shoot it.
         */
        ball_phy.setLinearVelocity(Vector3f.UNIT_Z.mult(-25));
        bubbleSound.playInstance();
    }

    private void initFish() {
        fish = (Node) assetManager.loadModel("Models/blub3 - animated.j3o");
        rootNode.attachChild(fish);
        fish.setLocalScale(1.5f);
        fish.setLocalTranslation(new Vector3f(0f, boxSize / 2, 0f));
        //fish.setLocalTranslation(cam.getLocation());
        fish.setLocalRotation(new Quaternion().fromAngleAxis((float) Math.PI, Vector3f.UNIT_Y));
        fish.addControl(new RigidBodyControl(0f));
        bulletAppState.getPhysicsSpace().add(fish);

        channel = fish.getChild("blub_quadrangulated").getControl(AnimControl.class).createChannel();
        channel.setAnim("Swim", 0.50f);
        channel.setLoopMode(LoopMode.Loop);

        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addListener(actionListener, "left");
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addListener(actionListener, "right");
        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addListener(actionListener, "up");
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addListener(actionListener, "down");
        inputManager.addMapping("forward", new KeyTrigger(KeyInput.KEY_PGUP));
        inputManager.addListener(actionListener, "forward");
        inputManager.addMapping("back", new KeyTrigger(KeyInput.KEY_PGDN));
        inputManager.addListener(actionListener, "back");
    }

    public void collision(PhysicsCollisionEvent event) {
        // Check for collision between a sphere and tray bottom
        if ((event.getNodeA().getName().startsWith("Bottom") || event.getNodeB().getName().startsWith("Bottom"))
                && ("bubble".equals(event.getNodeA().getName()) || "bubble".equals(event.getNodeB().getName()))) {

            Spatial trayBottom;
            Spatial bubble;
            if (event.getNodeA().getName().startsWith("Bottom")) {
                trayBottom = event.getNodeA();
                bubble = event.getNodeB();
            } else {
                trayBottom = event.getNodeB();
                bubble = event.getNodeB();
            }
            
            Tray t = getTrayByName(trayBottom.getName());
            
            if (!t.isScored()) {
                t.setScored();
                score ++;
                scoreText.setText(score+"");
                if (score%2 == 0 && traySpawnRate > 0.5f) {
                    traySpawnRate -= 0.2f;
                }
            }

            // acivate all spheres so that deactivated spheres will fall
            for (Spatial s : this.getRootNode().getChildren()) {
                if (s.getName().equals("bubble")) {
                    s.getControl(RigidBodyControl.class).activate();
                }
            }
            trayBottom.getControl(RigidBodyControl.class).activate();
            trayBottom.getControl(RigidBodyControl.class).setMass(1f);

            collisionBottomSound.setLocalTranslation(bubble.getLocalTranslation());
            collisionBottomSound.playInstance();
        } else if ((event.getNodeA().getName().startsWith("Tray") || event.getNodeB().getName().startsWith("Tray"))
                && ("bubble".equals(event.getNodeA().getName()) || "bubble".equals(event.getNodeB().getName()))) {

            Spatial bubble;
            if (event.getNodeA().getName().startsWith("Bottom")) {
                bubble = event.getNodeB();
            } else {
                bubble = event.getNodeB();
            }

            collisionTraySound.setLocalTranslation(bubble.getLocalTranslation());
            collisionTraySound.playInstance();
        } /*else if ((event.getNodeA().getName().equals("bubble") && event.getNodeB().getName().equals("bubble"))) {
         splashSound.setLocalTranslation(event.getNodeA().getLocalTranslation());
         splashSound.playInstance();
         }*/
    }
    
    private Tray getTrayByName(String name) {
        int num = Integer.parseInt(name.substring(6, name.length()));
        for (Tray t : trays) {
            if (t.getTrayNum() == num) {
                return t;
            }
        }
        
        return null;
    }
    
    /**
     * Every time the shoot action is triggered, a new cannon ball is produced.
     * The ball is set up to fly from the camera position in the camera
     * direction.
     */
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {

            if (name.equals("shoot") && !keyPressed) {
                shootBubble();
            }

            if (name.equals("left")) {
                if (keyPressed) {
                    left = true;
                } else {
                    left = false;
                }
            } else if (name.equals("right")) {
                if (keyPressed) {
                    right = true;
                } else {
                    right = false;
                }
            } else if (name.equals("up")) {
                if (keyPressed) {
                    up = true;
                } else {
                    up = false;
                }
            } else if (name.equals("down")) {
                if (keyPressed) {
                    down = true;
                } else {
                    down = false;
                }
            }
        }
    };

    @Override
    public void simpleUpdate(float tpf) {
        if (!isGameover) {
            trayTimer(tpf);
            float fishS = tpf * 10; // speed
            moveDirection.set(fish.getLocalTranslation().clone());
            if (left) {
                moveDirection.addLocal(new Vector3f(-fishS, 0f, 0f));
            }
            if (right) {
                moveDirection.addLocal(new Vector3f(fishS, 0f, 0f));
            }
            if (up) {
                moveDirection.addLocal(new Vector3f(0f, fishS, 0f));
            }
            if (down) {
                moveDirection.addLocal(new Vector3f(0f, -fishS, 0f));
            }
            moveDirection.addLocal(new Vector3f(0f, 0f, -fishS / 4)); // move fish forward

            // Move fish, camera and scoreBox
            fish.setLocalTranslation(moveDirection);
            fish.getControl(RigidBodyControl.class).setPhysicsLocation(moveDirection);
            score_geo.setLocalTranslation(0f, 2*boxSize, -boxSize + fish.getLocalTranslation().z);
            cam.setLocation(new Vector3f(0, boxSize * 2, 3 * boxSize + fish.getLocalTranslation().z));
            for (int i = 0; i < livesList.size(); i ++) {
                livesList.get(i).setLocalTranslation(livesList.get(i).getLocalTranslation().x, livesList.get(i).getLocalTranslation().y, fish.getLocalTranslation().z);
            }
        }

        // stop hinge joints from swinging past -90 degrees
        for (Spatial node : this.getRootNode().getChildren()) {
            //System.out.println(node.getName());
            if (node.getName().startsWith("Bottom")) {
                HingeJoint j = (HingeJoint) node.getControl(RigidBodyControl.class).getJoints().get(0);
                //System.out.println(j.getHingeAngle() + " : " + j.getUpperLimit());
                if (Math.abs(j.getHingeAngle() - (float) Math.PI / 2) <= 0.1) {
                    j.getBodyB().setMass(0f);
                    getTrayByName(node.getName()).destroy();
                    trays.remove(getTrayByName(node.getName()));
                }
            }
        }
        
    }
}