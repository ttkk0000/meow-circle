const p=await figma.getNodeByIdAsync("56:26");await figma.setCurrentPageAsync(p);
const createdNodeIds=[],mutatedNodeIds=[],components={},opacityVars={};
const all=await figma.variables.getLocalVariablesAsync(),vars=Object.fromEntries(all.map(v=>[v.name,v]));
const template=await figma.variables.getVariableByIdAsync("VariableID:160:889"),coll=await figma.variables.getVariableCollectionByIdAsync(template.variableCollectionId);
const atlas=await figma.getNodeByIdAsync("188:18662");const hash=atlas.fills[0].imageHash;
for(let i=0;i<STICKERS.length;i++){const a=STICKERS[i],name="M&D / Cat sticker / "+a.key;let main=p.findOne(n=>n.type==="COMPONENT"&&n.name===name);
if(!main){main=figma.createComponent();p.appendChild(main);main.name=name;main.resize(144,144);main.fills=[{type:"IMAGE",imageHash:hash,scaleMode:"CROP",imageTransform:[[1/6,0,(i%6)/6],[0,1/4,Math.floor(i/6)/4]]}];main.x=13500+(i%8)*168;main.y=1500+Math.floor(i/8)*168;main.description="M&D 原创 AI 猫咪贴纸 / "+a.label+" · 2026-10-19 image_gen · 原始透明图集 assets/mnd-cat-stickers-atlas-v1.png。标签为原生文本，非图内文字。";createdNodeIds.push(main.id);}components[a.key]=main.id;
let v=all.find(v=>v.name==="expression/show/"+a.key);if(!v){v=figma.variables.createVariable("expression/show/"+a.key,coll,"FLOAT");v.scopes=["OPACITY"];for(const m of coll.modes)v.setValueForMode(m.modeId,0);v.setVariableCodeSyntax("WEB","var(--mnd-expression-"+a.key+")");}opacityVars[a.key]=v.id;}
const modeVars={};
const font={family:"Noto Sans SC",style:"Regular"};await figma.loadFontAsync(font);
function paint(key){return figma.variables.setBoundVariableForPaint({type:"SOLID",color:{r:0,g:0,b:0}},"color",vars[key]);}
let tile=p.findOne(n=>n.type==="COMPONENT"&&n.name==="M&D / Emoji grid cell");
if(!tile){tile=figma.createComponent();p.appendChild(tile);tile.name="M&D / Emoji grid cell";tile.description="52×72 点按区域；36px 官方 Noto 矢量＋12px 中文标签。图形使用 INSTANCE_SWAP，保持比例；标签独立。";tile.resize(52,72);tile.layoutMode="VERTICAL";tile.primaryAxisAlignItems="CENTER";tile.counterAxisAlignItems="CENTER";tile.itemSpacing=4;tile.fills=[];tile.x=13500;tile.y=2100;createdNodeIds.push(tile.id);
const icon=(await figma.getNodeByIdAsync("160:833")).createInstance();tile.appendChild(icon);icon.name="Emoji graphic";icon.resize(36,36);icon.layoutSizingHorizontal="FIXED";icon.layoutSizingVertical="FIXED";const ip=tile.addComponentProperty("Icon","INSTANCE_SWAP","160:833");icon.componentPropertyReferences={mainComponent:ip};createdNodeIds.push(icon.id,...icon.findAll(()=>true).map(n=>n.id));
const label=figma.createText();tile.appendChild(label);label.name="Emoji label";label.fontName=font;label.characters="抱抱";label.fontSize=12;label.fills=[paint("color/muted")];await label.setTextStyleIdAsync("S:e31250e9288e02c9954a72976177f3677be7fbd6,");label.resize(52,18);label.textAutoResize="HEIGHT";label.textAlignHorizontal="CENTER";const lp=tile.addComponentProperty("Label","TEXT","抱抱");label.componentPropertyReferences={characters:lp};createdNodeIds.push(label.id);}
return{createdNodeIds,mutatedNodeIds,components,opacityVars,modeVars,emojiTile:tile.id,atlasHash:hash,firstSticker:components[STICKERS[0].key]};

