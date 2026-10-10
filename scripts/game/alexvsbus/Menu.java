/* SPDX-License-Identifier: GPL-3.0-or-later
 * Original game (C) 2021-2025 M374LX. Megalodonte adaptation.
 */
package game.alexvsbus;
import megalodonte.engine.*;


public final class Menu implements GameScript {
    private String page="main",difficulty="normal";

    @Override public void onStart(GameContext c){page=c.scene().id.equals("menu")?"main":c.scene().id.substring("menu-".length());difficulty=c.state().getOrDefault("difficulty","normal");c.scene().musicAssetPath=c.state().getOrDefault("menuMusic","assets/bgmtitle.mp3");c.pause(false);refresh(c);}
    private boolean enabled(GameContext c,String key){return !c.state().getOrDefault(key,key.equals("scanlines")?"false":"true").equals("false");}
    private void sound(GameContext c){if(enabled(c,"sfx"))c.playSound("assets/select.wav");}
    private void open(GameContext c,String next){sound(c);c.changeScene(next.equals("main")?"menu":"menu-"+next);}
    private void back(GameContext c){open(c,switch(page){case "levels"->"difficulty";case "display","sound"->"settings";case "vscreen"->"display";case "credits"->"about";default->"main";});}
    private boolean pressed(GameContext c,String id){return c.actionPressed(page.equals("main")?id:page+"-"+id);}
    private void toggle(GameContext c,String key){c.state().put(key,""+!enabled(c,key));sound(c);refresh(c);}
    @Override public void onUpdate(double dt,GameContext c){
        if(c.actionPressed("escape")||pressed(c,"back")){back(c);return;}
        if(c.actionPressed("f9")){toggle(c,"audio");return;}
        switch(page){
            case "main" -> {
                if(pressed(c,"audio"))toggle(c,"audio");
                else if(pressed(c,"start")||c.actionPressed("enter")||c.actionPressed("space"))open(c,"difficulty");
                else for(String next:new String[]{"jukebox","settings","about","quit"})if(pressed(c,next)){open(c,next);break;}
            }
            case "difficulty" -> {
                for(String choice:new String[]{"normal","hard","super"})if(pressed(c,choice)){difficulty=choice;c.state().put("difficulty",difficulty);open(c,"levels");break;}
                if(c.actionPressed("enter")){difficulty="normal";c.state().put("difficulty",difficulty);open(c,"levels");}
            }
            case "levels" -> {
                int count=difficulty.equals("super")?3:5;
                for(int i=1;i<=count;i++)if(pressed(c,"level"+i)||c.actionPressed("digit"+i)||(i==1&&c.actionPressed("enter"))){c.state().put("difficulty",difficulty);c.state().put("score","0");sound(c);c.changeScene(difficulty+"-"+i);break;}
            }
            case "settings" -> {if(pressed(c,"display"))open(c,"display");else if(pressed(c,"sound"))open(c,"sound");else if(pressed(c,"touch"))toggle(c,"touch");}
            case "display" -> {if(pressed(c,"size"))open(c,"vscreen");else if(pressed(c,"scanlines"))toggle(c,"scanlines");}
            case "sound" -> {for(String key:new String[]{"audio","music","sfx"})if(pressed(c,key)){toggle(c,key);break;}}
            case "vscreen" -> {
                if(pressed(c,"mode"))toggle(c,"auto");
                if(pressed(c,"width"))cycle(c,"width",new int[]{480,432,416,320,256});
                if(pressed(c,"height"))cycle(c,"height",new int[]{270,256,240,224,192});
            }
            case "about" -> {if(pressed(c,"credits"))open(c,"credits");}
            case "jukebox" -> {for(int i=0;i<4;i++)if(pressed(c,"track"+i)){c.scene().musicAssetPath="assets/"+(i==0?"bgmtitle":"bgm"+i)+".mp3";c.state().put("menuMusic",c.scene().musicAssetPath);sound(c);}}
            case "quit" -> {if(pressed(c,"yes"))c.quit();else if(pressed(c,"no"))back(c);}
        }
    }
    private void cycle(GameContext c,String key,int[] values){int value=Integer.parseInt(c.state().getOrDefault(key,""+values[0]));for(int i=0;i<values.length;i++)if(values[i]==value){c.state().put(key,""+values[(i+1)%values.length]);break;}c.state().put("auto","false");refresh(c);}
    private void variant(GameContext c,String id,String suffix){c.scene().hud.stream().filter(h->h.id.equals(id)).findFirst().ifPresent(h->h.assetPath="assets/menu/"+id+"-"+suffix+".png");}
    private void refresh(GameContext c){
        for(var h:c.scene().hud)h.visible=page.equals("main")?!h.id.contains("-"):h.id.startsWith(page+"-");
        if(page.equals("levels"))for(int i=1;i<=5;i++)c.findHud("levels-level"+i).visible=i<=(difficulty.equals("super")?3:5);
        c.mute(!enabled(c,"audio"));c.state().put("muted",""+!enabled(c,"audio"));c.scene().musicVolume=enabled(c,"music")?.5:0;
        if(page.equals("main"))c.findHud("audio").assetPath="assets/menu/main-audio"+(enabled(c,"audio")?"":"-off")+".png";
        for(String key:new String[]{"audio","music","sfx"})variant(c,"sound-"+key,enabled(c,key)?"on":"off");
        variant(c,"settings-touch",enabled(c,"touch")?"on":"off");variant(c,"display-scanlines",c.state().getOrDefault("scanlines","false").equals("true")?"on":"off");
        variant(c,"vscreen-mode",enabled(c,"auto")?"auto":"fixed");variant(c,"vscreen-width",c.state().getOrDefault("width","480"));variant(c,"vscreen-height",c.state().getOrDefault("height","270"));
    }
}
