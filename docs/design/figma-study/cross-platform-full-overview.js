// Native editable review index. Caller BASE, MOBILE, PLAN, STATE, CHECK_SUMMARY.
const page=await figma.getNodeByIdAsync('295:2');await figma.setCurrentPageAsync(page);
await Promise.all([{family:'Noto Sans SC',style:'Regular'},{family:'Noto Sans SC',style:'Medium'},{family:'Noto Sans SC',style:'Bold'},{family:'Manrope',style:'ExtraBold'}].map(f=>figma.loadFontAsync(f)));
const createdNodeIds=[],removedNodeIds=[],mutatedNodeIds=[],jobs=[];
const vars=await figma.variables.getLocalVariablesAsync(),byId=Object.fromEntries(vars.map(v=>[v.id,v]));
function cv(v){const x=v.valuesByMode['56:0']??Object.values(v.valuesByMode)[0];return x?.type==='VARIABLE_ALIAS'?cv(byId[x.id]):x;}
function paint(k){const v=byId[BASE.variables[k]],c=cv(v);return figma.variables.setBoundVariableForPaint({type:'SOLID',color:{r:c.r,g:c.g,b:c.b}},'color',v);}
function track(n){createdNodeIds.push(n.id);return n;}
function box(p,name,w,pad=0,gap=16,bg=null,dir='VERTICAL'){const n=track(figma.createAutoLayout(dir));p.appendChild(n);n.name=name;n.resize(w,80);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='HUG';n.paddingLeft=n.paddingRight=n.paddingTop=n.paddingBottom=pad;n.itemSpacing=gap;n.fills=bg?[paint(bg)]:[];return n;}
function tx(p,s,w,role='Body',color='ink'){const n=track(figma.createText());p.appendChild(n);n.fontName={family:'Noto Sans SC',style:['Title','Section','Strong'].includes(role)?'Bold':'Regular'};n.characters=s;n.name=role+' / '+s.slice(0,30);n.resize(w,24);n.textAutoResize='HEIGHT';n.fills=[paint(color)];jobs.push(n.setTextStyleIdAsync(MOBILE.styles[role]||BASE.styles[role]));return n;}
function link(n,id){n.reactions=[{trigger:{type:'ON_CLICK'},actions:[{type:'URL',url:'https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id='+id.replace(':','-'),openInNewTab:true}]}];}
const buttonMain=await figma.getNodeByIdAsync(MOBILE.components.secondary);
function button(p,label,id,w){const n=track(buttonMain.createInstance());p.appendChild(n);n.resize(w,48);const key=Object.keys(n.componentProperties).find(k=>k.split('#')[0]==='Label');if(key)n.setProperties({[key]:label});n.name='打开完整稿 / '+label;link(n,id);return n;}
const root=await figma.getNodeByIdAsync('301:27');
for(const c of [...root.children]){removedNodeIds.push(c.id);c.remove();}
root.name='M&D / 全平台完整流程索引 · 默认浅色 · 2026-10-20';root.layoutMode='VERTICAL';root.resize(1496,2000);root.layoutSizingVertical='HUG';root.paddingLeft=root.paddingRight=root.paddingTop=root.paddingBottom=40;root.itemSpacing=24;root.fills=[paint('background')];root.setExplicitVariableModeForCollection(BASE.collections.color,'56:0');mutatedNodeIds.push(root.id);
tx(root,'M&D · 每一段旅程，都有合适的空间',1416,'Title');
tx(root,'Web · Desktop · 平板竖屏 · 平板横屏',1416,'Section');
tx(root,'569 个产品与交互状态 × 4 种布局 = 2,276 张完整对应稿；另有 32 张窄窗／分屏样例。10 个手机评审入口归入本索引。',1416,'Body');
tx(root,'默认浅色 · Noto Sans SC / Manrope · 16 圆角 · 48 高动作；旧多主题只留参考。',1416,'Label','muted');
const platforms=box(root,'Platform entries',1416,0,24,null,'HORIZONTAL');
for(const p of PLAN.platforms){const c=box(platforms,'Platform / '+p.key,336,20,12,'surface');c.cornerRadius=16;tx(c,p.label,296,'Section');tx(c,p.w+' × '+p.h+' · 569 状态',296,'Label','muted');button(c,'打开'+p.label,STATE.roots[p.key+'/home'],296);}
tx(root,'四种布局 · 原生可编辑对照',1416,'Section');
const snapshots=box(root,'Editable native snapshots',1416,0,24,null,'HORIZONTAL');
for(const[key,label]of [['web/home','Web · 社区'],['desktop/notificationSettings','Desktop · 设置'],['tabletP/petProfile','平板竖屏 · 宠物'],['tabletL/lostList','平板横屏 · 互助']]){const col=box(snapshots,label,336,0,12);tx(col,label,336,'Strong');const source=await figma.getNodeByIdAsync(STATE.roots[key]);const n=source.clone();col.appendChild(n);n.rescale(336/source.width);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='FIXED';n.name='Editable snapshot / '+label;createdNodeIds.push(n.id);for(const a of n.findAllWithCriteria({types:['FRAME','INSTANCE','TEXT']})){if(!a.id.startsWith('I'))createdNodeIds.push(a.id);if('reactions'in a&&a.reactions.length)a.reactions=[];}link(n,source.id);}
tx(root,'以上为评审快照。点击模块入口进入完整原稿；源稿继续编辑后须手动刷新快照。',1416,'Caption','muted');
tx(root,'按模块检查完整流程',1416,'Section');
for(const[module,d]of Object.entries(PLAN.modules)){const entries={length:PLAN.entryCounts[module]};const r=box(root,'Module / '+module,1416,16,16,'surface','HORIZONTAL');r.cornerRadius=16;r.counterAxisAlignItems='CENTER';tx(r,d.label+' · '+entries.length+' 状态',300,'Strong');const key=d.menu[0];for(const p of PLAN.platforms)button(r,p.label,STATE.roots[p.key+'/'+key],180);const ex=PLAN.narrowExamples?.find(x=>x.module===module);if(ex)button(r,'窄窗 / 分屏',STATE.roots['tabletN/'+ex.key],180);}
tx(root,'布局与交互约束',1416,'Section');
for(const [a,b]of [['平板两向','所有 569 状态都有竖屏与横屏原稿。根据可用窗口收起模块侧栏；旋转保留宠物、会话、受众、草稿和版本。'],['完整状态','错误、失权、撤回、空态、材料补充、双方确认、上传恢复和表情输入均保留独立对应稿。'],['导航与权限','同模块内跳转，跨模块精确节点链接；保留主动同意与权限条件。Figma 演示不执行真实授权或发送。'],['验证边界',CHECK_SUMMARY]]){const row=box(root,a,1416,16,24,'surface','HORIZONTAL');tx(row,a,144,'Strong');tx(row,b,1216,'Body','muted');}
tx(root,'客户端代码尚未实施。Android / iOS 共享 KMP 逻辑、各自 Compose UI 与独立平板适配；设备、键盘、读屏和真实响应式运行另行验收。',1416,'Label','muted');
await Promise.all(jobs);
page.name='12 默认浅色 · 全平台完整索引';mutatedNodeIds.push(page.id);
for(const[id,name]of [['295:3','13 历史代表稿 · Web'],['295:4','14 历史代表稿 · Desktop'],['295:5','15 历史代表稿 · 平板']]){const p=await figma.getNodeByIdAsync(id);p.name=name;mutatedNodeIds.push(p.id);}
return {rootId:root.id,createdNodeIds,removedNodeIds,mutatedNodeIds,evidence:{w:root.width,h:root.height,moduleCount:16,platforms:4,states:569,fullFrames:2276,narrowFrames:32,editableSnapshots:4}};
