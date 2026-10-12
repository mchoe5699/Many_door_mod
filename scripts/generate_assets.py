"""Generate the mod's original pixel textures, block models, translations and recipes."""
from pathlib import Path
import json, re, struct, zlib, math
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
ASSET=RES/'assets/manydoors'
def write(path,data):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
def png(path, pixels):
 path.parent.mkdir(parents=True,exist_ok=True)
 def chunk(k,v):return struct.pack('>I',len(v))+k+v+struct.pack('>I',zlib.crc32(k+v)&0xffffffff)
 raw=b''.join(b'\0'+bytes(v for p in row for v in p) for row in pixels)
 path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',len(pixels[0]),len(pixels),8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))
colors={'wood':(144,96,47),'white':(224,226,229),'steel':(136,149,163),'brown':(85,53,39),'dark':(43,49,57),'red':(154,39,37),'glass':(130,210,224),'screen':(69,82,83)}
for mat,col in colors.items():
 pixels=[]
 for y in range(64):
  row=[]
  for x in range(64):
   grain=((x*97+y*31)%11)-5
   shade=grain/4
   if mat in ('wood','brown'):shade=math.sin(x*.8+math.sin(y*.04))*10+math.sin(x*.21)*7+grain*.5
   elif mat in ('steel','white','dark','red'):shade=math.cos((x-24)/64*math.pi)*18-8+grain*.25
   elif mat=='screen':shade=-25 if x%4==0 or y%4==0 else 3
   row.append(tuple(max(0,min(255,int(v+shade))) for v in col)+(255,))
  pixels.append(row)
 png(ASSET/f'textures/block/{mat}.png',pixels)
styles=re.findall(r'\w+\("([a-z_]+)", Motion\.(\w+), Surface\.(\w+)',(ROOT/'src/main/java/com/mchoe5699/manydoors/door/DoorStyle.java').read_text())
ko_names=['목재 여닫이문','패널문','평판문','전통 양개문','철제 현관문','아파트 현관문','방범 현관문','방화문','양개 현관문','유리 현관문','알루미늄 현관문','PVC 현관문','방음문','방충망문','덧문','유리 양개문','스윙 양개문','피벗문','미닫이문','포켓 미닫이문','유리 미닫이문','자동 양개 미닫이문','양개 미닫이문','텔레스코픽 미닫이문','접이문','2단 접이문','아코디언문','회전문','롤링 셔터','섹셔널 차고문','틸팅 차고문','로비폰 설치형 자동문','수직 승강문']
en={'itemGroup.manydoors':'Many Doors','screen.manydoors.settings':'Door settings','screen.manydoors.width':'Width (1–6 blocks)','screen.manydoors.height':'Height (1–6 blocks)','screen.manydoors.thickness':'Thickness 0.0625–0.5','screen.manydoors.angle':'Open angle 1–180°','screen.manydoors.close':'Auto-close 0–60 s','screen.manydoors.mode':'Operation','screen.manydoors.right_hinge':'Right hinge (single swing)','screen.manydoors.apply':'Apply','mode.manydoors.manual':'Manual','mode.manydoors.automatic':'Automatic','mode.manydoors.both':'Manual + automatic','item.manydoors.door_linker':'Door linking tool','item.manydoors.door_settings':'Door settings tool','tooltip.manydoors.door':'Place in clear space; settings tool changes size and operation.','tooltip.manydoors.linker':'Door → device/button/sensor. Sneak: clear selection or device links.','tooltip.manydoors.settings':'Click a door to edit. Click a sensor to cycle range (1–8 blocks).'}
ko={'itemGroup.manydoors':'다양한 문','screen.manydoors.settings':'문 설정','screen.manydoors.width':'너비 (1–6블록)','screen.manydoors.height':'높이 (1–6블록)','screen.manydoors.thickness':'두께 (0.0625–0.5블록)','screen.manydoors.angle':'최대 개방각 (1–180°)','screen.manydoors.close':'자동 닫힘 (0–60초)','screen.manydoors.mode':'작동 모드','screen.manydoors.right_hinge':'오른쪽 경첩 (한짝 여닫이)','screen.manydoors.apply':'적용','mode.manydoors.manual':'수동','mode.manydoors.automatic':'자동','mode.manydoors.both':'수동 + 자동','item.manydoors.door_linker':'문 연결 도구','item.manydoors.door_settings':'문 설정 도구','tooltip.manydoors.door':'움직일 공간을 비워 배치하세요. 설정 도구로 크기와 모드를 변경합니다.','tooltip.manydoors.linker':'문 → 기기·버튼·센서 클릭. 웅크리기: 선택 또는 기기 링크 초기화.','tooltip.manydoors.settings':'문 클릭: 설정 화면. 센서 클릭: 범위 변경 (1–8블록).'}
messages={
 'ok':('Settings saved.','설정을 저장했습니다.'),
 'no_space':('Keep the full door movement area clear.','문이 움직일 전체 공간을 비워 주세요.'),
 'invalid_settings':('Invalid value; check the displayed ranges.','입력값 오류: 표시된 범위를 확인하세요.'),
 'not_owner':('Only the installer or an operator can edit this door/device.','설치자 또는 관리자만 문·기기 설정과 링크를 변경할 수 있습니다.'),
 'protected':('This location is protected.','보호된 구역입니다.'),
 'outside_world':('The movement area is outside the world or unloaded.','움직일 공간이 월드 밖이거나 로드되지 않았습니다.'),
 'linked':('Door linked.','문을 연결했습니다.'),
 'unlinked':('Door link removed.','문 연결을 해제했습니다.'),
 'links_cleared':('Device links cleared.','기기의 문 연결을 초기화했습니다.'),
 'selection_cleared':('Door selection cleared.','문 선택을 초기화했습니다.'),
 'door_selected':('Door selected. Click a HomeNet device, button or sensor next.','문 선택 완료. 홈넷 기기·버튼·센서를 클릭하세요.'),
 'select_door':('Select a door first, in this dimension.','같은 차원에서 먼저 문을 선택하세요.'),
 'missing_door':('The selected door was removed or is unloaded.','선택한 문이 제거되었거나 로드되지 않았습니다.'),
 'link_range':('Devices must be within 64 blocks of the door.','문과 기기는 64블록 이내에 있어야 합니다.'),
 'link_full':('A device can link to at most 16 doors.','기기 하나에 최대 16개의 문을 연결할 수 있습니다.'),
 'links':('Linked doors: %s','연결된 문: %s'),
 'radius':('Sensor range: %s blocks','센서 범위: %s블록')}
for key,(e,k) in messages.items():en['message.manydoors.'+key]=e;ko['message.manydoors.'+key]=k
write(ASSET/'models/block/invisible.json',{'textures':{'particle':'minecraft:block/iron_block'},'elements':[]})
for i,(id,motion,material) in enumerate(styles):
 en['item.manydoors.'+id]=id.replace('_',' ').title();ko['item.manydoors.'+id]=ko_names[i]
 en['block.manydoors.'+id]=en['item.manydoors.'+id];ko['block.manydoors.'+id]=ko_names[i]
 col=colors[material.lower()]
 # Distinct trims, panels and apartment peepholes; all artwork is generated here.
 tex=[]
 for yy in range(128):
  row=[]
  for xx in range(64):
   shade=((i*13)%25)-12
   if material in ('WOOD','BROWN'):shade+=math.sin(xx*.43+math.sin(yy*.06))*6+((xx*97+yy*31)%7)/2
   else:shade+=math.cos((xx-20)/64*math.pi)*9+((xx*97+yy*31)%7)/4
   if xx in (0,1,62,63) or yy in (0,1,126,127):shade-=25
   if id in ('panel_door','traditional_door','wooden_door','bifold_door') and (xx in (12,13,50,51) or yy in (32,33,95,96)):shade-=22
   if id in ('steel_entrance','aluminum_entrance','armored_entrance') and yy%32==0:shade+=14
   if id in ('roller_shutter','sectional_garage','up_and_over_garage') and yy%16 in (0,1):shade-=28
   pixel=tuple(max(0,min(255,int(v+shade))) for v in col)+(255,)
   if id=='apartment_entrance' and 30<=xx<=33 and 36<=yy<=39:pixel=(19,25,31,255)
   if id=='fire_entrance' and 16<=xx<=47 and 16<=yy<=32:pixel=(230,231,226,255)
   row.append(pixel)
  tex.append(row)
 png(ASSET/f'textures/block/{id}.png',tex)
 pixels=[[(0,0,0,0) for x in range(16)] for y in range(16)]
 for y in range(1,15):
  for x in range(3,13):
   c=(42,44,48) if x in (3,12) or y in (1,14) else col
   if motion in ('DOUBLE_SWING','DOUBLE_SLIDE','REVOLVE') and x==8:c=(42,44,48)
   pixels[y][x]=c+(255,)
 pixels[8][10]=(243,209,108,255)
 png(ASSET/f'textures/item/{id}.png',pixels)
 write(ASSET/f'models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'manydoors:item/{id}'}})
 write(ASSET/f'blockstates/{id}.json',{'variants':{'':{'model':'manydoors:block/invisible'}}})
 write(RES/f'data/manydoors/loot_tables/blocks/{id}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'manydoors:{id}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 ingredient={'tag':'minecraft:planks'} if material=='WOOD' else {'item':'minecraft:glass'} if material in ('GLASS','SCREEN') else {'item':'minecraft:iron_ingot'}
 write(RES/f'data/manydoors/recipes/{id}.json',{'type':'minecraft:stonecutting','ingredient':ingredient,'result':f'manydoors:{id}','count':1})
write(ASSET/'blockstates/door_part.json',{'variants':{'':{'model':'manydoors:block/invisible'}}})
controls={'button':('Door release button','문 열림 버튼',(61,174,102)), 'motion_sensor':('Motion sensor','동작 감지 센서',(109,171,216)), 'pressure_sensor':('Pressure sensor','발판 센서',(122,136,146)), 'contact_sensor':('Door contact sensor','문 상태 센서',(212,190,69)), 'status_lamp':('Door status lamp','문 상태 표시등',(68,197,123)), 'emergency_button':('Emergency release button','비상 문 열림 버튼',(218,68,57))}
for id,(e,k,col) in controls.items():
 en['block.manydoors.'+id]=e;ko['block.manydoors.'+id]=k
 png(ASSET/f'textures/block/{id}.png',[[col+(255,) for x in range(16)]for y in range(16)])
 for powered in (False,True):
  suffix='_on' if powered else ''
  texture=f'manydoors:block/{id}' if not powered else 'minecraft:block/redstone_block'
  size=([1,0,1],[15,2,15]) if id=='pressure_sensor' else ([4,0,4],[12,4,12])
  model={'textures':{'all':texture,'particle':texture},'elements':[{'from':size[0],'to':size[1],'faces':{face:{'texture':'#all'} for face in ['north','south','east','west','up','down']}}]}
  write(ASSET/f'models/block/{id}{suffix}.json',model)
 write(ASSET/f'blockstates/{id}.json',{'variants':{f'powered={str(v).lower()}':{'model':f'manydoors:block/{id}'+('_on' if v else '')}for v in (False,True)}})
 write(ASSET/f'models/item/{id}.json',{'parent':f'manydoors:block/{id}'})
 write(RES/f'data/manydoors/loot_tables/blocks/{id}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'manydoors:{id}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 write(RES/f'data/manydoors/recipes/{id}.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'minecraft:iron_ingot'},{'item':'minecraft:redstone'},{'item':'minecraft:stone_button'},{'item':'minecraft:glass'}]+[{'item':'minecraft:redstone'} for _ in range(list(controls).index(id))],'result':{'item':f'manydoors:{id}'}})
for id,col in [('door_linker',(101,184,236)),('door_settings',(219,173,74))]:
 pixels=[[(0,0,0,0)for x in range(16)]for y in range(16)]
 for y in range(2,14):
  for x in range(2,14):
   if abs(x+y-15)<3 or y<6 and x>8:pixels[y][x]=col+(255,)
 png(ASSET/f'textures/item/{id}.png',pixels)
 write(ASSET/f'models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'manydoors:item/{id}'}})
 write(RES/f'data/manydoors/recipes/{id}.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'minecraft:iron_ingot'},{'item':'minecraft:stick'},{'item':'minecraft:redstone' if id=='door_linker' else 'minecraft:amethyst_shard'}],'result':{'item':f'manydoors:{id}'}})
extra={
 'screen.manydoors.device':('Wall device position','벽 부착 기기 위치'),
 'screen.manydoors.offset_x':('Horizontal ±0.45','좌우 (-0.45–0.45)'),
 'screen.manydoors.offset_y':('Vertical ±0.45','높이 (-0.45–0.45)'),
 'screen.manydoors.offset_depth':('Outward (0–0.3)','돌출 거리 (0–0.3)'),
 'screen.manydoors.device_scale':('Size (0.4–1.4)','크기 (0.4–1.4)'),
 'screen.manydoors.sensor_range':('Sensor range (1–8)','센서 범위 (1–8)'),
 'screen.manydoors.design':('Custom design','커스텀 디자인'),
 'screen.manydoors.color':('Paint color #RRGGBB','색상 #RRGGBB'),
 'screen.manydoors.brush':('Brush','브러시'),
 'screen.manydoors.import_photo':('Import PNG / JPEG','사진 불러오기'),
 'screen.manydoors.fill':('Fill canvas','전체 칠하기'),
 'screen.manydoors.reset_design':('Restore original design','기본 디자인 복원'),
 'message.manydoors.invalid_design':('Invalid design data.','디자인 데이터 오류입니다.'),
 'message.manydoors.shift_remove':('Sneak and hit the leaf to remove the door.','문짝 철거: 웅크린 채 타격하세요.'),
 'tooltip.manydoors.settings':('Door: settings/design. Device: position/range.','문: 설정·디자인. 기기: 위치·감지 범위.'),
 'tooltip.manydoors.door':('Moving leaves pass through blocks. Only the anchor needs space.','문짝은 벽을 통과합니다. 기준 블록 자리만 비워 배치하세요.'),
 'message.manydoors.no_space':('The anchor or fixed mounting column is occupied.','기준 블록 또는 고정 기둥 자리가 막혀 있습니다.')}
for key,(english,korean) in extra.items():en[key]=english;ko[key]=korean
for name,color in [('black',(19,22,27)),('blue',(15,66,175)),('green',(29,179,92))]:png(ASSET/f'textures/block/{name}.png',[[color+(255,) for x in range(16)]for y in range(16)])
write(ASSET/'blockstates/lobby_mount.json',{'variants':{'':{'model':'manydoors:block/invisible'}}})
write(ASSET/'lang/en_us.json',en);write(ASSET/'lang/ko_kr.json',ko)
write(RES/'data/minecraft/tags/blocks/mineable/pickaxe.json',{'replace':False,'values':[f'manydoors:{id}'for id,_,_ in styles]+[f'manydoors:{id}' for id in controls]})
print(f'Generated resources for {len(styles)} doors and {len(controls)} controls.')
