const plans=__PLANS__;
const page=await figma.getNodeByIdAsync(plans[0].page);
await figma.setCurrentPageAsync(page);
const created=new Set(),mutated=new Set(),screens=[],loaded=new Map();
const frames=new Map();
for(const p of plans)for(const [key,id] of Object.entries(p.roots)){
 const root=await figma.getNodeByIdAsync(id);if(!root)throw new Error("Missing root "+id);
 const content=root.findAllWithCriteria({types:["FRAME"]}).find(n=>n.name.startsWith("Task content"));
 if(!content||content.layoutMode!=="VERTICAL")throw new Error("Unexpected content "+id);
 frames.set(id,{root,content});
 for(const t of content.findAllWithCriteria({types:["TEXT"]}))
  for(const seg of t.getStyledTextSegments(["fontName"]))loaded.set(JSON.stringify(seg.fontName),seg.fontName);
}
await Promise.all([...loaded.values()].map(font=>figma.loadFontAsync(font)));
function visible(n,root){for(let a=n;a&&a!==root;a=a.parent)if(!a.visible)return false;return true;}
function buttonFor(content,labels){
 const text=content.findAllWithCriteria({types:["TEXT"]}).find(t=>visible(t,content)&&labels.includes(t.characters));
 if(!text)return null;
 let n=text.parent;while(n&&n!==content){if(n.type==="INSTANCE")return n;n=n.parent;}return null;
}
function setLabel(node,label){
 if(node.type!=="INSTANCE")throw new Error("Expected instance "+node.id);
 const key=Object.keys(node.componentProperties).find(k=>k.startsWith("Label#"));
 if(!key)throw new Error("No label property "+node.id);
 if(node.componentProperties[key].value!==label){node.setProperties({[key]:label});mutated.add(node.id);for(const t of node.findAllWithCriteria({types:["TEXT"]}))mutated.add(t.id);}
}
async function wire(node,destination){
 const actions=node.reactions?.flatMap(r=>r.actions||[r.action]).filter(Boolean)||[];
 if(actions.some(a=>a.type==="NODE"&&a.destinationId===destination)&&actions.length===1)return;
 await node.setReactionsAsync([{trigger:{type:"ON_CLICK"},actions:[{type:"NODE",destinationId:destination,navigation:"NAVIGATE",transition:null,resetScrollPosition:true}]}]);
 mutated.add(node.id);
}
function addScope(content,label){
 let note=content.children.find(n=>n.type==="TEXT"&&n.name==="Scope / collaboration reference");
 if(!note){
  const sample=content.findAllWithCriteria({types:["TEXT"]}).find(n=>n.parent?.type==="FRAME"&&n.parent.name.startsWith("Notice")&&n.name.startsWith("Caption"));
  if(!sample)throw new Error("Missing caption template "+content.id);
  note=sample.clone();created.add(note.id);content.insertChild(0,note);note.name="Scope / collaboration reference";mutated.add(content.id);
 }
 note.textAutoResize="HEIGHT";note.resize(Math.max(1,content.width-content.paddingLeft-content.paddingRight),note.height);
 note.layoutSizingHorizontal="FIXED";note.characters=label;mutated.add(note.id);return note.id;
}
for(const p of plans){
 for(const [key,id] of Object.entries(p.roots)){
  const {root,content}=frames.get(id);
  let scope=null,management=null,photo=null;
  const labels={
   caretakerLeaveConfirm:"学习示例 · 照护事项 #128 · 邀请 #2041 · 基准 V3",
   caretakerLeft:"学习示例 · 照护事项 #128 · 邀请 #2041 · 响应 V4",
   publisherLeaveConfirm:"学习示例 · 宠物 #36 · 邀请 #2068 · 基准 V2",
   publisherLeft:"学习示例 · 宠物 #36 · 邀请 #2068 · 响应 V3"
  };
  if(labels[key])scope=addScope(content,labels[key]);
  if(["caretakerWorkspace","caretakerReadOnly","publisherWorkspace"].includes(key)){
   let node=buttonFor(content,["查看协作与退出","退出本次照护协作","退出本次介绍协作"]);
   if(!node){
    if(key!=="caretakerReadOnly")throw new Error("Missing management action "+id);
    const source=frames.get(p.roots.caretakerWorkspace).content;
    const sourceButton=buttonFor(source,["查看协作与退出","退出本次照护协作"]);
    if(!sourceButton)throw new Error("Missing management template");
    node=sourceButton.clone();content.appendChild(node);node.name="Action / 查看协作与退出";
    created.add(node.id);for(const child of node.findAll(()=>true))created.add(child.id);mutated.add(content.id);
   }
   setLabel(node,"查看协作与退出");
   const target=key==="publisherWorkspace"?p.sources.introActive:p.sources.careActive;
   await wire(node,target);management={id:node.id,target};
  }
  if(key==="publisherWorkspace"){
   const node=buttonFor(content,["维护介绍照片"]);
   if(!node)throw new Error("Missing photo row "+id);
   await wire(node,p.photo);photo={id:node.id,target:p.photo};
  }
  for(const notice of content.findAllWithCriteria({types:["FRAME"]}).filter(n=>n.name.startsWith("Notice")&&n.layoutMode==="VERTICAL"&&visible(n,content))){
   for(const text of notice.children.filter(n=>n.type==="TEXT")){
    text.textAutoResize="HEIGHT";text.resize(Math.max(1,notice.width-notice.paddingLeft-notice.paddingRight),text.height);
    text.layoutSizingHorizontal="FIXED";mutated.add(text.id);
   }
   notice.layoutSizingVertical="HUG";mutated.add(notice.id);
  }
  const texts=content.findAllWithCriteria({types:["TEXT"]}).filter(t=>visible(t,content));
  const fonts=[...new Set(texts.flatMap(t=>t.getStyledTextSegments(["fontName"]).map(s=>s.fontName.family)))];
  if(fonts.some(f=>!["Noto Sans SC","Manrope"].includes(f)))throw new Error("Unexpected product font "+id);
  screens.push({layout:p.layout,key,id,content:content.id,scope,management,photo,fonts,contentHeight:content.height,descendants:root.findAll(()=>true).length});
 }
}
return {createdNodeIds:[...created],mutatedNodeIds:[...mutated],screens,screenCount:screens.length,fontFamilyAsserted:true};
