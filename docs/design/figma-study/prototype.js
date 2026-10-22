// Dated demonstration snapshot. STATE is supplied by the caller.
const name='04 可点击原型（示意）';
const existing=figma.root.children.find(p=>p.name===name);
if(existing)return {status:'already_exists',pageId:existing.id};
const originals={};
const fonts=new Map();
for(const [key,id] of Object.entries(STATE.screens)){
  const n=await figma.getNodeByIdAsync(id);
  if(!n||n.type!=='FRAME')throw new Error('Missing review screen: '+key);
  originals[key]=n;
  for(const t of n.findAllWithCriteria({types:['TEXT']}))for(const s of t.getStyledTextSegments(['fontName']))fonts.set(JSON.stringify(s.fontName),s.fontName);
}
await Promise.all([...fonts.values()].map(f=>figma.loadFontAsync(f)));
const page=figma.createPage();page.name=name;await figma.setCurrentPageAsync(page);
const createdNodeIds=[page.id],mutatedNodeIds=[],screens={},mapping={},reactions=[];
function pair(a,b){mapping[a.id]=b.id;createdNodeIds.push(b.id);if('children'in a)for(let i=0;i<a.children.length;i++)pair(a.children[i],b.children[i]);}
let i=0;
for(const [key,original]of Object.entries(originals)){
  const clone=original.clone();page.appendChild(clone);clone.name='Prototype / '+key;
  clone.x=160+(i%4)*470;clone.y=420+Math.floor(i/4)*960;pair(original,clone);screens[key]=clone.id;i++;
}
const variables=await figma.variables.getLocalVariablesAsync();
const fill=key=>figma.variables.setBoundVariableForPaint({type:'SOLID',color:{r:0,g:0,b:0}},'color',variables.find(v=>v.id===STATE.variables[key]));
const intro=figma.createAutoLayout('VERTICAL');page.appendChild(intro);createdNodeIds.push(intro.id);intro.name='Prototype / 使用说明';intro.resize(1800,200);intro.layoutSizingHorizontal='FIXED';intro.layoutSizingVertical='HUG';intro.itemSpacing=16;intro.paddingLeft=intro.paddingRight=intro.paddingTop=intro.paddingBottom=32;intro.x=160;intro.y=120;intro.fills=[fill('surface')];intro.cornerRadius=20;
for(const [value,role]of [
  ['M&D · 可点击演示 / 2026.10.16','Title'],
  ['演示：底部导航、领养与找宠详情、填写申请、查看预约、发布入口、会话中的安排变更和返回。消息 Tab 直接展示一条示例会话，非完整消息列表。','Body'],
  ['提交、接受／拒绝、支付、上传、输入与物种筛选未模拟。静态按钮不产生真实业务结果。先选下方任一屏再点击 Present，或在流程起点选择演示路径。','Body'],
  ['此页为 02／03 评审稿的独立快照；后续修改评审稿时需明确同步，不自动双向更新。','Caption']
]){const t=figma.createText();intro.appendChild(t);createdNodeIds.push(t.id);t.fontName={family:'Noto Sans SC',style:'Regular'};t.resize(1736,26);t.characters=value;await t.setTextStyleIdAsync(STATE.styles[role]);t.textAutoResize='HEIGHT';t.fills=[fill(role==='Title'?'ink':'muted')];}
async function navigate(sourceId,targetKey,key){const n=await figma.getNodeByIdAsync(mapping[sourceId]);if(!n)throw new Error('Missing cloned action '+key);await n.setReactionsAsync([{trigger:{type:'ON_CLICK'},actions:[{type:'NODE',destinationId:screens[targetKey],navigation:'NAVIGATE',transition:null,resetScrollPosition:true}]}]);mutatedNodeIds.push(n.id);reactions.push({key,sourceId:n.id,destinationId:screens[targetKey]});}
const nav={'社区':'community','领养':'adoption','找宠':'find','消息':'chat','我的':'mine'};
for(const [key,id]of Object.entries(STATE.links)){const parts=key.split('/');if(parts[1]==='nav'&&nav[parts[2]]!==parts[0])await navigate(id,nav[parts[2]],key);}
for(const [key,target]of Object.entries({'community/publish':'publish','adoption/detail':'adoptionDetail','find/detail':'petDetail','adoptionDetail/apply':'apply','petDetail/appointment':'appointment','mine/appointment':'appointment','mine/publish':'publish','chat/change':'change'}))await navigate(STATE.links[key],target,key);
for(const key of ['adoptionDetail','petDetail','apply','appointment','change','chat','publish','complaint']){const n=await figma.getNodeByIdAsync(screens[key]);const top=n.children.find(c=>c.name==='Top bar');await top.setReactionsAsync([{trigger:{type:'ON_CLICK'},actions:[{type:'BACK'}]}]);mutatedNodeIds.push(top.id);reactions.push({key:key+'/back',sourceId:top.id,type:'BACK'});}
page.flowStartingPoints=[{nodeId:screens.community,name:'社区、领养与找宠'},{nodeId:screens.appointment,name:'预约信息与责任'},{nodeId:screens.chat,name:'会话 → 安排变更'},{nodeId:screens.complaint,name:'无码也可报告（静态）'}];mutatedNodeIds.push(page.id);
return {status:'created',pageId:page.id,createdNodeIds,mutatedNodeIds,screens,reactions,sourceToClone:mapping,validation:{screenCount:Object.keys(screens).length,linkCount:reactions.length,allTopLevel:Object.values(screens).every(id=>page.children.some(n=>n.id===id)),fontFamilies:[...new Set([...fonts.values()].map(f=>f.family))],noBusinessSubmission:true}};
