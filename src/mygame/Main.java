package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.ui.Picture;
import com.jme3.util.SkyFactory;
import java.awt.Font;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

/** Sample 8 - how to let the user pick (select) objects in the scene
 * using the mouse or key presses. Can be used for shooting, opening doors, etc. */
public class Main extends SimpleApplication {

  public static void main(String[] args) {
    Main app = new Main();
    app.start();
  }
  
  //nodo degli elementi sparabili
  private Node shootables;
  //nodo delle armi
  private Node weapons;
  //nodo del pavimento principale
  private Node floor;
  //lista dei cubi da aggiungere agli sparabili
  private LinkedList<Geometry> cubi;
  private LinkedList<Enemy> nemici;
  
  //audio
  private AudioNode boom;
  private AudioNode amogus;
  private AudioNode winSound;
  private AudioNode failSound;
  
  //etichetta per visualizzare i punti
  private BitmapText hudText;
  private BitmapText roundText;
  private BitmapText winText;
  private BitmapText lifeText;
  private BitmapFont fnt;
  private int rnd = 1;
  private int punti = 0;
  private Giocatore player;
  private boolean winStat;
  
  private Picture armaSprite;
  private Arma arma = new Arma("Pistola");
  @Override
  public void simpleInitApp() {
    cubi = new LinkedList<Geometry>();
    nemici = new LinkedList<Enemy>();
    initCrossHairs();// puntatore
    initKeys();// tasti utili
    
    //audio dello sparo, inizio round e vittoria
    boom = new AudioNode(assetManager, "Sounds/boom.ogg", DataType.Buffer);
    boom.setPositional(false);
    
    amogus = new AudioNode(assetManager, "Sounds/amogus.ogg", DataType.Buffer);
    amogus.setPositional(false);
    
    winSound = new AudioNode(assetManager, "Sounds/siuu.ogg", DataType.Buffer);
    winSound.setPositional(false);
    
    failSound = new AudioNode(assetManager, "Sounds/fail.ogg", DataType.Buffer);
    failSound.setPositional(false);
    
    
    //inizializzazione giocatore
    player=new Giocatore();
    winStat = true;
    
    //skybox
    getRootNode().attachChild(SkyFactory.createSky(getAssetManager(), "Textures/Sky/Bright/BrightSky.dds", SkyFactory.EnvMapType.CubeMap));

    //inizializzazione generale
    Vector3f pos = new Vector3f(0, (float) 2.5,10);  //posizione camera per apparire sopra la torre
    cam.setLocation(pos);
    flyCam.setMoveSpeed(0);         //disabilita il movimento per rendere il gioco stazionario
    
    
    //creo i componenti e li metto nel nodo da appendere alla radice dell'albero grafico
    //la radice è ereditata dalla classe SimpleApplication
    shootables = new Node("Shootables");
    floor = new Node("Floor");
    rootNode.attachChild(shootables);
    rootNode.attachChild(floor);//creo il pavimento agganciandolo al nodo radice della grafica (non è sparabile)
    rootNode.attachChild(makeAmogusFloor());
    rootNode.attachChild(makeTower());
    floor.attachChild(makeFloor());
    
    //inizia il primo round
    round(rnd);
    
    //font
    fnt = assetManager.loadFont("Interface/Fonts/Stencil.fnt");
    fnt.setStyle(Font.PLAIN);
    
    //testo: va bene per i punti attuali e per i dati dei record caricati dai file
    hudText = new BitmapText(fnt, false);
    hudText.setSize(fnt.getCharSet().getRenderedSize());
    hudText.setLocalTranslation(0, 50, 0);
    guiNode.attachChild(hudText);
    hudText.setText("Punti: " + punti);
    //fine testo punti
    
    //testo round
    roundText = new BitmapText(fnt, false);
    roundText.setSize(fnt.getCharSet().getRenderedSize());
    roundText.setLocalTranslation(0, 75, 0);
    guiNode.attachChild(roundText);
    roundText.setText("Round: "+rnd);
    //fine testo round
    
    //testo con vita del giocatore
    lifeText = new BitmapText(fnt, false);
    lifeText.setSize(fnt.getCharSet().getRenderedSize());
    lifeText.setLocalTranslation(0, 100, 0);
    guiNode.attachChild(lifeText);
    lifeText.setText("HP: "+player.getHp());
    //setup armi
    weapons = new Node("Weapons");
    rootNode.attachChild(weapons);
    armaSprite = new Picture("arma");
    changeToPistol();
  }

  //definisco quali interazioni mi interessano
  private void initKeys() {
    inputManager.addMapping("Shoot",//l'evento si chiama Shoot 
    new KeyTrigger(KeyInput.KEY_SPACE),//sparo con la barra
    new MouseButtonTrigger(MouseInput.BUTTON_LEFT));//click del mouse
    inputManager.addListener(actionListener, "Shoot");//aggancio l'ascoltatore
    
    //l'arma viene cambiato al fucile, se disponibile (aggiungere condizione dei round)
    inputManager.addMapping("Chg_shot", new KeyTrigger(KeyInput.KEY_2));
    inputManager.addListener(actionListener, "Chg_shot");
    
    //cambio arma alla pistola
    inputManager.addMapping("Chg_pist", new KeyTrigger(KeyInput.KEY_1));
    inputManager.addListener(actionListener, "Chg_pist");
  }
  
  //creo la classe che ascolta l'interazione da parte dell'utente
  //uso final per evitare che si possano creare altre classi figle da essa
  //riduco i rischi di cattiva programmazione, 
  //perchè in realtà io compio azioni in base a cosa l'utente decide di fare
  private final ActionListener actionListener = new ActionListener() {

    @Override
    public void onAction(String name, boolean keyPressed, float tpf) {
      
        if (name.equals("Shoot") && !keyPressed) {
        
        //ho sparato creo la lista delle eventuali collisioni
        CollisionResults results = new CollisionResults();
        //creo il raggio dello sparo basandomi sulla posizione e la direzione della cam, ovvero da dove sono
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());
        //calcolo le collisioni e salvo i risultati
        shootables.collideWith(ray, results);
        boom.setVolume((float) 0.1);
        boom.playInstance();
        //se ho almeno una collisione
        if(results.size() > 0){
            //cerco la collisione più vicina
            CollisionResult closest = results.getClosestCollision();
            //ottengo il nome dell'oggetto che rappresenta la collisione più vicina
            String hit = closest.getGeometry().getName();
            
            //test del danneggiamento
            nemici.get(cubi.indexOf(closest.getGeometry())).hurt(arma.getDmg());
            if(nemici.get(cubi.indexOf(closest.getGeometry())).getHp()<=0){
                //elimino l'oggetto colpito
                nemici.remove(cubi.indexOf(closest.getGeometry()));
                shootables.detachChildNamed(hit);
                cubi.remove(closest.getGeometry());
            }
            //aggiorno i punti
            punti++;
        }
      }
        if(name.equals("Chg_shot") && rnd>2){
            changeToShotgun();
        }
        
        if(name.equals("Chg_pist")){
            changeToPistol();
        }
    }
  };
  
  //metodi per cambiare le armi
  public void changeToShotgun(){
      armaSprite.setImage(assetManager, "Materials/shotgun.png", true);
      armaSprite.setHeight(500);
      armaSprite.setPosition(700,0);
      arma.setSprite(armaSprite);
      arma.setDmg("Fucile");
      weapons.detachAllChildren();
      weapons.attachChild(armaSprite);
      guiNode.attachChild(weapons);
  }
  
  public void changeToPistol(){
     armaSprite.setImage(assetManager, "Materials/gun.png", true);
     armaSprite.setWidth(500);
     armaSprite.setHeight(settings.getHeight()/4);
     armaSprite.setPosition(700, 0);
     arma.setSprite(armaSprite);
     arma.setDmg("Pistola");
     weapons.detachAllChildren();
     weapons.attachChild(armaSprite);
     guiNode.attachChild(weapons);
  }

  //creo il cubo sparabile
  protected Geometry makeCube(String name, float x, float y, float z) {
    Box box = new Box(1, 1, 0);
    Geometry cube = new Geometry(name, box);
    
    //genera un cubo su lato random, con posizione variabile
    cube.setLocalTranslation(x, y, z);
    Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat1.setTexture("ColorMap", assetManager.loadTexture("Materials/amogus.png"));
    mat1.setColor("Color", ColorRGBA.randomColor());
    //impostazione trasparenza
    mat1.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
    cube.setQueueBucket(Bucket.Transparent);
    cube.setMaterial(mat1);
    return cube;
  }

  //creatore del mirino
  protected void initCrossHairs() {
    setDisplayStatView(false);
    guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
    BitmapText ch = new BitmapText(guiFont, false);
    ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
    ch.setText("+"); // crosshairs
    ch.setLocalTranslation( // center
      settings.getWidth() / 2 - ch.getLineWidth()/2,
      settings.getHeight() / 2 + ch.getLineHeight()/2, 0);
    guiNode.attachChild(ch);
  }
  
  //creatore del pavimento
  protected Geometry makeFloor() {
    Box box = new Box(15, .2f, 15);
    Geometry floor = new Geometry("floor", box);
    floor.setLocalTranslation(0, -2, 10);       //trasla il pavimento in modo che il giocatore sia al centro
    Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat1.setTexture("LightMap", assetManager.loadTexture("Materials/doomfloor.jpg"));
    //mat1.setColor("Color", ColorRGBA.Gray);
    floor.setMaterial(mat1);
    return floor;
  }
  
  protected Geometry makeTower() {
    Box box = new Box(2, (float) 3.5, 2);
    Geometry tower = new Geometry("floor", box);
    tower.setLocalTranslation(0, -2, 10);       //trasla il pavimento in modo che il giocatore sia al centro
    Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat1.setTexture("LightMap", assetManager.loadTexture("Materials/Torretta.png"));
    //mat1.setColor("Color", ColorRGBA.Gray);
    tower.setMaterial(mat1);
    return tower;
  }
  
  protected Geometry makeAmogusFloor(){
    Box box = new Box(70, .2f, 70);
    Geometry floor = new Geometry("floor", box);
    floor.setLocalTranslation(0, -3, 10);
    Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat1.setTexture("LightMap", assetManager.loadTexture("Materials/acid.png"));
    mat1.setColor("Color", ColorRGBA.Gray);
    floor.setMaterial(mat1);
    return floor;
  }
  
  //aggiorno il gioco
  @Override
  public void simpleUpdate(float tpf) {
      //come se usassi l'iteratore (scorro per tutti gli elementi della lista cubo e estraggo il riferimento all'elemento della lista
      //è solo una sintassi compatta
      
      for (Geometry g : cubi) {
          Random r = new Random();
          g.move(0, 0, ((r.nextInt(4-1)+1)+1)*tpf);//il cubo si avvicina dal fondo
      }
      
      if(!cubi.isEmpty()){
         if(cubi.get(0).getLocalTranslation().z>-5){
              for(int i=0;i<cubi.size();i++)
              {
                  player.danno();
                  nemici.remove(i);
                  cubi.remove(i);
              }
              shootables.detachAllChildren();
              if(rnd==6){
                  player.bossDamage();
                  winStat = false;
              }
                
        }
      }

      if(cubi.size() == 0 && rnd<7)
      {
          rnd++;
          round(rnd);
      }
        //visualizzo i punti
        hudText.setText("Punti: " + punti);
        roundText.setText("Round: "+rnd);
        lifeText.setText("HP: "+player.getHp());
        
        //game over
        if(player.getHp()<=0)
        {
            winStat = false; 
            rootNode.detachAllChildren();
            guiNode.detachAllChildren();
            winText= new BitmapText(fnt, false);
            winText.setText("PUNTI: "+punti);
            winText.setSize(30);
            winText.setLocalTranslation(900, 200, 0);
            guiNode.attachChild(winText);
            Picture lost = new Picture("loss");
            lost.setImage(assetManager, "Materials/sconfitta.png", true);
            lost.setWidth(500);
            lost.setHeight(625);
            lost.setPosition(710, settings.getHeight()/4);
            guiNode.attachChild(lost);
            failSound.play();
        }
    }
  
  public void round(int r){
      switch(r)
      {
          case 1:
              for(int i = 0; i<10; i++){
                    cubi.add(makeCube("0"+i, xRoll(), yRoll(), -50f));
                    Enemy e = new Enemy();
                    nemici.add(e);
              }
              appendShootables();
              amogus.setVolume((float) 0.10);
              amogus.play();
              break;
          case 2:
              for(int i = 0; i<15; i++){
                  
                  cubi.add(makeCube("0"+i, xRoll(), yRoll(), -50f));
                  Enemy e = new Enemy((float) 1.5);
                  nemici.add(e);
              }
              appendShootables();
              amogus.setVolume((float) 0.10);
              amogus.play();
              break;
          case 3:
              for(int i = 0; i<20; i++){
                  cubi.add(makeCube("0"+i, xRoll(), yRoll(), -50f));
                  Enemy e = new Enemy((float) 1.75);
                  nemici.add(e);
              }
              appendShootables();
              changeToShotgun();
              amogus.setVolume((float) 0.10);
              amogus.play();
              break;
          case 4:
              for(int i = 0; i<25; i++){
                  cubi.add(makeCube("0"+i, xRoll(), yRoll(), -50f));
                  Enemy e = new Enemy((float) 2);
                  nemici.add(e);
              }
              appendShootables();
              amogus.setVolume((float) 0.10);
              amogus.play();
              break;
          case 5:
              for(int i = 0; i<30; i++){
                  cubi.add(makeCube("0"+i, xRoll(), yRoll(), -50f));
                  Enemy e = new Enemy((float) 2.1);
                  nemici.add(e);
              }
              appendShootables();
              amogus.setVolume((float) 0.10);
              amogus.play();
              break;
          case 6:
              //boss fight
                Box box = new Box(20, 20, 0);
                Geometry cube = new Geometry("boss", box);
                cube.setLocalTranslation(0,10,-50f);
                Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                mat1.setTexture("ColorMap", assetManager.loadTexture("Materials/amogus.png"));
                mat1.setColor("Color", ColorRGBA.randomColor());
                mat1.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
                cube.setQueueBucket(Bucket.Transparent);
                cube.setMaterial(mat1);
                cubi.add(cube);
                Enemy e = new Enemy((float)30);
                nemici.add(e);
                appendShootables();
                amogus.setPitch(.5f);
                amogus.setVolume((float) 0.10);
                amogus.play();
                break;
          default:
              //fine del gioco, messaggio di vittoria
              if(winStat){
                rootNode.detachAllChildren();
                guiNode.detachAllChildren();
                winText= new BitmapText(fnt, false);
                winText.setText("PUNTI: "+punti);
                winText.setSize(30);
                winText.setLocalTranslation(900, 200, 0);
                guiNode.attachChild(winText);
                Picture win = new Picture("pepsiman");
                win.setImage(assetManager, "Models/pepsiman.png", true);
                win.setWidth(500);
                win.setHeight(625);
                win.setPosition(710, settings.getHeight()/4);
                guiNode.attachChild(win);
                winSound.setReverbEnabled(true);
                winSound.play();
              }
              break;
      }
  }
  
  //genera randomicamente le coordinate di spawn
  public int xRoll(){
      Random r = new Random();
      return r.nextInt((15-(-15))+1)-15;
  }
  
  public int yRoll(){
      Random r = new Random();
      return r.nextInt(5);
  }
  
  //funzione per agganciare gli amogus al nodo degli shootables
  public void appendShootables(){
      Iterator it = cubi.iterator();
    while(it.hasNext()){
        Geometry g = (Geometry)it.next();
        shootables.attachChild(g);
    }
  }

}