const p=await figma.getNodeByIdAsync('471:524');await figma.setCurrentPageAsync(p);
await Promise.all([{family:'Noto Sans SC',style:'Regular'},{family:'Noto Sans SC',style:'Medium'},{family:'Noto Sans SC',style:'Bold'},{family:'Manrope',style:'ExtraBold'}].map(f=>figma.loadFontAsync(f)));
const section=await figma.getNodeByIdAsync('460:17317'),C=[],M=[],sources={};for(const[k,id]of Object.entries(TEXT))sources[k]=await figma.getNodeByIdAsync(id);
const vars=await figma.variables.getLocalVariablesAsync(),byId=Object.fromEntries(vars.map(v=>[v.id,v]));
function cv(v){const c=v.valuesByMode['56:0']??Object.values(v.valuesByMode)[0];return c.type==='VARIABLE_ALIAS'?cv(byId[c.id]):c;}
function paint(k){const v=byId[V[k]],c=cv(v);return figma.variables.setBoundVariableForPaint({type:'SOLID',color:{r:c.r,g:c.g,b:c.b}},'color',v);}
function box(parent,name,w,pad=0,gap=16,bg='surface',dir='VERTICAL'){const n=figma.createAutoLayout(dir);parent.appendChild(n);C.push(n.id);n.name=name;n.resize(w,48);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='HUG';n.paddingTop=n.paddingBottom=n.paddingLeft=n.paddingRight=pad;n.itemSpacing=gap;n.fills=bg?[paint(bg)]:[];return n;}
function tx(parent,s,w,role='Body',color='ink'){const t=sources[role].clone();parent.appendChild(t);C.push(t.id);t.characters=s;t.resize(w,24);t.textAutoResize='HEIGHT';t.fills=[paint(color)];return t;}
const quiet=await figma.getNodeByIdAsync('56:181');function button(parent,label,key,platform='phone',w=636){const t=TARGETS[platform+'/'+key];if(!t)throw Error('Missing target '+key);const b=quiet.createInstance();parent.appendChild(b);C.push(b.id);b.resize(w,48);const prop=Object.keys(b.componentProperties).find(k=>k.startsWith('Label#'));b.setProperties({[prop]:label});b.reactions=[{trigger:{type:'ON_CLICK'},actions:[{type:'URL',url:'https://www.figma.com/proto/3OFOC3vu18Kx9sRPjummOC/MND?page-id='+t.page.replace(':','-')+'&node-id='+t.id.replace(':','-')+'&starting-point-node-id='+t.id.replace(':','-'),openInNewTab:false}]}];return b;}
let hub=section.children.find(n=>n.name==='2026-10-21 产品与安全修订 · 评审入口');if(hub)throw Error('Review hub already exists '+hub.id);
hub=box(section,'2026-10-21 产品与安全修订 · 评审入口',1440,32,20,'background');hub.x=80;hub.y=80;hub.setExplicitVariableModeForCollection('VariableCollectionId:14:6','56:0');
tx(hub,'M&D',1376,'MobileBrand');tx(hub,'产品与安全修订',1376,'Title');
tx(hub,'2026.10.21 · 默认浅色 · 手机 / Web / Desktop / 平板双向',1376,'Label','muted');
tx(hub,'已确认：14岁以上可交流；未满14岁需监护授权；商业发布与线下签约由成年人办理。',1376);
const cards=[['账号与未成年人','同一账号三个登录标识；验证后回到原任务再次提交。','登录与账号','login','监护与年龄','ageRange'],['领养与安全接回','同一宠物独占试养，其他申请进入候补；接回需双方确认实物交接。','候选与试养','candidates','安全接回','returnRequest'],['隐私、投诉与平台规则','分用途告知，保留期限与删除例外；游客可报告并持私密回执查询。','隐私与数据','privacy','公开规则与历史','rules'],['消息与跨端','完整8状态五端对照；未读不等于待处理，离线不伪造0。','手机消息中心','unreadChat','平板横屏确认范围','unreadConfirmAll']];
for(let i=0;i<4;i+=2){const row=box(hub,'Review topics '+i,1376,0,16,null,'HORIZONTAL');for(const d of cards.slice(i,i+2)){const card=box(row,d[0],680,20,12,'surface');card.cornerRadius=16;tx(card,d[0],640,'Section');tx(card,d[1],640,'Body','muted');button(card,d[2],d[3],'phone',640);button(card,d[4],d[5],d[0]==='消息与跨端'?'tabletL':'phone',640);}}
const gate=box(hub,'实施与上线条件',1376,20,8,'subtle');gate.cornerRadius=16;tx(gate,'设计交付与真实上线分别验收',1336,'Strong');tx(gate,'正式运营主体、公开联系渠道、收费及资质需填实；B04、年龄与监护核验、游客工单、数据期限和人工处置仍需服务端与运营接通。此页展示设计合同，不能证明真实验证或投诉已成功。',1336,'Body','muted');
button(hub,'查看预约安全暂停','safetyHold','phone',1376);
const account=CORE.filter(s=>s.group==='account'&&!s.existingKey);let row=0;for(const s of account){for(const [j,platform]of ['phone','web','desktop','tabletP','tabletL'].entries()){const n=await figma.getNodeByIdAsync(ROOTS[platform+'/'+s.id]);if(n?.parent?.id===section.id){n.x=[80,534,2038,3542,4440][j];n.y=1300+row*1350;M.push(n.id);}}row++;}
section.resize(5900,Math.max(3000,1300+row*1350));M.push(section.id);
return{createdNodeIds:C,mutatedNodeIds:M,hubId:hub.id,hubSize:{w:hub.width,h:hub.height},accountRows:row};
