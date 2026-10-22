const page=await figma.getNodeByIdAsync("56:26");await figma.setCurrentPageAsync(page);
const createdNodeIds=[],mutatedNodeIds=[],removedNodeIds=[];
const vars=await figma.variables.getLocalVariablesAsync(),v=Object.fromEntries(vars.map(x=>[x.name,x]));
function paint(name){return figma.variables.setBoundVariableForPaint({type:"SOLID",color:{r:0,g:0,b:0}},"color",v[name]);}
async function fonts(n){for(const t of n.findAllWithCriteria({types:["TEXT"]})){for(const s of t.getStyledTextSegments(["fontName"]))await figma.loadFontAsync(s.fontName);}}
const mains=await Promise.all(["105:321","105:322"].map(id=>figma.getNodeByIdAsync(id)));
for(let i=0;i<mains.length;i++){const n=mains[i];await fonts(n);n.layoutSizingHorizontal="FIXED";n.primaryAxisSizingMode="FIXED";n.resize(80,48);n.counterAxisAlignItems="CENTER";n.primaryAxisAlignItems="CENTER";n.setBoundVariable("itemSpacing",v["space/8"]);
const icon=n.findOne(x=>x.type==="INSTANCE");icon.resize(24,24);icon.layoutSizingHorizontal="FIXED";icon.layoutSizingVertical="FIXED";
for(const a of icon.findAllWithCriteria({types:["VECTOR"]})){a.strokes=[paint(i?"color/brand":"color/muted")];a.fills=i?[paint("color/brand")]:[];a.strokeWeight=1.8;mutatedNodeIds.push(a.id);}
const label=n.findOne(x=>x.type==="TEXT");label.resize(36,22);label.textAutoResize="HEIGHT";label.fills=[paint(i?"color/brand":"color/muted")];mutatedNodeIds.push(n.id,icon.id,label.id);}
const circle=await figma.getNodeByIdAsync("138:713");await fonts(circle);const like=circle.findOne(n=>n.name==="Circle 喜欢");like.swapComponent(mains[0]);like.name="Circle 喜欢";like.resize(102,48);const label=like.findOne(n=>n.type==="TEXT");label.characters="喜欢";mutatedNodeIds.push(like.id,label.id);
const reviewName="M&D / 互动图标 · 修订";
let review=page.findOne(n=>n.name===reviewName);
if(!review){review=figma.createAutoLayout("VERTICAL");page.appendChild(review);review.name=reviewName;review.x=Math.max(...page.children.filter(n=>n.id!==review.id).map(n=>n.x+n.width))+120;review.y=0;review.resize(620,200);review.layoutSizingHorizontal="FIXED";review.layoutSizingVertical="HUG";review.paddingLeft=24;review.paddingRight=24;review.paddingTop=24;review.paddingBottom=24;review.itemSpacing=16;review.fills=[paint("color/surface")];review.cornerRadius=16;createdNodeIds.push(review.id);
for(const text of ["互动图标：空心 / 实心","24px 图形 · 48px 点按区域 · 共用原有颜色"]){const t=figma.createText();review.appendChild(t);t.fontName={family:"Noto Sans SC",style:"Regular"};await figma.loadFontAsync(t.fontName);t.characters=text;t.fontSize=16;t.fills=[paint("color/ink")];createdNodeIds.push(t.id);}
const row=figma.createAutoLayout("HORIZONTAL");review.appendChild(row);row.fills=[];row.itemSpacing=24;createdNodeIds.push(row.id);for(const main of mains){const a=main.createInstance();row.appendChild(a);createdNodeIds.push(a.id,...a.findAll(()=>true).map(n=>n.id));}}
return{createdNodeIds,mutatedNodeIds,removedNodeIds,reviewId:review.id,likeComponents:mains.map(n=>({id:n.id,width:n.width,height:n.height,iconWidth:n.findOne(x=>x.type==="INSTANCE").width}))};
