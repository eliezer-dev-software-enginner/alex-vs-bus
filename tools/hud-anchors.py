# SPDX-License-Identifier: GPL-3.0-or-later
"""Choose readable HUD anchors while retaining the 1280x720 authored layout."""
def normalize(hud,content=False):
 w,h=hud['width'],hud['height'];x,y=hud.get('offsetX',0),hud.get('offsetY',0)
 old= hud.get('horizontalAnchor','left')
 if old=='center':x+=(1280-w)/2
 elif old=='right':x=1280-w-x
 old=hud.get('verticalAnchor','top')
 if old=='center':y+=(720-h)/2
 elif old=='bottom':y=720-h-y
 cx,cy=x+w/2,y+h/2
 horizontal='center' if abs(cx-640)<=128 else 'left' if cx<640 else 'right'
 vertical='top' if cy<240 else 'bottom' if cy>480 else 'center'
 hud['horizontalAnchor']=horizontal;hud['verticalAnchor']=vertical
 hud['referenceArea']='content' if content else hud.get('referenceArea','viewport')
 hud['offsetX']=round(x if horizontal=='left' else 1280-w-x if horizontal=='right' else x-(1280-w)/2,6)
 hud['offsetY']=round(y if vertical=='top' else 720-h-y if vertical=='bottom' else y-(720-h)/2,6)
