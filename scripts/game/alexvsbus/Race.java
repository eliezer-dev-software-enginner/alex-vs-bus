/* SPDX-License-Identifier: GPL-3.0-or-later
 * Megalodonte adaptation of Alex vs Bus gameplay and levels.
 * Original design/programming/assets (C) 2021-2025 M374LX.
 */
package game.alexvsbus;
import megalodonte.engine.*;
import java.util.*;

public final class Race implements GameScript {
    private Game.Entity player,bus;
    private double vx,vy,time=90,clock,jumpBuffer,coyote,hurt,invincible,respawnDelay,checkpointX=.125,checkpointY=7-1.0/6,winTime,bonusSeconds;
    private int score,goalScene;
    private String phase="run",ropeId="",lastRope="";
    private final Map<String,Double> springCooldown=new HashMap<>();
    private void sound(GameContext c,String path){if(!c.state().getOrDefault("sfx","true").equals("false"))c.playSound(path);}
    @Override public void onStart(GameContext c){
        player=c.findObject("alex");bus=c.findObject("bus");score=Integer.parseInt(c.state().getOrDefault("score","0"));goalScene=Integer.parseInt(player.properties.get("goalScene"));
        c.mute(Boolean.parseBoolean(c.state().getOrDefault("muted","false")));c.pause(false);c.scene().musicVolume=c.state().getOrDefault("music","true").equals("false")?0:.55;
        for(String id:new String[]{"left","right","jump"})c.findHud(id).visible=!c.state().getOrDefault("touch","true").equals("false");
        c.findHud("scanlines").visible=c.state().getOrDefault("scanlines","false").equals("true");
        if(c.state().getOrDefault("auto","true").equals("false")){c.scene().camera.width=Double.parseDouble(c.state().getOrDefault("width","480"))/24;c.scene().camera.height=Double.parseDouble(c.state().getOrDefault("height","270"))/24;}
        c.findHud("time-bonus").visible=false;c.animate(player,"stand");hideOverlay(c);c.findHud("level").text=c.scene().name;refresh(c);
    }
    @Override public void onUpdate(double dt,GameContext c){
        if(c.actionPressed("f9")){boolean muted=!Boolean.parseBoolean(c.state().getOrDefault("muted","false"));c.state().put("muted",""+muted);c.state().put("audio",""+!muted);c.mute(muted);}
        if(c.actionPressed("f10")){c.findHud("scanlines").visible=!c.findHud("scanlines").visible;c.state().put("scanlines",""+c.findHud("scanlines").visible);}
        if(c.actionPressed("menu")){c.pause(false);c.changeScene("menu");return;}
        if(c.actionPressed("retry")){c.state().put("score","0");c.pause(false);c.changeScene(c.scene().id);return;}
        if(phase.equals("pause")){
            if(c.actionPressed("pad_b")){c.pause(false);c.changeScene("menu");return;}
            if(c.actionPressed("resume")||c.actionPressed("escape")||c.actionPressed("pause")||c.actionPressed("pad_start")||c.actionPressed("pad_a")){phase="run";hideOverlay(c);c.pause(false);}return;
        }
        bonusSeconds=Math.max(0,bonusSeconds-dt);c.findHud("time-bonus").visible=bonusSeconds>0;
        if(phase.equals("timeup")){bus.x+=dt*12;return;}
        if(phase.equals("win")){finish(dt,c);return;}
        if(c.actionPressed("escape")||c.actionPressed("pause")||c.actionPressed("pad_start")){phase="pause";overlay(c,"PAUSADO",true);c.pause(true);return;}
        clock+=dt;time=Math.max(0,time-dt);
        if(time<=0){phase="timeup";overlay(c,"TEMPO ESGOTADO! O ÔNIBUS PARTIU.",false);sound(c,"assets/time.wav");refresh(c);return;}
        if(time<10&&((int)(time+dt)!=(int)time))sound(c,"assets/time.wav");
        invincible=Math.max(0,invincible-dt);hurt=Math.max(0,hurt-dt);
        player.visible=invincible<=0||((int)(clock*12)%2==0);
        if(respawnDelay>0){respawnDelay-=dt;refresh(c);return;}
        double direction=Math.max(-1,Math.min(1,c.horizontal()+(c.actionDown("right")?1:0)-(c.actionDown("left")?1:0)));
        boolean jump=c.actionPressed("jump")||c.actionPressed("space")||c.actionPressed("pad_a");
        jumpBuffer=jump?.2:Math.max(0,jumpBuffer-dt);
        boolean grounded=c.grounded(player);coyote=grounded?.1:Math.max(0,coyote-dt);
        if(!ropeId.isEmpty()){
            var rope=c.findObject(ropeId);player.x+=10.75*dt;player.y=rope.y-1.1;c.animate(player,"rope");
            if(jump||player.x>=Double.parseDouble(rope.properties.get("endX"))){lastRope=ropeId;ropeId="";vy=6.5;vx=8.75;c.animate(player,"jump");}
        }else{
            if(hurt<=0){vx=approach(vx,direction*(direction<0?3.75:8.75),dt*(direction==0?10.5:9));if(direction!=0)player.flipX=direction<0;}
            if(jumpBuffer>0&&coyote>0&&hurt<=0){vy=6.5;jumpBuffer=0;coyote=0;grounded=false;}
            vy=Math.max(-12.5,vy-9.75*dt);
            var movement=c.move(player,vx*dt,vy*dt);
            if(movement.blockedY()){if(vy<0){grounded=true;coyote=.1;}vy=0;}
            if(movement.blockedX()){
                boolean pushed=false;
                for(var e:c.scene().objects)if(kind(e).equals("pushable")&&e.solid&&c.overlaps(player,e,.2)){
                    var push=c.move(e,Math.max(0,vx*dt),0);pushed=push.x()>0;
                    if(e.x-Double.parseDouble(e.properties.get("startX"))>.95){e.solid=false;e.visible=false;sound(c,"assets/crate.wav");}
                }
                if(pushed)c.move(player,vx*dt,0);else vx=0;
            }
            if(hurt<=0){player.width=32.0/24;c.animate(player,grounded?(Math.abs(vx)>.25?"walk":"stand"):"jump");}
        }
        interact(dt,c);
        if(player.x>=c.scene().worldWidth-5.2&&player.y>=6.7){
            phase="win";winTime=0;Progress.complete(c);score+=(int)Math.ceil(time)*10;c.state().put("score",""+score);player.visible=true;sound(c,"assets/score.wav");c.findHud("hint").text="Ônibus alcançado! +"+((int)Math.ceil(time)*10)+" pontos de tempo";
        }
        refresh(c);
    }
    private void interact(double dt,GameContext c){
        for(var e:List.copyOf(c.scene().objects)){
            String kind=kind(e);
            if(kind.equals("checkpoint")){if(player.x>=e.x&&e.x>checkpointX){checkpointX=e.x;checkpointY=e.y;}continue;}
            if(kind.equals("hole")){if(c.overlaps(player,e)){respawn(c);return;}continue;}
            if(kind.startsWith("trigger-")){trigger(e,dt,c);continue;}
            if(!e.visible)continue;
            if(kind.equals("gush")){
                e.y=Double.parseDouble(e.properties.get("baseY"))+.3*Math.sin(clock*5)+(clock%4>3?1:0);
            }
            if(kind.equals("spring")){
                springCooldown.computeIfPresent(e.id,(id,value)->Math.max(0,value-dt));if(e.animationTime>.25)e.animation="";
                if(c.overlaps(player,e,.08)&&vy<=0&&springCooldown.getOrDefault(e.id,0.0)<=0){vy=10.25;springCooldown.put(e.id,.4);c.animate(e,"bounce");e.animationTime=0;sound(c,"assets/spring.wav");}
            }else if(kind.equals("rope")&&!e.id.equals(lastRope)&&ropeId.isEmpty()&&hurt<=0&&c.overlaps(player,e)){
                ropeId=e.id;vy=0;vx=10.75;c.animate(player,"rope");
            }else if(kind.startsWith("coin")&&c.overlaps(player,e)){
                e.visible=false;int before=score;score+=kind.equals("coin-gold")?100:50;int bonus=15*(score/1000-before/1000);time+=bonus;if(bonus>0){bonusSeconds=1.5;var notice=c.findHud("time-bonus");notice.text="+ "+bonus+" seconds";notice.visible=true;}sound(c,"assets/coin.wav");
            }else if(kind.equals("banana")){
                if(e.properties.containsKey("velocityY")){
                    double speed=Double.parseDouble(e.properties.get("velocityY"))-9.75*dt;e.y+=speed*dt;e.x+=6*dt;e.properties.put("velocityY",""+speed);if(e.y<7){e.y=7;e.properties.remove("velocityY");}
                }
                if(invincible<=0&&hurt<=0&&c.overlaps(player,e)){e.visible=false;hurt=1;invincible=1.5;vx=-.5;vy=1;player.width=64.0/24;c.animate(player,"slip");sound(c,"assets/slip.wav");}
            }else if((kind.equals("gush")||kind.equals("gush-body")||kind.equals("gush-crack"))&&invincible<=0&&hurt<=0&&c.overlaps(player,e,.08)){
                hurt=.6;invincible=1.5;vx=-4.25;vy=6;ropeId="";c.animate(player,"hit");sound(c,"assets/hit.wav");
                if(kind.equals("gush-crack")){e.properties.put("kind","gush");e.properties.put("baseY",""+(e.y+.5));e.height=1.5;e.colliderOffsetX=3.0/24;e.colliderOffsetY=0;e.colliderWidth=9.0/24;e.colliderHeight=e.height;c.animate(e,"flow");}
            }
        }
    }
    private void trigger(Game.Entity e,double dt,GameContext c){
        if(!e.properties.containsKey("fired")&&player.x>=Double.parseDouble(e.properties.get("triggerX"))){e.properties.put("fired","true");e.visible=true;e.x=player.x-10;if(kind(e).equals("trigger-hen"))c.animate(e,"move");}
        if(!e.visible)return;
        boolean hen=kind(e).equals("trigger-hen");e.x+=(hen?15:50)*dt;
        if(!hen&&!e.properties.containsKey("thrown")&&e.x>=player.x-3){
            e.properties.put("thrown","true");var peel=new Game.Entity();peel.name="Banana arremessada";peel.assetPath="assets/banana_peel-0.png";peel.x=e.x+2;peel.y=e.y+e.height;peel.width=peel.height=1.0/3;peel.solid=true;peel.trigger=true;peel.colliderOffsetX=1.0/24;peel.colliderOffsetY=1.0/24;peel.colliderWidth=6.0/24;peel.colliderHeight=5.0/24;peel.properties.put("kind","banana");peel.properties.put("velocityY","3");c.scene().objects.add(peel);
        }
        if(hen&&invincible<=0&&c.overlaps(player,e)){hurt=.6;invincible=1.5;vx=-4;vy=6;c.animate(player,"hit");sound(c,"assets/hit.wav");}
        if(e.x>player.x+35)e.visible=false;
    }
    private void respawn(GameContext c){
        player.x=checkpointX;player.y=checkpointY;player.visible=true;player.width=32.0/24;vx=vy=0;ropeId="";lastRope="";respawnDelay=.5;invincible=1.5;c.animate(player,"stand");sound(c,"assets/fall.wav");
    }
    private void finish(double dt,GameContext c){
        winTime+=dt;String[] captions={"","Alex embarca a tempo!","Mais um passageiro chegou correndo.","Uma última casca de banana...","Até o pássaro resolveu participar!","O ônibus saiu. Alex corre atrás dele!"};
        c.findHud("hint").text=captions[goalScene]+"  SCORE: "+score;
        double door=bus.x+342.0/24;
        if(goalScene==3&&winTime<1){c.animate(player,"slip");player.width=64.0/24;}
        else if(goalScene==4&&winTime<1.6){c.animate(player,"clean");}
        else if(goalScene==5){c.animate(player,"run");player.width=32.0/24;player.x+=12*dt;bus.x+=16*dt;}
        else if(winTime<1){c.animate(player,"walk");player.x=approach(player.x,door,6*dt);}
        else{player.width=32.0/24;c.animate(player,"jump");player.x=door;player.y=7+Math.sin(Math.min(1,(winTime-1)/1.2)*Math.PI)*2;}
        if(goalScene!=5&&winTime>2.2){player.visible=false;bus.x+=10*dt;}
        if(goalScene==2&&winTime>1&&winTime<3){
            var man=c.scene().objects.stream().filter(e->kind(e).equals("late-man")).findFirst().orElse(null);
            if(man==null){man=new Game.Entity();man.name="Passageiro atrasado";man.assetPath="assets/bearded_man_stand-0.png";man.x=c.scene().worldWidth-1;man.y=7;man.width=32.0/24;man.height=64.0/24;man.solid=false;man.properties.put("kind","late-man");c.scene().objects.add(man);}man.x-=6*dt;
        }
        if(goalScene==4&&winTime<1.6){
            var bird=c.scene().objects.stream().filter(e->kind(e).equals("bird")).findFirst().orElse(null);
            if(bird==null){bird=new Game.Entity();bird.name="Pássaro";bird.assetPath="assets/bird-0.png";bird.x=player.x-5;bird.y=player.y+4;bird.width=2.0/3;bird.height=1.0/3;bird.solid=false;bird.properties.put("kind","bird");c.scene().objects.add(bird);}bird.x+=8*dt;
        }
        for(var e:c.scene().objects)if(kind(e).equals("bus-part")&&e.animations.containsKey("roll")&&winTime>2)c.animate(e,"roll");
        if(winTime>4){String[] parts=c.scene().id.split("-");int next=Integer.parseInt(parts[1])+1;int max=parts[0].equals("super")?3:5;c.changeScene(next<=max?parts[0]+"-"+next:"ending");}
    }
    private static String kind(Game.Entity e){return e.properties.getOrDefault("kind","");}
    private static double approach(double from,double to,double amount){return from<to?Math.min(to,from+amount):Math.max(to,from-amount);}
    private void refresh(GameContext c){c.findHud("score").text=String.format(java.util.Locale.ROOT,"SCORE: %06d",score);c.findHud("time").text="TIME: "+(int)Math.ceil(time);}
    private static void hideOverlay(GameContext c){for(String id:new String[]{"panel","notice","resume","retry","menu"})c.findHud(id).visible=false;}
    private static void overlay(GameContext c,String message,boolean resume){for(String id:new String[]{"panel","notice","retry","menu"})c.findHud(id).visible=true;c.findHud("resume").visible=resume;c.findHud("notice").text=message;}
}
