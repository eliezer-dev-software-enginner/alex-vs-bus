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
    static void save(GameContext c){String score=c.state().remove("score");c.saveState();if(score!=null)c.state().put("score",score);}
}
