package a5;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.joints.HingeJoint;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;

public class Tray {
    
    private final float traySize = 1.5f, trayWidth = 0.1f;
    private float x, y, z;
    private Material tray_mat, trayBottom_mat, target_mat;
    private Node rootNode;
    private BulletAppState bulletAppState;
    private AssetManager assetManager;
    private Geometry trayRight_geo, trayLeft_geo, trayFront_geo, trayBack_geo;
    private Node holderNode, hammerNode;
    private HingeJoint joint;
    private RigidBodyControl trayRight_phy, trayLeft_phy, trayFront_phy, trayBack_phy, trayBottom_phy;
    private boolean isScored = false, isLifeLost = false;
    private int trayNum;
    
    public Tray(int trayNum, BulletAppState bulletAppState, Node rootNode, AssetManager manager, float x, float y, float z) {
        this.bulletAppState = bulletAppState;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rootNode = rootNode;
        this.assetManager = manager;
        this.trayNum = trayNum;
        
        initMaterials();
        initTray();
    }
    
    public boolean isLifeLost() {
        return isLifeLost;
    }
    
    public void setLifeLost() {
        tray_mat.setColor("Color", new ColorRGBA( 0.7f, 0f, 0f, 0.5f)); // red
        isLifeLost = true;
    }
    
    public void setScored() {
        isScored = true;
    }
    
    public boolean isScored() {
        return isScored;
    }
    
    public int getTrayNum() {
        return trayNum;
    }
    
    public float getZ() {
        return this.z;
    }
    
    public void destroy() {
        rootNode.detachChild(trayLeft_geo);
        rootNode.detachChild(trayFront_geo);
        rootNode.detachChild(trayBack_geo);
        rootNode.detachChild(holderNode);
        rootNode.detachChild(hammerNode);
        
        bulletAppState.getPhysicsSpace().remove(trayLeft_phy);
        bulletAppState.getPhysicsSpace().remove(trayRight_phy);
        bulletAppState.getPhysicsSpace().remove(trayFront_phy);
        bulletAppState.getPhysicsSpace().remove(trayBack_phy);
        bulletAppState.getPhysicsSpace().remove(trayBottom_phy);
        bulletAppState.getPhysicsSpace().remove(holderNode);
        bulletAppState.getPhysicsSpace().remove(hammerNode);
        bulletAppState.getPhysicsSpace().remove(joint);
    }
    
    private void initMaterials() {
        tray_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        // Use transparency - just to make sure we can always see the target
        tray_mat.setColor("Color", new ColorRGBA( 1f, 1f, 1f, 0.5f)); // red
        tray_mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        
        trayBottom_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        trayBottom_mat.setColor("Color", new ColorRGBA( 1f, 1f, 1f, 1f)); // white
        
        target_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key3 = new TextureKey("Textures/bullseye.png");
        key3.setGenerateMips(true);
        Texture tex3 = assetManager.loadTexture(key3);
        target_mat.setTexture("ColorMap", tex3);
    }
    
    private void initTray() {
        Box trayRight = new Box(trayWidth, traySize, traySize/2);
        trayRight_geo = new Geometry("TrayRight", trayRight);
        trayRight_geo.setMaterial(tray_mat);
        trayRight_geo.setQueueBucket(RenderQueue.Bucket.Transparent);
        // Make the tray right wall physical with mass 0.0f!
        trayRight_phy = new RigidBodyControl(0.0f);
        
        Box trayLeft = new Box(trayWidth, traySize, traySize/2);
        trayLeft_geo = new Geometry("TrayLeft", trayLeft);
        trayLeft_geo.setMaterial(tray_mat);
        trayLeft_geo.setQueueBucket(RenderQueue.Bucket.Transparent);
        trayLeft_geo.setLocalTranslation(x-traySize-trayWidth, y, z);
        //trayLeft_geo.setLocalRotation(new Quaternion().fromAngleAxis((float)Math.PI, Vector3f.UNIT_X));
        rootNode.attachChild(trayLeft_geo);
        // Make the tray left wall physical with mass 0.0f!
        trayLeft_phy = new RigidBodyControl(0.0f);
        trayLeft_geo.addControl(trayLeft_phy);
        bulletAppState.getPhysicsSpace().add(trayLeft_phy);
        
        Box trayFront = new Box(traySize, trayWidth, traySize/2);
        trayFront_geo = new Geometry("TrayFront", trayFront);
        trayFront_geo.setMaterial(tray_mat);
        trayFront_geo.setQueueBucket(RenderQueue.Bucket.Transparent);
        trayFront_geo.setLocalTranslation(x, y-traySize+trayWidth, z);
        trayFront_geo.setLocalRotation(new Quaternion().fromAngleAxis((float)Math.toRadians(3), Vector3f.UNIT_X));
        this.rootNode.attachChild(trayFront_geo);
        // Make the tray right wall physical with mass 0.0f!
        trayFront_phy = new RigidBodyControl(0.0f);
        trayFront_geo.addControl(trayFront_phy);
        bulletAppState.getPhysicsSpace().add(trayFront_phy);
        
        Box trayBack = new Box(traySize, trayWidth, traySize/2);
        trayBack_geo = new Geometry("TrayBack", trayBack);
        trayBack_geo.setMaterial(tray_mat);
        trayBack_geo.setQueueBucket(RenderQueue.Bucket.Transparent);
        trayBack_geo.setLocalTranslation(x, y+traySize-trayWidth, z);
        //trayBack_geo.setLocalRotation(new Quaternion().fromAngleAxis((float)Math.PI, Vector3f.UNIT_X));
        this.rootNode.attachChild(trayBack_geo);
        // Make the tray right wall physical with mass 0.0f!
        trayBack_phy = new RigidBodyControl(0.0f);
        trayBack_geo.addControl(trayBack_phy);
        bulletAppState.getPhysicsSpace().add(trayBack_phy);
        
        // Node containing right wall of tray. Tray bottom will be hinged to this
        holderNode=createPhysicsTestNode("TrayRight", new BoxCollisionShape(new Vector3f(trayWidth, traySize, traySize/2)),0);
        holderNode.setLocalTranslation(x+traySize-trayWidth, y, z);
        holderNode.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(x+traySize-trayWidth, y, z));
        holderNode.addControl(trayRight_phy);
        bulletAppState.getPhysicsSpace().add(trayRight_phy);
        holderNode.attachChild(trayRight_geo);
        rootNode.attachChild(holderNode);
        bulletAppState.getPhysicsSpace().add(holderNode);

        
        Box trayBottom = new Box(traySize, traySize, trayWidth);
        //Cylinder trayBottom = new Cylinder(20,50,traySize,trayWidth, true);
        Geometry trayBottom_geo = new Geometry("TrayBottom", trayBottom);
        trayBottom_geo.setMaterial(target_mat);
        // Make the tray bottom physical with mass 0.0f!
        trayBottom_phy = new RigidBodyControl(0.0f);
        
        // Node containing tray bottom
        hammerNode=createPhysicsTestNode("Bottom"+trayNum, new BoxCollisionShape(new Vector3f(traySize, traySize, trayWidth)),0);
        hammerNode.setLocalTranslation(x, y, z-traySize/2);
        hammerNode.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(x, y, z-traySize/2));
        hammerNode.addControl(trayBottom_phy);
        bulletAppState.getPhysicsSpace().add(trayBottom_phy);
        
        hammerNode.attachChild(trayBottom_geo);
        rootNode.attachChild(hammerNode);
        bulletAppState.getPhysicsSpace().add(hammerNode);
        
        // Joint node containing right wall of tray and tray bottom
        joint=new HingeJoint(holderNode.getControl(RigidBodyControl.class), hammerNode.getControl(RigidBodyControl.class), new Vector3f(0f,0f,-traySize/2-0.2f), new Vector3f(traySize,0f,0f), Vector3f.UNIT_Y, Vector3f.UNIT_Y);
        //joint.setLimit(0f, (float)Math.PI/2f);
        bulletAppState.getPhysicsSpace().add(joint);
    }
    
    /**
     * creates an empty node with a RigidBodyControl
     * @param manager
     * @param shape
     * @param mass
     * @return
     */
    public static Node createPhysicsTestNode(String name, CollisionShape shape, float mass) {
        Node node = new Node(name);
        RigidBodyControl control = new RigidBodyControl(shape, mass);
        node.addControl(control);
        return node;
    }
    
}
