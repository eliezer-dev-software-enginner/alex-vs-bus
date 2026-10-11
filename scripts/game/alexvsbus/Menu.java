/* SPDX-License-Identifier: GPL-3.0-or-later
 * Original game (C) 2021-2025 M374LX. Megalodonte adaptation.
 */
package game.alexvsbus;
import megalodonte.engine.*;


public final class Menu implements GameScript {
    private String page="main",difficulty="normal",focus="";
    private boolean transferBusy;

    @Override public void onStart(GameContext c){page=c.scene().hud.stream().filter(h->h.id.endsWith("-back")).map(h->h.id.substring(0,h.id.length()-5)).findFirst().orElse("main");difficulty=c.state().getOrDefault("difficulty","normal");c.scene().musicAssetPath=c.state().getOrDefault("menuMusic","assets/bgmtitle.mp3");c.pause(false);refresh(c);}
    private boolean enabled(GameContext c,String key){return !c.state().getOrDefault(key,key.equals("scanlines")?"false":"true").equals("false");}
    private void sound(GameContext c){if(enabled(c,"sfx"))c.playSound("assets/select.wav");}
    private void open(GameContext c,String next){sound(c);c.changeScene(next.equals("main")?"menu":"menu-"+next);}
    private void back(GameContext c){open(c,switch(page){case "levels"->"difficulty";case "display","sound"->"settings";case "vscreen"->"display";case "credits"->"about";default->"main";});}
    private boolean pressed(GameContext c,String id){return c.actionPressed(page.equals("main")?id:page+"-"+id);}
    private void toggle(GameContext c,String key){c.state().put(key,""+!enabled(c,key));sound(c);refresh(c);Progress.save(c);}
    @Override public void onUpdate(double dt,GameContext c){
        if(transferBusy)return;
        navigateController(c);
        if(c.actionPressed("pad_b")&&page.equals("main")){open(c,"quit");return;}
        if(c.actionPressed("pad_b")||c.actionPressed("escape")||pressed(c,"back")){back(c);return;}
        if(c.actionPressed("f9")){toggle(c,"audio");return;}
        switch(page){
            case "main" -> {
                if(pressed(c,"audio"))toggle(c,"audio");
                else if(pressed(c,"start")||c.actionPressed("enter")||c.actionPressed("space"))open(c,"difficulty");
                else for(String next:new String[]{"jukebox","settings","about","quit"})if(pressed(c,next)){open(c,next);break;}
            }
            case "difficulty" -> {
                for(String choice:new String[]{"normal","hard","super"})if(c.findHud("difficulty-"+choice).booleanProperty("liberado",false)&&pressed(c,choice)){difficulty=choice;c.state().put("difficulty",difficulty);open(c,"levels");break;}
                if(c.actionPressed("enter")){difficulty="normal";c.state().put("difficulty",difficulty);open(c,"levels");}
            }
            case "levels" -> {
                int count=difficulty.equals("super")?3:5;
                for(int i=1;i<=count;i++)if(c.findHud("levels-level"+i).booleanProperty("liberado",false)&&(pressed(c,"level"+i)||c.actionPressed("digit"+i)||(i==1&&c.actionPressed("enter")))){c.state().put("difficulty",difficulty);c.state().put("score","0");sound(c);c.changeScene(difficulty+"-"+i);break;}
            }
            case "settings" -> {if(pressed(c,"display"))open(c,"display");else if(pressed(c,"sound"))open(c,"sound");else if(pressed(c,"touch"))toggle(c,"touch");else if(pressed(c,"export")){transferBusy=true;c.exportSave(Progress.portable(c),message->{transferBusy=false;status(c,message);});}else if(pressed(c,"import")){transferBusy=true;c.importSave(Progress::validate,message->{transferBusy=false;difficulty=c.state().getOrDefault("difficulty","normal");c.scene().musicAssetPath=c.state().getOrDefault("menuMusic","assets/bgmtitle.mp3");refresh(c);status(c,message);});}}
            case "display" -> {if(pressed(c,"size"))open(c,"vscreen");else if(pressed(c,"scanlines"))toggle(c,"scanlines");}
            case "sound" -> {for(String key:new String[]{"audio","music","sfx"})if(pressed(c,key)){toggle(c,key);break;}}
            case "vscreen" -> {
                if(pressed(c,"mode"))toggle(c,"auto");
                if(pressed(c,"width"))cycle(c,"width",new int[]{480,432,416,320,256});
                if(pressed(c,"height"))cycle(c,"height",new int[]{270,256,240,224,192});
            }
            case "about" -> {if(pressed(c,"credits"))open(c,"credits");}
            case "jukebox" -> {for(int i=0;i<4;i++)if(pressed(c,"track"+i)){c.scene().musicAssetPath="assets/"+(i==0?"bgmtitle":"bgm"+i)+".mp3";c.state().put("menuMusic",c.scene().musicAssetPath);sound(c);Progress.save(c);}}
            case "quit" -> {if(pressed(c,"yes"))c.quit();else if(pressed(c,"no"))back(c);}
        }
    }
    private void status(GameContext c,String message){c.findHud("settings-data-status").text=message;c.log(message);}
    private void navigateController(GameContext c){
        var choices=c.scene().hud.stream().filter(h->h.visible&&h.type.equals("button")&&HudPresentation.enabled(h)).toList();if(choices.isEmpty())return;
        boolean input=c.actionPressed("pad_up")||c.actionPressed("pad_down")||c.actionPressed("pad_left")||c.actionPressed("pad_right")||c.actionPressed("pad_a")||c.actionPressed("pad_start");
        if(!input&&focus.isEmpty())return;
        int index=0;for(int i=0;i<choices.size();i++)if(choices.get(i).id.equals(focus))index=i;
        if(focus.isEmpty()){index=choices.get(0).id.endsWith("-back")&&choices.size()>1?1:0;}
        else if(c.actionPressed("pad_down")||c.actionPressed("pad_right"))index=(index+1)%choices.size();
        else if(c.actionPressed("pad_up")||c.actionPressed("pad_left"))index=(index+choices.size()-1)%choices.size();
        focus=choices.get(index).id;for(var h:c.scene().hud)h.focused=h.id.equals(focus);
        if(c.actionPressed("pad_a")||c.actionPressed("pad_start")){var h=choices.get(index);c.pressAction(h.action.isBlank()?h.id:h.action);}
    }
    private void cycle(GameContext c,String key,int[] values){int value=Integer.parseInt(c.state().getOrDefault(key,""+values[0]));for(int i=0;i<values.length;i++)if(values[i]==value){c.state().put(key,""+values[(i+1)%values.length]);break;}c.state().put("auto","false");refresh(c);Progress.save(c);}
    private void variant(GameContext c,String id,String suffix){c.scene().hud.stream().filter(h->h.id.equals(id)).findFirst().ifPresent(h->h.assetPath="assets/menu/"+id+"-"+suffix+".png");}
    private void refresh(GameContext c){
        for(var h:c.scene().hud)h.visible=page.equals("main")?!h.id.contains("-"):h.id.startsWith(page+"-");
        if(page.equals("levels"))for(int i=1;i<=5;i++){var h=c.findHud("levels-level"+i);h.visible=i<=(difficulty.equals("super")?3:5);h.setBooleanProperty("liberado",h.booleanProperty("liberado",false)||i<=Progress.level(c,difficulty));}
        if(page.equals("difficulty"))for(String name:Progress.DIFFICULTIES){var h=c.findHud("difficulty-"+name);h.setBooleanProperty("liberado",h.booleanProperty("liberado",false)||Progress.rank(name)<=Progress.highest(c));}
        c.mute(!enabled(c,"audio"));c.state().put("muted",""+!enabled(c,"audio"));c.scene().musicVolume=enabled(c,"music")?.5:0;
        if(page.equals("main"))c.findHud("audio").assetPath="assets/menu/main-audio"+(enabled(c,"audio")?"":"-off")+".png";
        for(String key:new String[]{"audio","music","sfx"})variant(c,"sound-"+key,enabled(c,key)?"on":"off");
        variant(c,"settings-touch",enabled(c,"touch")?"on":"off");variant(c,"display-scanlines",c.state().getOrDefault("scanlines","false").equals("true")?"on":"off");
        variant(c,"vscreen-mode",enabled(c,"auto")?"auto":"fixed");variant(c,"vscreen-width",c.state().getOrDefault("width","480"));variant(c,"vscreen-height",c.state().getOrDefault("height","270"));
    }
}
