# SPDX-License-Identifier: GPL-3.0-or-later
"""Compose the original 8px bitmap type and border sprites into data driven HUDs."""
from PIL import Image, ImageOps
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
atlas=Image.open(ROOT/'upstream/assets/gfx.png').convert('RGBA')
r=json.loads((ROOT/'upstream/sprite-regions.json').read_text())
out=ROOT/'assets/menu';out.mkdir(exist_ok=True)
def sprite(name):
 x,y,w,h=r[name];return atlas.crop((x,y,x+w,y+h))
def text(im,s,x,y,color='WHITE'):
 ox,oy,_,_=r['CHARSET_'+color]
 for ch in s:
  n=ord(ch)-32
  if 0<=n<128:im.alpha_composite(atlas.crop((ox+n%16*8,oy+n//16*8,ox+n%16*8+8,oy+n//16*8+8)),(int(x),int(y)))
  x+=8

def panel(name,w,h,caption='',icon='',value='',gray=False,lines=None):
 im=Image.new('RGBA',(w,h),(0,0,0,255))
 left=sprite('MENU_BORDER_LEFT');top=sprite('MENU_BORDER_TOP');corner=sprite('MENU_BORDER_TOPLEFT')
 for x in range(8,w-8,8):
  im.alpha_composite(top,(x,0));im.alpha_composite(ImageOps.flip(top),(x,h-8))
 for y in range(8,h-8,8):
  im.alpha_composite(left,(0,y));im.alpha_composite(ImageOps.mirror(left),(w-8,y))
 for x,y,flipx,flipy in [(0,0,False,False),(w-8,0,True,False),(0,h-8,False,True),(w-8,h-8,True,True)]:
  tile=ImageOps.mirror(corner) if flipx else corner
  if flipy:tile=ImageOps.flip(tile)
  im.alpha_composite(tile,(x,y))
 if icon:
  tile=sprite(icon);im.alpha_composite(tile,((w-tile.width)//2,(h-tile.height)//2))
 if caption:text(im,caption,8,(h-8)//2)
 if value:text(im,value,w-8-len(value)*8,(h-8)//2)
 if lines:
  for s,x,y,color in lines:text(im,s,x,y,color)
 if gray:
  px=im.load()
  for y in range(h):
   for x in range(w):
    rr,gg,bb,aa=px[x,y]
    if rr>200 and gg>200 and bb>200:px[x,y]=(150,150,150,aa)
 im.save(out/(name+'.png'))
 return 'assets/menu/'+name+'.png'

def menus(hud):
 items=[];S=8/3
 def add(page,id,x,y,w,h,caption='',icon='',value='',gray=False,button=True,lines=None):
  name=page+'-'+id;path=panel(name,w,h,caption,icon,value,gray,lines)
  item=hud(id if page=='main' else page+'-'+id,'',(x+w/2-240)*S,y*S,w*S,h*S,type='button' if button else 'image',action=id if page=='main' else page+'-'+id,anchor='center',image=path)
  item['visible']=page=='main'
  if page in ['levels','difficulty'] and id not in ['back','header']:
   item['properties']={'liberado':id in ['level1','normal']}
   item['condition']={'property':'liberado','trueAssetPath':path if page=='levels' else 'assets/menu/'+name+'-unlocked.png','falseAssetPath':'assets/menu/'+name+'-locked.png','disableWhenFalse':True}
  items.append(item)
 def header(page,title):
  add(page,'back',8,8,40,40,icon='MENU_RETURN')
  add(page,'header',144,8,192,40,caption=title,button=False)
 def rows(page,labels):
  first=164-(len(labels)*24+(len(labels)-1)*8)/2
  for i,(id,label,val) in enumerate(labels):add(page,id,136,int(first+i*32),208,24,caption=label,value=val)
 logo=hud('logo','',0,16*S,296*S,80*S,type='image',anchor='center',image='assets/logo_large-0.png');items.append(logo)
 add('main','start',192,128,96,40,icon='MENU_PLAY')
 for i,(id,icon) in enumerate([('jukebox','MENU_JUKEBOX'),('settings','MENU_SETTINGS'),('about','MENU_ABOUT'),('quit','MENU_QUIT')]):add('main',id,136+i*56,184,40,40,icon=icon)
 add('main','audio',432,8,40,40,icon='MENU_AUDIO_ON')
 for page,title in [('difficulty','DIFFICULTY SELECT'),('levels','LEVEL SELECT'),('settings','SETTINGS'),('display','DISPLAY SETTINGS'),('sound','AUDIO SETTINGS'),('vscreen','VSCREEN SIZE'),('about','ABOUT'),('credits','CREDITS'),('jukebox','JUKEBOX'),('quit','QUIT?')]:header(page,title)
 for i,name in enumerate(['normal','hard','super']):add('difficulty',name,128+i*80,144,64,40,caption=name.upper(),gray=i>0)
 for name in ['normal','hard','super']:
  panel('difficulty-'+name+'-locked',64,40,caption=name.upper(),gray=True)
  panel('difficulty-'+name+'-unlocked',64,40,caption=name.upper())
 for i in range(1,6):panel('levels-level'+str(i)+'-locked',40,48,icon='MENU_LOCKED',gray=True)
 for i in range(1,6):add('levels','level'+str(i),104+(i-1)*56,144,40,48,icon='MENU_'+str(i))
 rows('settings',[('display','DISPLAY SETTINGS',''),('sound','AUDIO SETTINGS',''),('touch','TOUCHSCREEN BUTTONS','ON')])
 rows('display',[('size','VSCREEN SIZE',''),('scanlines','SCANLINES','OFF')])
 rows('sound',[('audio','AUDIO','ON'),('music','MUSIC','ON'),('sfx','SFX','ON')])
 rows('vscreen',[('mode','MODE','AUTO'),('width','WIDTH','480'),('height','HEIGHT','270')])
 for page,id,label in [('settings','touch','TOUCHSCREEN BUTTONS'),('display','scanlines','SCANLINES'),('sound','audio','AUDIO'),('sound','music','MUSIC'),('sound','sfx','SFX')]:
  for value in ['ON','OFF']:panel(page+'-'+id+'-'+value.lower(),208,24,label,value=value)
 for value in ['AUTO','FIXED']:panel('vscreen-mode-'+value.lower(),208,24,'MODE',value=value)
 for key,values in [('width',[480,432,416,320,256]),('height',[270,256,240,224,192])]:
  for value in values:panel('vscreen-'+key+'-'+str(value),208,24,key.upper(),value=str(value))
 panel('main-audio-off',40,40,icon='MENU_AUDIO_OFF')
 lines=[('Alex vs Bus: The Race',16,16,'GREEN'),('(C) 2021-2025 M374LX',16,24,'WHITE'),('Original release',16,40,'GREEN'),(' 2025.06.16.0',16,48,'WHITE'),('Repository',16,64,'GREEN'),(' github.com/M374LX/alexvsbus',16,72,'WHITE'),('Licenses',16,88,'GREEN'),(' Code: GNU GPLv3 or later',16,96,'WHITE'),(' Assets: CC BY-SA 4.0',16,104,'WHITE'),('Megalodonte adaptation',16,120,'GREEN'),(' Built with Megalodonte Engine',16,128,'WHITE')]
 add('about','body',40,72,408,152,button=False,lines=lines);add('about','credits',176,240,128,24,caption='CREDITS')
 add('credits','body',40,72,408,152,button=False,lines=[('Original game',16,16,'GREEN'),(' Alexandre Almeida (M374LX)',16,32,'WHITE'),('Additional level design',16,48,'GREEN'),(' Hoton Bastos',16,64,'WHITE'),('Playtesting',16,80,'GREEN'),(' Harim Pires',16,96,'WHITE'),('Fonts',16,112,'GREEN'),(' Press Start 2P: codeman38',16,128,'WHITE')])
 for i in range(4):add('jukebox','track'+str(i),132+i*56,144,40,48,icon='MENU_'+str(i+1))
 add('quit','yes',176,144,56,40,caption='YES');add('quit','no',248,144,56,40,caption='NO')
 return items
