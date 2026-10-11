/* SPDX-License-Identifier: GPL-3.0-or-later. Original game (C) 2021-2025 M374LX. */
package game.alexvsbus;
import megalodonte.engine.*;
public final class Ending implements GameScript {
    private double time;
    @Override public void onStart(GameContext c){c.scene().musicVolume=c.state().getOrDefault("music","true").equals("false")?0:.55;c.findHud("ending-title").text="ALEX VENCEU A CORRIDA!  SCORE: "+c.state().getOrDefault("score","0");}
    @Override public void onUpdate(double dt,GameContext c){
        if(c.actionPressed("menu")||c.actionPressed("enter")||c.actionPressed("escape")||c.actionPressed("pad_a")||c.actionPressed("pad_b")||c.actionPressed("pad_start")){c.changeScene("menu");return;}
        time+=dt;var player=c.findObject("alex");var flag=c.findObject("flag");var hen=c.findObject("hen");
        if(player.x<50)player.x+=8.75*dt;else c.animate(player,"stand");
        if(player.x>=flag.x)c.animate(flag,"wave");
        if(time>2){hen.visible=true;if(hen.x<49)hen.x+=15*dt;}
        boolean trafficMoving=time%4<1.5&&time>2;
        if(trafficMoving&&time<12)for(var e:c.scene().objects)if(e.name.equals("traffic")||e.name.equals("bus"))e.x+=3*dt;
    }
}
