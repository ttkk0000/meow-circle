// Native Figma review board; caller supplies CONFIG, BASE, MOBILE and STATE.
await figma.setCurrentPageAsync(await figma.getNodeByIdAsync(CONFIG.pages.overview));
await Promise.all([{family:'Noto Sans SC',style:'Regular'},{family:'Noto Sans SC',style:'Medium'},{family:'Noto Sans SC',style:'Bold'},{family:'Manrope',style:'ExtraBold'}].map(f=>figma.loadFontAsync(f)));
const createdNodeIds=[],jobs=[],page=figma.currentPage;
const vs=await figma.variables.getLocalVariablesAsync(),byId=Object.fromEntries(vs.map(v=>[v.id,v]));
function cv(v){const a=v.valuesByMode['56:0']??Object.values(v.valuesByMode)[0];return a.type==='VARIABLE_ALIAS'?cv(byId[a.id]):a;}
function paint(k){const v=byId[BASE.variables[k]],c=cv(v);return figma.variables.setBoundVariableForPaint({type:'SOLID',color:{r:c.r,g:c.g,b:c.b}},'color',v);}
const track=n=>(createdNodeIds.push(n.id),n);
function box(p,name,w,pad=0,gap=16,bg=null,dir='VERTICAL'){const n=track(figma.createAutoLayout(dir));p.appendChild(n);n.name=name;n.resize(w,80);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='HUG';n.paddingLeft=n.paddingRight=n.paddingTop=n.paddingBottom=pad;n.itemSpacing=gap;n.fills=bg?[paint(bg)]:[];n.setExplicitVariableModeForCollection(BASE.collections.color,'56:0');return n;}
function tx(p,s,role='Body',color='ink',w=null){const n=track(figma.createText());p.appendChild(n);n.name=role+' / '+s.slice(0,32);n.fontName={family:'Noto Sans SC',style:['Title','Section','Strong'].includes(role)?'Bold':role==='Label'?'Medium':'Regular'};n.characters=s;n.fontSize={Title:28,Section:20,Strong:16,Body:16,Label:14,Caption:12}[role];n.lineHeight={unit:'PERCENT',value:150};n.resize(w??p.width-p.paddingLeft-p.paddingRight,24);n.textAutoResize='HEIGHT';n.fills=[paint(color)];jobs.push(n.setTextStyleIdAsync(MOBILE.styles[role]||BASE.styles[role]));return n;}
function url(n,id){n.reactions=[{trigger:{type:'ON_CLICK'},actions:[{type:'URL',url:'https://www.figma.com/design/'+CONFIG.fileKey+'/MND?node-id='+id.replace(':','-'),openInNewTab:true}]}];}
async function button(p,label,id,w){const c=await figma.getNodeByIdAsync(MOBILE.components.primary);const n=track(c.createInstance());p.appendChild(n);n.resize(w,48);const prop=Object.keys(n.componentProperties).find(k=>k.split('#')[0]==='Label');if(prop)n.setProperties({[prop]:label});n.name='打开原稿 / '+label;url(n,id);return n;}
let existing=page.children.find(n=>n.name==='M&D / 浅色跨端评审 · 2026-10-20');if(existing)throw Error('Review board already exists');
const root=box(page,'M&D / 浅色跨端评审 · 2026-10-20',1496,40,32,'background');root.x=80;root.y=80;
tx(root,'M&D · 和手机同一种亲切','Title');tx(root,'Web、Desktop、平板，沿用手机 V2 浅色样式。','Section');tx(root,'浅中性背景 · 珊瑚主动作 · Noto Sans SC / Manrope · 16 圆角卡片 · 48 高动作','Body','muted');
const trio=box(root,'Platform entries',1416,0,24,null,'HORIZONTAL');
const entries=[['Web','18 张页面与状态','浏览、分享、游客登录续接；内容区最大 1248。','web/home'],['Desktop','17 张页面与状态','常驻侧导航、三栏会话、图文草稿与预约版本。','desktop/messages'],['平板','21 张页面与状态','834 竖屏 / 1194 横屏；744 宽退回单页阅读。','tablet/adoptL']];
for(const[name,count,body,key]of entries){const c=box(trio,'Entry / '+name,456,24,16,'surface');c.cornerRadius=16;tx(c,name,'Section');tx(c,count,'Strong','brand');tx(c,body,'Body','muted');await button(c,'打开 '+name+' 浅色稿',STATE.roots[key],408);}
tx(root,'同一套样式，不同的空间安排','Section');
const views=box(root,'Editable representative snapshots',1416,0,24,null,'HORIZONTAL');
for(const[name,key]of [['Web · 社区','web/home'],['Desktop · 私信','desktop/messages'],['平板 · 横屏领养','tablet/adoptL']]){const col=box(views,'Snapshot / '+name,456,0,12);tx(col,name,'Strong');const source=await figma.getNodeByIdAsync(STATE.roots[key]);const n=source.clone();col.appendChild(n);n.name='Editable snapshot / '+name;n.rescale(456/source.width);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='FIXED';createdNodeIds.push(n.id,...n.findAll(()=>true).map(a=>a.id));for(const a of [n,...n.findAll(()=>true)])if('reactions'in a)a.reactions=[];}
tx(root,'以上为原生可编辑对照快照，不自动同步；请从各端入口查看原稿与导航。','Caption','muted');
tx(root,'关键适配规则','Section');
const rows=[['导航','同一任务语义：社区 / 领养 / 发布 / 消息 / 我的。走失互助保留为上下文入口。'],['平板','竖屏双列浏览；横屏有足够宽度再展开主从。窄分屏退回单页阅读，不压缩全部栏目。'],['沟通','桌面同时显示会话、聊天和宠物上下文；平板竖屏收起第三栏。输入栏与消息历史分区。'],['发布','首版图文。多图失败保留成功项；私密核实材料不进入公开日记。表情面板与输入区域分开。'],['预约','V2 当前生效、V1 历史、恢复旧时间须提出 V3。各端不改变双方重新确认规则。'],['身份与隐私','游客可读，操作再登录；返回原任务。关系、资料、费用逐项披露，不用统一保证章。']];
for(const[label,body]of rows){const r=box(root,'Mapping / '+label,1416,16,24,'surface','HORIZONTAL');tx(r,label,'Strong','ink',120);tx(r,body,'Body','muted',1240);}
tx(root,'组件状态延续手机','Section');const states=box(root,'Navigation state examples',1416,0,24,null,'HORIZONTAL');for(const[id,label]of [['296:2','默认'],['296:8','选中'],['296:14','悬停'],['296:20','焦点'],['296:26','禁用']]){const col=box(states,label,240,0,8);tx(col,label,'Label','muted');const c=await figma.getNodeByIdAsync(id),n=track(c.createInstance());col.appendChild(n);}
tx(root,'设计范围与验证边界','Section');tx(root,'56 张为页面与固定状态样例，覆盖核心浏览、领养、互助、沟通、发布、预约、账号与恢复。完整手机状态仍是后续细化依据；本稿不代表每端复制了 579 个手机参考根。','Body');tx(root,'本轮仅 Figma 设计和静态原型：不接入真实登录、短信、上传、定位或提交。真实输入、软键盘、读屏、设备、自适应运行和跨设备同步未验证。暗色不在本轮范围。','Label','muted');
await Promise.all(jobs);
const all=root.findAll(()=>true),types={};for(const n of all)types[n.type]=(types[n.type]||0)+1;
return {rootId:root.id,pageId:page.id,createdNodeIds,evidence:{w:root.width,h:root.height,nodes:all.length,types,editableSnapshotCount:3,flattenedUiImages:0}};
