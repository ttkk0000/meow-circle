// Figma MCP only. Pass a layout plan and the state list from nodes-and-validation.json.

const page=await figma.getNodeByIdAsync(plan.page);
await figma.setCurrentPageAsync(page);
const sourceIds=[...new Set(Object.values(plan.sources).concat(['242:374140','145:5164']))];
const sourceNodes=await Promise.all(sourceIds.map(id=>figma.getNodeByIdAsync(id)));
if(sourceNodes.some(n=>!n))throw new Error('A required source node is missing');
const sources=Object.fromEntries(sourceNodes.map(n=>[n.id,n]));
const fontMap=new Map();
for(const root of sourceNodes)for(const t of root.findAllWithCriteria({types:['TEXT']}))for(const s of t.getStyledTextSegments(['fontName']))fontMap.set(JSON.stringify(s.fontName),s.fontName);
await Promise.all([...fontMap.values()].map(f=>figma.loadFontAsync(f)));
const created=new Set(),mutated=new Set(),roots={};
function track(n){created.add(n.id);if('findAll'in n)for(const c of n.findAll())created.add(c.id);return n;}
function texts(n){return n.findAllWithCriteria({types:['TEXT']});}
function labelNode(n,label){return n.findAllWithCriteria({types:['INSTANCE']}).find(i=>Object.values(i.componentProperties).some(p=>p.type==='TEXT'&&p.value===label));}
function replace(n,changes){
 for(const i of (n.type==='INSTANCE'?[n,...n.findAllWithCriteria({types:['INSTANCE']})]:n.findAllWithCriteria({types:['INSTANCE']}))){const props={};for(const [k,p]of Object.entries(i.componentProperties))if(p.type==='TEXT'&&Object.hasOwn(changes,p.value))props[k]=changes[p.value];if(Object.keys(props).length){i.setProperties(props);mutated.add(i.id);for(const t of texts(i))mutated.add(t.id);}}
 for(const t of texts(n))if(Object.hasOwn(changes,t.characters)){t.characters=changes[t.characters];mutated.add(t.id);}
}
function content(n){const c=n.findAllWithCriteria({types:['FRAME']}).find(c=>c.name==='Task content'||c.name.startsWith('Task content /'));if(!c)throw new Error('Task content missing: '+n.name);return c;}
async function wire(n,target){if(!n)throw new Error('Expected action is missing');await n.setReactionsAsync([{trigger:{type:'ON_CLICK'},actions:[{type:'NODE',destinationId:target,navigation:'NAVIGATE',transition:null,resetVideoPosition:false}]}]);mutated.add(n.id);}
for(const id of Object.values(plan.sources))content(sources[id]);
const sectionName='协作退出与有限授权 · 2026-10-28 · '+plan.layout;
let wrapper=page.children.find(n=>n.name===sectionName);
if(wrapper)throw new Error('Existing supplement requires read before retry: '+wrapper.id);
wrapper=track(figma.createSection());wrapper.name=sectionName;
wrapper.fills=[];wrapper.x=Math.max(...page.children.filter(n=>n.id!==wrapper.id).map(n=>n.x+n.width),0)+256;wrapper.y=64;

let maxRight=0,maxBottom=0;
for(let i=0;i<states.length;i++){
 const state=states[i],root=track(sources[plan.sources[state.template]].clone());
 wrapper.appendChild(root);root.x=64+(i%3)*(root.width+64);root.y=64+Math.floor(i/3)*(root.height+96);maxRight=Math.max(maxRight,root.x+root.width);maxBottom=Math.max(maxBottom,root.y+root.height);
 root.name=plan.label+' / '+state.title+' ['+state.key+']';
 root.setExplicitVariableModeForCollection('VariableCollectionId:14:6','56:0');
 replace(root,state.changes);roots[state.key]=root;
}
wrapper.resizeWithoutConstraints(maxRight+64,maxBottom+64);
for(const [key,root]of Object.entries(roots)){
 const body=content(root);
 if(key==='caretakerWorkspace'||key==='caretakerReadOnly'){
  const donor=sources[plan.sources.careActive].findAllWithCriteria({types:['FRAME']}).find(n=>n.children.some(c=>c.type==='TEXT'&&c.characters==='当前状态'));
  if(!donor)throw new Error('Care notice component not found');
  const card=track(donor.clone());body.appendChild(card);card.layoutSizingHorizontal='FILL';
  replace(card,{'当前状态':'学习示例 · 今日照护近况','你可以查看与记录小满的照护近况，授权范围仅限本次照护。':'食欲正常，开始主动玩逗猫棒。本示例仅限获授权照护成员查阅。'});
 }
 if(key==='publisherWorkspace'){
  const field=track(sources['242:374140'].clone());body.appendChild(field);field.layoutSizingHorizontal='FILL';
  replace(field,{'饮食与精神':'领养介绍','食欲正常，开始主动玩逗猫棒。':'介绍小满的性格、生活习惯与需要照护的事项。'});
  const photos=track(sources['145:5164'].clone());body.appendChild(photos);photos.layoutSizingHorizontal='FILL';
  replace(photos,{'更换资料照片':'维护介绍照片','仅使用自己有权使用的照片':'仅限这只宠物，使用有权使用的素材'});
 }
}
await wire(labelNode(roots.caretakerLeaveConfirm,'确认退出照护协作'),roots.caretakerLeft.id);
await wire(labelNode(roots.caretakerLeaveConfirm,'保留当前协作'),plan.sources.careActive);
await wire(labelNode(roots.publisherLeaveConfirm,'确认退出介绍协作'),roots.publisherLeft.id);
await wire(labelNode(roots.publisherLeaveConfirm,'保留当前协作'),plan.sources.introActive);
for(const key of ['caretakerLeft','publisherLeft','caretakerUnavailable'])await wire(labelNode(roots[key],'返回消息'),plan.messages);
await wire(labelNode(roots.caretakerWorkspace,'记录私密照护近况'),plan.followup);
await wire(labelNode(roots.caretakerReadOnly,'重新读取当前权限'),roots.caretakerWorkspace.id);
await wire(labelNode(roots.publisherWorkspace,'保存介绍与照片'),plan.sources.introActive);
const secondary=labelNode(sources[plan.sources.introConfirm],'保留协作');
for(const [root,scope]of [[sources[plan.sources.careActive],'caretaker'],[sources[plan.sources.introActive],'publisher'],[roots.caretakerWorkspace,'caretaker'],[roots.publisherWorkspace,'publisher'],[roots.caretakerReadOnly,'caretaker']]){
 const body=content(root),button=track(secondary.clone());body.appendChild(button);button.layoutSizingHorizontal='FILL';
 const direct=root.id===plan.sources.careActive||root.id===plan.sources.introActive;
 replace(button,{'保留协作':direct?(scope==='caretaker'?'退出本次照护协作':'退出本次介绍协作'):'查看协作与退出'});
 await wire(button,direct?(scope==='caretaker'?roots.caretakerLeaveConfirm.id:roots.publisherLeaveConfirm.id):(scope==='caretaker'?plan.sources.careActive:plan.sources.introActive));mutated.add(body.id);
}
await wire(labelNode(sources[plan.sources.careActive],'查看照护更新'),roots.caretakerWorkspace.id);
replace(sources[plan.sources.introActive],{'可以维护领养介绍、照片与答疑，其他宠物和私密档案未开放。':'仅维护该宠物的领养介绍与照片；其他宠物、私密申请与协议不在授权范围。'});
const introAction=labelNode(sources[plan.sources.introActive],'查看领养介绍');
replace(introAction,{'查看领养介绍':'维护该宠物介绍'});
await wire(introAction,roots.publisherWorkspace.id);
figma.skipInvisibleInstanceChildren=true;
const hiddenMenus=[];for(const root of [...Object.values(roots),sources[plan.sources.careActive],sources[plan.sources.introActive]])for(const instance of root.findAllWithCriteria({types:['INSTANCE']}))if(Object.values(instance.componentProperties).some(p=>p.type==='TEXT'&&['维护这只领养宠物','小满的照护成员','记录照护更新'].includes(p.value)))hiddenMenus.push(instance);
for(const instance of hiddenMenus){instance.visible=false;mutated.add(instance.id);}
const summary=Object.entries(roots).map(([key,n])=>({key,id:n.id,name:n.name,width:n.width,height:n.height,instances:n.findAllWithCriteria({types:['INSTANCE']}).length,images:n.findAll(x=>'fills'in x&&Array.isArray(x.fills)&&x.fills.some(p=>p.type==='IMAGE')).map(x=>({id:x.id,width:x.width,height:x.height}))}));
return {layout:plan.layout,wrapperId:wrapper.id,createdNodeIds:[...created],mutatedNodeIds:[...mutated],roots:summary};
