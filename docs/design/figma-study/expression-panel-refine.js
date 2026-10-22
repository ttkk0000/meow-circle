// Inputs: CONTEXT, KIND, ITEMS, EMOJI_TILE, STICKER_TILE. Existing screen IDs are preserved.
const p=await figma.getNodeByIdAsync("56:25");await figma.setCurrentPageAsync(p);
const root=await figma.getNodeByIdAsync(KIND==="emoji"?CONTEXT.id:CONTEXT.sticker);
const viewport=root.children.find(n=>n.name==="Scrollable viewport");
const content=viewport.children.find(n=>n.name==="Task content");
const oldText=content.findOne(n=>n.type==="TEXT");const contextLabel=oldText?.characters||"和宠友聊聊";
for(const t of content.findAllWithCriteria({types:["TEXT"]}))for(const s of t.getStyledTextSegments(["fontName"]))await figma.loadFontAsync(s.fontName);
await Promise.all([{family:"Noto Sans SC",style:"Regular"},{family:"Noto Sans SC",style:"Medium"},{family:"Noto Sans SC",style:"Bold"}].map(f=>figma.loadFontAsync(f)));
const removedNodeIds=content.children.map(n=>n.id);for(const n of [...content.children])n.remove();
const createdNodeIds=[],mutatedNodeIds=[content.id],vars=Object.fromEntries((await figma.variables.getLocalVariablesAsync()).map(v=>[v.name,v]));
function paint(k){return figma.variables.setBoundVariableForPaint({type:"SOLID",color:{r:0,g:0,b:0}},"color",vars[k]);}
function box(parent,name,w,dir="VERTICAL",gap=8){const n=figma.createAutoLayout(dir);parent.appendChild(n);n.name=name;n.resize(w,48);n.layoutSizingHorizontal="FIXED";n.layoutSizingVertical="HUG";n.itemSpacing=gap;n.fills=[];createdNodeIds.push(n.id);return n;}
async function text(parent,value,role="Caption",color="color/muted",width=358){const n=figma.createText();parent.appendChild(n);n.fontName={family:"Noto Sans SC",style:role==="Strong"?"Bold":"Regular"};n.characters=value;n.resize(width,24);n.textAutoResize="HEIGHT";await n.setTextStyleIdAsync(role==="Strong"?"S:09ac17cb5dbd83a9fb9f1911635c115cf59ac631,":"S:e31250e9288e02c9954a72976177f3677be7fbd6,");n.fills=[paint(color)];createdNodeIds.push(n.id);return n;}
function nav(node,to,scroll=false){node.reactions=[{trigger:{type:"ON_CLICK"},actions:[{type:"NODE",destinationId:to,navigation:scroll?"SCROLL_TO":"NAVIGATE",transition:null}]}];}
async function button(parent,label,to,width=173,active=false){const main=await figma.getNodeByIdAsync(active?"56:180":"56:181"),n=main.createInstance();parent.appendChild(n);const prop=Object.keys(n.componentProperties).find(k=>k.split("#")[0]==="Label");n.setProperties({[prop]:label});n.resize(width,48);if(to)nav(n,to);createdNodeIds.push(n.id);return n;}
await text(content,contextLabel,"Strong","color/ink");const switcher=box(content,"Expression type switch",358,"HORIZONTAL",12);
await button(switcher,"表情 · 128",KIND==="emoji"?null:CONTEXT.id,173,KIND==="emoji");
await button(switcher,"猫咪贴纸 · 24",KIND==="sticker"?null:CONTEXT.sticker,173,KIND==="sticker");
await text(content,KIND==="emoji"?"选择表情，确认后发送":"同一只猫，24 种小心情");
const main=await figma.getNodeByIdAsync(KIND==="emoji"?EMOJI_TILE:STICKER_TILE),owner=main.parent.type==="COMPONENT_SET"?main.parent:main,defs=owner.componentPropertyDefinitions;
const labelProp=Object.keys(defs).find(k=>k.split("#")[0]==="Label"),iconProp=Object.keys(defs).find(k=>k.split("#")[0]==="Icon");
const categories=KIND==="emoji"?["常用","情绪","手势","猫狗","心意","日常"]:["猫咪贴纸"];let tabs=null;if(KIND==="emoji")tabs=box(content,"Emoji categories",358,"HORIZONTAL",8);
const counts={};for(const category of categories){const section=box(content,"Expression category / "+category,358,"VERTICAL",8);await text(section,category,"Strong","color/ink");const entries=ITEMS.filter(a=>a.category===category),cols=KIND==="emoji"?6:3,gap=KIND==="emoji"?8:14,cellWidth=(358-gap*(cols-1))/cols;counts[category]=entries.length;
for(let start=0;start<entries.length;start+=cols){const row=box(section,"Expression row "+start,358,"HORIZONTAL",gap);for(const a of entries.slice(start,start+cols)){const cell=main.createInstance();row.appendChild(cell);cell.name="Expression choice / "+a.key;cell.setProperties({[labelProp]:a.label,[iconProp]:a.componentId});cell.resize(cellWidth,KIND==="emoji"?72:132);
const actions=[];if(a.key!=="hug")actions.push({type:"SET_VARIABLE",variableId:"VariableID:160:889",variableValue:{type:"FLOAT",resolvedType:"FLOAT",value:0}});
actions.push({type:"SET_VARIABLE",variableId:a.opacityVariableId,variableValue:{type:"FLOAT",resolvedType:"FLOAT",value:100}},{type:"SET_VARIABLE",variableId:"VariableID:141:3",variableValue:{type:"STRING",resolvedType:"STRING",value:a.glyph}},{type:"SET_VARIABLE",variableId:"VariableID:141:4",variableValue:{type:"STRING",resolvedType:"STRING",value:a.label}},{type:"NODE",destinationId:CONTEXT.ready,navigation:"NAVIGATE",transition:null});cell.reactions=[{trigger:{type:"ON_CLICK"},actions}];createdNodeIds.push(cell.id);}}
if(tabs){const tab=await button(tabs,category,null,53,category==="常用");nav(tab,section.id,true);}}
await text(content,"选中后先预览，可更换或取消。");
return{createdNodeIds,mutatedNodeIds,removedNodeIds,screenId:root.id,context:CONTEXT.context,kind:KIND,count:ITEMS.length,categories:counts,contentHeight:content.height,viewportWidth:viewport.width};