/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

/**
 *
 * @author Nick
 */
public class Enemy {
    private int hp=100;
    
    public Enemy(){}
    
    public Enemy(float molt){
        hp= (int) (100*molt);
    }
    
    public int getHp(){
        return hp;
    }
    
    public void hurt(int dmg){
        hp = hp-dmg;
    }
}
