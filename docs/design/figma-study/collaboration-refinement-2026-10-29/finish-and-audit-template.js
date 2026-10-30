const plans=__PLANS__;
const page=await figma.getNodeByIdAsync(plans[0].page);await figma.setCurrentPageAsync(page);
const changed=new Set(),returnLinks=[],rows=[],targets=new Map(),badFonts=[],outside=[],fullScreenImages=[],fonts=new Set(),componentKeys=new Set();
const visibleCache=new Map();
function visible(n){if(!n||n.type==="PAGE"||n.type==="DOCUMENT")return true;if(visibleCache.has(n.id))return visibleCache.get(n.id);const v=n.visible!==false&&visible(n.parent);visibleCache.set(n.id,v);return v;}
async function setActions(n,actions){await n.setReactionsAsync([{trigger:{type:"ON_CLICK"},actions}]);changed.add(n.id);}
function instanceFor(root,text){
const t=root.findAllWithCriteria({types:["TEXT"]}).find(n=>visible(n)&&n.characters===text);if(!t)return null;
let n=t.parent;while(n&&n!==root){if(n.type==="INSTANCE")return n;n=n.parent;}return null;
}
for(const p of plans){
 const photo=await figma.getNodeByIdAsync(p.photo);
 for(const label of ["使用这张照片","拒绝或取消，继续写文字"]){
  const n=instanceFor(photo,label);if(!n)throw new Error("Missing photo control "+p.layout+label);
  await setActions(n,[{type:"BACK"}]);returnLinks.push({layout:p.layout,id:n.id,label,action:"BACK",meaning:"return to the invoking editor"});
 }
 for(const key of ["caretakerLeft","publisherLeft","caretakerUnavailable"]){
  const root=await figma.getNodeByIdAsync(p.roots[key]);
  const back=root.findAllWithCriteria({types:["FRAME"]}).find(n=>n.name==="Back");
  if(!back)throw new Error("Missing back control "+root.id);
  await setActions(back,[{type:"NODE",destinationId:p.messages,navigation:"NAVIGATE",transition:null,resetScrollPosition:true}]);
 }
 for(const [key,id] of Object.entries(p.roots)){
  const root=await figma.getNodeByIdAsync(id),nodes=[root,...root.findAll(()=>true)],counts={},images=[],links=[];
  for(const n of nodes){
   counts[n.type]=(counts[n.type]||0)+1;if(!visible(n))continue;
   if(n.type==="INSTANCE"&&n.mainComponent?.key)componentKeys.add(n.mainComponent.key);
   if(n.type==="TEXT"){
    const fs=n.fontName===figma.mixed?n.getStyledTextSegments(["fontName"]).map(s=>s.fontName):[n.fontName];
    for(const f of fs){fonts.add(f.family);if(!["Noto Sans SC","Manrope"].includes(f.family))badFonts.push({id:n.id,font:f.family});}
    const b=n.absoluteBoundingBox,parent=n.parent,box=parent&&"absoluteBoundingBox"in parent?parent.absoluteBoundingBox:null;
    if(b&&box&&(b.x<box.x-1||b.x+b.width>box.x+box.width+1||b.y<box.y-1||b.y+b.height>box.y+box.height+1))outside.push({id:n.id,root:id,parent:parent.id,text:n.characters,width:n.width,height:n.height});
   }
   if("fills"in n&&Array.isArray(n.fills)&&n.fills.some(f=>f.type==="IMAGE")){
    images.push({id:n.id,name:n.name,type:n.type,width:n.width,height:n.height});
    if(n.width>=root.width*0.9&&n.height>=root.height*0.9)fullScreenImages.push(n.id);
   }
   if("reactions"in n)for(const r of n.reactions||[])for(const a of r.actions||[r.action]){
    if(a?.type==="NODE"&&a.destinationId){links.push({from:n.id,to:a.destinationId});targets.set(a.destinationId,null);}
   }
  }
  const required=[];
  function expect(label,to){const n=instanceFor(root,label);const found=!!n&&n.reactions.some(r=>(r.actions||[r.action]).some(a=>a?.type==="NODE"&&a.destinationId===to));required.push({label,from:n?.id,target:to,passed:found});}
  if(["caretakerWorkspace","caretakerReadOnly"].includes(key))expect("查看协作与退出",p.sources.careActive);
  if(key==="publisherWorkspace"){expect("查看协作与退出",p.sources.introActive);expect("维护介绍照片",p.photo);}
  if(key==="caretakerLeaveConfirm"){expect("确认退出照护协作",p.roots.caretakerLeft);expect("保留当前协作",p.sources.careActive);}
  if(key==="publisherLeaveConfirm"){expect("确认退出介绍协作",p.roots.publisherLeft);expect("保留当前协作",p.sources.introActive);}
  if(["caretakerLeft","publisherLeft","caretakerUnavailable"].includes(key))expect("返回消息",p.messages);
  const scope=root.findAllWithCriteria({types:["TEXT"]}).filter(n=>n.name==="Scope / collaboration reference").map(n=>n.characters);
  rows.push({layout:p.layout,key,id,descendants:nodes.length-1,counts,imageFilledNodes:images,links,requiredActions:required,scopeReferences:scope});
 }
}
await Promise.all([...targets.keys()].map(async id=>{const n=await figma.getNodeByIdAsync(id);let p=n;while(p&&p.type!=="PAGE")p=p.parent;targets.set(id,{exists:!!n,page:p?.id});}));
const invalid=[];for(const row of rows)for(const link of row.links){const target=targets.get(link.to);if(!target?.exists||target.page!==page.id||link.to===row.id)invalid.push({...link,root:row.id,target});}
const failed=rows.flatMap(r=>r.requiredActions.filter(a=>!a.passed).map(a=>({...a,root:r.id})));
return {createdNodeIds:[],mutatedNodeIds:[...changed],screenCount:rows.length,visibleNodeLinks:rows.reduce((n,r)=>n+r.links.length,0),requiredActions:rows.reduce((n,r)=>n+r.requiredActions.length,0),failedRequiredActions:failed,invalidTargets:invalid,fonts:[...fonts],badFonts,textOutsideParent:outside,fullScreenImages,componentKeys:[...componentKeys],photoReturnLinks:returnLinks,screens:rows};
