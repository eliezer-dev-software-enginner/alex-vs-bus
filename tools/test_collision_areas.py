import importlib.util
import json
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parent.parent
spec=importlib.util.spec_from_file_location('collision_areas',ROOT/'tools/collision-areas.py')
areas=importlib.util.module_from_spec(spec);spec.loader.exec_module(areas)
class CollisionAreasTest(unittest.TestCase):
    def test_all_scenes_have_explicit_areas_and_contacts_enabled(self):
        game=json.loads((ROOT/'game.json').read_text())
        for scene in game['scenes']:
            for e in scene['objects']:
                self.assertGreater(e['colliderWidth'],0,e['name']);self.assertGreater(e['colliderHeight'],0,e['name'])
                kind=e.get('properties',{}).get('kind','')
                if kind in areas.CONTACT_KINDS or kind.startswith('coin-'):
                    self.assertTrue(e['solid'],kind)
                    if kind!='player':self.assertTrue(e['trigger'],kind)
                if kind in ('gush','gush-body'):
                    self.assertAlmostEqual(e['colliderWidth'],9/24)
                    self.assertLess(e['colliderWidth'],e['width'])
                if kind in ('decoration','bus-part','vehicle','bus','checkpoint'):
                    self.assertFalse(e['solid'],kind)
    def test_migration_is_idempotent_and_preserves_player_area(self):
        e={'name':'gush','width':32/24,'height':8/24,'solid':False,'assetPath':'assets/gush_top-0.png','properties':{'kind':'gush'}}
        areas.configure(e,ROOT);before=dict(e);areas.configure(e,ROOT);self.assertEqual(before,e)
        player={'width':4,'height':3,'solid':False,'colliderWidth':.5,'colliderHeight':2.5,'colliderOffsetX':.33,'properties':{'kind':'player'}}
        areas.configure(player,ROOT);self.assertEqual(player['colliderWidth'],.5);self.assertTrue(player['solid'])
