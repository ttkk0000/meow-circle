// Native Figma design adaptation only. Caller: BASE, MOBILE, PLAN, STATE, BATCH.
// Source content stays editable; no screen bitmap, production code or network activity.
// Resolve actual source PAGE after user reorganization; historical pageByKey is archival.
function currentPageOf(n){while(n&&n.type!=='PAGE')n=n.parent;return n;}
const batchPages=[];for(const s of BATCH){const n=await figma.getNodeByIdAsync(s.mobileId);const p=currentPageOf(n);if(!p)throw Error('Missing current phone source '+s.mobileKey);batchPages.push(p);}
if(!batchPages.length||batchPages.some(p=>p.id!==batchPages[0].id))throw Error('Split batch by current PAGE ancestor');
const page=batchPages[0];
await figma.setCurrentPageAsync(page);
const fonts=[{family:'Noto Sans SC',style:'Regular'},{family:'Noto Sans SC',style:'Medium'},{family:'Noto Sans SC',style:'Bold'},{family:'Manrope',style:'ExtraBold'}];
await Promise.all(fonts.map(f=>figma.loadFontAsync(f)));
const vars=await figma.variables.getLocalVariablesAsync(),byId=Object.fromEntries(vars.map(v=>[v.id,v]));
const V=Object.fromEntries(Object.entries(BASE.variables).map(([k,id])=>[k,byId[id]]));
const created=[],mutated=[],removed=[],evidence=[],links=[];
const track=n=>(created.push(n.id),n);
function cv(v){const x=v.valuesByMode['56:0']??Object.values(v.valuesByMode)[0];return x?.type==='VARIABLE_ALIAS'?cv(byId[x.id]):x;}
function paint(k){const v=V[k],c=cv(v);return figma.variables.setBoundVariableForPaint({type:'SOLID',color:{r:c.r,g:c.g,b:c.b}},'color',v);}
function dim(n,w,h){n.resize(w,h);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='FIXED';return n;}
function box(p,name,w,pad=0,gap=12,bg=null,dir='VERTICAL'){const n=track(figma.createAutoLayout(dir));p.appendChild(n);n.name=name;n.resize(w,64);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='HUG';n.paddingLeft=n.paddingRight=n.paddingTop=n.paddingBottom=pad;n.itemSpacing=gap;n.fills=bg?[paint(bg)]:[];return n;}
function tx(p,s,w,role='Body',color='ink'){const n=track(figma.createText());p.appendChild(n);n.name=role+' / '+s.slice(0,40);n.fontName=role==='Brand'?fonts[3]:role==='Section'?fonts[2]:fonts[0];n.fontSize=role==='Brand'?24:role==='Section'?20:role==='Caption'?12:16;n.lineHeight={unit:'PERCENT',value:150};n.letterSpacing={unit:'PIXELS',value:0};n.characters=s;n.resize(w,24);n.textAutoResize='HEIGHT';n.fills=[paint(color)];return n;}
function collect(n){created.push(n.id);if('children'in n&&n.type!=='INSTANCE')for(const c of n.children)collect(c);}
function clearSafe(n){if(!('findAllWithCriteria'in n))return;for(const c of n.findAllWithCriteria({types:['FRAME']})){if(/Home indicator|Bottom safe area|safe area indicator/i.test(c.name)){c.visible=false;mutated.push(c.id);}}}
function detached(n){if(n.type==='INSTANCE'&&n.width>112&&!/Expression panel|M&D Keyboard \/.*panel|Instance \/ (primary|secondary|quiet|disabled)$/.test(n.name)){removed.push(n.id);return n.detachInstance();}return n;}
// Resize containers and wrap text. Discrete icons/emoji/photos retain their aspect.
function reflow(raw,w){let n=detached(raw);const oldW=n.width;
 if(n.type==='TEXT'){n.resize(Math.max(16,w),n.height);n.textAutoResize='HEIGHT';return n;}
 if(!('children'in n)){if(n.type==='RECTANGLE'&&oldW>112)n.resize(w,n.height);return n;}
 if(n.type==='INSTANCE'){if(oldW>112&&!/Expression panel|M&D Keyboard \/.*panel/.test(n.name))n.resize(w,n.height);return n;}
 if(!n.layoutMode||n.layoutMode==='NONE'){
   if(oldW>112){if(n.children.length&&/map|地图/i.test(n.name)){const scale=w/oldW;n.rescale(scale);}else if(Array.isArray(n.fills)&&n.fills.some(p=>p.type==='IMAGE'))n.resize(w,Math.min(360,n.height*w/oldW));else n.resize(w,n.height);}return n;
 }
 n.resize(Math.max(16,w),n.height);n.layoutSizingHorizontal='FIXED';
 const kids=n.children.filter(c=>c.visible&&c.layoutPositioning!=='ABSOLUTE');
 const aw=Math.max(16,w-n.paddingLeft-n.paddingRight);
 if(n.layoutMode==='VERTICAL'){
   for(const c of kids){const grow=c.type==='TEXT'||c.width>oldW*.55||c.width>aw;reflow(c,grow?aw:Math.min(c.width,aw));}
   if(!/Viewport|Switch|Thumb|Digit|emoji.*cell|Insert emoji|Category.*tab/i.test(n.name)&&n.height>48)n.layoutSizingVertical='HUG';
 }else{
   const gap=Math.max(0,kids.length-1)*n.itemSpacing;
   const flexible=kids.filter(c=>c.type==='TEXT'||c.width>112||/Feed left|Feed right|Row copy|Draft input/.test(c.name));
   const fixed=kids.filter(c=>!flexible.includes(c)).reduce((s,c)=>s+c.width,0);
   const budget=Math.max(32*flexible.length,aw-gap-fixed);
   const total=flexible.reduce((s,c)=>s+c.width,0)||1;
   for(const c of kids)reflow(c,flexible.includes(c)?Math.max(32,budget*c.width/total):c.width);
   if(n.height>64&&!/Composer|Toolbar|Switch/.test(n.name))n.layoutSizingVertical='HUG';
 }
 return n;
}
const mains={};for(const k of ['navDefaultCross','navActiveCross'])mains[k]=await figma.getNodeByIdAsync(STATE.components[k]);
for(const k of ['house','heart-handshake','message-circle','user-round','paw-print','chevron-left','search','bell'])mains[k]=await figma.getNodeByIdAsync(MOBILE.components['icon/'+k]);
function icon(p,key){const n=track(mains[key].createInstance());p.appendChild(n);dim(n,24,24);return n;}
function navItem(p,label,key,platform,w,active=false,ic=null){const n=track(mains[active?'navActiveCross':'navDefaultCross'].createInstance());p.appendChild(n);const props={};for(const k of Object.keys(n.componentProperties)){if(k.split('#')[0]==='Label')props[k]=w<80?'':label.length>8?label.slice(0,7)+'…':label;if(k.split('#')[0]==='Icon'&&ic)props[k]=mains[ic].id;}if(Object.keys(props).length)n.setProperties(props);dim(n,w,48);n.name='Navigate / '+label+' → '+key;links.push({id:n.id,key,platform});return n;}
for(const s of BATCH){
 const source=await figma.getNodeByIdAsync(s.mobileId);if(!source)throw Error('Missing source '+s.mobileKey);
 const task=source.findOne(x=>x.name==='Task content');
 const panelOnly=!task&&s.module==='expressions';if(!task&&!panelOnly)throw Error('No semantic task '+s.mobileKey);
 const sf=panelOnly?[]:source.children.filter(c=>!/Status|Toolbar|Scrollable viewport|Persistent navigation|Home indicator/i.test(c.name));
 const sourceToolbar=source.children.find(n=>n.name==='Toolbar'),sourceTitle=sourceToolbar?.children?.find(n=>n.type==='TEXT')?.characters;
 const displayTitle=sourceTitle||s.title;
 for(const platform of PLAN.platforms){
  const id=STATE.roots[platform.key+'/'+s.mobileKey],root=await figma.getNodeByIdAsync(id);if(!root)throw Error('Root missing '+id);
  if(root.children.length){if(typeof REBUILD_IDS==='undefined'||!REBUILD_IDS.includes(id))throw Error('Root already rendered '+id);for(const child of [...root.children]){removed.push(child.id);child.remove();}}
  const W=platform.w,H=platform.h,tablet=platform.key.startsWith('tablet'),portrait=platform.key==='tabletP'||W<800,desktop=platform.key==='desktop',compactWeb=W<600&&!tablet;
  root.layoutMode='VERTICAL';dim(root,W,H);root.itemSpacing=0;root.cornerRadius=0;root.clipsContent=true;root.fills=[paint('background')];root.setExplicitVariableModeForCollection(BASE.collections.color,'56:0');root.name=platform.label+' / '+displayTitle+' ['+s.mobileKey+']';mutated.push(root.id);
  if(desktop){const title=box(root,'Desktop window chrome',W,0,0,'subtle','HORIZONTAL');dim(title,W,32);title.paddingLeft=20;title.counterAxisAlignItems='CENTER';tx(title,'M&D · meow & doggie',W-40,'Caption','muted');}
  const header=box(root,'Platform header',W,16,24,'surface','HORIZONTAL');dim(header,W,72);header.counterAxisAlignItems='CENTER';tx(header,'M&D',tablet?94:160,'Brand');
  if(!tablet&&!desktop&&!compactWeb){for(const[k,l,i]of [['home','社区','house'],['adopt','领养','heart-handshake'],['lostList','互助','paw-print'],['messages','消息','message-circle']])navItem(header,l,k,platform.key,104,false,i);}
  else if(compactWeb)tx(header,PLAN.modules[s.module].label,W-224,'Caption','muted');
  else tx(header,PLAN.modules[s.module].label,Math.max(150,W-430),'Section');
  if(!compactWeb)tx(header,'meow & doggie',tablet?170:200,'Caption','muted');
  const remain=H-72-(desktop?32:0)-(compactWeb?72:0),workspace=box(root,'Responsive workspace',W,0,0,null,'HORIZONTAL');dim(workspace,W,remain);
  const rw=desktop?184:tablet?80:0;
  if(rw){const rail=box(workspace,'Primary navigation',rw,tablet?12:16,12,'surface');dim(rail,rw,remain);for(const[k,l,i]of [['home','社区','house'],['adopt','领养','heart-handshake'],['lostList','互助','paw-print'],['messages','消息','message-circle'],['mine','我的','user-round']])navItem(rail,l,k,platform.key,rw-(tablet?24:32),false,i);}
  const hasAside=!portrait,asideW=hasAside?(desktop?232:tablet?224:256):0;
  if(hasAside){const aside=box(workspace,'Module navigation and context',asideW,24,12,'subtle');dim(aside,asideW,remain);tx(aside,PLAN.modules[s.module].label,asideW-48,'Section');
   for(const key of PLAN.modules[s.module].menu){const item=PLAN.titles[key];if(item)navItem(aside,item,key,platform.key,asideW-48,key===s.mobileKey);}
   tx(aside,'学习示意 · 非真实数据',asideW-48,'Caption','muted');
  }
  const mw=W-rw-asideW,main=box(workspace,'Current task',mw,0,0,null);dim(main,mw,remain);
  const bc=box(main,'Task header',mw,12,12,'surface','HORIZONTAL');bc.counterAxisAlignItems='CENTER';const back=box(bc,'Back / preserve current task',48,12,0,null);dim(back,48,48);icon(back,'chevron-left');links.push({id:back.id,back:true,platform:platform.key});tx(bc,displayTitle,mw-84,'Section');
  const footerSources=sf.filter(n=>!/^Task content$/.test(n.name));
  const footer=box(main,'Persistent task actions / input',mw,0,0,'surface');
  const cw=Math.min(mw,820);
  // Clone independent footer semantics, preserving guarded interactions and composer states.
  for(const f of footerSources){const c=f.clone();footer.appendChild(c);clearSafe(c);reflow(c,cw);c.x=0;c.y=0;}
  footer.counterAxisAlignItems='CENTER';footer.layoutSizingVertical='HUG';if(!footer.children.length){dim(footer,mw,0);}
  const fh=Math.max(0,footer.height),vh=Math.max(160,remain-bc.height-fh);
  const viewport=box(main,'Scrollable task viewport',mw,0,0,null);dim(viewport,mw,vh);viewport.clipsContent=true;viewport.overflowDirection='VERTICAL';main.insertChild(1,viewport);viewport.counterAxisAlignItems='CENTER';
  let content;
  if(task){content=task.clone();viewport.appendChild(content);reflow(content,cw);}
  else{content=box(viewport,'Expression popover and composer',cw,24,16);content.counterAxisAlignItems='CENTER';
   const context=/Circle/.test(s.mobileKey)?'宠友圈 · 日常与回复':/Diary/.test(s.mobileKey)?'宠物日记 · 记录与评论':/Adoption/.test(s.mobileKey)?'领养近况 · 私密交流':/Group/.test(s.mobileKey)?'武汉猫咪小分队 · 群聊':/Lead/.test(s.mobileKey)?'私密线索会话':'布丁的铲屎官 · 私信与回复';
   tx(content,context,cw-48,'Section');tx(content,'选择后保留在当前草稿，确认发送前可更换或取消。',cw-48,'Caption','muted');
   for(const child of source.children.filter(n=>!/Dismiss/.test(n.name))){const c=child.clone();content.appendChild(c);reflow(c,Math.min(cw-48,390));c.layoutPositioning='AUTO';}}
  content.name='Task content / '+s.mobileKey+' / '+platform.key;content.layoutSizingVertical='HUG';clearSafe(content);
  // Keep the action area visible even when the original sheet contained a keyboard panel.
  if(footer.height>remain-160-bc.height){footer.resize(mw,Math.floor(remain-160-bc.height));footer.layoutSizingVertical='FIXED';footer.clipsContent=true;footer.overflowDirection='VERTICAL';viewport.resize(mw,Math.max(100,remain-bc.height-footer.height));}
  else viewport.resize(mw,Math.max(100,remain-bc.height-footer.height));
  if(compactWeb){const bottom=box(root,'Web compact navigation',W,12,0,'surface','HORIZONTAL');dim(bottom,W,72);for(const[k,l,i]of [['home','社区','house'],['adopt','领养','heart-handshake'],['lostList','互助','paw-print'],['messages','消息','message-circle'],['mine','我的','user-round']])navItem(bottom,l,k,platform.key,(W-24)/5,false,i);}
  collect(root);
  const all=root.findAllWithCriteria({types:['FRAME','INSTANCE','TEXT']});evidence.push({key:s.mobileKey,platform:platform.key,id,source:s.mobileId,w:W,h:H,contentWidth:content.width,layoutNodes:all.length,instances:all.filter(n=>n.type==='INSTANCE').length,texts:all.filter(n=>n.type==='TEXT').length});
 }
}
// IDs are losslessly grouped by prefix to fit large native batches in the tool response.
function pack(ids){const out={};for(const id of new Set(ids)){const k=id.lastIndexOf(':'),p=id.slice(0,k);(out[p]??=[]).push(+id.slice(k+1));}return Object.fromEntries(Object.entries(out).map(([p,ns])=>{ns.sort((a,b)=>a-b);return[p,{start:ns[0],deltaBase36:ns.slice(1).map((n,i)=>(n-ns[i]).toString(36)).join('.')}];}));}
return {pageId:page.id,createdNodeIdsCodec:'prefix-start-base36-delta-v1',createdNodeIdsByPrefix:pack(created),mutatedNodeIds:[...new Set(mutated)],removedNodeIds:[...new Set(removed)],newNavigationCount:links.length,evidence};
