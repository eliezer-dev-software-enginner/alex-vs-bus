#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
# Port of level data/render layout from Alex vs Bus (C) 2021-2025 M374LX.
import json, math, shutil, subprocess, sys, uuid
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parent.parent
UP=ROOT/'upstream'; ASSETS=ROOT/'assets'; ASSETS.mkdir(exist_ok=True)
if (ROOT/'game.json').exists() and '--force' not in sys.argv: raise SystemExit('game.json já existe. Use --force para regenerar e substituir o documento.')
regions=json.loads((UP/'sprite-regions.json').read_text()); atlas=Image.open(UP/'assets/gfx.png').convert('RGBA')
shutil.copy(UP/'assets/gfx.png',ASSETS/'gfx.png')
shutil.copy(ROOT/'upstream/icons/icon128.png',ASSETS/'icon.png')
frames={'PLAYER_WALK':6,'PLAYER_SLIP':4,'PLAYER_THROWBACK':3,'PLAYER_RUN':4,'PLAYER_CLEAN_DUNG':9,'COIN_GOLD':4,'COIN_SILVER':4,'GUSH_TOP':3,'SPRING':3,'BUS_WHEEL':3,'BUS_DOOR':4,'BUS_ROUTE':5,'CAR_WHEEL':2,'HEN':2,'BIRD':4,'FLAGMAN':4,'BEARDED_MAN_WALK':6}
for name,(x,y,w,h) in regions.items():
 if w<=0 or h<=0:continue
 for i in range(frames.get(name,1)):
  atlas.crop((x+i*w,y,x+(i+1)*w,y+h)).save(ASSETS/f'{name.lower()}-{i}.png')
for file in (UP/'assets').glob('*.wav'):shutil.copy(file,ASSETS/file.name)
for file in (UP/'assets').glob('*.xm'):
 target=ASSETS/(file.stem+'.mp3')
 if not target.exists():subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(file),'-ar','44100','-c:a','libmp3lame','-q:a','4',str(target)],check=True)
legal=ASSETS/'legal';legal.mkdir(exist_ok=True)
for file in (ROOT/'licenses').glob('*.txt'):shutil.copy(file,legal/file.name)
shutil.copy(ROOT/'LICENSE.txt',legal/'ATTRIBUTION.txt')
shutil.copy(UP/'ORIGIN.json',legal/'ORIGIN.json')
Image.new('RGBA',(4,4),(0,0,0,218)).save(ASSETS/'panel.png')
scan=Image.new('RGBA',(1280,720))
for y in range(1,720,3):
 for x in range(1280):scan.putpixel((x,y),(0,0,0,65))
scan.save(ASSETS/'scanlines.png')

def asset(sprite,frame=0):return f'assets/{sprite.lower()}-{frame}.png'
def entity(sprite,x,y,kind='',solid=False,id=None):
 w,h=regions[sprite][2:]
 return {'id':id or str(uuid.uuid4()),'name':kind or sprite,'assetPath':asset(sprite),'x':x/24,'y':(432-y-h)/24,'width':w/24,'height':h/24,'solid':solid,'properties':{'kind':kind} if kind else {}}
def anim(e,name,sprite,fps=10,loop=True):
 e.setdefault('animations',{})[name]={'frames':[asset(sprite,i) for i in range(frames.get(sprite,1))],'fps':fps,'loop':loop}
def hud(id,text,x,y,w,h=50,size=30,type='text',action='',anchor='left',vertical='top',image=''):
 return {'id':id,'type':type,'text':text,'offsetX':x,'offsetY':y,'width':w,'height':h,'fontSize':size,'action':action,'horizontalAnchor':anchor,'verticalAnchor':vertical,'assetPath':image}
def button(id,text,x,y,w=220,h=64,**kwargs):return hud(id,text,x,y,w,h,type='button',action=id,**kwargs)
def overlay():
 return [hud('panel','',0,170,900,380,type='image',anchor='center',image='assets/panel.png'),hud('notice','',0,205,820,110,36,anchor='center'),button('resume','CONTINUAR',-250,350,240,anchor='center'),button('retry','REINICIAR',30,350,240,anchor='center'),button('menu','MENU',300,350,180,anchor='center')]
def game_hud():
 result=[hud('score','SCORE: 000000',24,4,400,size=26),hud('time','TIME: 90',0,4,240,size=26,anchor='center'),hud('level','',24,64,650,size=22),button('pause','II',24,4,56,56,anchor='right'),hud('hint','A/D: correr • espaço: pular • Esc: pausa',180,14,850,34,20,vertical='bottom')]
 result += [button('left','',24,12,160,160,vertical='bottom',image=asset('TOUCH_LEFT')),button('right','',200,12,160,160,vertical='bottom',image=asset('TOUCH_RIGHT')),button('jump','',24,12,160,160,anchor='right',vertical='bottom',image=asset('TOUCH_JUMP'))]
 bonus=hud('time-bonus','+ 15 seconds',0,34,200,28,20,anchor='center');bonus.update(color='#ffe66d',visible=False);result.append(bonus)
 result += overlay()+[hud('scanlines','',0,0,1280,720,type='image',image='assets/scanlines.png')]
 for h in result:
  if h['id'] in ['panel','notice','resume','retry','menu','scanlines','level','hint']:h['visible']=False
 return result

def invisible(scene,x,y,w,h,kind='terrain'):
 e={'id':str(uuid.uuid4()),'name':kind,'x':x/24,'y':(432-y-h)/24,'width':w/24,'height':h/24,'solid':True,'visible':False,'properties':{'kind':kind}};scene['objects'].append(e);return e

def bus(scene,x,id='bus'):
 e=entity('BUS',x,128,'bus',id=id);scene['objects'].append(e)
 for dx in [104,296]:
  wheel=entity('BUS_WHEEL',x+dx,208,'bus-part');wheel['parentId']=id;wheel['x']-=e['x'];wheel['y']-=e['y'];anim(wheel,'roll','BUS_WHEEL',12);scene['objects'].append(wheel)
 for dx in [64,344]:
  door=entity('BUS_DOOR',x+dx,144,'bus-part');door['parentId']=id;door['x']-=e['x'];door['y']-=e['y'];door['assetPath']=asset('BUS_DOOR',3);scene['objects'].append(door)
 return e

def build_level(difficulty,index):
 suffix={'normal':'n','hard':'h','super':'s'}[difficulty];path=UP/'assets'/f'level{index}{suffix}'
 commands=[line.split() for line in path.read_text().splitlines() if line.strip()]
 metadata={parts[0]:int(parts[1]) for parts in commands[:4]};columns=metadata['level-size']*20
 sky=atlas.getpixel((regions[f'BG_SKY{metadata["sky-color"]}'][0],0));color='#%02x%02x%02x'%sky[:3]
 scene={'id':f'{difficulty}-{index}','name':f'{difficulty.title()} • fase {index}','background':color,'worldWidth':columns,'worldHeight':18,'scriptClassName':'game.alexvsbus.Race','musicAssetPath':f'assets/bgm{metadata["bgm"]}.mp3','musicVolume':.55,'objects':[],'hud':game_hud(),'tileMaps':[]}
 types=[0]*columns;crates=[0]*columns;events=[];x=20
 for parts in commands[4:]:
  cmd=parts[0];a=[int(v) for v in parts[1:]];x+=a[0];events.append((cmd,x,a[1:]))
  if cmd=='deep-hole':
   for i in range(a[1]):types[x+i]=1 if i==0 else 3 if i==a[1]-1 else 2
  elif cmd.startswith('passageway'):
   for i in range(a[1]):types[x+i]=4 if i==0 else 6 if i==a[1]-1 else 5
  elif cmd=='crates':
   for i in range(a[1]):crates[x+i]=a[2]
 table=[[4,5,6,6,6,6,0,0],[7,8,9,9,9,9,0,0],[10,11,12,12,12,12,0,0],[13,14,15,15,15,15,0,0],[16,17,18,18,18,6,0,0],[4,19,12,12,12,6,0,0],[20,21,15,15,15,6,0,0]]
 tiles={};data=[0]*(columns*18)
 for i in range(22):
  sx,sy,w,h=regions[f'LEVEL_BLOCK_{i}'];tiles[str(i+1)]={'assetPath':'assets/gfx.png','sourceX':sx,'sourceY':sy,'width':w,'height':h}
 for col in range(columns):
  for row in range(7,18):
   block=row-6 if row<10 else table[types[col]][row-10]
   data[row*columns+col]=block+1
 # Shared map DTO: scenery is visible through the engine's ordinary tile renderer.
 scene['tileMaps'].append({'id':scene['id']+'-map','name':path.name,'columns':columns,'rows':18,'tileWidth':24,'tileHeight':24,'unitScale':1/24,'tiles':tiles,'layers':[{'name':'cidade','data':data}],'obstacles':[]})
 # Normal floor is grouped into broad collision strips; hole edges remain solid.
 start=None
 for col,kind in enumerate(types):
  if kind in (1,3):invisible(scene,col*24,264,24,168,'hole-edge')
 for col in range(columns+1):
  normal=col<columns and types[col]==0
  if normal and start is None:start=col
  if not normal and start is not None:invisible(scene,start*24,264,(col-start)*24,168);start=None
 for col,height in enumerate(crates):
  for j in range(height):scene['objects'].append(entity('CRATE',col*24,(11-height+j)*24,'crate',True))
 for cmd,col,args in events:
  px=col*24
  if cmd.startswith('coin-'):
   sprite='COIN_GOLD' if cmd=='coin-gold' else 'COIN_SILVER';e=entity(sprite,px+8,args[0]*24,cmd);anim(e,'spin',sprite,10);e['animation']='spin';scene['objects'].append(e)
  elif cmd=='banana-peel':scene['objects'].append(entity('BANANA_PEEL',px+16,args[0]*24+16,'banana'))
  elif cmd in ['gush','gush-crack']:
   e=entity('GUSH_TOP' if cmd=='gush' else 'GUSH_CRACK',px,232 if cmd=='gush' else 260,cmd);e['properties']['baseY']=str(e['y']);anim(e,'flow','GUSH_TOP',10);e['animation']='flow' if cmd=='gush' else '';scene['objects'].append(e)
   if cmd=='gush':
    body=entity('GUSH_MIDDLE',px,240,'gush-body');body['height']=1.0;scene['objects'].append(body)
  elif cmd=='spring':
   e=entity('SPRING',px+8,248,'spring');anim(e,'bounce','SPRING',15,False);scene['objects'].append(e)
  elif cmd=='hydrant':
   scene['objects'].append(entity('HYDRANT',px,240,'hydrant'));invisible(scene,px+12,240,4,32,'hydrant-solid')
  elif cmd=='overhead-sign':
   y=args[0]*24+16;scene['objects'].append(entity('OVERHEAD_SIGN',px,y,'sign'));invisible(scene,px+4,y+8,8,4,'sign-solid')
   for by in range(y+32,248,24):scene['objects'].append(entity('OVERHEAD_SIGN_BASE_MIDDLE',px+4,by,'decoration'))
  elif cmd.startswith('car-') or cmd=='truck':
   sprite={'car-blue':'CAR_BLUE','car-silver':'CAR_SILVER','car-yellow':'CAR_YELLOW','truck':'TRUCK'}[cmd];y=136 if cmd=='truck' else 208
   scene['objects'].append(entity(sprite,px,y,'vehicle'))
   if cmd=='truck':invisible(scene,px,y+4,224,96,'vehicle-solid');invisible(scene,px+224,y+23,55,80,'vehicle-solid')
   else:invisible(scene,px+28,y+2,60,30,'vehicle-solid');invisible(scene,px+4,y+18,24,14,'vehicle-solid');invisible(scene,px+88,y+20,40,12,'vehicle-solid')
   if cmd!='truck':
    for dx in [16,96]:scene['objects'].append(entity('CAR_WHEEL',px+dx,240,'decoration'))
  elif cmd=='deep-hole':
   scene['objects'].append({'id':str(uuid.uuid4()),'name':'Buraco','x':col,'y':0,'width':args[0],'height':4.8,'visible':False,'solid':False,'properties':{'kind':'hole'}})
  elif cmd=='respawn-point':
   e={'id':str(uuid.uuid4()),'name':'Checkpoint','x':(px+3)/24,'y':(432-(args[0]*24-12)-64)/24,'width':.25,'height':4,'visible':False,'solid':False,'properties':{'kind':'checkpoint'}};scene['objects'].append(e)
  elif cmd.startswith('trigger-'):
   sprite='HEN' if cmd=='trigger-hen' else {'trigger-car-blue':'CAR_BLUE','trigger-car-silver':'CAR_SILVER','trigger-car-yellow':'CAR_YELLOW'}[cmd]
   e=entity(sprite,px,224 if sprite=='HEN' else 184,cmd);e['visible']=False;e['properties']['triggerX']=str(px/24);anim(e,'move',sprite if sprite=='HEN' else 'CAR_WHEEL',10) if sprite=='HEN' else None
   scene['objects'].append(e)
  elif cmd=='rope':
   e=entity('ROPE_VERTICAL',px+32,144,'rope');e['properties']['startX']=str(e['x']);e['properties']['endX']=str((px+384)/24);scene['objects'].append(e)
   for i in range(16):scene['objects'].append(entity('ROPE_HORIZONTAL_MIDDLE',px+24+i*24,144,'decoration'))
  elif cmd.startswith('passageway'):
   width=args[0]*24;invisible(scene,px+8,360,width-8,12,'passage-floor');invisible(scene,px+24,264,width-48,13,'passage-roof')
   e=entity('CRATE',px,240,'pushable',True);e['properties']['startX']=str(px/24);scene['objects'].append(e)
   spring=entity('SPRING',px+width-16,344,'spring');anim(spring,'bounce','SPRING',15,False);scene['objects'].append(spring)
 # Street lighting and bus stop, original spacing.
 for px in range(384,columns*24,384):scene['objects'].append(entity('POLE',px,120,'decoration'))
 bus(scene,columns*24-456);scene['objects'].append(entity('BUS_STOP_SIGN',columns*24-40,176,'decoration'))
 player=entity('PLAYER_STAND',3,200,'player',id='alex');player['y']=7-1/6;player['colliderOffsetY']=4/24;player['colliderOffsetX']=8/24;player['colliderWidth']=12/24;player['colliderHeight']=60/24
 player['properties']['goalScene']=str(metadata['goal-scene'])
 for name,sprite,fps,loop in [('stand','PLAYER_STAND',1,True),('walk','PLAYER_WALK',10,True),('jump','PLAYER_JUMP',1,True),('slip','PLAYER_SLIP',12,False),('hit','PLAYER_THROWBACK',15,False),('rope','PLAYER_GRABROPE',1,True),('run','PLAYER_RUN',12,True),('clean','PLAYER_CLEAN_DUNG',8,False)]:anim(player,name,sprite,fps,loop)
 scene['objects'].append(player)
 scene['camera']={'mode':'follow','targetId':'alex','x':10,'y':11.875,'width':20,'height':10.25,'viewportTop':64,'followSpeed':12,'followOffsetY':2.5,'holdGroundDuringJump':True,'deadZoneWidth':4,'deadZoneHeight':1,'fadeSeconds':.5}
 return scene

scenes=[build_level(d,i) for d,count in [('normal',5),('hard',5),('super',3)] for i in range(1,count+1)]
# Levels without imported edge colliders need checkpoint relocation.
for scene in scenes:
 if scene['id'] in ('normal-3','hard-3','normal-4','hard-4','normal-5'):
  for obj in scene['objects']:
   if obj.get('properties',{}).get('kind')!='checkpoint':continue
   fixes={'normal-4':(120.125,128.125),'hard-4':(120.125,128.125),'normal-5':(25.125,31.125)}
   if scene['id'] not in fixes:continue
   old,new=fixes[scene['id']]
   if abs(obj['x']-old)<.01:obj['x']=new

import importlib.util
spec=importlib.util.spec_from_file_location('menu_assets',ROOT/'tools/menu-assets.py');ui=importlib.util.module_from_spec(spec);spec.loader.exec_module(ui)
menu={'id':'menu','name':'Alex vs Bus • título','worldWidth':20,'worldHeight':11.25,'background':'#000000','scriptClassName':'game.alexvsbus.Menu','musicAssetPath':'assets/bgmtitle.mp3','musicVolume':.5,'camera':{'x':10,'y':5.625,'width':20,'height':11.25},'objects':[],'hud':ui.menus(hud)}
ending={'id':'ending','name':'Final • Alex venceu','worldWidth':64,'worldHeight':18,'background':'#54a5ff','scriptClassName':'game.alexvsbus.Ending','musicAssetPath':'assets/bgm3.mp3','musicVolume':.55,'camera':{'x':42,'y':11.25,'width':20,'height':13.5},'objects':[],'tileMaps':[],'hud':[hud('ending-title','',0,28,980,90,36,anchor='center'),button('menu','VOLTAR AO MENU',0,610,500,75,anchor='center')]}
# Copy a strip of the regular street into the ending scene.
ending['tileMaps']=[json.loads(json.dumps(scenes[0]['tileMaps'][0]))];m=ending['tileMaps'][0];old=m['columns'];m['columns']=64;m['name']='Final';m['layers'][0]['data']=[0]*64*18
for row in range(7,18):
 tile=row-6 if row<10 else [4,5,6,6,6,6,0,0][row-10]
 for col in range(64):m['layers'][0]['data'][row*64+col]=tile+1
bus(ending,560)
flag=entity('FLAGMAN',992,176,'flag',id='flag');anim(flag,'wave','FLAGMAN',8);ending['objects'].append(flag)
for i in range(6):ending['objects'].append(entity(['CAR_BLUE','CAR_SILVER','CAR_YELLOW'][i%3],200+i*140,208,'traffic'))
p=entity('PLAYER_RUN',744,200,'player',id='alex');anim(p,'run','PLAYER_RUN',12);anim(p,'stand','PLAYER_STAND',1);p['animation']='run';ending['objects'].append(p)
hen=entity('HEN',720,224,'hen',id='hen');anim(hen,'run','HEN',10);hen['animation']='run';hen['visible']=False;ending['objects'].append(hen)
import copy
menu_scenes=[]
for page,title in [('main','Título'),('difficulty','Dificuldade'),('levels','Seleção de fase'),('settings','Configurações'),('display','Vídeo'),('sound','Áudio'),('vscreen','Tamanho virtual'),('jukebox','Jukebox'),('about','Sobre'),('credits','Créditos'),('quit','Sair')]:
 screen=copy.deepcopy(menu);screen['id']='menu' if page=='main' else 'menu-'+page;screen['name']='Menu • '+title
 screen['hud']=[h for h in screen['hud'] if (not '-' in h['id'] if page=='main' else h['id'].startswith(page+'-'))]
 for h in screen['hud']:h['visible']=True
 menu_scenes.append(screen)
scenes=menu_scenes+scenes+[ending]
anchor_spec=importlib.util.spec_from_file_location('hud_anchors',ROOT/'tools/hud-anchors.py');anchors=importlib.util.module_from_spec(anchor_spec);anchor_spec.loader.exec_module(anchors)
for scene in scenes:
 for h in scene.get('hud',[]):anchors.normalize(h,scene['id'].startswith('menu') or h['id'] in ['panel','notice','resume','retry','menu','scanlines','level','hint'])
collision_spec=importlib.util.spec_from_file_location('collision_areas',ROOT/'tools/collision-areas.py');collisions=importlib.util.module_from_spec(collision_spec);collision_spec.loader.exec_module(collisions)
collisions.update({'scenes':scenes},ROOT)
(ROOT/'game.json').write_text(json.dumps({'version':1,'versionCode':1,'versionName':'0.1.0','name':'Alex vs Bus: The Race — Megalodonte','iconAssetPath':'assets/icon.png','applicationId':'megalodonte.games.alexvsbus','entrySceneId':'menu','lastEditedSceneId':'normal-1','scenes':scenes},ensure_ascii=False,indent=2)+'\n')
print('Criadas 13 fases + 11 cenas de menus + final:',ROOT/'game.json')
