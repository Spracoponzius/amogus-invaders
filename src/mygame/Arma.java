/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.ui.Picture;

/**
 *
 * @author Nicolò
 */
public class Arma {
    private Picture sprite;
    private int dmg;
    
    public Arma(String s)
    {
        if(s.equalsIgnoreCase("Pistola")){
            dmg = 100;
        }
        else if(s.equalsIgnoreCase("Fucile")){
            dmg = 200;
        }
    }
    
    public void setSprite(Picture p){
        sprite = p;
    }
    
    public void setDmg(String s){
        if(s.equalsIgnoreCase("Pistola")){
            dmg = 100;
        }
         else if(s.equalsIgnoreCase("Fucile")){
             dmg = 200;
         }
    }
    
    public Picture getSprite(){
        return sprite;
    }
    
    public int getDmg(){
        return dmg;
    }
    
}
