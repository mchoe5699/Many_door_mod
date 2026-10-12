"""Prepare only the named disposable visual world and image fixtures; never overwrite saves."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[1]
source=ROOT/'run-gametest-homenet/world'
target=ROOT/'run/saves/manydoors_visual_test'
if not source.is_dir():raise SystemExit('Run the HomeNet GameTest server first.')
if not target.exists():
 target.parent.mkdir(parents=True,exist_ok=True);shutil.copytree(source,target)
# Pillow is only a development fixture dependency; not a mod/runtime dependency.
from PIL import Image, ImageDraw
image=Image.new('RGB',(300,500),(28,51,76));draw=ImageDraw.Draw(image)
for i,color in enumerate([(160,40,40),(40,170,80),(35,80,220),(220,185,45),(155,75,195)]):draw.rectangle((20,20+i*90,280,90+i*90),fill=color)
folder=Path('/workspace/.tools');folder.mkdir(parents=True,exist_ok=True)
image.save(folder/'test-photo.png');image.save(folder/'test-photo.jpg')
print('Prepared:',target)
