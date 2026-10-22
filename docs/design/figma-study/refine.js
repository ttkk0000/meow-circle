// Targeted refinements after the first composition review; PHASE / STATE are supplied by the caller.
const key=PHASE, page=await figma.getNodeByIdAsync(STATE.pages[key]);
await figma.setCurrentPageAsync(page);
await Promise.all([{family:'Noto Sans SC',style:'Regular'},{family:'Noto Sans SC',style:'Medium'},{family:'Noto Sans SC',style:'Bold'},{family:'Manrope',style:'Bold'},{family:'Manrope',style:'ExtraBold'}].map(f=>figma.loadFontAsync(f)));
const wrapper=await figma.getNodeByIdAsync(STATE.wrappers[key]);
const variables=await figma.variables.getLocalVariablesAsync();
const v=k=>variables.find(v=>v.id===STATE.variables[k]);
const paint=k=>figma.variables.setBoundVariableForPaint({type:'SOLID',color:{r:0,g:0,b:0}},'color',v(k));
const createdNodeIds=[],mutatedNodeIds=[],removedNodeIds=[],links=[];
function track(n){createdNodeIds.push(n.id);return n;}
function box(parent,name,w,pad=0,dir='VERTICAL',bg=null){const n=track(figma.createAutoLayout(dir));parent.appendChild(n);n.name=name;n.resize(w,64);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='HUG';n.itemSpacing=12;n.paddingLeft=pad;n.paddingRight=pad;n.paddingTop=pad;n.paddingBottom=pad;n.fills=bg?[paint(bg)]:[];if(bg)n.cornerRadius=14;return n;}
async function text(parent,value,role='Label',color='ink',width){const t=track(figma.createText());parent.appendChild(t);t.name='Spec / '+value.slice(0,30);t.fontName={family:'Noto Sans SC',style:role==='Body'?'Regular':role==='Section'?'Bold':'Medium'};t.resize(width||parent.width-parent.paddingLeft-parent.paddingRight,26);t.characters=value;t.fills=[paint(color)];await t.setTextStyleIdAsync(STATE.styles[role]);t.textAutoResize='HEIGHT';return t;}
if(key==='foundations'){
  const gallery=wrapper.findOne(n=>n.name==='Component preview');gallery.layoutMode='HORIZONTAL';gallery.layoutWrap='WRAP';gallery.counterAxisSpacing=24;gallery.primaryAxisSizingMode='FIXED';gallery.counterAxisSizingMode='AUTO';gallery.layoutSizingHorizontal='FIXED';gallery.resize(1312,gallery.height);gallery.primaryAxisAlignItems='MIN';gallery.counterAxisAlignItems='CENTER';mutatedNodeIds.push(gallery.id);
}
if(key==='core'){
  for(const [name,id] of Object.entries(STATE.screens).filter(e=>['community','adoption','find','adoptionDetail','petDetail','mine'].includes(e[0]))){const n=await figma.getNodeByIdAsync(id);n.strokes=[paint('border')];n.strokeWeight=1;mutatedNodeIds.push(n.id);}
  const detail=await figma.getNodeByIdAsync(STATE.screens.petDetail);
  const rel=detail.findOne(n=>n.name==='Disclosure / relationships');
  if(!rel.children.some(n=>n.name==='Spec / 来源：发布者称自行繁育，尚未独立核实。')){
    await text(rel,'来源：发布者称自行繁育，尚未独立核实。','Caption','muted');
    await text(rel,'健康：病史未确认；医疗材料仅上传。','Caption','muted');
    await text(rel,'责任：发布主体负责交接前照护与现场核对；交接、售后范围需双方明确约定。','Caption','muted');
  }
  const primary=await figma.getNodeByIdAsync(STATE.components.primary);
  for(const [screenKey,oldKey,label]of [['adoptionDetail','adoptionDetail/apply','填写领养申请'],['petDetail','petDetail/appointment','查看并确认预约约定']]){
    const n=await figma.getNodeByIdAsync(STATE.screens[screenKey]);
    const existingBar=n.children.find(c=>c.name==='Sticky action');if(existingBar){STATE.links[oldKey]=existingBar.children.find(c=>c.type==='INSTANCE').id;continue;}
    const viewport=n.children.find(c=>c.name==='Scrollable viewport');viewport.resize(390,648);mutatedNodeIds.push(viewport.id);
    const bar=box(n,'Sticky action',390,16,'VERTICAL','surface');bar.resize(390,80);bar.layoutSizingVertical='FIXED';
    const b=track(primary.createInstance());bar.appendChild(b);b.resize(358,48);const labelKey=Object.keys(primary.componentPropertyDefinitions).find(k=>k.split('#')[0]==='Label');b.setProperties({[labelKey]:label});for(const x of b.findAll(()=>true))createdNodeIds.push(x.id);
    const old=await figma.getNodeByIdAsync(STATE.links[oldKey]);if(old&&old.type==='INSTANCE'){removedNodeIds.push(old.id);old.remove();}
    STATE.links[oldKey]=b.id;
  }
}
if(key==='trust'){
  for(const name of ['apply','appointment','change','chat','publish','complaint']){const n=await figma.getNodeByIdAsync(STATE.screens[name]);n.strokes=[paint('border')];n.strokeWeight=1;mutatedNodeIds.push(n.id);}
  const appointment=await figma.getNodeByIdAsync(STATE.screens.appointment);
  const health=appointment.findAllWithCriteria({types:['TEXT']}).find(t=>t.characters==='查看已知情况、交接及售后约定');
  if(health){health.characters='病史未确认；医疗材料仅上传。发布主体负责交接前照护、现场核对；交接及售后范围需双方确认。';mutatedNodeIds.push(health.id);}
  if(!wrapper.children.some(n=>n.name==='Aftercare / specifications')){
    const specs=box(wrapper,'Aftercare / specifications',1312);await text(specs,'后续状态与异常：设计交接必读','Section');
    const items=[
      ['预约状态与关键变更','只有双方确认同一版本才成立。地点、接待主体、宠物、必付费用、预计收款、健康纠正或责任变化都需新版本。change_pending 时旧安排暂停；拒绝可取消且不计失约；超时 expired，不默认沿用旧地址。'],
      ['到访反馈与交接','分别询问宠物、接待方、地点、费用：一致／不一致／不确定。未到访、未回应与未反馈分开；卖家不能代填。到访码只关联预约；到访完成不等于付款或交接已确认。'],
      ['领养交接与 30 天回访','申请可补充、撤回或结束；试养可选，交接由双方确认。30 天回访默认私密，公开安家故事另行预览授权。退养建立关联新案例，不覆盖原交接记录。'],
      ['核验边界与恢复','未提供、待审、待补充、有效、过期、未通过、撤销、争议分开。每项展示对象、方法、日期和时效。随机码视频只支持近期接触；医疗材料已上传不等于来源已确认，复核后才恢复有效。'],
      ['投诉、复核与申诉','无预约、无到访码也可报告。受理后给编号与补充入口；获支持、未获支持、无法判断分别说明。未获支持不等于证明无欺骗。申诉保存原结论、证据与改判理由，公开信息需脱敏。'],
      ['响应式与无障碍实施','320 宽度单列，差异与费用不靠横向表格隐藏；文字放大后动作不遮挡内容。键盘、读屏标签、焦点返回和错误汇总需在代码实现。当前原型只连接部分页面，按钮与输入不构成真实业务提交。']
    ];
    for(let i=0;i<items.length;i+=2){const row=box(specs,'Specification row '+i,1312,0,'HORIZONTAL');row.itemSpacing=24;for(const [title,body]of items.slice(i,i+2)){const c=box(row,title,644,20,'VERTICAL','surface');await text(c,title,'Section');await text(c,body,'Body','muted');}}
  }
}
if(key==='overview'){
  for(let i=0;i<STATE.assetNodeIds.length;i++){const n=await figma.getNodeByIdAsync(STATE.assetNodeIds[i]);if(n&&n.type==='FRAME'&&n.fills.some(f=>f.type==='IMAGE'&&Object.values(STATE.images).includes(f.imageHash))){n.name='Source photo / '+Object.keys(STATE.images)[i];n.x=1800;n.y=120+i*300;n.resize(400,240);mutatedNodeIds.push(n.id);}}
}
const nodes=wrapper.findAll(()=>true),types={},fontFamilies=new Set(),imageNodes=[];
for(const n of nodes){types[n.type]=(types[n.type]||0)+1;if(n.type==='TEXT')for(const s of n.getStyledTextSegments(['fontName']))fontFamilies.add(s.fontName.family);if('fills' in n&&Array.isArray(n.fills)&&n.fills.some(f=>f.type==='IMAGE'))imageNodes.push({id:n.id,name:n.name,type:n.type,width:n.width,height:n.height});}
return{createdNodeIds,mutatedNodeIds,removedNodeIds,prototypeLinks:links,updatedLinks:STATE.links,validation:{descendants:nodes.length,types,imageNodes,fontFamilies:[...fontFamilies],fontPass:[...fontFamilies].every(f=>['Noto Sans SC','Manrope'].includes(f)),uiRasterPresent:imageNodes.some(n=>!n.name.startsWith('Photo /')),bounds:{width:wrapper.width,height:wrapper.height}}};
