// Figma Plugin API source for the non-commercial M&D study file.
// The caller supplies PHASE and STATE as JSON literals; no app build is needed.
const createdNodeIds = [];
const changedNodeIds = [];
function track(n) { createdNodeIds.push(n.id); return n; }
function rgb(h) { return {r:parseInt(h.slice(1,3),16)/255,g:parseInt(h.slice(3,5),16)/255,b:parseInt(h.slice(5,7),16)/255}; }
// Adapted from figma-generate-library/scripts/createVariableCollection.js.
function collection(name, modes) {
  const c = figma.variables.createVariableCollection(name);
  c.renameMode(c.modes[0].modeId, modes[0]);
  const ids = {[modes[0]]:c.modes[0].modeId};
  for (const m of modes.slice(1)) ids[m] = c.addMode(m);
  return {c, ids};
}
const fontDefs = {
  Display:['Manrope','Bold',64,76], Brand:['Manrope','ExtraBold',32,40],
  Title:['Noto Sans SC','Bold',28,40], Section:['Noto Sans SC','Bold',20,30],
  Body:['Noto Sans SC','Regular',16,26], Label:['Noto Sans SC','Medium',14,22],
  Caption:['Noto Sans SC','Regular',12,20]
};
await Promise.all(Object.values(fontDefs).map(d=>figma.loadFontAsync({family:d[0],style:d[1]})));
if (PHASE === 'foundations') {
  const existing = await figma.variables.getLocalVariableCollectionsAsync();
  if (existing.some(c=>c.name==='M&D Study / Color')) return {status:'already_exists',pages:figma.root.children.map(p=>({id:p.id,name:p.name})),collections:existing.map(c=>({id:c.id,name:c.name,modes:c.modes}))};
  const pages = {}, wrappers = {};
  const first = figma.root.children[0]; first.name = '00 设计总览'; pages.overview = first.id; changedNodeIds.push(first.id);
  for (const [key,name] of Object.entries({foundations:'01 视觉与组件',core:'02 核心体验',trust:'03 信任与异常'})) {
    const p = track(figma.createPage()); p.name=name; pages[key]=p.id;
  }
  const P=collection('M&D Study / Primitives',['Value']);
  const C=collection('M&D Study / Color',['Light','Dark']);
  const G=collection('M&D Study / Geometry',['Value']);
  const palette={paper:'#F6F4EF',white:'#FFFFFF',mist:'#EEECE5',ink:'#202823',secondary:'#626C64',clay:'#A9442B',peach:'#F8E6DD',pine:'#235C43',sage:'#E8F1E9',amber:'#86540E',cream:'#FFF1D6',red:'#9B3435',rose:'#FCEBEC',line:'#D9DED7',strong:'#89958C',night:'#161B18',elevated:'#222A25',mutedNight:'#303B33',whiteNight:'#F3F6F2',secondaryNight:'#BCC5BD',clayNight:'#E5A38D',brown:'#30180F',greenNight:'#A8D2B8',sageNight:'#273D30',amberNight:'#F3CB85',redNight:'#F3A7A7',redBgNight:'#4A292B',lineNight:'#49544C'};
  function syntax(v){v.setVariableCodeSyntax('WEB','var(--mnd-study-'+v.name.replace(/[\s/]+/g,'-').toLowerCase()+')');}
  const prim={}, variables={}, objects={}, geometry={}, styles={};
  for(const [name,hex] of Object.entries(palette)){const v=figma.variables.createVariable(name,P.c,'COLOR');v.scopes=[];v.setValueForMode(P.ids.Value,rgb(hex));syntax(v);prim[name]=v;}
  const map={background:['paper','night'],surface:['white','elevated'],subtle:['mist','mutedNight'],ink:['ink','whiteNight'],muted:['secondary','secondaryNight'],brand:['clay','clayNight'],onBrand:['white','brown'],brandSoft:['peach','brown'],success:['pine','greenNight'],successSoft:['sage','sageNight'],warning:['amber','amberNight'],warningSoft:['cream','mutedNight'],danger:['red','redNight'],dangerSoft:['rose','redBgNight'],border:['line','lineNight'],controlBorder:['strong','secondaryNight']};
  for(const [name,pair] of Object.entries(map)){const v=figma.variables.createVariable('color/'+name,C.c,'COLOR');v.scopes=['border','controlBorder'].includes(name)?['STROKE_COLOR']:['FRAME_FILL','SHAPE_FILL','TEXT_FILL'];v.setValueForMode(C.ids.Light,{type:'VARIABLE_ALIAS',id:prim[pair[0]].id});v.setValueForMode(C.ids.Dark,{type:'VARIABLE_ALIAS',id:prim[pair[1]].id});syntax(v);variables[name]=v.id;objects[name]=v;}
  for(const [name,value,scope] of [...[4,8,12,16,20,24,32,48,64].map(v=>['space/'+v,v,'GAP']),...[8,14,20,28,999].map(v=>['radius/'+v,v,'CORNER_RADIUS']),['size/touch',48,'WIDTH_HEIGHT']]){const v=figma.variables.createVariable(name,G.c,'FLOAT');v.scopes=[scope];v.setValueForMode(G.ids.Value,value);syntax(v);geometry[name]=v.id;}
  for(const [name,d]of Object.entries(fontDefs)){const s=figma.createTextStyle();s.name='M&D Study/'+name;s.fontName={family:d[0],style:d[1]};s.fontSize=d[2];s.lineHeight={unit:'PIXELS',value:d[3]};s.letterSpacing={unit:'PIXELS',value:0};styles[name]=s.id;}
  for(const [key,id]of Object.entries(pages)){const p=figma.root.children.find(p=>p.id===id);const w=track(figma.createAutoLayout('VERTICAL'));w.name='M&D Study / '+key;p.appendChild(w);w.resize(1440,200);w.layoutSizingHorizontal='FIXED';w.layoutSizingVertical='HUG';w.paddingLeft=64;w.paddingRight=64;w.paddingTop=64;w.paddingBottom=64;w.itemSpacing=32;w.fills=[figma.variables.setBoundVariableForPaint({type:'SOLID',color:rgb('#F6F4EF')},'color',objects.background)];w.x=120;w.y=120;w.placeholder=true;wrappers[key]=w.id;}
  return {status:'created',createdNodeIds,mutatedNodeIds:changedNodeIds,pages,wrappers,variables,geometry,styles,collections:{primitives:P.c.id,color:C.c.id,geometry:G.c.id},modes:C.ids,variableCount:Object.keys(palette).length+Object.keys(map).length+Object.keys(geometry).length};
}
const page=await figma.getNodeByIdAsync(STATE.pages[PHASE==='components'?'foundations':PHASE]);
await figma.setCurrentPageAsync(page);
const wrapper=await figma.getNodeByIdAsync(STATE.wrappers[PHASE==='components'?'foundations':PHASE]);
const allVars=await figma.variables.getLocalVariablesAsync();
const V=Object.fromEntries(Object.entries(STATE.variables).map(([k,id])=>[k,allVars.find(v=>v.id===id)]));
const G=Object.fromEntries(Object.entries(STATE.geometry).map(([k,id])=>[k,allVars.find(v=>v.id===id)]));
const styleJobs=[];
function fill(key){return figma.variables.setBoundVariableForPaint({type:'SOLID',color:{r:0,g:0,b:0}},'color',V[key]);}
function radius(n,value){for(const field of ['topLeftRadius','topRightRadius','bottomLeftRadius','bottomRightRadius'])n.setBoundVariable(field,G['radius/'+value]);}
function box(parent,name,w,gap=12,pad=0,bg=null,rad=0,dir='VERTICAL'){
  const n=track(figma.createAutoLayout(dir));n.name=name;parent.appendChild(n);n.resize(w,64);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='HUG';n.fills=bg?[fill(bg)]:[];
  n.itemSpacing=gap; if(G['space/'+gap])n.setBoundVariable('itemSpacing',G['space/'+gap]);
  for(const f of ['paddingTop','paddingRight','paddingBottom','paddingLeft']){n[f]=pad;if(G['space/'+pad])n.setBoundVariable(f,G['space/'+pad]);}
  if(rad)radius(n,rad);return n;
}
function text(parent,value,role='Body',color='ink',width=null){
  const d=fontDefs[role],t=track(figma.createText());t.name=role+' / '+value.slice(0,36);t.fontName={family:d[0],style:d[1]};t.fontSize=d[2];t.lineHeight={unit:'PIXELS',value:d[3]};t.letterSpacing={unit:'PIXELS',value:0};t.textAutoResize='HEIGHT';parent.appendChild(t);t.resize(width||parent.width-parent.paddingLeft-parent.paddingRight,d[3]);t.characters=value;t.fills=[fill(color)];styleJobs.push(t.setTextStyleIdAsync(STATE.styles[role]));return t;
}
function rule(parent,w){const n=track(figma.createRectangle());parent.appendChild(n);n.name='Divider';n.resize(w,1);n.fills=[fill('border')];return n;}
function photo(parent,w,h,kind='cat'){
  const n=track(figma.createFrame());n.name='Photo / '+kind+' / 示例素材';parent.appendChild(n);n.resize(w,h);radius(n,20);n.clipsContent=true;
  n.fills=STATE.images&&STATE.images[kind]?[{type:'IMAGE',imageHash:STATE.images[kind],scaleMode:'FILL'}]:[fill('subtle')];return n;
}
function findProp(comp,name){return Object.keys(comp.componentPropertyDefinitions).find(k=>k.split('#')[0]===name);}
function instance(parent,kind,label,w){const comp=STATE.components&&STATE.components[kind];if(!comp)throw new Error('Missing component '+kind);const main=componentNodes[kind];const n=track(main.createInstance());parent.appendChild(n);n.resize(w||main.width,main.height);if(label)n.setProperties({[findProp(main,'Label')]:label});for(const c of n.findAll(()=>true))createdNodeIds.push(c.id);return n;}
const componentNodes={};if(STATE.components){const entries=Object.entries(STATE.components);const nodes=await Promise.all(entries.map(e=>figma.getNodeByIdAsync(e[1])));entries.forEach((e,i)=>componentNodes[e[0]]=nodes[i]);}
function labelChip(parent,label,kind='chip'){return instance(parent,kind,label,Math.max(60,label.length*14+24));}
function note(parent,title,body,tone='subtle',width=null){const n=box(parent,title,width||parent.width-parent.paddingLeft-parent.paddingRight,8,16,tone,14);text(n,title,'Label',tone==='dangerSoft'?'danger':tone==='successSoft'?'success':'ink');text(n,body,'Caption','muted');return n;}
function kv(parent,label,value){const r=box(parent,label,parent.width-parent.paddingLeft-parent.paddingRight,12,0,null,0,'HORIZONTAL');text(r,label,'Label','muted',88);text(r,value,'Label','ink',r.width-100);return r;}
function field(parent,label,value,error=false){text(parent,label,'Label');const n=instance(parent,error?'inputError':'input',value,parent.width-parent.paddingLeft-parent.paddingRight);return n;}
function species(parent){const r=box(parent,'物种筛选',350,8,0,null,0,'HORIZONTAL');labelChip(r,'猫咪','chipSelected');labelChip(r,'狗狗');labelChip(r,'其他宠物');return r;}
const links={};
function button(parent,label,key,secondary=false){const n=instance(parent,secondary?'secondary':'primary',label,parent.width-parent.paddingLeft-parent.paddingRight);if(key)links[key]=n.id;return n;}
function heading(parent,kicker,title,desc){text(parent,kicker,'Label','brand');text(parent,title,'Title');if(desc)text(parent,desc,'Body','muted');}
function screen(parent,key,title,tab=null){
  const col=box(parent,'Review / '+title,390,16);text(col,key.toUpperCase()+'  /  '+title,'Label','muted');
  const phone=box(col,'Screen / '+key,390,0,0,'background',28);phone.resize(390,844);phone.layoutSizingVertical='FIXED';phone.clipsContent=true;
  const status=box(phone,'Status bar',390,0,20);text(status,'9:41                                      100%','Caption');status.resize(390,44);status.layoutSizingVertical='FIXED';
  const top=box(phone,'Top bar',390,0,20,'background');top.resize(390,72);top.layoutSizingVertical='FIXED';text(top,tab?'M&D   /   '+title:'‹  '+title,'Section');
  const viewport=box(phone,'Scrollable viewport',390,0);viewport.resize(390,tab?650:728);viewport.layoutSizingVertical='FIXED';viewport.clipsContent=true;viewport.overflowDirection='VERTICAL';
  const body=box(viewport,'Content / '+key,390,16,20);body.paddingTop=8;body.paddingBottom=32;
  if(tab){const nav=box(phone,'Bottom navigation',390,0,8,'surface',0,'HORIZONTAL');nav.resize(390,78);nav.layoutSizingVertical='FIXED';nav.counterAxisAlignItems='CENTER';for(const item of ['社区','领养','找宠','消息','我的']){const cell=box(nav,'Nav / '+item,74,0,0,item===tab?'brandSoft':null,14);cell.resize(74,54);cell.layoutSizingVertical='FIXED';cell.primaryAxisAlignItems='CENTER';const t=text(cell,item,'Label',item===tab?'brand':'muted',74);t.textAlignHorizontal='CENTER';links[key+'/nav/'+item]=cell.id;}}
  return {col,phone,body};
}
if(PHASE==='components'){
  const existing=page.findAllWithCriteria({types:['COMPONENT']});if(existing.length)return{status:'already_exists',components:existing.map(n=>({id:n.id,name:n.name}))};
  heading(wrapper,'FOUNDATIONS / 01','更安静，也更可信','独立学习探索 · Light / Dark · 不覆盖 Stitch 生产规范');
  const samples=box(wrapper,'Semantic swatches',1312,16,0,null,0,'HORIZONTAL');
  for(const k of ['background','surface','ink','brand','success','warning','danger']){const c=box(samples,k,168,8);const sw=track(figma.createRectangle());c.appendChild(sw);sw.resize(168,88);sw.fills=[fill(k)];radius(sw,14);text(c,k,'Label');text(c,'var(--mnd-study-color-'+k+')','Caption','muted');}
  text(wrapper,'思源黑体承载中文阅读，Manrope 承载品牌。','Title');text(wrapper,'正文 16 / 26 · 标签 14 / 22 · 辅助 12 / 20 · 关键触控 48','Body','muted');
  text(wrapper,'组件与可恢复状态','Section');
  const gallery=box(wrapper,'Component preview',1312,24,24,'surface',20);
  const mains=box(page,'Construction / main components',1312,20);mains.x=1800;mains.y=120;
  const components={};
  const defs=[['primary','Primary action','brand','onBrand',48,14],['secondary','Secondary action','surface','ink',48,14],['chip','Filter / unselected','surface','muted',48,999],['chipSelected','Filter / selected','ink','surface',48,999],['input','Input / default','surface','ink',52,14],['inputError','Input / error','dangerSoft','danger',52,14],['status','Status / pending','warningSoft','warning',36,8],['statusValid','Status / reviewed','successSoft','success',36,8],['statusDisputed','Status / disputed','dangerSoft','danger',36,8],['disabled','Action / disabled','subtle','muted',48,14]];
  for(const [key,name,bg,fg,h,r]of defs){const c=track(figma.createComponent());c.name='M&D Study / '+name;mains.appendChild(c);c.layoutMode='HORIZONTAL';c.resize(key.startsWith('chip')?124:350,h);c.primaryAxisSizingMode='FIXED';c.counterAxisSizingMode='FIXED';c.primaryAxisAlignItems='CENTER';c.counterAxisAlignItems='CENTER';c.setBoundVariable('paddingLeft',G['space/16']);c.setBoundVariable('paddingRight',G['space/16']);radius(c,r);c.fills=[fill(bg)];if(['secondary','input','chip'].includes(key)){c.strokes=[fill('controlBorder')];c.strokeWeight=1;}const t=text(c,key==='primary'?'确认预约':key==='disabled'?'等待对方确认':key==='inputError'?'请补充具体场所':key.startsWith('input')?'请输入或选择':key.startsWith('chip')?'猫咪':key==='statusValid'?'近期视频已审阅':key==='statusDisputed'?'该项复核中':key==='status'?'等待确认':'查看详细披露','Label',fg,c.width-32);t.textAlignHorizontal='CENTER';const property=c.addComponentProperty('Label','TEXT',t.characters);t.componentPropertyReferences={characters:property};c.description='学习稿组件；状态使用明确文字，不以颜色替代语义。';components[key]=c.id;const i=track(c.createInstance());gallery.appendChild(i);for(const n of i.findAll(()=>true))createdNodeIds.push(n.id);}
  note(wrapper,'无障碍落地约束','正文对比至少 4.5:1；关键触控 48；错误与字段关联；键盘焦点可见；状态以文字+图形+颜色表达；支持字体放大、减少动效与安全区。Figma 静态稿不代表已通过读屏或设备验收。');
  const dark=box(wrapper,'Dark semantic specimen',1312,16,24,'surface',20);dark.setExplicitVariableModeForCollection(STATE.collections.color,STATE.modes.Dark);text(dark,'夜间阅读，仍然看清关键约定','Title');text(dark,'同一组语义 Token 切换模式，不靠降低透明度模拟暗色。','Body','muted');const darkAction=track((await figma.getNodeByIdAsync(components.primary)).createInstance());dark.appendChild(darkAction);for(const n of darkAction.findAll(()=>true))createdNodeIds.push(n.id);
  await Promise.all(styleJobs);wrapper.placeholder=false;changedNodeIds.push(wrapper.id);
  return{status:'created',createdNodeIds,mutatedNodeIds:changedNodeIds,components,preview:wrapper.id,componentCount:defs.length,bounds:{width:wrapper.width,height:wrapper.height}};
}
if(PHASE==='overview'){
  if(wrapper.children.length)return{status:'already_exists',id:wrapper.id};
  heading(wrapper,'M&D / MEOW & DOGGIE','让每一次相遇，信息都说清楚。','产品与交互设计 V1 · 2026.10.15 · 仅供非商业学习');
  const hero=box(wrapper,'Editorial cover',1312,32,32,'surface',28,'HORIZONTAL');const copy=box(hero,'Brand narrative',640,24);text(copy,'M&D','Display','brand');text(copy,'猫咪是主角，\n每一种陪伴都被看见。','Title');text(copy,'社区与领养是主线，找宠解决来源不明、身份借用、临时转场与隐藏费用。允许商家与公开代理；可信来自信息和记录，不来自身份刻板印象。','Body','muted');photo(hero,576,400,'cat');
  const pets=box(wrapper,'Species inclusion',1312,24,0,null,0,'HORIZONTAL');for(const [name,kind,caption]of [['猫咪','cat','默认内容与品牌情感中心'],['狗狗','dog','同层分类、领养与交流'],['其他宠物','rabbit','按物种适配档案与照护信息']]){const c=box(pets,name,421,12);photo(c,421,160,kind);text(c,name,'Section');text(c,caption,'Label','muted');}
  const principles=box(wrapper,'Design principles',1312,24,0,null,0,'HORIZONTAL');for(const [title,body]of [['真实先于装饰','用宠物影像、真实档案和来源说明建立情感。示例素材不能冒充已核实信息。'],['友好但不幼稚','暖纸色、墨绿、陶土主动作；中文阅读优先，控制装饰与促销噪声。'],['控制感来自明确出口','申请可撤回，变更可拒绝，无码也能报告。每个关键状态都给出下一步。']]){const c=box(principles,title,421,12,24,'surface',20);text(c,title,'Section');text(c,body,'Body','muted');}
  text(wrapper,'01  /  信息架构','Section');const nav=box(wrapper,'Navigation map',1312,16,0,null,0,'HORIZONTAL');for(const [title,body]of [['社区','日常 / 互助 / 安家故事'],['领养','同城 / 条件 / 申请 / 回访'],['找宠','出售与有偿转让 / 透明约见'],['消息','宠物 / 申请 / 预约上下文'],['我的','申请 / 预约 / 发布工作台']]){const n=box(nav,title,249,12,20,'surface',20);text(n,title,'Section');text(n,body,'Label','muted');}
  note(wrapper,'猫咪优先，其他宠物不被排除','默认筛选猫咪；狗狗与其他宠物入口同层可达。发布、照护问题与材料按物种适配。找宠是相对旧版“找猫”的本轮新提案。');
  text(wrapper,'02  /  三条关键路径','Section');
  for(const [title,steps]of [['领养','发现 → 详情与条件 → 最少必要申请 → 匹配与约见 → 可选试养 → 双方交接 → 30 天私密回访'],['透明找宠','物种与区域 → 具体宠物 → 主体/关系/费用 → 双方确认同版本预约 → 到访分项反馈 → 独立交接记录'],['关键变更','提出新版本 → 旧安排暂停 → 查看差异 → 接受并重确认 / 拒绝取消 → 未回复到期；拒绝不计失约']])note(wrapper,title,steps,'surface');
  text(wrapper,'03  /  依据、边界与趋势取舍','Section');
  note(wrapper,'本稿的依据','PRD 2.0 / UX 2.0 / 真实性与披露规范为业务依据。本轮用户明确授权新视觉与多宠分类；其他已确认业务原则不变。已有客户端与本稿不可混同。');
  note(wrapper,'趋势取舍，而非套皮','借鉴 Material 3 Expressive 的信息重点与可扫读层级；遵循 Apple 对控制层与内容层的区分。涉及主体、价格、地址与关键确认时保持实体背景，不用玻璃效果降低可读性。');
  text(wrapper,'资料：blog.google / Material 3 Expressive；developer.apple.com / HIG Materials','Caption','muted');
  note(wrapper,'验收与未做','本轮为可编辑设计文档与 12 个关键屏，不是完整上线 PRD 的所有页面；不实现支付、担保、后端或生产 UI。读屏、键盘、设备和实际用户走查仍需实施阶段验收。');
  await Promise.all(styleJobs);wrapper.placeholder=false;changedNodeIds.push(wrapper.id);return{status:'created',createdNodeIds,mutatedNodeIds:changedNodeIds,preview:wrapper.id,bounds:{width:wrapper.width,height:wrapper.height}};
}
if(['core','trust'].includes(PHASE)){
  if(wrapper.children.length)return{status:'already_exists',id:wrapper.id};
  heading(wrapper,PHASE==='core'?'CORE EXPERIENCE / 02':'TRUST & RECOVERY / 03',PHASE==='core'?'先感受到陪伴，再看清每个选择。':'信任不是徽章，是可确认的约定。','390 × 844 · 学习示意档案 · 页面内纵向滚动 · 文案与图层均可编辑');
  const screens={},bodies={};
  const defs=PHASE==='core'?[['community','社区','社区'],['adoption','领养','领养'],['find','找宠','找宠'],['adoptionDetail','领养详情'],['petDetail','来源与约见'],['mine','我的','我的']]:[['apply','申请领养'],['appointment','确认预约'],['change','安排变更'],['chat','预约会话'],['publish','发布信息'],['complaint','报告问题']];
  for(let i=0;i<defs.length;i+=3){const row=box(wrapper,'Screen row '+i,1312,40,0,null,0,'HORIZONTAL');for(const [key,title,tab]of defs.slice(i,i+3)){const s=screen(row,key,title,tab);screens[key]=s.phone.id;bodies[key]=s.body;}}
  if(PHASE==='core'){
    let b=bodies.community;heading(b,'MEOW & DOGGIE','今天，也有小事值得分享。','和同城的它们，慢慢熟悉。');species(b);const post=box(b,'Post / sunny afternoon',350,12,0,'surface',20);photo(post,350,210);const p=box(post,'Post content',350,8,16);text(p,'小满的铲屎官 · 上海','Label');text(p,'把下午的阳光，分给你一点。','Section');text(p,'小满终于愿意在我旁边睡午觉了。','Label','muted');text(p,'喜欢 28     评论 6     收藏     分享','Caption','muted');button(b,'分享今天的小事','community/publish');
    b=bodies.adoption;heading(b,'ADOPTION / 上海','给陪伴，一个家的位置。','领养，不只是带回家。');species(b);text(b,'同城优先    条件可匹配    更多筛选','Label','muted');const ar=box(b,'Adoption animal cards',350,12,0,null,0,'HORIZONTAL');for(const [name,kind,meta]of [['小满','cat','猫咪 · 8 个月'],['阿福','dog','狗狗 · 2 岁']]){const c=box(ar,name,169,8,0,'surface',20);photo(c,169,170,kind);const info=box(c,'Animal summary',169,8,12);text(info,name,'Section');text(info,meta,'Label','muted');text(info,'救助领养 · 上海','Caption','success');}note(b,'先了解，再申请','健康未知项和照护条件都会展示。合理医疗支出另行说明，不捆绑消费。');button(b,'了解小满的领养条件','adoption/detail');
    b=bodies.find;heading(b,'FIND A COMPANION','相遇之前，先把信息说清。','出售与有偿转让 · 默认猫咪');species(b);const fc=box(b,'Specific cat offer',350,12,0,'surface',20);photo(fc,350,200,'catOffer');const fi=box(fc,'Offer summary',350,8,16);text(fi,'奶糖  ·  橘色猫','Section');text(fi,'商家发布 / 员工接待 / 上海静安','Label','muted');text(fi,'¥ 2,280  必付合计','Section','brand');text(fi,'宠物 ¥2,200 + 必付服务 ¥80；无代理费','Caption','muted');button(b,'查看关系与费用披露','find/detail');note(b,'也欢迎其他陪伴','可切换狗狗与其他宠物。所有发布都需明确性质和适用的物种信息。');
    b=bodies.adoptionDetail;photo(b,350,210);heading(b,'救助领养 / CAT A-008','小满，正在等一个家。','8 个月 · 已绝育自述 · 上海');kv(b,'当前照护','南岸救助小组');kv(b,'健康资料','医疗材料已上传，来源未确认');kv(b,'相处需求','室内照护，愿意慢慢建立信任');note(b,'领养条件先看清','封窗；家庭成员同意；支持私密回访。未知健康信息如实标注，不承诺未来无病。','successSoft');button(b,'填写领养申请','adoptionDetail/apply');button(b,'先向照护方提问',null,true);
    b=bodies.petDetail;photo(b,350,164,'catOffer');heading(b,'出售 / CAT S-026','奶糖','¥ 2,280  必付合计');const rel=box(b,'Disclosure / relationships',350,8,16,'surface',20);text(rel,'谁在发布，谁来接待','Section');kv(rel,'发布主体','眠山猫舍 · 员工小林');kv(rel,'当前照护','眠山猫舍');kv(rel,'接待地点','静安门店；非饲养场所');kv(rel,'预计收款','眠山猫舍（发布者声明）');note(b,'费用已拆分','宠物 ¥2,200 + 必付服务 ¥80 = ¥2,280\n可选用品另购；代理费 ¥0。','brandSoft');instance(b,'statusValid','近期视频已审阅 · 10月12日',350);text(b,'仅支持近期接触材料，不代表所有权或健康保证。','Caption','muted');button(b,'查看并确认预约约定','petDetail/appointment');
    b=bodies.mine;heading(b,'MY COMPANIONS','陪伴，有迹可循。','小满的铲屎官 · 示例账号');const actions=box(b,'My task overview',350,16,20,'surface',20);text(actions,'我的待办','Section');text(actions,'领养申请 1     预约安排 1','Body');text(actions,'30 天回访 · 待填写（私密）','Label','muted');button(actions,'查看我的预约','mine/appointment');for(const [t,d]of [['我的宠物','猫咪 / 狗狗 / 其他宠物档案'],['发布工作台','草稿 / 待补充 / 已发布'],['安全与隐私','资料权限 / 屏蔽 / 报告记录']])note(b,t,d,'surface');button(b,'发布一条信息','mine/publish');
  }else{
    let b=bodies.apply;heading(b,'申请 / 小满 A-008','合适，比快一点更重要。','仅收集匹配必要的信息；不对外公开。');field(b,'照护地区','上海 · 徐汇');field(b,'共同生活成员','2 位成年人，均已同意');field(b,'物种与经验','猫咪 · 有 2 年照护经验');field(b,'居住安全与照护安排','已封窗；每天在家照护');note(b,'你可以补充，也可以撤回','仅照护方与授权人员可查看。买宠预约不需要填写这份家庭摘要。');button(b,'提交申请','apply/chat');button(b,'保存草稿',null,true);
    b=bodies.appointment;heading(b,'预约版本 V1 / S-026','确认的是这一次具体约见。','你与接待方都确认 V1 后才算约好。');kv(b,'具体宠物','奶糖 / S-026');kv(b,'接待方','小林 · 眠山猫舍员工');kv(b,'时间','10 月 18 日 14:00');kv(b,'地点','静安门店 · 非饲养场所');kv(b,'必付合计','¥2,280（宠物 2,200 + 服务 80）');kv(b,'预计收款','眠山猫舍（声明，未核验付款）');kv(b,'健康与责任','查看已知情况、交接及售后约定');note(b,'私人信息按需可见','具体地址仅在必要确认流程内授权；不进入公开分享链接。平台不在此收取猫款或承诺担保。');button(b,'确认 V1 预约约定','appointment/chat');button(b,'需要修改',null,true);
    b=bodies.change;heading(b,'V1 → V2 / 等待你的决定','约见安排发生了变化。','原安排已暂停，请不要按旧地址赴约。');const delta=box(b,'Version differences',350,16,16,'surface',20);text(delta,'地点','Section');text(delta,'原：静安门店','Label','muted');text(delta,'新：合作门店 · 普陀','Label','brand');rule(delta,318);text(delta,'接待方','Section');text(delta,'原：员工小林 → 新：合作方小周','Label');text(delta,'必付合计仍为 ¥2,280；其他约定未变。','Caption','muted');note(b,'你有选择权','拒绝变更不计为失约。恢复原安排也需要双方重确认。10月17日18:00 前未确认则过期。','warningSoft');button(b,'接受 V2，重新确认','change/accept');button(b,'拒绝并取消','change/reject',true);text(b,'对方要求直接转场？可保存记录并报告。','Caption','danger');
    b=bodies.chat;note(b,'奶糖 / S-026 · 预约 V1','地点、费用、接待方以双方确认版本为准。','brandSoft');instance(b,'status','对方提出 V2 · 待确认',350);const msg=box(b,'Seller message',302,8,16,'surface',20);text(msg,'小林 · 已披露员工关系','Caption','muted');text(msg,'接待安排需要调整，我已提交新版本，请你先查看差异。','Body');note(b,'系统提醒','聊天文字不能替代预约确认；旧安排已暂停。','warningSoft');button(b,'查看安排变更','chat/change');field(b,'发送消息','输入问题或补充说明…');button(b,'发送',null,true);text(b,'发送失败时保留草稿，重试不重复发送。','Caption','muted');
    b=bodies.publish;heading(b,'PUBLISH / 步骤 1–2','先把信息性质说清楚。','可保存草稿；预览与消费者展示一致。');species(b);field(b,'信息性质','出售／有偿转让');field(b,'宠物与稳定编号','奶糖 / 新建具体宠物档案');field(b,'你的关系','商家员工，代表眠山猫舍');note(b,'后续需要披露','当前照护方、来源、接待方与场所、所有必付项、预计收款方及责任。代理可发布，但授权和收费必须清楚。');field(b,'约见场所','请选择具体场所',true);text(b,'请补充场所及其与照护方的关系。','Caption','danger');button(b,'保存并继续',null);text(b,'未知项如实标注；按物种显示字段，不强求救助宠物的亲代资料。','Caption','muted');
    b=bodies.complaint;heading(b,'SUPPORT / 报告问题','没有到访码，也可以报告。','请描述事实；原始证据仅限授权人员查看。');field(b,'问题类型','未确认就要求转往其他场所');field(b,'关联信息（可选）','奶糖 S-026 / 预约 V1');field(b,'发生了什么','对方要求前往未披露门店…');field(b,'补充证据（可选）','添加脱敏截图或说明');note(b,'受理，不等于已作出判断','提交后提供编号和补充入口；处理结果可为获支持、未获支持或无法判断。未获支持不等于证明没有欺骗。');button(b,'提交并获取受理编号',null);text(b,'对处理有异议，可补充材料并申请复核。','Caption','muted');
  }
  for(const [key,,]of defs){const col=(await figma.getNodeByIdAsync(screens[key])).parent;text(col,PHASE==='core'?'内容性质持续可见；示例档案，不代表平台核验。':'状态与下一步明确；敏感内容按权限展示。','Caption','muted',390);}
  await Promise.all(styleJobs);wrapper.placeholder=false;changedNodeIds.push(wrapper.id);
  const stats={};for(const [k,id]of Object.entries(screens)){const n=await figma.getNodeByIdAsync(id);const descendants=n.findAll(()=>true);stats[k]={id,width:n.width,height:n.height,descendants:descendants.length,texts:descendants.filter(x=>x.type==='TEXT').length,instances:descendants.filter(x=>x.type==='INSTANCE').length};}
  return{status:'created',createdNodeIds,mutatedNodeIds:changedNodeIds,screens,links,stats,preview:wrapper.id,bounds:{width:wrapper.width,height:wrapper.height}};
}
return {status:'unknown_phase',phase:PHASE};
