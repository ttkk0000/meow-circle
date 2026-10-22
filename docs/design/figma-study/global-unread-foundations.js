// Component construction follows the bundled createComponentWithVariants helper:
// Cartesian variants -> combineAsVariants -> explicit positioning -> token bindings.
if(badgeSet||rowSet) return {skipped:true,pageId:page.id,reason:'Components already exist; inspect before updating.'};
const variants=[];
for(const tone of ['Accent','Muted','Dot']){
  const c=figma.createComponent();page.appendChild(c);created.push(c.id);c.name='Tone='+tone;c.layoutMode='HORIZONTAL';
  c.resize(tone==='Dot'?8:32,tone==='Dot'?8:20);c.primaryAxisAlignItems='CENTER';c.counterAxisAlignItems='CENTER';
  c.primaryAxisSizingMode='AUTO';c.counterAxisSizingMode='FIXED';c.fills=[paint(tone==='Muted'?'subtle':'primary')];round(c,999);
  if(tone!=='Dot'){c.paddingLeft=6;c.paddingRight=6;txt(c,'8','Caption',null,tone==='Muted'?'muted':'onPrimary');}variants.push(c);
}
const badges=figma.combineAsVariants(variants,page);created.push(badges.id);badges.name='M&D / Unread / Badge';badges.description='0 hidden; 1–99 literal; 100+ shown as 99+. Count is unread events, never pending tasks. Muted is excluded from the proposed global numeric total.';
const countKey=badges.addComponentProperty('Count','TEXT','8');
variants.forEach((c,i)=>{c.x=24+i*100;c.y=24;for(const t of c.findAllWithCriteria({types:['TEXT']}))t.componentPropertyReferences={characters:countKey};});badges.resize(340,80);badges.x=1800;badges.y=200;
const rows=[];
for(const kind of ['Chat','Notice']){
  const c=figma.createComponent();page.appendChild(c);created.push(c.id);c.name='Kind='+kind;c.layoutMode='HORIZONTAL';c.resize(358,88);c.primaryAxisSizingMode='FIXED';c.counterAxisSizingMode='FIXED';c.counterAxisAlignItems='CENTER';pad(c,0,12);c.paddingTop=8;c.paddingBottom=8;c.fills=[];
  if(kind==='Chat'){const av=avatarSource.createInstance();c.appendChild(av);remember(av);av.name='Avatar';}
  else {const slot=round(frame(c,'Notice icon slot','HORIZONTAL',48,48,'soft'),16);slot.primaryAxisAlignItems='CENTER';slot.counterAxisAlignItems='CENTER';const ic=icon(slot,bellSource);ic.name='Notice icon';}
  const copy=frame(c,'Message copy','VERTICAL',234,66);copy.layoutSizingHorizontal='FILL';
  const title=txt(copy,'布丁的铲屎官','Strong',234);title.name='Row title';
  const preview=txt(copy,'今天的小猫也很开心。','Label',234);preview.name='Row preview';
  const meta=txt(copy,'日常交流','Caption',234,'muted');meta.name='Row meta';
  const trailing=pad(frame(c,'Time and unread','VERTICAL',52,60),0,8);trailing.counterAxisAlignItems='MAX';const time=txt(trailing,'刚刚','Caption',52,'muted');time.name='Row time';time.textAlignHorizontal='RIGHT';
  const counter=variants[0].createInstance();trailing.appendChild(counter);remember(counter);counter.name='Unread counter';
  rows.push(c);
}
const rowFamily=figma.combineAsVariants(rows,page);created.push(rowFamily.id);rowFamily.name='M&D / Unread / Message row';rowFamily.description='Editable conversation and notice rows. Unread counts are distinct from pending workflow status. Photo and icon assets reuse the current M&D library.';
const keys={};for(const [name,value] of Object.entries({Title:'布丁的铲屎官',Preview:'今天的小猫也很开心。',Meta:'日常交流',Time:'刚刚'}))keys[name]=rowFamily.addComponentProperty(name,'TEXT',value);
for(const c of rows){for(const [name,nodeName] of Object.entries({Title:'Row title',Preview:'Row preview',Meta:'Row meta',Time:'Row time'})){const t=c.findOne(n=>n.type==='TEXT'&&n.name===nodeName);t.componentPropertyReferences={characters:keys[name]};}}
rows.forEach((c,i)=>{c.x=24;c.y=24+i*120;});rowFamily.resize(410,270);rowFamily.x=1800;rowFamily.y=340;
const wrapper=await figma.getNodeByIdAsync('352:3');
txt(wrapper,'全局未读，让每一条回应都有去处','Title',1256);
txt(wrapper,'M&D · 默认浅色 · 2026-10-20 · 设计示意，非真实消息','Caption',1256,'muted');
note(wrapper,'统一规则','0 不显示；1–99 显示数量；100 及以上显示 99+。进入消息中心不自动清空全部未读。');
return {createdNodeIds:created,pageId:page.id,wrapperId:wrapper.id,components:{badge:badges.id,row:rowFamily.id},countProperty:countKey,componentProperties:keys,variantCounts:{badge:variants.length,row:rows.length},fontFamilies:['Noto Sans SC','Manrope']};
