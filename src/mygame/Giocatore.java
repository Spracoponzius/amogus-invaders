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
public class Giocatore {
    private int hp = 100;
    
    public Giocatore(){}
    
    public void danno(){
        hp=hp-10;       //per ogni amogus che raggiunge il giocatore
    }                   //perde 10 hp
    
    public void bossDamage(){
        hp=hp-100;
    }
    
    public int getHp(){
        return hp;
    }
}
