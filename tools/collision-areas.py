#!/usr/bin/env python3
"""Author explicit collision areas without resizing sprites or changing object IDs."""
import json
from pathlib import Path
from PIL import Image

CONTACT_KINDS = {'player','banana','gush','gush-body','gush-crack','spring','rope','hole','trigger-hen'}

def configure(entity, root):
    kind = entity.get('properties', {}).get('kind', '')
    physical = entity.get('solid', False) and not entity.get('trigger', False)
    contact = kind in CONTACT_KINDS or kind.startswith('coin-')
    # Decorative sprites do not gain physics just because they have an image.
    entity['solid'] = physical or contact
    entity['trigger'] = contact and kind != 'player' and not physical
    if kind == 'player':
        entity['colliderFollowsFlip'] = False
    if kind == 'player' and entity.get('colliderWidth', 0) > 0:
        return
    width, height = entity['width'], entity['height']
    box = (0., 0., width, height)
    asset = entity.get('assetPath', '')
    if asset:
        paths = [asset]
        if kind != 'gush-crack':
            for animation in entity.get('animations', {}).values():
                paths.extend(animation['frames'])
        bounds = []
        for path in set(paths):
            with Image.open(root/path) as image:
                image = image.convert('RGBA')
                bbox = image.getchannel('A').getbbox()
                if bbox:
                    l,t,r,b = bbox
                    bounds.append((l/image.width*width,(image.height-b)/image.height*height,r/image.width*width,(image.height-t)/image.height*height))
        if bounds:
            left,bottom,right,top = min(b[0] for b in bounds),min(b[1] for b in bounds),max(b[2] for b in bounds),max(b[3] for b in bounds)
            box = (left,bottom,right-left,top-bottom)
    # Original play.c uses a narrow water column, not the transparent atlas frame.
    if kind in ('gush','gush-body'):
        box = (3/24,0,9/24,height)
    elif kind == 'gush-crack':
        box = (3/24,0,10/24,height)
    elif kind == 'player':
        box = (8/24,4/24,12/24,60/24)
    entity.update(zip(('colliderOffsetX','colliderOffsetY','colliderWidth','colliderHeight'),box))

def update(game, root):
    for scene in game['scenes']:
        for entity in scene['objects']:
            configure(entity,root)

if __name__ == '__main__':
    root=Path(__file__).resolve().parent.parent
    document=root/'game.json'
    game=json.loads(document.read_text())
    update(game,root)
    document.write_text(json.dumps(game,ensure_ascii=False,indent=2)+'\n')
