/* SPDX-License-Identifier: GPL-3.0-or-later */
package game.alexvsbus;
import megalodonte.engine.*;
final class Progress {
    static final String[] DIFFICULTIES={"normal","hard","super"};
    static int rank(String difficulty){return java.util.List.of(DIFFICULTIES).indexOf(difficulty);}
    static int highest(GameContext c){return Math.max(0,Math.min(2,Integer.parseInt(c.state().getOrDefault("progressDifficulty","0"))));}
    static int level(GameContext c,String difficulty){return rank(difficulty)<highest(c)?(difficulty.equals("super")?3:5):rank(difficulty)>highest(c)?0:Integer.parseInt(c.state().getOrDefault("progressLevel","1"));}
    static void complete(GameContext c){String[] parts=c.scene().id.split("-");int rank=rank(parts[0]),level=Integer.parseInt(parts[1]);int max=rank==2?3:5;
        if(rank==highest(c)&&level==level(c,parts[0])){if(level<max)c.state().put("progressLevel",""+(level+1));else if(rank<2){c.state().put("progressDifficulty",""+(rank+1));c.state().put("progressLevel","1");}save(c);}}
    static java.util.Map<String,String> portable(GameContext c){var result=new java.util.HashMap<>(c.state());result.remove("score");return validate(result);}
    static java.util.Map<String,String> validate(java.util.Map<String,String> values){
        var allowed=java.util.Set.of("progressDifficulty","progressLevel","difficulty","audio","music","sfx","touch","scanlines","auto","width","height","menuMusic","muted");
        for(var e:values.entrySet()){
            String k=e.getKey(),v=e.getValue();if(!allowed.contains(k))throw new IllegalArgumentException("Propriedade desconhecida: "+k);
            if(java.util.Set.of("audio","music","sfx","touch","scanlines","auto","muted").contains(k)&&!java.util.Set.of("true","false").contains(v))throw new IllegalArgumentException("Booleano inválido: "+k);
            if(k.equals("difficulty")&&!java.util.List.of(DIFFICULTIES).contains(v))throw new IllegalArgumentException("Dificuldade inválida");
            if(k.equals("width")&&!java.util.Set.of("480","432","416","320","256").contains(v))throw new IllegalArgumentException("Largura inválida");
            if(k.equals("height")&&!java.util.Set.of("270","256","240","224","192").contains(v))throw new IllegalArgumentException("Altura inválida");
            if(k.equals("menuMusic")&&!java.util.Set.of("assets/bgmtitle.mp3","assets/bgm1.mp3","assets/bgm2.mp3","assets/bgm3.mp3").contains(v))throw new IllegalArgumentException("Música inválida");
        }
        int rank=Integer.parseInt(values.getOrDefault("progressDifficulty","0")),level=Integer.parseInt(values.getOrDefault("progressLevel","1"));
        if(rank<0||rank>2||level<1||level>(rank==2?3:5))throw new IllegalArgumentException("Progresso inválido");
        return values;
    }
    static void save(GameContext c){String score=c.state().remove("score");c.saveState();if(score!=null)c.state().put("score",score);}
}
